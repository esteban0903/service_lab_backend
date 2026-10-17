import React, { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { AssignmentResponseDto, WorkerResponseDto } from '../types';
import { AlertBanner } from '../components/AlertBanner';
import { formatDate } from '../utils/date';
import {
  computeWorkloadGrid,
  getMondayOfWeek,
  getWorkingDaysOfWeek,
  resolveDefaultMonday,
  shiftDate,
  type WorkloadCell,
} from '../utils/workload';

export const WorkloadView: React.FC = () => {
  const [workers, setWorkers] = useState<WorkerResponseDto[]>([]);
  const [assignments, setAssignments] = useState<AssignmentResponseDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Selected week's Monday (YYYY-MM-DD)
  const [selectedMonday, setSelectedMonday] = useState<string>('');
  // Active detail modal for clicked cell
  const [detailCell, setDetailCell] = useState<{
    workerName: string;
    cell: WorkloadCell;
  } | null>(null);

  const loadData = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [fetchedWorkers, fetchedAssignments] = await Promise.all([
        api.getWorkers(),
        api.getAssignments(),
      ]);
      setWorkers(fetchedWorkers);
      setAssignments(fetchedAssignments);

      // Determine default Monday only if not already set
      if (!selectedMonday) {
        const todayStr = new Date().toISOString().slice(0, 10);
        const defaultMon = resolveDefaultMonday(fetchedAssignments, todayStr);
        setSelectedMonday(defaultMon);
      }
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Failed to load workload data';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handlePrevWeek = () => {
    if (selectedMonday) {
      setSelectedMonday(shiftDate(selectedMonday, -7));
    }
  };

  const handleNextWeek = () => {
    if (selectedMonday) {
      setSelectedMonday(shiftDate(selectedMonday, 7));
    }
  };

  const handleTodayWeek = () => {
    const todayStr = new Date().toISOString().slice(0, 10);
    setSelectedMonday(getMondayOfWeek(todayStr));
  };

  const workingDays = selectedMonday ? getWorkingDaysOfWeek(selectedMonday) : [];
  const workloadRows = selectedMonday
    ? computeWorkloadGrid(workers, assignments, workingDays)
    : [];

  return (
    <div className="view-container">
      <div className="view-header">
        <div>
          <h2>Team Workload & Capacity</h2>
          <p className="view-subtitle">
            Weekly calendar grid showing daily allocated hours against worker capacity (Mon–Fri).
          </p>
        </div>
      </div>

      <AlertBanner type="error" message={errorMessage} onClose={() => setErrorMessage(null)} />

      {/* Week Navigation Bar */}
      <div className="workload-nav-bar">
        <div className="week-controls">
          <button type="button" className="btn btn-sm btn-outline" onClick={handlePrevWeek}>
            &larr; Previous Week
          </button>
          <button type="button" className="btn btn-sm btn-outline" onClick={handleTodayWeek}>
            Current Week
          </button>
          <button type="button" className="btn btn-sm btn-outline" onClick={handleNextWeek}>
            Next Week &rarr;
          </button>
        </div>

        {workingDays.length > 0 && (
          <div className="week-title font-semibold">
            Week: {formatDate(workingDays[0])} &ndash; {formatDate(workingDays[4])}
          </div>
        )}
      </div>

      {isLoading ? (
        <div className="loading-state">Loading workload schedule...</div>
      ) : workers.length === 0 ? (
        <div className="empty-state">No workers registered in the system.</div>
      ) : (
        <div className="table-responsive">
          <table className="workload-table">
            <thead>
              <tr>
                <th className="worker-col-header">Worker</th>
                {workingDays.map((day) => (
                  <th key={day} className="day-col-header">
                    <div className="day-header-title">{formatDate(day)}</div>
                  </th>
                ))}
                <th className="total-col-header">Week Total</th>
              </tr>
            </thead>
            <tbody>
              {workloadRows.map((row) => (
                <tr key={row.workerId}>
                  <td className="worker-cell">
                    <div className="font-semibold">{row.workerName}</div>
                    <div className="text-xs text-muted">{row.capacityHours}h/day max</div>
                  </td>
                  {workingDays.map((day) => {
                    const cell = row.cells[day];
                    const tooltipText =
                      cell.tasks.length > 0
                        ? cell.tasks.map((t) => `${t.taskName} (${t.hours}h)`).join('\n')
                        : 'No tasks scheduled';

                    return (
                      <td key={day} className="workload-cell-container">
                        <button
                          type="button"
                          className={`workload-cell-btn cell-${cell.status}`}
                          onClick={() =>
                            setDetailCell({
                              workerName: row.workerName,
                              cell,
                            })
                          }
                          title={tooltipText}
                        >
                          <span className="cell-ratio">
                            {cell.usedHours}/{cell.capacityHours}h
                          </span>
                          {cell.tasks.length > 0 && (
                            <span className="cell-task-count">
                              {cell.tasks.length} {cell.tasks.length === 1 ? 'task' : 'tasks'}
                            </span>
                          )}
                        </button>
                      </td>
                    );
                  })}
                  <td className="total-cell">
                    <span className="font-semibold">{row.totalWeekHours}h</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Legend */}
      <div className="workload-legend">
        <span className="legend-label font-medium text-xs text-muted">Legend:</span>
        <div className="legend-item">
          <span className="legend-color cell-empty"></span>
          <span className="text-xs">Empty (0h)</span>
        </div>
        <div className="legend-item">
          <span className="legend-color cell-partial"></span>
          <span className="text-xs">Partial (1 to capacity-1)</span>
        </div>
        <div className="legend-item">
          <span className="legend-color cell-full"></span>
          <span className="text-xs">Full (100% capacity)</span>
        </div>
        <div className="legend-item">
          <span className="legend-color cell-overload"></span>
          <span className="text-xs">Overload (&gt; capacity)</span>
        </div>
      </div>

      {/* Task Details Modal on Cell Click */}
      {detailCell && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="modal-header">
              <h3>
                {detailCell.workerName} &ndash; {formatDate(detailCell.cell.date)}
              </h3>
              <button
                type="button"
                className="modal-close"
                onClick={() => setDetailCell(null)}
              >
                &times;
              </button>
            </div>
            <div className="modal-form">
              <div className="mb-4">
                <strong>Daily Allocation:</strong> {detailCell.cell.usedHours} /{' '}
                {detailCell.cell.capacityHours} hours ({detailCell.cell.status})
              </div>

              {detailCell.cell.tasks.length === 0 ? (
                <p className="text-muted text-sm">No tasks allocated for this worker on this day.</p>
              ) : (
                <ul className="cell-task-details-list">
                  {detailCell.cell.tasks.map((task) => (
                    <li key={task.taskId} className="cell-task-detail-item">
                      <div className="font-semibold">#{task.taskId} {task.taskName}</div>
                      <div className="badge badge-skill">{task.hours} hours</div>
                    </li>
                  ))}
                </ul>
              )}

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => setDetailCell(null)}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
