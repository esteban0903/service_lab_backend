import React, { useEffect, useState } from 'react';
import { TasksView } from './views/TasksView';
import { WorkersView } from './views/WorkersView';
import { AssignmentsView } from './views/AssignmentsView';
import { WorkloadView } from './views/WorkloadView';
import { api, ApiError } from './api/client';
import { AlertBanner } from './components/AlertBanner';

type TabType = 'tasks' | 'workers' | 'assignments' | 'workload';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('assignments');
  const [isSeeding, setIsSeeding] = useState<boolean>(false);
  const [seedEnabled, setSeedEnabled] = useState<boolean>(false);
  const [globalSuccess, setGlobalSuccess] = useState<string | null>(null);
  const [globalError, setGlobalError] = useState<string | null>(null);
  // View reload trigger
  const [reloadKey, setReloadKey] = useState<number>(0);

  useEffect(() => {
    api.getConfig()
      .then((cfg) => setSeedEnabled(cfg.seedEnabled))
      .catch(() => setSeedEnabled(false));
  }, []);

  const handleSeedData = async () => {
    if (!window.confirm('Reset database and seed demo data (5 workers, 12 tasks, 2 assignments)?')) {
      return;
    }
    setIsSeeding(true);
    setGlobalError(null);
    setGlobalSuccess(null);
    try {
      const summary = await api.seed();
      setGlobalSuccess(
        `Demo data seeded successfully: ${summary.workersCreated} workers, ${summary.tasksCreated} tasks, ${summary.assignmentsCreated} manual assignments (relative to ${summary.demoStartDate}).`
      );
      // Trigger active view reload
      setReloadKey((prev) => prev + 1);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Failed to seed demo data';
      setGlobalError(msg);
    } finally {
      setIsSeeding(false);
    }
  };

  return (
    <div className="app-root">
      <header className="app-header">
        <div className="header-container">
          <div className="brand">
            <span className="brand-title">ServiceLab</span>
            <span className="brand-badge">Scheduler</span>
          </div>

          <nav className="nav-tabs">
            <button
              type="button"
              className={`nav-tab ${activeTab === 'assignments' ? 'active' : ''}`}
              onClick={() => setActiveTab('assignments')}
            >
              Assignments
            </button>
            <button
              type="button"
              className={`nav-tab ${activeTab === 'workload' ? 'active' : ''}`}
              onClick={() => setActiveTab('workload')}
            >
              Workload
            </button>
            <button
              type="button"
              className={`nav-tab ${activeTab === 'tasks' ? 'active' : ''}`}
              onClick={() => setActiveTab('tasks')}
            >
              Tasks
            </button>
            <button
              type="button"
              className={`nav-tab ${activeTab === 'workers' ? 'active' : ''}`}
              onClick={() => setActiveTab('workers')}
            >
              Workers
            </button>
          </nav>

          {seedEnabled && (
            <button
              type="button"
              className="btn btn-sm btn-outline"
              onClick={handleSeedData}
              disabled={isSeeding}
              title="Wipe database and repopulate with demo dataset"
            >
              {isSeeding ? 'Seeding...' : '🌱 Reset & Seed Data'}
            </button>
          )}
        </div>
      </header>

      <main className="main-content">
        <AlertBanner type="success" message={globalSuccess} onClose={() => setGlobalSuccess(null)} />
        <AlertBanner type="error" message={globalError} onClose={() => setGlobalError(null)} />

        {activeTab === 'tasks' && <TasksView key={`tasks-${reloadKey}`} />}
        {activeTab === 'workers' && <WorkersView key={`workers-${reloadKey}`} />}
        {activeTab === 'assignments' && <AssignmentsView key={`assignments-${reloadKey}`} />}
        {activeTab === 'workload' && <WorkloadView key={`workload-${reloadKey}`} />}
      </main>
    </div>
  );
};

export default App;
