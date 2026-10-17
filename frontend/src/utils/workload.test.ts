import { describe, expect, it } from 'vitest';
import type { AssignmentResponseDto, WorkerResponseDto } from '../types';
import {
  computeWorkloadGrid,
  getMondayOfWeek,
  getWorkingDaysOfWeek,
  resolveDefaultMonday,
  shiftDate,
} from './workload';

describe('Workload Grid Computation Logic', () => {
  it('computes Monday of week correctly across different days', () => {
    // 2026-10-19 is Monday
    expect(getMondayOfWeek('2026-10-19')).toBe('2026-10-19');
    // 2026-10-21 is Wednesday -> should be 2026-10-19
    expect(getMondayOfWeek('2026-10-21')).toBe('2026-10-19');
    // 2026-10-23 is Friday -> should be 2026-10-19
    expect(getMondayOfWeek('2026-10-23')).toBe('2026-10-19');
    // 2026-10-25 is Sunday -> should be 2026-10-19
    expect(getMondayOfWeek('2026-10-25')).toBe('2026-10-19');
  });

  it('generates 5 working days from Monday to Friday', () => {
    const days = getWorkingDaysOfWeek('2026-10-19');
    expect(days).toEqual([
      '2026-10-19',
      '2026-10-20',
      '2026-10-21',
      '2026-10-22',
      '2026-10-23',
    ]);
  });

  it('shifts dates forward and backward by 7 days', () => {
    expect(shiftDate('2026-10-19', 7)).toBe('2026-10-26');
    expect(shiftDate('2026-10-19', -7)).toBe('2026-10-12');
  });

  it('resolves default Monday based on earliest upcoming assignment', () => {
    const assignments: AssignmentResponseDto[] = [
      {
        id: 1,
        task: { id: 10, name: 'Future Task' },
        worker: { id: 1, name: 'Ana' },
        startDate: '2026-10-28', // Wednesday in week of 2026-10-26
        endDate: '2026-10-28',
        allocations: [{ date: '2026-10-28', hours: 4 }],
      },
      {
        id: 2,
        task: { id: 11, name: 'Past Task' },
        worker: { id: 2, name: 'Bruno' },
        startDate: '2026-10-12',
        endDate: '2026-10-12',
        allocations: [{ date: '2026-10-12', hours: 6 }],
      },
    ];

    // Reference today is 2026-10-17. Upcoming starts 2026-10-28 -> Monday is 2026-10-26
    const monday = resolveDefaultMonday(assignments, '2026-10-17');
    expect(monday).toBe('2026-10-26');
  });

  it('falls back to current week Monday when no assignments exist', () => {
    const monday = resolveDefaultMonday([], '2026-10-21'); // Wednesday
    expect(monday).toBe('2026-10-19');
  });

  it('computes grid cells with empty, partial, and full load states and task details', () => {
    const workers: WorkerResponseDto[] = [
      { id: 1, name: 'Ana', hoursPerDay: 8, skills: ['java'] },
      { id: 2, name: 'Bruno', hoursPerDay: 6, skills: ['react'] },
    ];

    const workingDays = ['2026-10-19', '2026-10-20', '2026-10-21', '2026-10-22', '2026-10-23'];

    const assignments: AssignmentResponseDto[] = [
      {
        id: 1,
        task: { id: 101, name: 'Backend API' },
        worker: { id: 1, name: 'Ana' },
        startDate: '2026-10-19',
        endDate: '2026-10-20',
        allocations: [
          { date: '2026-10-19', hours: 8 }, // Full: 8/8
          { date: '2026-10-20', hours: 5 }, // Partial: 5/8
        ],
      },
      {
        id: 2,
        task: { id: 102, name: 'Bug Fixes' },
        worker: { id: 1, name: 'Ana' },
        startDate: '2026-10-20',
        endDate: '2026-10-20',
        allocations: [
          { date: '2026-10-20', hours: 3 }, // Added to 5 -> 8/8 (Full)
        ],
      },
      {
        id: 3,
        task: { id: 103, name: 'React UI' },
        worker: { id: 2, name: 'Bruno' },
        startDate: '2026-10-21',
        endDate: '2026-10-21',
        allocations: [
          { date: '2026-10-21', hours: 4 }, // Partial: 4/6
        ],
      },
    ];

    const grid = computeWorkloadGrid(workers, assignments, workingDays);
    expect(grid).toHaveLength(2);

    // Ana's row
    const anaRow = grid.find((r) => r.workerId === 1)!;
    expect(anaRow.workerName).toBe('Ana');
    expect(anaRow.totalWeekHours).toBe(16); // 8 + (5+3)

    // Day 1: 2026-10-19 -> Full (8/8)
    const anaMon = anaRow.cells['2026-10-19'];
    expect(anaMon.usedHours).toBe(8);
    expect(anaMon.capacityHours).toBe(8);
    expect(anaMon.status).toBe('full');
    expect(anaMon.tasks).toHaveLength(1);
    expect(anaMon.tasks[0].taskName).toBe('Backend API');

    // Day 2: 2026-10-20 -> Two tasks combined = 8/8 (Full)
    const anaTue = anaRow.cells['2026-10-20'];
    expect(anaTue.usedHours).toBe(8);
    expect(anaTue.status).toBe('full');
    expect(anaTue.tasks).toHaveLength(2);
    expect(anaTue.tasks.map((t) => t.taskName)).toEqual(['Backend API', 'Bug Fixes']);

    // Day 3: 2026-10-21 -> Empty (0/8)
    const anaWed = anaRow.cells['2026-10-21'];
    expect(anaWed.usedHours).toBe(0);
    expect(anaWed.status).toBe('empty');
    expect(anaWed.tasks).toHaveLength(0);

    // Bruno's row
    const brunoRow = grid.find((r) => r.workerId === 2)!;
    expect(brunoRow.cells['2026-10-21'].usedHours).toBe(4);
    expect(brunoRow.cells['2026-10-21'].capacityHours).toBe(6);
    expect(brunoRow.cells['2026-10-21'].status).toBe('partial');
  });
});

