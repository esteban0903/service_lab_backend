import type { AssignmentResponseDto, WorkerResponseDto } from '../types';

export interface DayTaskContribution {
  taskId: number;
  taskName: string;
  hours: number;
}

export type CellStatus = 'empty' | 'partial' | 'full' | 'overload';

export interface WorkloadCell {
  date: string; // YYYY-MM-DD
  usedHours: number;
  capacityHours: number;
  status: CellStatus;
  tasks: DayTaskContribution[];
}

export interface WorkerWorkloadRow {
  workerId: number;
  workerName: string;
  capacityHours: number;
  cells: Record<string, WorkloadCell>; // date -> WorkloadCell
  totalWeekHours: number;
}

/**
 * Returns the Monday date (YYYY-MM-DD) for the week containing the given date.
 */
export function getMondayOfWeek(dateStr: string): string {
  const parts = dateStr.split('-').map(Number);
  const date = new Date(Date.UTC(parts[0], parts[1] - 1, parts[2]));
  const day = date.getUTCDay(); // 0 is Sunday, 1 is Monday...
  const diff = (day === 0 ? -6 : 1) - day; // Distance to Monday
  date.setUTCDate(date.getUTCDate() + diff);

  const y = date.getUTCFullYear();
  const m = String(date.getUTCMonth() + 1).padStart(2, '0');
  const d = String(date.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * Returns Monday through Friday dates (5 days) for the week starting at mondayStr.
 */
export function getWorkingDaysOfWeek(mondayStr: string): string[] {
  const parts = mondayStr.split('-').map(Number);
  const result: string[] = [];

  for (let i = 0; i < 5; i++) {
    const d = new Date(Date.UTC(parts[0], parts[1] - 1, parts[2] + i));
    const year = d.getUTCFullYear();
    const month = String(d.getUTCMonth() + 1).padStart(2, '0');
    const day = String(d.getUTCDate()).padStart(2, '0');
    result.push(`${year}-${month}-${day}`);
  }

  return result;
}

/**
 * Advances or rewinds a YYYY-MM-DD date by a given number of days.
 */
export function shiftDate(dateStr: string, days: number): string {
  const parts = dateStr.split('-').map(Number);
  const d = new Date(Date.UTC(parts[0], parts[1] - 1, parts[2] + days));
  const year = d.getUTCFullYear();
  const month = String(d.getUTCMonth() + 1).padStart(2, '0');
  const day = String(d.getUTCDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Resolves the default Monday:
 * "the one containing the earliest upcoming assignment, otherwise the current week according to the user's date."
 */
export function resolveDefaultMonday(
  assignments: AssignmentResponseDto[],
  todayStr: string
): string {
  // 1. Check for upcoming assignments (startDate >= todayStr)
  const upcoming = assignments
    .filter((a) => a.startDate >= todayStr)
    .sort((a, b) => a.startDate.localeCompare(b.startDate));

  if (upcoming.length > 0) {
    return getMondayOfWeek(upcoming[0].startDate);
  }

  // 2. Fall back to earliest assignment overall if any exist
  if (assignments.length > 0) {
    const earliest = [...assignments].sort((a, b) => a.startDate.localeCompare(b.startDate))[0];
    return getMondayOfWeek(earliest.startDate);
  }

  // 3. Otherwise current week Monday
  return getMondayOfWeek(todayStr);
}

/**
 * Pure calculation mapping workers and assignments to a weekly grid of cells.
 */
export function computeWorkloadGrid(
  workers: WorkerResponseDto[],
  assignments: AssignmentResponseDto[],
  workingDays: string[]
): WorkerWorkloadRow[] {
  // Pre-index allocations: workerId -> date -> DayTaskContribution[]
  const workerDailyTasks = new Map<number, Map<string, DayTaskContribution[]>>();

  for (const assignment of assignments) {
    const workerId = assignment.worker.id;
    if (!workerDailyTasks.has(workerId)) {
      workerDailyTasks.set(workerId, new Map());
    }
    const dayMap = workerDailyTasks.get(workerId)!;

    for (const alloc of assignment.allocations) {
      if (!dayMap.has(alloc.date)) {
        dayMap.set(alloc.date, []);
      }
      dayMap.get(alloc.date)!.push({
        taskId: assignment.task.id,
        taskName: assignment.task.name,
        hours: alloc.hours,
      });
    }
  }

  return workers.map((worker) => {
    const dayMap = workerDailyTasks.get(worker.id);
    const cells: Record<string, WorkloadCell> = {};
    let totalWeekHours = 0;

    for (const day of workingDays) {
      const taskList = dayMap?.get(day) || [];
      const usedHours = taskList.reduce((acc, t) => acc + t.hours, 0);
      totalWeekHours += usedHours;

      let status: CellStatus = 'empty';
      if (usedHours > worker.hoursPerDay) {
        status = 'overload';
      } else if (usedHours === worker.hoursPerDay) {
        status = 'full';
      } else if (usedHours > 0) {
        status = 'partial';
      }

      cells[day] = {
        date: day,
        usedHours,
        capacityHours: worker.hoursPerDay,
        status,
        tasks: taskList,
      };
    }

    return {
      workerId: worker.id,
      workerName: worker.name,
      capacityHours: worker.hoursPerDay,
      cells,
      totalWeekHours,
    };
  });
}

