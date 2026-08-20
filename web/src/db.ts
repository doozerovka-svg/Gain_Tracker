import type { Category, Exercise, ProgressConfig, SetEntry, WorkoutSession, WorkoutSessionWithSets } from './types';

const STORAGE_KEYS = {
  CATEGORIES: 'wt_categories',
  EXERCISES: 'wt_exercises',
  SESSIONS: 'wt_sessions',
  SETS: 'wt_sets',
  CONFIGS: 'wt_configs',
  ACTIVE_SESSION_ID: 'wt_active_session_id',
};

const DEFAULT_CATEGORIES: Category[] = [
  { id: 1, name: 'Грудные' },
  { id: 2, name: 'Спина' },
  { id: 3, name: 'Ноги' },
  { id: 4, name: 'Плечи' },
  { id: 5, name: 'Руки' },
  { id: 6, name: 'Пресс и кор' },
];

const DEFAULT_EXERCISES: Exercise[] = [
  // Грудные
  { id: 1, name: 'Жим штанги лежа', categoryId: 1, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  { id: 2, name: 'Жим гантелей', categoryId: 1, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  { id: 3, name: 'Брусья', categoryId: 1, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: true },
  // Спина
  { id: 4, name: 'Подтягивания', categoryId: 2, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: true },
  { id: 5, name: 'Тяга штанги в наклоне', categoryId: 2, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  { id: 6, name: 'Тяга верхнего блока', categoryId: 2, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  { id: 7, name: 'Становая тяга', categoryId: 2, defaultRestTimeSeconds: 120, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  // Ноги
  { id: 8, name: 'Приседания со штангой', categoryId: 3, defaultRestTimeSeconds: 120, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  { id: 9, name: 'Румынская тяга', categoryId: 3, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  { id: 10, name: 'Жим ногами', categoryId: 3, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  { id: 11, name: 'Выпады с гантелями', categoryId: 3, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  // Плечи
  { id: 12, name: 'Армейский жим', categoryId: 4, defaultRestTimeSeconds: 90, defaultExerciseRestTimeSeconds: 180, isBodyweight: false },
  { id: 13, name: 'Махи гантелями в стороны', categoryId: 4, defaultRestTimeSeconds: 60, defaultExerciseRestTimeSeconds: 120, isBodyweight: false },
  // Руки
  { id: 14, name: 'Сгибания на бицепс', categoryId: 5, defaultRestTimeSeconds: 60, defaultExerciseRestTimeSeconds: 120, isBodyweight: false },
  { id: 15, name: 'Французский жим', categoryId: 5, defaultRestTimeSeconds: 60, defaultExerciseRestTimeSeconds: 120, isBodyweight: false },
  { id: 16, name: 'Молотковые сгибания', categoryId: 5, defaultRestTimeSeconds: 60, defaultExerciseRestTimeSeconds: 120, isBodyweight: false },
  // Пресс
  { id: 17, name: 'Планка', categoryId: 6, defaultRestTimeSeconds: 60, defaultExerciseRestTimeSeconds: 90, isBodyweight: true },
  { id: 18, name: 'Скручивания', categoryId: 6, defaultRestTimeSeconds: 60, defaultExerciseRestTimeSeconds: 90, isBodyweight: true },
  { id: 19, name: 'Подъем ног в висе', categoryId: 6, defaultRestTimeSeconds: 60, defaultExerciseRestTimeSeconds: 90, isBodyweight: true },
];

export class AppDatabase {
  static init() {
    if (!localStorage.getItem(STORAGE_KEYS.CATEGORIES)) {
      localStorage.setItem(STORAGE_KEYS.CATEGORIES, JSON.stringify(DEFAULT_CATEGORIES));
    }
    if (!localStorage.getItem(STORAGE_KEYS.EXERCISES)) {
      localStorage.setItem(STORAGE_KEYS.EXERCISES, JSON.stringify(DEFAULT_EXERCISES));
    }
    if (!localStorage.getItem(STORAGE_KEYS.SESSIONS)) {
      // Seed sample history for demonstration (last 3 weeks)
      const now = Date.now();
      const oneDay = 24 * 60 * 60 * 1000;
      const sampleSessions: WorkoutSession[] = [
        { id: 101, date: now - 14 * oneDay, status: 'COMPLETED', notes: 'Грудь и трицепс - неделя 1' },
        { id: 102, date: now - 7 * oneDay, status: 'COMPLETED', notes: 'Грудь и трицепс - неделя 2' },
        { id: 103, date: now - 2 * oneDay, status: 'COMPLETED', notes: 'Грудь и трицепс - неделя 3' },
      ];
      const sampleSets: SetEntry[] = [
        // Week 1
        { id: 1, workoutSessionId: 101, exerciseId: 1, setNumber: 1, weightKg: 80, reps: 8, rir: 2, timestamp: now - 14 * oneDay, isCompleted: true },
        { id: 2, workoutSessionId: 101, exerciseId: 1, setNumber: 2, weightKg: 80, reps: 8, rir: 1, timestamp: now - 14 * oneDay, isCompleted: true },
        { id: 3, workoutSessionId: 101, exerciseId: 1, setNumber: 3, weightKg: 80, reps: 8, rir: 1, timestamp: now - 14 * oneDay, isCompleted: true },
        { id: 4, workoutSessionId: 101, exerciseId: 15, setNumber: 1, weightKg: 30, reps: 10, rir: 2, timestamp: now - 14 * oneDay, isCompleted: true },
        // Week 2 (+5% progression)
        { id: 5, workoutSessionId: 102, exerciseId: 1, setNumber: 1, weightKg: 85, reps: 8, rir: 2, timestamp: now - 7 * oneDay, isCompleted: true },
        { id: 6, workoutSessionId: 102, exerciseId: 1, setNumber: 2, weightKg: 85, reps: 8, rir: 1, timestamp: now - 7 * oneDay, isCompleted: true },
        { id: 7, workoutSessionId: 102, exerciseId: 1, setNumber: 3, weightKg: 85, reps: 8, rir: 0, timestamp: now - 7 * oneDay, isCompleted: true },
        { id: 8, workoutSessionId: 102, exerciseId: 15, setNumber: 1, weightKg: 32.5, reps: 10, rir: 1, timestamp: now - 7 * oneDay, isCompleted: true },
        // Week 3 (+5% progression -> 90kg)
        { id: 9, workoutSessionId: 103, exerciseId: 1, setNumber: 1, weightKg: 90, reps: 8, rir: 1, timestamp: now - 2 * oneDay, isCompleted: true },
        { id: 10, workoutSessionId: 103, exerciseId: 1, setNumber: 2, weightKg: 90, reps: 7, rir: 0, timestamp: now - 2 * oneDay, isCompleted: true },
        { id: 11, workoutSessionId: 103, exerciseId: 15, setNumber: 1, weightKg: 35, reps: 9, rir: 1, timestamp: now - 2 * oneDay, isCompleted: true },
      ];
      localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(sampleSessions));
      localStorage.setItem(STORAGE_KEYS.SETS, JSON.stringify(sampleSets));
    }
    if (!localStorage.getItem(STORAGE_KEYS.CONFIGS)) {
      const defaultConfigs: Record<number, ProgressConfig> = {};
      DEFAULT_EXERCISES.forEach((ex) => {
        defaultConfigs[ex.id] = {
          exerciseId: ex.id,
          minStepKg: ex.isBodyweight ? 1.25 : 2.5,
          progressionPercentHeavy: 0.05,
          progressionPercentModerate: 0.02,
          targetReps: 8,
        };
      });
      localStorage.setItem(STORAGE_KEYS.CONFIGS, JSON.stringify(defaultConfigs));
    }
  }

  static getCategories(): Category[] {
    this.init();
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.CATEGORIES) || '[]');
  }

  static getExercises(): Exercise[] {
    this.init();
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.EXERCISES) || '[]');
  }

  static getConfigs(): Record<number, ProgressConfig> {
    this.init();
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.CONFIGS) || '{}');
  }

  static getConfig(exerciseId: number): ProgressConfig {
    const configs = this.getConfigs();
    return configs[exerciseId] || {
      exerciseId,
      minStepKg: 2.5,
      progressionPercentHeavy: 0.05,
      progressionPercentModerate: 0.02,
      targetReps: 8,
    };
  }

  static getSessions(): WorkoutSession[] {
    this.init();
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.SESSIONS) || '[]');
  }

  static getSets(): SetEntry[] {
    this.init();
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.SETS) || '[]');
  }

  static getAllSessionsWithSets(): WorkoutSessionWithSets[] {
    const sessions = this.getSessions();
    const sets = this.getSets();
    return sessions.map((session) => ({
      session,
      sets: sets.filter((s) => s.workoutSessionId === session.id),
    }));
  }

  static getActiveSession(): WorkoutSessionWithSets | null {
    const activeIdStr = localStorage.getItem(STORAGE_KEYS.ACTIVE_SESSION_ID);
    if (!activeIdStr) {
      // Find latest draft session if any
      const sessions = this.getSessions();
      const draft = sessions.find((s) => s.status === 'DRAFT');
      if (draft) {
        localStorage.setItem(STORAGE_KEYS.ACTIVE_SESSION_ID, draft.id.toString());
        return {
          session: draft,
          sets: this.getSets().filter((s) => s.workoutSessionId === draft.id),
        };
      }
      return null;
    }
    const activeId = parseInt(activeIdStr, 10);
    const session = this.getSessions().find((s) => s.id === activeId);
    if (!session || session.status === 'COMPLETED') {
      localStorage.removeItem(STORAGE_KEYS.ACTIVE_SESSION_ID);
      return null;
    }
    return {
      session,
      sets: this.getSets().filter((s) => s.workoutSessionId === activeId),
    };
  }

  static startNewSession(date = Date.now(), notes = ''): number {
    const sessions = this.getSessions();
    const newId = sessions.length > 0 ? Math.max(...sessions.map((s) => s.id)) + 1 : 1;
    const newSession: WorkoutSession = {
      id: newId,
      date,
      status: 'DRAFT',
      notes,
    };
    sessions.push(newSession);
    localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(sessions));
    localStorage.setItem(STORAGE_KEYS.ACTIVE_SESSION_ID, newId.toString());
    return newId;
  }

  static completeSession(sessionId: number, notes?: string) {
    const sessions = this.getSessions();
    const session = sessions.find((s) => s.id === sessionId);
    if (session) {
      session.status = 'COMPLETED';
      if (notes !== undefined) session.notes = notes;
      localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(sessions));
    }
    const activeId = localStorage.getItem(STORAGE_KEYS.ACTIVE_SESSION_ID);
    if (activeId && parseInt(activeId, 10) === sessionId) {
      localStorage.removeItem(STORAGE_KEYS.ACTIVE_SESSION_ID);
    }
  }

  static deleteSession(sessionId: number) {
    let sessions = this.getSessions();
    sessions = sessions.filter((s) => s.id !== sessionId);
    localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(sessions));

    let sets = this.getSets();
    sets = sets.filter((s) => s.workoutSessionId !== sessionId);
    localStorage.setItem(STORAGE_KEYS.SETS, JSON.stringify(sets));

    const activeId = localStorage.getItem(STORAGE_KEYS.ACTIVE_SESSION_ID);
    if (activeId && parseInt(activeId, 10) === sessionId) {
      localStorage.removeItem(STORAGE_KEYS.ACTIVE_SESSION_ID);
    }
  }

  static insertSet(set: Omit<SetEntry, 'id'>): SetEntry {
    const sets = this.getSets();
    const newId = sets.length > 0 ? Math.max(...sets.map((s) => s.id)) + 1 : 1;
    const newSet: SetEntry = { ...set, id: newId };
    sets.push(newSet);
    localStorage.setItem(STORAGE_KEYS.SETS, JSON.stringify(sets));
    return newSet;
  }

  static deleteSet(setId: number) {
    let sets = this.getSets();
    sets = sets.filter((s) => s.id !== setId);
    localStorage.setItem(STORAGE_KEYS.SETS, JSON.stringify(sets));
  }

  static getLastCompletedSetForExercise(exerciseId: number, beforeDate = Date.now()): SetEntry | null {
    const sessions = this.getSessions().filter(
      (s) => s.status === 'COMPLETED' && s.date <= beforeDate
    );
    if (sessions.length === 0) return null;
    const sessionIds = new Set(sessions.map((s) => s.id));
    const sets = this.getSets().filter(
      (s) => s.exerciseId === exerciseId && s.isCompleted && sessionIds.has(s.workoutSessionId)
    );
    if (sets.length === 0) return null;
    sets.sort((a, b) => b.timestamp - a.timestamp);
    return sets[0];
  }

  static cloneSession(sourceSessionId: number, targetDate: number): number {
    const sourceSession = this.getSessions().find((s) => s.id === sourceSessionId);
    if (!sourceSession) throw new Error('Исходная сессия не найдена');

    const sourceSets = this.getSets().filter((s) => s.workoutSessionId === sourceSessionId);
    const newSessionId = this.startNewSession(targetDate, sourceSession.notes ? `Копия: ${sourceSession.notes}` : '');

    // Clone structure with last completed weights if available
    sourceSets.forEach((srcSet) => {
      const lastSet = this.getLastCompletedSetForExercise(srcSet.exerciseId, targetDate);
      this.insertSet({
        workoutSessionId: newSessionId,
        exerciseId: srcSet.exerciseId,
        setNumber: srcSet.setNumber,
        weightKg: lastSet ? lastSet.weightKg : srcSet.weightKg,
        reps: lastSet ? lastSet.reps : srcSet.reps,
        rir: 2,
        timestamp: targetDate,
        isCompleted: false,
      });
    });

    return newSessionId;
  }
}
