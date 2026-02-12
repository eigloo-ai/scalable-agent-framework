import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppHeader from '../components/layout/AppHeader';
import GraphList from '../components/graph-list/GraphList';
import CreateGraphDialog from '../components/graph-list/CreateGraphDialog';
import { runApi } from '../api/client';
import { useGraphs } from '../hooks/useGraphs';
import { useAppContext } from '../hooks/useAppContext';
import type { GraphRunSummary, RunTimelineEvent, RunTimelineResponse } from '../types';
import './GraphListPage.css';

const GraphListPage: React.FC = () => {
  const navigate = useNavigate();
  const { state } = useAppContext();
  const { graphs, loadGraph, deleteGraph, submitForExecution } = useGraphs();
  const [showCreateDialog, setShowCreateDialog] = useState(false);

  const [selectedGraphId, setSelectedGraphId] = useState('');
  const [runs, setRuns] = useState<GraphRunSummary[]>([]);
  const [selectedRunId, setSelectedRunId] = useState('');
  const [timeline, setTimeline] = useState<RunTimelineResponse | null>(null);
  const [runsLoading, setRunsLoading] = useState(false);
  const [runsError, setRunsError] = useState<string | null>(null);
  const [timelineLoading, setTimelineLoading] = useState(false);
  const [timelineError, setTimelineError] = useState<string | null>(null);

  const selectedGraphName = useMemo(() => {
    return graphs.find((graph) => graph.id === selectedGraphId)?.name ?? '';
  }, [graphs, selectedGraphId]);

  const selectedRun = useMemo(() => {
    return runs.find((run) => run.lifetimeId === selectedRunId) ?? null;
  }, [runs, selectedRunId]);

  const handleCreateNew = () => {
    setShowCreateDialog(true);
  };

  const handleCreateConfirm = async (graphName: string) => {
    setShowCreateDialog(false);
    navigate(`/editor?name=${encodeURIComponent(graphName)}`);
  };

  const handleCreateCancel = () => {
    setShowCreateDialog(false);
  };

  const handleLoadGraph = async (graphId: string) => {
    try {
      await loadGraph(graphId);
      navigate('/editor');
    } catch (error) {
      console.error('Failed to load graph:', error);
    }
  };

  const handleDeleteGraph = async (graphId: string) => {
    try {
      await deleteGraph(graphId);
    } catch (error) {
      console.error('Failed to delete graph:', error);
    }
  };

  const loadRuns = useCallback(async (graphId: string) => {
    if (!graphId) {
      setRuns([]);
      setSelectedRunId('');
      setTimeline(null);
      return;
    }

    setRunsLoading(true);
    setRunsError(null);

    try {
      const runList = await runApi.listGraphRuns(graphId, state.tenantId, 50);
      setRuns(runList);

      setSelectedRunId((previousRunId) => {
        if (previousRunId && runList.some((run) => run.lifetimeId === previousRunId)) {
          return previousRunId;
        }
        return runList[0]?.lifetimeId ?? '';
      });

      if (runList.length === 0) {
        setTimeline(null);
      }
    } catch (error) {
      console.error('Failed to load graph runs:', error);
      setRunsError(error instanceof Error ? error.message : 'Failed to load graph runs');
      setRuns([]);
      setSelectedRunId('');
      setTimeline(null);
    } finally {
      setRunsLoading(false);
    }
  }, [state.tenantId]);

  const loadTimeline = useCallback(async (graphId: string, lifetimeId: string) => {
    if (!graphId || !lifetimeId) {
      setTimeline(null);
      return;
    }

    setTimelineLoading(true);
    setTimelineError(null);

    try {
      const runTimeline = await runApi.getRunTimeline(graphId, lifetimeId, state.tenantId);
      setTimeline(runTimeline);
    } catch (error) {
      console.error('Failed to load run timeline:', error);
      setTimelineError(error instanceof Error ? error.message : 'Failed to load run timeline');
      setTimeline(null);
    } finally {
      setTimelineLoading(false);
    }
  }, [state.tenantId]);

  const handleSubmitForExecution = async (graphId: string) => {
    try {
      const execution = await submitForExecution(graphId);
      if (graphId === selectedGraphId) {
        await loadRuns(graphId);
        setSelectedRunId(execution.executionId);
      }
    } catch (error) {
      console.error('Failed to submit for execution:', error);
    }
  };

  useEffect(() => {
    if (graphs.length === 0) {
      setSelectedGraphId('');
      setRuns([]);
      setSelectedRunId('');
      setTimeline(null);
      return;
    }

    if (!graphs.some((graph) => graph.id === selectedGraphId)) {
      setSelectedGraphId(graphs[0].id);
    }
  }, [graphs, selectedGraphId]);

  useEffect(() => {
    if (!selectedGraphId) {
      return;
    }

    void loadRuns(selectedGraphId);
  }, [selectedGraphId, loadRuns]);

  useEffect(() => {
    if (!selectedGraphId || !selectedRunId) {
      setTimeline(null);
      return;
    }

    void loadTimeline(selectedGraphId, selectedRunId);
  }, [selectedGraphId, selectedRunId, loadTimeline]);

  const formatDate = (value?: string | null): string => {
    if (!value) {
      return 'n/a';
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return value;
    }

    return parsed.toLocaleString();
  };

  const runStatusClassName = (status: string) => {
    switch (status) {
      case 'QUEUED':
        return 'run-status run-status-queued';
      case 'RUNNING':
        return 'run-status run-status-running';
      case 'SUCCEEDED':
        return 'run-status run-status-succeeded';
      case 'FAILED':
        return 'run-status run-status-failed';
      case 'CANCELED':
        return 'run-status run-status-canceled';
      default:
        return 'run-status';
    }
  };

  const renderEventDetail = (event: RunTimelineEvent): string => {
    const details: string[] = [];

    if (event.parentNodeName) {
      details.push(`parent: ${event.parentNodeName}`);
    }

    if (event.nextTaskNames && event.nextTaskNames.length > 0) {
      details.push(`next tasks: ${event.nextTaskNames.join(', ')}`);
    }

    if (event.errorMessage) {
      details.push(`error: ${event.errorMessage}`);
    }

    return details.join(' | ');
  };

  return (
    <div className="graph-list-page">
      <AppHeader />

      <main className="list-content">
        <div className="page-header">
          <h1>Tenant Dashboard</h1>
          <button className="create-graph-button" onClick={handleCreateNew}>
            Create New Graph
          </button>
        </div>

        <section className="dashboard-section">
          <div className="section-title-row">
            <h2>Graphs</h2>
            <p>Manage graph definitions and trigger execution.</p>
          </div>
          <GraphList
            graphs={graphs}
            loading={state.isLoading}
            error={state.error}
            onLoadGraph={handleLoadGraph}
            onDeleteGraph={handleDeleteGraph}
            onSubmitForExecution={handleSubmitForExecution}
            onCreateNew={handleCreateNew}
          />
        </section>

        <section className="dashboard-section execution-section">
          <div className="section-title-row">
            <h2>Execution Observability</h2>
            <p>Track graph lifetimes, statuses, and detailed plan/task timelines.</p>
          </div>

          <div className="execution-toolbar">
            <label htmlFor="graph-selector">Selected graph</label>
            <select
              id="graph-selector"
              value={selectedGraphId}
              onChange={(event) => setSelectedGraphId(event.target.value)}
              disabled={graphs.length === 0}
            >
              {graphs.length === 0 ? (
                <option value="">No graphs available</option>
              ) : (
                graphs.map((graph) => (
                  <option key={graph.id} value={graph.id}>
                    {graph.name}
                  </option>
                ))
              )}
            </select>
            <button
              className="refresh-runs-button"
              onClick={() => {
                if (selectedGraphId) {
                  void loadRuns(selectedGraphId);
                }
              }}
              disabled={!selectedGraphId || runsLoading}
            >
              {runsLoading ? 'Refreshing...' : 'Refresh Runs'}
            </button>
          </div>

          <div className="execution-grid">
            <div className="runs-panel">
              <h3>Graph Runs{selectedGraphName ? ` (${selectedGraphName})` : ''}</h3>

              {runsError && <p className="panel-error">{runsError}</p>}
              {!runsError && runs.length === 0 && !runsLoading && (
                <p className="panel-empty">No runs found for this graph.</p>
              )}

              {runs.length > 0 && (
                <div className="runs-table-wrapper">
                  <table className="runs-table">
                    <thead>
                      <tr>
                        <th>Lifetime</th>
                        <th>Status</th>
                        <th>Created</th>
                        <th>Completed</th>
                      </tr>
                    </thead>
                    <tbody>
                      {runs.map((run) => (
                        <tr
                          key={run.lifetimeId}
                          className={run.lifetimeId === selectedRunId ? 'selected-run-row' : ''}
                          onClick={() => setSelectedRunId(run.lifetimeId)}
                        >
                          <td className="mono-cell">{run.lifetimeId}</td>
                          <td>
                            <span className={runStatusClassName(run.status)}>{run.status}</span>
                          </td>
                          <td>{formatDate(run.createdAt)}</td>
                          <td>{formatDate(run.completedAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="timeline-panel">
              <h3>Run Timeline{selectedRun ? ` (${selectedRun.lifetimeId})` : ''}</h3>

              {timelineLoading && <p className="panel-empty">Loading timeline...</p>}
              {!timelineLoading && timelineError && <p className="panel-error">{timelineError}</p>}

              {!timelineLoading && !timelineError && timeline && (
                <>
                  <div className="timeline-meta">
                    <div>
                      <span className="meta-label">Status</span>
                      <span className={runStatusClassName(timeline.status)}>{timeline.status}</span>
                    </div>
                    <div>
                      <span className="meta-label">Plans</span>
                      <span>{timeline.planExecutions}</span>
                    </div>
                    <div>
                      <span className="meta-label">Tasks</span>
                      <span>{timeline.taskExecutions}</span>
                    </div>
                    <div>
                      <span className="meta-label">Started</span>
                      <span>{formatDate(timeline.startedAt)}</span>
                    </div>
                    <div>
                      <span className="meta-label">Completed</span>
                      <span>{formatDate(timeline.completedAt)}</span>
                    </div>
                  </div>

                  {timeline.events.length === 0 ? (
                    <p className="panel-empty">No timeline events recorded yet.</p>
                  ) : (
                    <div className="events-table-wrapper">
                      <table className="events-table">
                        <thead>
                          <tr>
                            <th>Time</th>
                            <th>Type</th>
                            <th>Node</th>
                            <th>Status</th>
                            <th>Details</th>
                          </tr>
                        </thead>
                        <tbody>
                          {timeline.events.map((event) => (
                            <tr key={`${event.executionId}-${event.createdAt ?? 'na'}`}>
                              <td>{formatDate(event.createdAt ?? event.persistedAt)}</td>
                              <td>{event.eventType}</td>
                              <td>{event.nodeName}</td>
                              <td>{event.status}</td>
                              <td>{renderEventDetail(event)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </>
              )}

              {!timelineLoading && !timelineError && !timeline && (
                <p className="panel-empty">Select a run to view timeline details.</p>
              )}
            </div>
          </div>
        </section>

        <CreateGraphDialog
          isOpen={showCreateDialog}
          onClose={handleCreateCancel}
          onConfirm={handleCreateConfirm}
          loading={state.isLoading}
        />
      </main>
    </div>
  );
};

export default GraphListPage;
