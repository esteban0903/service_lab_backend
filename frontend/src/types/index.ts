export type TaskStatus = 'NOT_DONE' | 'DONE';

export interface TaskResponseDto {
  id: number;
  name: string;
  effortHours: number;
  status: TaskStatus;
  skills: string[];
}

export interface TaskRequestDto {
  name: string;
  effortHours: number;
  status?: TaskStatus;
  skills: string[];
}

export interface WorkerResponseDto {
  id: number;
  name: string;
  hoursPerDay: number;
  skills: string[];
}

export interface WorkerRequestDto {
  name: string;
  hoursPerDay: number;
  skills: string[];
}

export interface DailyAllocationDto {
  date: string;
  hours: number;
}

export interface TaskSummaryDto {
  id: number;
  name: string;
}

export interface WorkerSummaryDto {
  id: number;
  name: string;
}

export interface AssignmentResponseDto {
  id: number;
  task: TaskSummaryDto;
  worker: WorkerSummaryDto;
  startDate: string;
  endDate: string;
  allocations: DailyAllocationDto[];
}

export interface AssignmentRequestDto {
  taskId: number;
  workerId: number;
  startDate: string;
}

export interface AutoAssignmentRequestDto {
  startDate?: string;
}

export interface UnassignedTaskDto {
  task: TaskSummaryDto;
  reason: string;
}

export interface AutoAssignmentResultDto {
  assigned: AssignmentResponseDto[];
  unassigned: UnassignedTaskDto[];
}

export interface SeedSummaryDto {
  workersCreated: number;
  tasksCreated: number;
  assignmentsCreated: number;
  demoStartDate: string;
}

export interface ErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
}



export interface AppConfigDto {
  seedEnabled: boolean;
}
