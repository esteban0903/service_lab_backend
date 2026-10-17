import { ApiError } from '../api/client';

export interface RetryOptions {
  slowThresholdMs?: number;
  maxTotalDurationMs?: number;
  initialDelayMs?: number;
  backoffFactor?: number;
  maxDelayMs?: number;
  onWakingUpChange?: (isWakingUp: boolean) => void;
  sleepFn?: (ms: number) => Promise<void>;
  nowFn?: () => number;
}

export function isRetryableError(err: unknown): boolean {
  if (err instanceof ApiError) {
    return [502, 503, 504].includes(err.status);
  }
  if (err instanceof TypeError) {
    return true; // standard fetch network error (e.g. "Failed to fetch")
  }
  if (err instanceof Error) {
    const msg = err.message.toLowerCase();
    return msg.includes('network') || msg.includes('failed to fetch') || msg.includes('econnrefused');
  }
  return false;
}

export async function executeWithRetry<T>(
  action: () => Promise<T>,
  options?: RetryOptions
): Promise<T> {
  const slowThreshold = options?.slowThresholdMs ?? 3000;
  const maxTotalDuration = options?.maxTotalDurationMs ?? 90000;
  const initialDelay = options?.initialDelayMs ?? 2000;
  const backoffFactor = options?.backoffFactor ?? 1.5;
  const maxDelay = options?.maxDelayMs ?? 10000;
  const onWakingUpChange = options?.onWakingUpChange;
  const sleep = options?.sleepFn ?? ((ms: number) => new Promise<void>((res) => setTimeout(res, ms)));
  const now = options?.nowFn ?? (() => Date.now());

  const startTime = now();
  let currentDelay = initialDelay;
  let isMarkedWakingUp = false;

  const markWakingUp = () => {
    if (!isMarkedWakingUp) {
      isMarkedWakingUp = true;
      onWakingUpChange?.(true);
    }
  };

  const clearWakingUp = () => {
    if (isMarkedWakingUp) {
      isMarkedWakingUp = false;
      onWakingUpChange?.(false);
    }
  };

  while (true) {
    let slowTimer: ReturnType<typeof setTimeout> | null = null;
    if (!isMarkedWakingUp) {
      slowTimer = setTimeout(() => {
        markWakingUp();
      }, slowThreshold);
    }

    try {
      const result = await action();
      if (slowTimer) clearTimeout(slowTimer);
      clearWakingUp();
      return result;
    } catch (err: unknown) {
      if (slowTimer) clearTimeout(slowTimer);

      const isRetryable = isRetryableError(err);
      const elapsed = now() - startTime;
      if (!isRetryable || elapsed >= maxTotalDuration) {
        clearWakingUp();
        throw err;
      }

      markWakingUp();

      const remainingBudget = maxTotalDuration - (now() - startTime);
      if (remainingBudget <= 0) {
        clearWakingUp();
        throw err;
      }

      const waitTime = Math.min(currentDelay, remainingBudget);
      await sleep(waitTime);
      currentDelay = Math.min(currentDelay * backoffFactor, maxDelay);
    }
  }
}
