import React, { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type {
  AssignmentResponseDto,
  AutoAssignmentResultDto,
  TaskResponseDto,
  WorkerResponseDto,
} from '../types';
import { AlertBanner } from '../components/AlertBanner';
import { formatDate, getNextMonday } from '../utils/date';

export const AssignmentsView: React.FC = () => {
  const [assignments, setAssignments] = useState<AssignmentResponseDto[]>([]);
  const [tasks, setTasks] = useState<TaskResponseDto[]>([]);
  const [workers, setWorkers] = useState<WorkerResponseDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Status banners
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Manual assignment form
  const [selectedTaskId, setSelectedTaskId] = useState<string>('');
  const [selectedWorkerId, setSelectedWorkerId] = useState<string>('');
  const [manualStartDate, setManualStartDate] = useState<string>(getNextMonday());
  const [isManualSubmitting, setIsManualSubmitting] = useState<boolean>(false);

  // Auto assignment section
  const [autoStartDate, setAutoStartDate] = useState<string>(getNextMonday());
  const [isAutoSubmitting, setIsAutoSubmitting] = useState<boolean>(false);
  const [autoReport, setAutoReport] = useState<AutoAssignmentResultDto | null>(null);

  const loadAll = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [fetchedAssignments, fetchedTasks, fetchedWorkers] = await Promise.all([
        api.getAssignments(),
        api.getTasks(),
        api.getWorkers(),
      ]);
      setAssignments(fetchedAssignments);
      setTasks(fetchedTasks);
      setWorkers(fetchedWorkers);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Failed to load assignment data';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  // Filter tasks to only NOT_DONE and unassigned
  const assignedTaskIds = new Set(assignments.map((a) => a.task.id));
  const availableTasks = tasks.filter(
    (t) => t.status === 'NOT_DONE' && !assignedTaskIds.has(t.id)
  );

  const handleManualAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTaskId || !selectedWorkerId) {
      setErrorMessage('Please select both a task and a worker');
      return;
    }

    setErrorMessage(null);
    setSuccessMessage(null);
    setIsManualSubmitting(true);

    try {
      const res = await api.createAssignment({
        taskId: Number(selectedTaskId),
        workerId: Number(selectedWorkerId),
        startDate: manualStartDate,
      });

      setSuccessMessage(
        `Assigned "${res.task.name}" to ${res.worker.name}. Effective period: ${formatDate(
          res.startDate
        )} → ${formatDate(res.endDate)}`
      );

      // Reset form selection
      setSelectedTaskId('');
      setSelectedWorkerId('');
      await loadAll();
    } catch (err) {
      // Show backend error verbatim (e.g. 422 missing skills or 409 conflict)
      const msg = err instanceof ApiError ? err.message : 'Assignment failed';
      setErrorMessage(msg);
    } finally {
      setIsManualSubmitting(false);
    }
  };

  const handleAutoAssign = async () => {
    setErrorMessage(null);
    setSuccessMessage(null);
    setAutoReport(null);
    setIsAutoSubmitting(true);

    try {
      const result = await api.autoAssign({
        startDate: autoStartDate || undefined,
      });
      setAutoReport(result);
      setSuccessMessage(
        `Auto-assignment completed: ${result.assigned.length} assigned, ${result.unassigned.length} unassigned.`
      );
      await loadAll();
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Auto-assignment failed';
      setErrorMessage(msg);
    } finally {
      setIsAutoSubmitting(false);
    }
  };

  const handleUnassign = async (assignment: AssignmentResponseDto) => {
    if (
      !window.confirm(
        `Are you sure you want to unassign task "${assignment.task.name}" from ${assignment.worker.name}?`
      )
    ) {
      return;
    }

    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      await api.deleteAssignment(assignment.id);
      setSuccessMessage(`Unassigned task "${assignment.task.name}" successfully`);
      await loadAll();
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Failed to unassign task';
      setErrorMessage(msg);
    }
  };

  return (
    <div className="view-container">
      <div className="view-header">
        <div>
          <h2>Assignments</h2>
          <p className="view-subtitle">
            Perform manual skill-validated scheduling or run the deterministic auto-assignment engine.
          </p>
        </div>
      </div>

      <AlertBanner type="error" message={errorMessage} onClose={() => setErrorMessage(null)} />
      <AlertBanner type="success" message={successMessage} onClose={() => setSuccessMessage(null)} />

      {/* Grid: Manual Assignment Form & Auto-Assign Section */}
      <div className="assignment-grid">
        {/* Manual Assignment Panel */}
        <div className="card">
          <div className="card-header">
            <h3>Manual Assignment</h3>
            <span className="card-badge">Strict Skill Validation</span>
          </div>
          <form onSubmit={handleManualAssign} className="assignment-form">
            <div className="form-group">
              <label className="form-label" htmlFor="selectTask">
                Select Task (NOT_DONE & Unassigned) *
              </label>
              <select
                id="selectTask"
                className="form-control"
                value={selectedTaskId}
                onChange={(e) => setSelectedTaskId(e.target.value)}
                required
              >
                <option value="">-- Choose an eligible task --</option>
                {availableTasks.map((t) => (
                  <option key={t.id} value={t.id}>
                    #{t.id} {t.name} ({t.effortHours}h) - Skills: [{t.skills.join(', ') || 'None'}]
                  </option>
                ))}
              </select>
              {availableTasks.length === 0 && (
                <small className="form-hint text-muted">
                  No unassigned NOT_DONE tasks available. Create a new task or unassign one below.
                </small>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="selectWorker">
                Select Worker *
              </label>
              <select
                id="selectWorker"
                className="form-control"
                value={selectedWorkerId}
                onChange={(e) => setSelectedWorkerId(e.target.value)}
                required
              >
                <option value="">-- Choose a worker --</option>
                {workers.map((w) => (
                  <option key={w.id} value={w.id}>
                    #{w.id} {w.name} ({w.hoursPerDay}h/day) - Skills: [{w.skills.join(', ') || 'None'}]
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="startDate">
                Start Date (Mon-Fri) *
              </label>
              <input
                id="startDate"
                type="date"
                className="form-control"
                value={manualStartDate}
                onChange={(e) => setManualStartDate(e.target.value)}
                required
              />
              <small className="form-hint text-muted">
                Weekend dates automatically advance to the following Monday.
              </small>
            </div>

            <button
              type="submit"
              className="btn btn-primary btn-full"
              disabled={isManualSubmitting || availableTasks.length === 0}
            >
              {isManualSubmitting ? 'Validating & Assigning...' : 'Assign Task'}
            </button>
          </form>
        </div>

        {/* Auto-Assign Section */}
        <div className="card">
          <div className="card-header">
            <h3>Greedy Auto-Assignment</h3>
            <span className="card-badge card-badge-accent">Deterministic</span>
          </div>
          <div className="card-body">
            <p className="text-secondary text-sm">
              Automatically assigns all pending <code>NOT_DONE</code> tasks to workers with matching skills,
              prioritizing most constrained tasks first and earliest end dates.
            </p>

            <div className="form-group">
              <label className="form-label" htmlFor="autoStartDate">
                Simulation Start Date
              </label>
              <input
                id="autoStartDate"
                type="date"
                className="form-control"
                value={autoStartDate}
                onChange={(e) => setAutoStartDate(e.target.value)}
              />
            </div>

            <button
              type="button"
              className="btn btn-accent btn-full"
              onClick={handleAutoAssign}
              disabled={isAutoSubmitting}
            >
              {isAutoSubmitting ? 'Simulating & Assigning...' : '⚡ Run Auto-Assignment'}
            </button>
          </div>
        </div>
      </div>

      {/* Auto-Assignment Report Display */}
      {autoReport && (
        <div className="card auto-report-card">
          <div className="card-header">
            <h3>Auto-Assignment Execution Report</h3>
            <button
              type="button"
              className="btn btn-sm btn-outline"
              onClick={() => setAutoReport(null)}
            >
              Dismiss Report
            </button>
          </div>
          <div className="report-sections">
            {/* Assigned Report */}
            <div className="report-column">
              <h4 className="report-title text-success">
                ✅ Assigned ({autoReport.assigned.length})
              </h4>
              {autoReport.assigned.length === 0 ? (
                <div className="text-muted text-sm">No tasks could be assigned in this run.</div>
              ) : (
                <ul className="report-list">
                  {autoReport.assigned.map((item) => (
                    <li key={item.id} className="report-item report-item-success">
                      <div className="font-semibold">{item.task.name}</div>
                      <div className="text-sm">
                        Assigned to: <span className="font-medium">{item.worker.name}</span>
                      </div>
                      <div className="text-xs text-muted">
                        {formatDate(item.startDate)} &rarr; {formatDate(item.endDate)} (
                        {item.allocations.reduce((sum, a) => sum + a.hours, 0)}h total)
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {/* Unassigned Report */}
            <div className="report-column">
              <h4 className="report-title text-danger">
                ⚠️ Unassigned Tasks ({autoReport.unassigned.length})
              </h4>
              {autoReport.unassigned.length === 0 ? (
                <div className="text-muted text-sm">All eligible tasks were successfully assigned!</div>
              ) : (
                <ul className="report-list">
                  {autoReport.unassigned.map((item) => (
                    <li key={item.task.id} className="report-item report-item-danger">
                      <div className="font-semibold">{item.task.name}</div>
                      <div className="text-sm text-danger font-medium mt-1">
                        Reason: {item.reason}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Current Assignments List */}
      <div className="assignments-table-section mt-6">
        <div className="section-header">
          <h3>Current Assignments ({assignments.length})</h3>
        </div>

        {isLoading ? (
          <div className="loading-state">Loading assignments...</div>
        ) : assignments.length === 0 ? (
          <div className="empty-state">No assignments currently scheduled.</div>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Task</th>
                  <th>Worker</th>
                  <th>Effective Start Date</th>
                  <th>End Date</th>
                  <th>Allocations</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {assignments.map((assignment) => {
                  const totalHours = assignment.allocations.reduce((acc, d) => acc + d.hours, 0);
                  return (
                    <tr key={assignment.id}>
                      <td>#{assignment.id}</td>
                      <td className="font-semibold">{assignment.task.name}</td>
                      <td>
                        <span className="font-medium text-primary">{assignment.worker.name}</span>
                      </td>
                      <td>{formatDate(assignment.startDate)}</td>
                      <td>{formatDate(assignment.endDate)}</td>
                      <td>
                        <span className="badge badge-neutral">
                          {assignment.allocations.length} days ({totalHours}h)
                        </span>
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-sm btn-danger"
                          onClick={() => handleUnassign(assignment)}
                        >
                          Unassign
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
