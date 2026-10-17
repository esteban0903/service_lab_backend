import type {
  AppConfigDto,
  AssignmentRequestDto,
  AssignmentResponseDto,
  AutoAssignmentRequestDto,
  AutoAssignmentResultDto,
  ErrorResponse,
  SeedSummaryDto,
  TaskRequestDto,
  TaskResponseDto,
  WorkerRequestDto,
  WorkerResponseDto,
} from '../types';

const BASE_URL = import.meta.env.VITE_API_URL || '';

export class ApiError extends Error {
  public status: number;
  public error: string;

  constructor(status: number, error: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.error = error;
  }
}

async function request<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const url = `${BASE_URL}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    ...(options?.headers || {}),
  };

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');

  if (!response.ok) {
    let errorMessage = `HTTP error ${response.status}`;
    let errorTitle = response.statusText;

    if (isJson) {
      try {
        const errorData: ErrorResponse = await response.json();
        errorMessage = errorData.message || errorMessage;
        errorTitle = errorData.error || errorTitle;
      } catch {
        // Fallback to default message
      }
    } else {
      const text = await response.text();
      if (text) {
        errorMessage = text;
      }
    }

    throw new ApiError(response.status, errorTitle, errorMessage);
  }

  return isJson ? await response.json() : ({} as T);
}

export const api = {
  // Tasks
  getTasks: () => request<TaskResponseDto[]>('/api/tasks'),
  getTask: (id: number) => request<TaskResponseDto>(`/api/tasks/${id}`),
  createTask: (data: TaskRequestDto) =>
    request<TaskResponseDto>('/api/tasks', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  updateTask: (id: number, data: TaskRequestDto) =>
    request<TaskResponseDto>(`/api/tasks/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  deleteTask: (id: number) =>
    request<void>(`/api/tasks/${id}`, {
      method: 'DELETE',
    }),

  // Workers
  getWorkers: () => request<WorkerResponseDto[]>('/api/workers'),
  getWorker: (id: number) => request<WorkerResponseDto>(`/api/workers/${id}`),
  createWorker: (data: WorkerRequestDto) =>
    request<WorkerResponseDto>('/api/workers', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  updateWorker: (id: number, data: WorkerRequestDto) =>
    request<WorkerResponseDto>(`/api/workers/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  deleteWorker: (id: number) =>
    request<void>(`/api/workers/${id}`, {
      method: 'DELETE',
    }),

  // Assignments
  getAssignments: () => request<AssignmentResponseDto[]>('/api/assignments'),
  createAssignment: (data: AssignmentRequestDto) =>
    request<AssignmentResponseDto>('/api/assignments', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  deleteAssignment: (id: number) =>
    request<void>(`/api/assignments/${id}`, {
      method: 'DELETE',
    }),
  autoAssign: (data: AutoAssignmentRequestDto) =>
    request<AutoAssignmentResultDto>('/api/assignments/auto', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Seed
  seed: () => request<SeedSummaryDto>('/api/seed', { method: 'POST' }),

  // Config
  getConfig: () => request<AppConfigDto>('/api/config'),
};
