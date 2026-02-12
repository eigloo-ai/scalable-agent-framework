import React, { useState, useMemo } from 'react';
import type { AgentGraphSummary, GraphStatus } from '../../types';
import './GraphList.css';

interface GraphListProps {
  graphs: AgentGraphSummary[];
  loading: boolean;
  error: string | null;
  onLoadGraph: (graphId: string) => void;
  onDeleteGraph: (graphId: string) => void;
  onSubmitForExecution: (graphId: string) => void;
  onCreateNew: () => void;
}

type SortField = 'name' | 'createdAt' | 'updatedAt' | 'status';
type SortDirection = 'asc' | 'desc';

const GraphList: React.FC<GraphListProps> = ({
  graphs,
  loading,
  error,
  onLoadGraph,
  onDeleteGraph,
  onSubmitForExecution,
  onCreateNew,
}) => {
  const [sortField, setSortField] = useState<SortField>('updatedAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [statusFilter, setStatusFilter] = useState<GraphStatus | 'ALL'>('ALL');
  const [nameFilter, setNameFilter] = useState('');
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  const filteredAndSortedGraphs = useMemo(() => {
    let filtered = graphs;

    // Apply status filter
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(graph => graph.status === statusFilter);
    }

    // Apply name filter
    if (nameFilter.trim()) {
      filtered = filtered.filter(graph =>
        graph.name.toLowerCase().includes(nameFilter.toLowerCase())
      );
    }

    // Apply sorting
    return filtered.sort((a, b) => {
      let aValue: string | Date;
      let bValue: string | Date;

      switch (sortField) {
        case 'name':
          aValue = a.name;
          bValue = b.name;
          break;
        case 'createdAt':
          aValue = new Date(a.createdAt);
          bValue = new Date(b.createdAt);
          break;
        case 'updatedAt':
          aValue = new Date(a.updatedAt);
          bValue = new Date(b.updatedAt);
          break;
        case 'status':
          aValue = a.status;
          bValue = b.status;
          break;
        default:
          return 0;
      }

      if (aValue < bValue) {
        return sortDirection === 'asc' ? -1 : 1;
      }
      if (aValue > bValue) {
        return sortDirection === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }, [graphs, statusFilter, nameFilter, sortField, sortDirection]);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const getStatusBadgeClass = (status: GraphStatus) => {
    switch (status) {
      case 'NEW':
        return 'status-badge status-new';
      case 'ACTIVE':
        return 'status-badge status-active';
      case 'ARCHIVED':
        return 'status-badge status-archived';
      default:
        return 'status-badge';
    }
  };

  const handleDeleteClick = (graphId: string) => {
    setDeleteConfirmId(graphId);
  };

  const handleDeleteConfirm = () => {
    if (deleteConfirmId) {
      onDeleteGraph(deleteConfirmId);
      setDeleteConfirmId(null);
    }
  };

  const handleDeleteCancel = () => {
    setDeleteConfirmId(null);
  };

  if (loading) {
    return (
      <div className="graph-list">
        <div className="loading-state">
          <div className="loading-spinner"></div>
          <p>Loading graphs...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="graph-list">
        <div className="error-state">
          <p className="error-message">Error: {error}</p>
          <button onClick={onCreateNew} className="create-button">
            Create New Graph
          </button>
        </div>
      </div>
    );
  }

  if (graphs.length === 0) {
    return (
      <div className="graph-list">
        <div className="empty-state">
          <h3>No graphs found</h3>
          <p>Get started by creating your first agent graph.</p>
          <button onClick={onCreateNew} className="create-button">
            Create New Graph
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="graph-list">
      <div className="list-header">
        <div className="filters">
          <div className="filter-group">
            <label htmlFor="name-filter">Filter by name:</label>
            <input
              id="name-filter"
              type="text"
              value={nameFilter}
              onChange={(e) => setNameFilter(e.target.value)}
              placeholder="Search graphs..."
              className="filter-input"
            />
          </div>
          <div className="filter-group">
            <label htmlFor="status-filter">Filter by status:</label>
            <select
              id="status-filter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as GraphStatus | 'ALL')}
              className="filter-select"
            >
              <option value="ALL">All Statuses</option>
              <option value="NEW">New</option>
              <option value="ACTIVE">Active</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </div>
        </div>
        <button onClick={onCreateNew} className="create-button">
          Create New Graph
        </button>
      </div>

      <div className="table-container">
        <table className="graphs-table">
          <thead>
            <tr>
              <th 
                className={`sortable ${sortField === 'name' ? `sorted-${sortDirection}` : ''}`}
                onClick={() => handleSort('name')}
              >
                Name
              </th>
              <th 
                className={`sortable ${sortField === 'status' ? `sorted-${sortDirection}` : ''}`}
                onClick={() => handleSort('status')}
              >
                Status
              </th>
              <th 
                className={`sortable ${sortField === 'createdAt' ? `sorted-${sortDirection}` : ''}`}
                onClick={() => handleSort('createdAt')}
              >
                Created
              </th>
              <th 
                className={`sortable ${sortField === 'updatedAt' ? `sorted-${sortDirection}` : ''}`}
                onClick={() => handleSort('updatedAt')}
              >
                Last Modified
              </th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredAndSortedGraphs.map((graph) => (
              <tr key={graph.id}>
                <td className="graph-name">{graph.name}</td>
                <td>
                  <span className={getStatusBadgeClass(graph.status)}>
                    {graph.status}
                  </span>
                </td>
                <td>{formatDate(graph.createdAt)}</td>
                <td>{formatDate(graph.updatedAt)}</td>
                <td className="actions-cell">
                  <div className="action-buttons">
                    <button
                      onClick={() => onLoadGraph(graph.id)}
                      className="action-button load-button"
                      title="Load graph in editor"
                    >
                      Load
                    </button>
                    <button
                      onClick={() => onSubmitForExecution(graph.id)}
                      className="action-button execute-button"
                      title="Submit for execution"
                      disabled={graph.status === 'ARCHIVED'}
                    >
                      Execute
                    </button>
                    <button
                      onClick={() => handleDeleteClick(graph.id)}
                      className="action-button delete-button"
                      title="Delete graph"
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

      {deleteConfirmId && (
        <div className="modal-overlay">
          <div className="confirmation-modal">
            <h3>Confirm Delete</h3>
            <p>
              Are you sure you want to delete this graph? This action cannot be undone.
            </p>
            <div className="modal-actions">
              <button onClick={handleDeleteCancel} className="cancel-button">
                Cancel
              </button>
              <button onClick={handleDeleteConfirm} className="confirm-delete-button">
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default GraphList;
