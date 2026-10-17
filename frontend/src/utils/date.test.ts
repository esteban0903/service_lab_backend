import { describe, expect, it } from 'vitest';
import { formatDate, getNextMonday } from './date';

describe('Date Utilities', () => {
  it('renders "2026-10-19" as "Mon 19 Oct" without timezone rollback', () => {
    const formatted = formatDate('2026-10-19');
    expect(formatted).toBe('Mon 19 Oct');
  });

  it('renders various dates correctly', () => {
    expect(formatDate('2026-10-22')).toBe('Thu 22 Oct');
    expect(formatDate('2026-10-26')).toBe('Mon 26 Oct');
    expect(formatDate('2026-01-01')).toBe('Thu 1 Jan');
  });

  it('handles empty or malformed strings gracefully', () => {
    expect(formatDate('')).toBe('');
    expect(formatDate(undefined)).toBe('');
    expect(formatDate('invalid-date')).toBe('invalid-date');
  });

  it('calculates next Monday correctly from a Wednesday', () => {
    // 2026-10-14 is Wednesday
    const wednesday = new Date(2026, 9, 14);
    expect(getNextMonday(wednesday)).toBe('2026-10-19');
  });
});
