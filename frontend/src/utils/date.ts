/**
 * Format a YYYY-MM-DD date string into a friendly format like "Mon 26 Oct 2026"
 */
export function formatDate(dateStr?: string): string {
  if (!dateStr) return '';
  const [year, month, day] = dateStr.split('-').map(Number);
  if (!year || !month || !day) return dateStr;

  const date = new Date(year, month - 1, day);
  const weekday = date.toLocaleDateString('en-US', { weekday: 'short' });
  const dayOfMonth = date.getDate();
  const monthName = date.toLocaleDateString('en-US', { month: 'short' });

  return `${weekday} ${dayOfMonth} ${monthName}`;
}

/**
 * Returns next Monday's date formatted as YYYY-MM-DD
 */
export function getNextMonday(): string {
  const today = new Date();
  const dayOfWeek = today.getDay(); // 0 is Sunday, 1 is Monday
  const daysUntilNextMonday = ((8 - dayOfWeek) % 7) || 7;
  const nextMonday = new Date(today);
  nextMonday.setDate(today.getDate() + daysUntilNextMonday);

  const year = nextMonday.getFullYear();
  const month = String(nextMonday.getMonth() + 1).padStart(2, '0');
  const day = String(nextMonday.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

