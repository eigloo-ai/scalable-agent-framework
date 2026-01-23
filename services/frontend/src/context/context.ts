import { createContext } from 'react';
import type { AgentGraphDto, ExecutorFile } from '../types';

export interface AppState {
  tenantId: string;
  currentGraph: AgentGraphDto | null;
  selectedNodeId: string | null;
  selectedFile: ExecutorFile | null;
  selectedTool: 'select' | 'task' | 'plan' | 'edge' | 'eraser' | null;
  isLoading: boolean;
  error: string | null;
}

export type AppAction =
  | { type: 'SET_TENANT_ID'; payload: string }
  | { type: 'SET_CURRENT_GRAPH'; payload: AgentGraphDto | null }
  | { type: 'SET_SELECTED_NODE'; payload: string | null }
  | { type: 'SET_SELECTED_FILE'; payload: ExecutorFile | null }
  | { type: 'SET_SELECTED_TOOL'; payload: 'select' | 'task' | 'plan' | 'edge' | 'eraser' | null }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'CLEAR_ERROR' }
  | { type: 'UPDATE_FILE_CONTENT'; payload: { fileName: string; content: string } };

interface AppContextType {
  state: AppState;
  dispatch: React.Dispatch<AppAction>;
}

export const AppContext = createContext<AppContextType | undefined>(undefined);