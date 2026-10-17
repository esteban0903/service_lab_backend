import { describe, expect, it, vi } from 'vitest';
import { executeWithRetry, isRetryableError } from './retry';
import { ApiError } from '../api/client';

describe('retry utility', () => {
  it('identifies retryable vs non-retryable errors correctly', () => {
    expect(isRetryableError(new TypeError('Failed to fetch'))).toBe(true);
    expect(isRetryableError(new Error('NetworkError when attempting to fetch resource'))).toBe(true);
    expect(isRetryableError(new ApiError(502, 'Bad Gateway', 'Gateway error'))).toBe(true);
    expect(isRetryableError(new ApiError(503, 'Service Unavailable', 'Unavailable'))).toBe(true);
    expect(isRetryableError(new ApiError(504, 'Gateway Timeout', 'Timeout'))).toBe(true);

    expect(isRetryableError(new ApiError(400, 'Bad Request', 'Validation failed'))).toBe(false);
    expect(isRetryableError(new ApiError(404, 'Not Found', 'Not found'))).toBe(false);
    expect(isRetryableError(new ApiError(409, 'Conflict', 'Already assigned'))).toBe(false);
    expect(isRetryableError(new ApiError(422, 'Unprocessable', 'Missing skills'))).toBe(false);
  });

  it('resolves fast requests without triggering waking up', async () => {
    const onWakingUpChange = vi.fn();
    const action = vi.fn().mockResolvedValue({ data: 'success' });

    const result = await executeWithRetry(action, {
      slowThresholdMs: 100,
      onWakingUpChange,
    });

    expect(result).toEqual({ data: 'success' });
    expect(action).toHaveBeenCalledTimes(1);
    expect(onWakingUpChange).not.toHaveBeenCalledWith(true);
  });

  it('triggers waking up notification if request exceeds slow threshold', async () => {
    const onWakingUpChange = vi.fn();
    const action = vi.fn().mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve('done'), 150))
    );

    const result = await executeWithRetry(action, {
      slowThresholdMs: 50,
      onWakingUpChange,
    });

    expect(result).toBe('done');
    expect(onWakingUpChange).toHaveBeenCalledWith(true);
    expect(onWakingUpChange).toHaveBeenLastCalledWith(false);
  });

  it('retries on network error with backoff and succeeds', async () => {
    const onWakingUpChange = vi.fn();
    let attempts = 0;
    const action = vi.fn().mockImplementation(async () => {
      attempts++;
      if (attempts < 3) {
        throw new TypeError('Failed to fetch');
      }
      return 'recovered';
    });

    let simulatedTime = 0;
    const fakeSleep = vi.fn().mockImplementation(async (ms: number) => {
      simulatedTime += ms;
    });

    const result = await executeWithRetry(action, {
      slowThresholdMs: 50,
      initialDelayMs: 100,
      backoffFactor: 2,
      sleepFn: fakeSleep,
      nowFn: () => simulatedTime,
      onWakingUpChange,
    });

    expect(result).toBe('recovered');
    expect(attempts).toBe(3);
    expect(fakeSleep).toHaveBeenCalledTimes(2);
    expect(fakeSleep).toHaveBeenNthCalledWith(1, 100);
    expect(fakeSleep).toHaveBeenNthCalledWith(2, 200);
    expect(onWakingUpChange).toHaveBeenCalledWith(true);
    expect(onWakingUpChange).toHaveBeenLastCalledWith(false);
  });

  it('does not retry non-retryable errors (e.g. 400 Bad Request)', async () => {
    const onWakingUpChange = vi.fn();
    const action = vi.fn().mockRejectedValue(new ApiError(400, 'Bad Request', 'Invalid input'));

    await expect(
      executeWithRetry(action, {
        onWakingUpChange,
      })
    ).rejects.toThrow(ApiError);

    expect(action).toHaveBeenCalledTimes(1);
    expect(onWakingUpChange).not.toHaveBeenCalledWith(true);
  });

  it('times out and fails after maxTotalDurationMs has elapsed', async () => {
    const onWakingUpChange = vi.fn();
    const action = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));

    let simulatedTime = 0;
    const fakeSleep = vi.fn().mockImplementation(async (ms: number) => {
      simulatedTime += ms;
    });

    await expect(
      executeWithRetry(action, {
        maxTotalDurationMs: 500,
        initialDelayMs: 200,
        backoffFactor: 2,
        sleepFn: fakeSleep,
        nowFn: () => simulatedTime,
        onWakingUpChange,
      })
    ).rejects.toThrow('Failed to fetch');

    expect(onWakingUpChange).toHaveBeenCalledWith(true);
    expect(onWakingUpChange).toHaveBeenLastCalledWith(false);
  });
});

