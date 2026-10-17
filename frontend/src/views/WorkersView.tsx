import React, { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { WorkerRequestDto, WorkerResponseDto } from '../types';
import { ChipsInput } from '../components/ChipsInput';
import { AlertBanner } from '../components/AlertBanner';

export const WorkersView: React.FC = () => {
  const [workers, setWorkers] = useState<WorkerResponseDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form modal state
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingWorkerId, setEditingWorkerId] = useState<number | null>(null);
  const [formName, setFormName] = useState<string>('');
  const [formHours, setFormHours] = useState<number>(8);
  const [formSkills, setFormSkills] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const loadWorkers = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const fetchedWorkers = await api.getWorkers();
      setWorkers(fetchedWorkers);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Failed to load workers';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadWorkers();
  }, []);

  const openCreateModal = () => {
    setEditingWorkerId(null);
    setFormName('');
    setFormHours(8);
    setFormSkills([]);
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsModalOpen(true);
  };

  const openEditModal = (worker: WorkerResponseDto) => {
    setEditingWorkerId(worker.id);
    setFormName(worker.name);
    setFormHours(worker.hoursPerDay);
    setFormSkills([...worker.skills]);
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingWorkerId(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsSubmitting(true);

    const payload: WorkerRequestDto = {
      name: formName.trim(),
      hoursPerDay: Number(formHours),
      skills: formSkills,
    };

    try {
      if (editingWorkerId) {
        await api.updateWorker(editingWorkerId, payload);
        setSuccessMessage(`Worker "${payload.name}" updated successfully`);
      } else {
        await api.createWorker(payload);
        setSuccessMessage(`Worker "${payload.name}" created successfully`);
      }
      closeModal();
      await loadWorkers();
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Operation failed';
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (worker: WorkerResponseDto) => {
    if (!window.confirm(`Are you sure you want to delete worker "${worker.name}"?`)) {
      return;
    }
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.deleteWorker(worker.id);
      setSuccessMessage(`Worker "${worker.name}" deleted successfully`);
      await loadWorkers();
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Failed to delete worker';
      setErrorMessage(msg);
    }
  };

  return (
    <div className="view-container">
      <div className="view-header">
        <div>
          <h2>Workers</h2>
          <p className="view-subtitle">Manage team members, their daily work capacity, and skills.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreateModal}>
          + Create Worker
        </button>
      </div>

      <AlertBanner type="error" message={errorMessage} onClose={() => setErrorMessage(null)} />
      <AlertBanner type="success" message={successMessage} onClose={() => setSuccessMessage(null)} />

      {isLoading ? (
        <div className="loading-state">Loading workers...</div>
      ) : workers.length === 0 ? (
        <div className="empty-state">No workers available. Click "+ Create Worker" or seed demo data.</div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Daily Capacity</th>
                <th>Skills</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {workers.map((worker) => (
                <tr key={worker.id}>
                  <td>#{worker.id}</td>
                  <td className="font-semibold">{worker.name}</td>
                  <td>
                    <span className="badge badge-neutral">{worker.hoursPerDay} hours/day</span>
                  </td>
                  <td>
                    <div className="chips-list">
                      {worker.skills.length > 0 ? (
                        worker.skills.map((skill) => (
                          <span key={skill} className="badge badge-skill">
                            {skill}
                          </span>
                        ))
                      ) : (
                        <span className="text-muted">No skills assigned</span>
                      )}
                    </div>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        type="button"
                        className="btn btn-sm btn-outline"
                        onClick={() => openEditModal(worker)}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm btn-danger"
                        onClick={() => handleDelete(worker)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal Dialog */}
      {isModalOpen && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="modal-header">
              <h3>{editingWorkerId ? 'Edit Worker' : 'Create New Worker'}</h3>
              <button type="button" className="modal-close" onClick={closeModal}>
                &times;
              </button>
            </div>
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-group">
                <label className="form-label" htmlFor="workerName">
                  Worker Name *
                </label>
                <input
                  id="workerName"
                  type="text"
                  className="form-control"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="e.g. Ana Gomez"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="workerHours">
                  Hours Per Day (1 - 24) *
                </label>
                <input
                  id="workerHours"
                  type="number"
                  min="1"
                  max="24"
                  className="form-control"
                  value={formHours}
                  onChange={(e) => setFormHours(Number(e.target.value))}
                  required
                />
              </div>

              <ChipsInput
                label="Worker Skills (Type and press Enter or comma)"
                chips={formSkills}
                onChange={setFormSkills}
                placeholder="e.g. java, spring, react"
              />

              <div className="modal-actions">
                <button type="button" className="btn btn-outline" onClick={closeModal} disabled={isSubmitting}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                  {isSubmitting ? 'Saving...' : editingWorkerId ? 'Save Changes' : 'Create Worker'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
