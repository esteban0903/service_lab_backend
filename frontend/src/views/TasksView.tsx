import React, { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { AssignmentResponseDto, TaskRequestDto, TaskResponseDto, TaskStatus } from '../types';
import { ChipsInput } from '../components/ChipsInput';
import { AlertBanner } from '../components/AlertBanner';
import { formatDate } from '../utils/date';

export const TasksView: React.FC = () => {
  const [tasks, setTasks] = useState<TaskResponseDto[]>([]);
  const [assignments, setAssignments] = useState<AssignmentResponseDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form modal state
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingTaskId, setEditingTaskId] = useState<number | null>(null);
  const [formName, setFormName] = useState<string>('');
  const [formEffort, setFormEffort] = useState<number>(8);
  const [formStatus, setFormStatus] = useState<TaskStatus>('NOT_DONE');
  const [formSkills, setFormSkills] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const loadData = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [fetchedTasks, fetchedAssignments] = await Promise.all([
        api.getTasks(),
        api.getAssignments(),
      ]);
      setTasks(fetchedTasks);
      setAssignments(fetchedAssignments);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Failed to load tasks data';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openCreateModal = () => {
    setEditingTaskId(null);
    setFormName('');
    setFormEffort(8);
    setFormStatus('NOT_DONE');
    setFormSkills([]);
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsModalOpen(true);
  };

  const openEditModal = (task: TaskResponseDto) => {
    setEditingTaskId(task.id);
    setFormName(task.name);
    setFormEffort(task.effortHours);
    setFormStatus(task.status);
    setFormSkills([...task.skills]);
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingTaskId(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsSubmitting(true);

    const payload: TaskRequestDto = {
      name: formName.trim(),
      effortHours: Number(formEffort),
      status: formStatus,
      skills: formSkills,
    };

    try {
      if (editingTaskId) {
        await api.updateTask(editingTaskId, payload);
        setSuccessMessage(`Task "${payload.name}" updated successfully`);
      } else {
        await api.createTask(payload);
        setSuccessMessage(`Task "${payload.name}" created successfully`);
      }
      closeModal();
      await loadData();
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Operation failed';
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (task: TaskResponseDto) => {
    if (!window.confirm(`Are you sure you want to delete task "${task.name}"?`)) {
      return;
    }
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.deleteTask(task.id);
      setSuccessMessage(`Task "${task.name}" deleted successfully`);
      await loadData();
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Failed to delete task';
      setErrorMessage(msg);
    }
  };

  // Build client-side lookup for assignments by taskId
  const assignmentByTaskId = new Map<number, AssignmentResponseDto>();
  for (const a of assignments) {
    assignmentByTaskId.set(a.task.id, a);
  }

  return (
    <div className="view-container">
      <div className="view-header">
        <div>
          <h2>Tasks</h2>
          <p className="view-subtitle">Manage project tasks, effort requirements, skills, and current allocations.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreateModal}>
          + Create Task
        </button>
      </div>

      <AlertBanner type="error" message={errorMessage} onClose={() => setErrorMessage(null)} />
      <AlertBanner type="success" message={successMessage} onClose={() => setSuccessMessage(null)} />

      {isLoading ? (
        <div className="loading-state">Loading tasks...</div>
      ) : tasks.length === 0 ? (
        <div className="empty-state">No tasks created yet. Click "+ Create Task" or seed demo data.</div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Task Name</th>
                <th>Effort</th>
                <th>Required Skills</th>
                <th>Status</th>
                <th>Assignment</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((task) => {
                const assignment = assignmentByTaskId.get(task.id);
                return (
                  <tr key={task.id}>
                    <td>#{task.id}</td>
                    <td className="font-semibold">{task.name}</td>
                    <td>{task.effortHours}h</td>
                    <td>
                      <div className="chips-list">
                        {task.skills.length > 0 ? (
                          task.skills.map((skill) => (
                            <span key={skill} className="badge badge-skill">
                              {skill}
                            </span>
                          ))
                        ) : (
                          <span className="text-muted">None</span>
                        )}
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${task.status === 'DONE' ? 'badge-success' : 'badge-warning'}`}>
                        {task.status}
                      </span>
                    </td>
                    <td>
                      {assignment ? (
                        <div className="assignment-info">
                          <span className="font-medium text-primary">{assignment.worker.name}</span>
                          <span className="text-muted text-xs">
                            {formatDate(assignment.startDate)} &rarr; {formatDate(assignment.endDate)}
                          </span>
                        </div>
                      ) : (
                        <span className="text-muted">Unassigned</span>
                      )}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline"
                          onClick={() => openEditModal(task)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="btn btn-sm btn-danger"
                          onClick={() => handleDelete(task)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal Dialog */}
      {isModalOpen && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="modal-header">
              <h3>{editingTaskId ? 'Edit Task' : 'Create New Task'}</h3>
              <button type="button" className="modal-close" onClick={closeModal}>
                &times;
              </button>
            </div>
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-group">
                <label className="form-label" htmlFor="taskName">
                  Task Name *
                </label>
                <input
                  id="taskName"
                  type="text"
                  className="form-control"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="e.g. Database Schema Migration"
                  required
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label" htmlFor="taskEffort">
                    Effort (Hours) *
                  </label>
                  <input
                    id="taskEffort"
                    type="number"
                    min="1"
                    className="form-control"
                    value={formEffort}
                    onChange={(e) => setFormEffort(Number(e.target.value))}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="taskStatus">
                    Status
                  </label>
                  <select
                    id="taskStatus"
                    className="form-control"
                    value={formStatus}
                    onChange={(e) => setFormStatus(e.target.value as TaskStatus)}
                  >
                    <option value="NOT_DONE">NOT_DONE</option>
                    <option value="DONE">DONE</option>
                  </select>
                </div>
              </div>

              <ChipsInput
                label="Required Skills (Type and press Enter or comma)"
                chips={formSkills}
                onChange={setFormSkills}
                placeholder="e.g. java, sql, react"
              />

              <div className="modal-actions">
                <button type="button" className="btn btn-outline" onClick={closeModal} disabled={isSubmitting}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                  {isSubmitting ? 'Saving...' : editingTaskId ? 'Save Changes' : 'Create Task'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
