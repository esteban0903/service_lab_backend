const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/**
 * Format a YYYY-MM-DD date string into a friendly format like "Mon 19 Oct"
 * Parses the parts manually in UTC so it never shifts due to local timezones like America/Bogota.
 */
export function formatDate(dateStr?: string): string {
  if (!dateStr) return '';
  const parts = dateStr.split('-');
  if (parts.length !== 3) return dateStr;

  const year = parseInt(parts[0], 10);
  const month = parseInt(parts[1], 10);
  const day = parseInt(parts[2], 10);

  if (isNaN(year) || isNaN(month) || isNaN(day) || month < 1 || month > 12) {
    return dateStr;
  }

  // Use UTC to compute the exact day of week
  const utcDate = new Date(Date.UTC(year, month - 1, day));
  const weekday = WEEKDAYS[utcDate.getUTCDay()];
  const monthName = MONTHS[month - 1];

  return `${weekday} ${day} ${monthName}`;
}

/**
 * Returns next Monday's date formatted as YYYY-MM-DD
 */
export function getNextMonday(referenceDate: Date = new Date()): string {
  const d = new Date(referenceDate);
  const dayOfWeek = d.getDay(); // 0 is Sunday, 1 is Monday
  const daysUntilNextMonday = ((8 - dayOfWeek) % 7) || 7;
  d.setDate(d.getDate() + daysUntilNextMonday);

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}
