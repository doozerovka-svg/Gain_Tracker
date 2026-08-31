export interface Category {
  id: number;
  name: string;
}

export interface Exercise {
  id: number;
  name: string;
  categoryId: number;
  defaultRestTimeSeconds: number;
  defaultExerciseRestTimeSeconds: number;
  isBodyweight: boolean;
}

export type WorkoutStatus = 'DRAFT' | 'COMPLETED';

export interface WorkoutSession {
  id: number;
  date: number; // unix timestamp ms
  status: WorkoutStatus;
  notes: string;
}

export type SetType = 'NORMAL' | 'WARMUP' | 'DROP_SET' | 'FAILURE';

export interface SetEntry {
  id: number;
  workoutSessionId: number;
  exerciseId: number;
  setNumber: number;
  weightKg: number;
  reps: number;
  rir: number; // 0..5
  setType?: SetType;
  superSetId?: number | null;
  timestamp: number;
  isCompleted: boolean;
}

export interface WorkoutSessionWithSets {
  session: WorkoutSession;
  sets: SetEntry[];
}

export interface BodyMeasurement {
  id: number;
  date: number;
  weightKg?: number;
  bodyFatPercentage?: number;
  chestCm?: number;
  waistCm?: number;
  bicepsCm?: number;
  thighsCm?: number;
  calvesCm?: number;
  neckCm?: number;
  notes?: string;
}

export interface ProgressConfig {
  exerciseId: number;
  minStepKg: number; // default 2.5 or 1.25
  progressionPercentHeavy: number; // 0.05
  progressionPercentModerate: number; // 0.02
  targetReps: number; // 8
}

export interface ProgressionResult {
  recommendedWeightKg: number;
  recommendedReps: number;
  deltaApplied: number;
  explanationRu: string;
}

export type TabType = 'active' | 'calendar' | 'history' | 'analytics' | 'body' | 'export';

