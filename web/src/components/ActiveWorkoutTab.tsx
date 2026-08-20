import React, { useState, useEffect, useMemo } from 'react';
import { AppDatabase } from '../db';
import type { Category, Exercise, ProgressConfig, WorkoutSessionWithSets } from '../types';
import { ProgressionEngine } from '../progression';
import { AudioNotificationService } from '../sound';
import { 
  Play, CheckCircle2, PlusCircle, 
  Sparkles, Pause, SkipForward, Trash2, Timer, Zap,
  Plus, Search, X
} from 'lucide-react';
import confetti from 'canvas-confetti';

interface Props {
  onRefresh: () => void;
}

type SortMode = 'BY_CATEGORY' | 'ALPHABETICAL' | 'RECENT';

export const ActiveWorkoutTab: React.FC<Props> = ({ onRefresh }) => {
  const [categories] = useState<Category[]>(() => AppDatabase.getCategories());
  const [exercises, setExercises] = useState<Exercise[]>(() => AppDatabase.getExercises());
  const [selectedExerciseId, setSelectedExerciseId] = useState<number>(() => {
    const active = AppDatabase.getActiveSession();
    const lastSet = active?.sets && active.sets.length > 0 ? active.sets[active.sets.length - 1] : null;
    return lastSet ? lastSet.exerciseId : (exercises[0]?.id || 1);
  });
  const [activeSession, setActiveSession] = useState<WorkoutSessionWithSets | null>(() => AppDatabase.getActiveSession());
  
  // Exercise filtering & sorting
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [sortMode, setSortMode] = useState<SortMode>('BY_CATEGORY');
  const [isExercisePickerOpen, setIsExercisePickerOpen] = useState<boolean>(false);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState<boolean>(false);

  // New exercise form
  const [newExName, setNewExName] = useState<string>('');
  const [newExCategoryId, setNewExCategoryId] = useState<number>(1);
  const [newExIsBodyweight, setNewExIsBodyweight] = useState<boolean>(false);
  const [newExRestSeconds, setNewExRestSeconds] = useState<number>(90);
  const [newExStepKg, setNewExStepKg] = useState<number>(2.5);
  const [newExTargetReps, setNewExTargetReps] = useState<number>(8);

  // Set inputs
  const [weightKg, setWeightKg] = useState<number>(80);
  const [reps, setReps] = useState<number>(8);
  const [rir, setRir] = useState<number>(2);

  // Rest timer
  const [isTimerRunning, setIsTimerRunning] = useState(false);
  const [isTimerPaused, setIsTimerPaused] = useState(false);
  const [timerSecondsLeft, setTimerSecondsLeft] = useState(90);

  // Status message
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const selectedExercise = useMemo(
    () => exercises.find((e) => e.id === selectedExerciseId) || exercises[0],
    [exercises, selectedExerciseId]
  );

  // Filtered & sorted exercises
  const filteredExercises = useMemo(() => {
    let list = [...exercises];
    if (selectedCategoryId !== null) {
      list = list.filter((e) => e.categoryId === selectedCategoryId);
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      list = list.filter((e) => e.name.toLowerCase().includes(q));
    }
    switch (sortMode) {
      case 'ALPHABETICAL':
        return list.sort((a, b) => a.name.localeCompare(b.name, 'ru'));
      case 'BY_CATEGORY':
        return list.sort((a, b) => a.categoryId - b.categoryId || a.name.localeCompare(b.name, 'ru'));
      case 'RECENT':
        return list;
    }
  }, [exercises, selectedCategoryId, searchQuery, sortMode]);

  // Calculate Progression recommendation
  const progressionResult = useMemo(() => {
    const lastSet = AppDatabase.getLastCompletedSetForExercise(selectedExerciseId);
    if (lastSet) {
      const config: ProgressConfig = AppDatabase.getConfig(selectedExerciseId);
      return ProgressionEngine.calculateProgression(lastSet.weightKg, lastSet.reps, lastSet.rir, config);
    }
    return null;
  }, [selectedExerciseId, activeSession]);

  // When exercise explicitly changes, update inputs with previous values or progression
  useEffect(() => {
    const lastSet = AppDatabase.getLastCompletedSetForExercise(selectedExerciseId);
    const ex = exercises.find((e) => e.id === selectedExerciseId);
    if (lastSet) {
      const config = AppDatabase.getConfig(selectedExerciseId);
      const prog = ProgressionEngine.calculateProgression(lastSet.weightKg, lastSet.reps, lastSet.rir, config);
      setWeightKg(prog ? prog.recommendedWeightKg : lastSet.weightKg);
      setReps(prog ? prog.recommendedReps : lastSet.reps);
      setRir(lastSet.rir || 2);
    } else {
      setWeightKg(ex?.isBodyweight ? 0 : 50);
      setReps(8);
      setRir(2);
    }
  }, [selectedExerciseId, exercises]);

  // Timer countdown ticker
  useEffect(() => {
    let interval: number | null = null;
    if (isTimerRunning && !isTimerPaused && timerSecondsLeft > 0) {
      interval = window.setInterval(() => {
        setTimerSecondsLeft((prev) => {
          if (prev <= 1) {
            setIsTimerRunning(false);
            AudioNotificationService.showTimerNotification(
              'Время отдыха вышло!',
              `Пора начинать следующий подход (${selectedExercise?.name || 'упражнение'})`
            );
            return 0;
          }
          if (prev === 4 || prev === 3) {
            AudioNotificationService.playBeep(440, 0.1, 1);
          } else if (prev === 2) {
            AudioNotificationService.playBeep(880, 0.15, 1);
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isTimerRunning, isTimerPaused, timerSecondsLeft, selectedExercise]);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleStartWorkout = () => {
    AudioNotificationService.requestNotificationPermission();
    AppDatabase.startNewSession(Date.now(), 'Силовая тренировка');
    setActiveSession(AppDatabase.getActiveSession());
    onRefresh();
    showToast('Тренировка начата!');
  };

  const handleIncrementWeight = (delta: number) => {
    const next = Math.max(0, Math.round((weightKg + delta) * 10) / 10);
    setWeightKg(next);
  };

  const handleCreateExercise = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newExName.trim()) return;

    const created = AppDatabase.insertExercise(
      {
        name: newExName.trim(),
        categoryId: newExCategoryId,
        defaultRestTimeSeconds: newExRestSeconds,
        defaultExerciseRestTimeSeconds: 180,
        isBodyweight: newExIsBodyweight,
      },
      {
        minStepKg: newExIsBodyweight ? 1.25 : newExStepKg,
        targetReps: newExTargetReps,
      }
    );

    const updatedList = AppDatabase.getExercises();
    setExercises(updatedList);
    setSelectedExerciseId(created.id);
    setIsCreateDialogOpen(false);
    setIsExercisePickerOpen(false);
    setNewExName('');
    showToast(`Упражнение «${created.name}» создано!`);
  };

  const handleSaveSet = () => {
    AudioNotificationService.requestNotificationPermission();
    let sessionId = activeSession?.session.id;
    if (!sessionId) {
      sessionId = AppDatabase.startNewSession();
    }

    const currentSets = activeSession?.sets.filter((s) => s.exerciseId === selectedExerciseId) || [];
    const setNumber = currentSets.length + 1;

    AppDatabase.insertSet({
      workoutSessionId: sessionId,
      exerciseId: selectedExerciseId,
      setNumber,
      weightKg,
      reps,
      rir,
      timestamp: Date.now(),
      isCompleted: true,
    });

    AudioNotificationService.playSuccess();
    setActiveSession(AppDatabase.getActiveSession());
    onRefresh();

    // Start auto rest timer
    const restTime = selectedExercise?.defaultRestTimeSeconds || 90;
    setTimerSecondsLeft(restTime);
    setIsTimerRunning(true);
    setIsTimerPaused(false);

    showToast(`Подход #${setNumber} зафиксирован!`);
  };

  const handleDeleteSet = (setId: number) => {
    AppDatabase.deleteSet(setId);
    setActiveSession(AppDatabase.getActiveSession());
    onRefresh();
    showToast('Подход удален');
  };

  const handleCompleteWorkout = () => {
    if (!activeSession) return;
    AppDatabase.completeSession(activeSession.session.id);
    setActiveSession(null);
    setIsTimerRunning(false);
    onRefresh();
    confetti({
      particleCount: 120,
      spread: 70,
      origin: { y: 0.6 },
    });
    showToast('🎉 Тренировка успешно завершена!');
  };

  const currentExerciseSets = activeSession?.sets.filter((s) => s.exerciseId === selectedExerciseId) || [];
  const totalVolume = activeSession?.sets.reduce((sum, s) => sum + s.weightKg * s.reps, 0) || 0;
  const totalSets = activeSession?.sets.length || 0;

  // Group all completed sets of the active workout by exercise
  const groupedWorkoutSets = useMemo(() => {
    if (!activeSession || !activeSession.sets) return [];
    const map = new Map<number, typeof activeSession.sets>();
    activeSession.sets.forEach((s) => {
      const list = map.get(s.exerciseId) || [];
      list.push(s);
      map.set(s.exerciseId, list);
    });
    return Array.from(map.entries()).map(([exId, exSets]) => {
      const ex = exercises.find((e) => e.id === exId);
      const volume = exSets.reduce((sum, s) => sum + s.weightKg * s.reps, 0);
      const maxWeight = Math.max(...exSets.map((s) => s.weightKg), 0);
      return {
        exerciseId: exId,
        exerciseName: ex?.name || `Упражнение #${exId}`,
        isBodyweight: ex?.isBodyweight || false,
        sets: exSets,
        volume,
        maxWeight,
        isActive: exId === selectedExerciseId,
      };
    });
  }, [activeSession, exercises, selectedExerciseId]);

  const rirOptions = [
    { value: 0, label: '0', desc: 'Отказ', color: 'bg-red-500/20 text-red-300 border-red-500/40' },
    { value: 1, label: '1', desc: 'Предел', color: 'bg-orange-500/20 text-orange-300 border-orange-500/40' },
    { value: 2, label: '2', desc: 'Рабочий', color: 'bg-yellow-500/20 text-yellow-300 border-yellow-500/40' },
    { value: 3, label: '3', desc: 'Запас', color: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40' },
    { value: 4, label: '4', desc: 'Легко', color: 'bg-blue-500/20 text-blue-300 border-blue-500/40' },
    { value: 5, label: '5+', desc: 'Разминка', color: 'bg-indigo-500/20 text-indigo-300 border-indigo-500/40' },
  ];

  return (
    <div className="space-y-2.5 max-w-xl mx-auto pb-16">
      {/* Toast Alert */}
      {toastMessage && (
        <div className="fixed top-14 left-1/2 -translate-x-1/2 z-50 bg-blue-600 text-white px-4 py-1.5 rounded-full shadow-xl text-xs font-semibold animate-bounce">
          {toastMessage}
        </div>
      )}

      {/* 1. Compact Header Bar (Status + Timer + Finish) */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-2.5 flex items-center justify-between shadow-sm">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <div className="text-xs">
            <span className="text-slate-400">Сетов: </span>
            <b className="text-white">{totalSets}</b>
            <span className="text-slate-400 ml-2">Объём: </span>
            <b className="text-white">{totalVolume.toFixed(0)} кг</b>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {isTimerRunning && (
            <div className="flex items-center gap-1 bg-blue-950/80 border border-blue-500/50 px-2 py-0.5 rounded-lg text-blue-300 text-xs font-mono font-bold">
              <Timer size={13} className="text-blue-400 animate-spin" />
              <span>{Math.floor(timerSecondsLeft / 60)}:{(timerSecondsLeft % 60).toString().padStart(2, '0')}</span>
              <button
                onClick={() => setIsTimerPaused(!isTimerPaused)}
                className="hover:text-white ml-0.5 p-0.5"
              >
                {isTimerPaused ? <Play size={11} /> : <Pause size={11} />}
              </button>
              <button
                onClick={() => setIsTimerRunning(false)}
                className="hover:text-red-400 p-0.5"
              >
                <SkipForward size={11} />
              </button>
            </div>
          )}

          {activeSession ? (
            <button
              onClick={handleCompleteWorkout}
              className="touch-target h-8 px-3 text-xs bg-emerald-600 hover:bg-emerald-500 text-white font-semibold rounded-lg flex items-center gap-1 shadow transition"
            >
              <CheckCircle2 size={14} />
              Завершить
            </button>
          ) : (
            <button
              onClick={handleStartWorkout}
              className="touch-target h-8 px-3 text-xs bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-lg flex items-center gap-1 shadow transition"
            >
              <Play size={14} />
              Старт
            </button>
          )}
        </div>
      </div>

      {/* 2. Compact Exercise Selector */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-2.5 flex items-center gap-2 shadow-sm">
        <button
          onClick={() => setIsExercisePickerOpen(true)}
          className="flex-1 min-w-0 bg-slate-950 hover:bg-slate-800 border border-slate-700 rounded-lg px-2.5 py-2 text-white text-xs font-semibold flex items-center justify-between text-left transition"
        >
          <span className="truncate mr-1 font-bold">
            {selectedExercise?.name} {selectedExercise?.isBodyweight ? '(Свой вес)' : ''}
          </span>
          <span className="text-[11px] text-blue-400 font-bold shrink-0">Сменить</span>
        </button>

        <button
          onClick={() => setIsCreateDialogOpen(true)}
          className="touch-target h-9 px-3 bg-blue-600/20 hover:bg-blue-600 text-blue-300 hover:text-white border border-blue-500/40 rounded-lg text-xs font-bold flex items-center gap-1 shrink-0 transition"
        >
          <Plus size={14} />
          <span>Новое</span>
        </button>
      </div>

      {/* Adaptive Progression Recommendation Card (No Truncation / Full Responsiveness) */}
      {progressionResult && (
        <div className="bg-gradient-to-br from-indigo-950/70 via-slate-900 to-blue-950/60 border border-indigo-500/40 rounded-xl p-3 space-y-2 shadow-md">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-1.5 text-indigo-400 font-bold text-xs">
              <Sparkles size={14} className="text-indigo-400 shrink-0" />
              <span>Рекомендация нагрузки (No-AI)</span>
            </div>
            <div className="bg-indigo-500/20 border border-indigo-400/40 px-2.5 py-1 rounded-lg text-xs font-black text-indigo-200 flex items-center gap-1.5 shadow-sm">
              <Zap size={13} className="text-indigo-400" />
              <span>Цель: {progressionResult.recommendedWeightKg} кг × {progressionResult.recommendedReps}</span>
            </div>
          </div>

          <p className="text-xs sm:text-sm text-indigo-100/90 leading-relaxed break-words whitespace-normal">
            {progressionResult.explanationRu}
          </p>
        </div>
      )}

      {/* 3. Two-Column Input Grid (Weight on Left + Reps on Right) */}
      <div className="grid grid-cols-2 gap-2">
        {/* Weight Column */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-2.5 space-y-1.5 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between text-[11px] font-bold text-slate-400 uppercase tracking-wider">
            <span>Вес</span>
            <span className="text-white font-extrabold text-xs">{weightKg} кг</span>
          </div>

          {/* Stepper */}
          <div className="flex items-center justify-between gap-1 bg-slate-950 border border-slate-700 rounded-lg p-1">
            <button
              onClick={() => handleIncrementWeight(-2.5)}
              className="touch-target w-9 h-9 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-md flex items-center justify-center transition"
            >
              -2.5
            </button>
            <span className="font-black text-base text-white tracking-tight">{weightKg}</span>
            <button
              onClick={() => handleIncrementWeight(2.5)}
              className="touch-target w-9 h-9 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-md flex items-center justify-center transition"
            >
              +2.5
            </button>
          </div>

          {/* Quick Increments */}
          <div className="grid grid-cols-4 gap-1 pt-1">
            {[1, 2.5, 5, 10].map((inc) => (
              <button
                key={inc}
                onClick={() => handleIncrementWeight(inc)}
                className="touch-target h-7 bg-slate-800 hover:bg-blue-600 hover:text-white border border-slate-700/60 rounded-md text-[11px] font-bold text-slate-300 transition"
              >
                +{inc}
              </button>
            ))}
          </div>
        </div>

        {/* Reps Column */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-2.5 space-y-1.5 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between text-[11px] font-bold text-slate-400 uppercase tracking-wider">
            <span>Повторения</span>
            <span className="text-white font-extrabold text-xs">{reps}</span>
          </div>

          {/* Stepper */}
          <div className="flex items-center justify-between gap-1 bg-slate-950 border border-slate-700 rounded-lg p-1">
            <button
              onClick={() => setReps(Math.max(1, reps - 1))}
              className="touch-target w-9 h-9 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-md flex items-center justify-center transition"
            >
              -1
            </button>
            <span className="font-black text-base text-white tracking-tight">{reps}</span>
            <button
              onClick={() => setReps(reps + 1)}
              className="touch-target w-9 h-9 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-md flex items-center justify-center transition"
            >
              +1
            </button>
          </div>

          {/* Quick Reps */}
          <div className="grid grid-cols-4 gap-1 pt-1">
            {[6, 8, 10, 12].map((r) => (
              <button
                key={r}
                onClick={() => setReps(r)}
                className={`touch-target h-7 rounded-md text-[11px] font-bold transition ${
                  reps === r
                    ? 'bg-blue-600 text-white'
                    : 'bg-slate-800 text-slate-300 hover:bg-slate-700 border border-slate-700/60'
                }`}
              >
                {r}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 4. Horizontal RIR Segment Selector (0..5+) */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-2 space-y-1.5 shadow-sm">
        <div className="flex items-center justify-between text-[11px] px-1 font-bold text-slate-400 uppercase">
          <span>RIR (запас сил)</span>
          <span className="text-xs text-blue-400 font-extrabold">
            {rir === 0 ? '0 (Отказ)' : rir === 1 ? '1 (Предел)' : rir === 2 ? '2 (Рабочий)' : rir === 3 ? '3 (Запас)' : rir === 4 ? '4 (Легко)' : '5+ (Разминка)'}
          </span>
        </div>

        <div className="grid grid-cols-6 gap-1">
          {rirOptions.map((opt) => (
            <button
              key={opt.value}
              onClick={() => setRir(opt.value)}
              className={`touch-target h-9 rounded-lg border flex flex-col items-center justify-center transition ${
                rir === opt.value
                  ? `${opt.color} font-black ring-2 ring-blue-400 scale-[1.02]`
                  : 'bg-slate-950/80 border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
            >
              <span className="text-xs font-black leading-none">{opt.label}</span>
              <span className="text-[8px] font-medium leading-tight mt-0.5 opacity-80">{opt.desc}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 5. Main Action Button (>=48px touch target) */}
      <button
        onClick={handleSaveSet}
        className="touch-target w-full h-12 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl shadow-lg shadow-blue-600/30 flex items-center justify-center gap-2 transition active:scale-[0.99]"
      >
        <PlusCircle size={18} />
        <span>Зафиксировать подход #{currentExerciseSets.length + 1}</span>
      </button>

      {/* 6. Completed Sets Grouped by Exercise */}
      {groupedWorkoutSets.length > 0 && (
        <div className="space-y-2.5 pt-1">
          <div className="flex items-center justify-between text-xs px-1">
            <span className="font-bold text-slate-300 uppercase tracking-wider">
              Упражнения в тренировке ({groupedWorkoutSets.length})
            </span>
            <span className="text-slate-400 font-medium">
              Всего подходов: {totalSets}
            </span>
          </div>

          <div className="space-y-2">
            {groupedWorkoutSets.map((group) => (
              <div
                key={group.exerciseId}
                className={`border rounded-xl p-3 space-y-2 transition shadow-sm ${
                  group.isActive
                    ? 'bg-slate-900/95 border-blue-500/60 ring-1 ring-blue-500/30'
                    : 'bg-slate-900/70 border-slate-800 hover:border-slate-700'
                }`}
              >
                {/* Exercise Header */}
                <div className="flex items-center justify-between gap-2">
                  <div
                    className="flex items-center gap-2 cursor-pointer min-w-0"
                    onClick={() => setSelectedExerciseId(group.exerciseId)}
                  >
                    <span className={`w-2 h-2 rounded-full shrink-0 ${group.isActive ? 'bg-blue-400 ring-2 ring-blue-400/40' : 'bg-slate-600'}`} />
                    <span className="font-bold text-white text-xs sm:text-sm truncate">
                      {group.exerciseName}
                    </span>
                    {group.isActive && (
                      <span className="px-1.5 py-0.5 bg-blue-600/30 text-blue-300 border border-blue-500/40 text-[10px] font-bold rounded-md shrink-0">
                        Выбрано
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2 text-[11px] text-slate-400 shrink-0">
                    <span>{group.sets.length} подх.</span>
                    <span>•</span>
                    <span className="text-slate-300 font-semibold">{group.volume.toFixed(0)} кг</span>
                    {!group.isActive && (
                      <button
                        onClick={() => setSelectedExerciseId(group.exerciseId)}
                        className="px-2 py-0.5 bg-slate-800 hover:bg-blue-600 hover:text-white text-blue-400 border border-slate-700 rounded-md text-[10px] font-bold transition ml-1"
                      >
                        Выбрать
                      </button>
                    )}
                  </div>
                </div>

                {/* Sets Grid / Chips */}
                <div className="flex flex-wrap gap-1.5 pt-0.5">
                  {group.sets.map((set) => (
                    <div
                      key={set.id}
                      className="bg-slate-950 border border-slate-800/90 px-2.5 py-1 rounded-lg flex items-center gap-2 text-xs"
                    >
                      <span className="font-extrabold text-blue-400">#{set.setNumber}</span>
                      <span className="font-bold text-white">{set.weightKg} кг × {set.reps}</span>
                      <span className="text-[10px] px-1.5 py-0.5 bg-slate-900 border border-slate-700 text-slate-300 rounded font-medium">
                        RIR {set.rir}
                      </span>
                      <button
                        onClick={() => handleDeleteSet(set.id)}
                        className="text-slate-500 hover:text-red-400 p-0.5"
                        title="Удалить подход"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ================= MODAL: EXERCISE PICKER & MUSCLE SORT ================= */}
      {isExercisePickerOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-3 animate-fade-in">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md max-h-[85vh] flex flex-col shadow-2xl overflow-hidden">
            {/* Header */}
            <div className="p-3 border-b border-slate-800 flex items-center justify-between">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                Выбор упражнения
              </h3>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    setIsExercisePickerOpen(false);
                    setIsCreateDialogOpen(true);
                  }}
                  className="px-2 py-1 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-xs font-bold flex items-center gap-1"
                >
                  <Plus size={13} />
                  <span>Создать</span>
                </button>
                <button
                  onClick={() => setIsExercisePickerOpen(false)}
                  className="text-slate-400 hover:text-white p-1"
                >
                  <X size={16} />
                </button>
              </div>
            </div>

            {/* Filters & Search */}
            <div className="p-3 space-y-2 border-b border-slate-800 bg-slate-950/60">
              {/* Search input */}
              <div className="relative">
                <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Поиск упражнения..."
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg pl-8 pr-3 py-1.5 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-blue-500"
                />
              </div>

              {/* Muscle Group Chips */}
              <div className="flex gap-1.5 overflow-x-auto pb-1 scrollbar-none">
                <button
                  onClick={() => setSelectedCategoryId(null)}
                  className={`px-2 py-1 rounded-md text-[11px] font-semibold shrink-0 transition ${
                    selectedCategoryId === null
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  Все группы
                </button>
                {categories.map((cat) => (
                  <button
                    key={cat.id}
                    onClick={() => setSelectedCategoryId(cat.id)}
                    className={`px-2 py-1 rounded-md text-[11px] font-semibold shrink-0 transition ${
                      selectedCategoryId === cat.id
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    {cat.name}
                  </button>
                ))}
              </div>

              {/* Sort Bar */}
              <div className="flex items-center gap-1.5 text-[11px]">
                <span className="text-slate-500">Сортировка:</span>
                {(['BY_CATEGORY', 'ALPHABETICAL', 'RECENT'] as SortMode[]).map((mode) => (
                  <button
                    key={mode}
                    onClick={() => setSortMode(mode)}
                    className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                      sortMode === mode ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {mode === 'BY_CATEGORY' ? 'По группам' : mode === 'ALPHABETICAL' ? 'А–Я' : 'Недавние'}
                  </button>
                ))}
              </div>
            </div>

            {/* List */}
            <div className="flex-1 overflow-y-auto p-2 space-y-1">
              {filteredExercises.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-500">
                  Ничего не найдено
                </div>
              ) : (
                filteredExercises.map((ex) => {
                  const catName = categories.find((c) => c.id === ex.categoryId)?.name || 'Другое';
                  const isSelected = ex.id === selectedExerciseId;
                  return (
                    <button
                      key={ex.id}
                      onClick={() => {
                        setSelectedExerciseId(ex.id);
                        setIsExercisePickerOpen(false);
                      }}
                      className={`w-full p-2 rounded-lg text-left flex items-center justify-between transition ${
                        isSelected
                          ? 'bg-blue-600/20 border border-blue-500/60 text-white'
                          : 'bg-slate-950/40 hover:bg-slate-800 border border-slate-800/80 text-slate-300'
                      }`}
                    >
                      <div>
                        <div className="text-xs font-bold">{ex.name}</div>
                        <div className="text-[10px] text-slate-400">{catName}</div>
                      </div>
                      {ex.isBodyweight && (
                        <span className="text-[9px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded">
                          Свой вес
                        </span>
                      )}
                    </button>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: CREATE CUSTOM EXERCISE ================= */}
      {isCreateDialogOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-3 animate-fade-in">
          <form
            onSubmit={handleCreateExercise}
            className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-4 space-y-3 shadow-2xl"
          >
            <div className="flex items-center justify-between border-b border-slate-800 pb-2">
              <h3 className="text-sm font-bold text-white">Новое упражнение</h3>
              <button
                type="button"
                onClick={() => setIsCreateDialogOpen(false)}
                className="text-slate-400 hover:text-white p-1"
              >
                <X size={16} />
              </button>
            </div>

            {/* Name */}
            <div>
              <label className="text-[11px] font-bold text-slate-400 block mb-1">Название упражнения</label>
              <input
                type="text"
                required
                value={newExName}
                onChange={(e) => setNewExName(e.target.value)}
                placeholder="Например: Жим гантелей под углом"
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-blue-500"
              />
            </div>

            {/* Muscle Group */}
            <div>
              <label className="text-[11px] font-bold text-slate-400 block mb-1">Группа мышц</label>
              <div className="grid grid-cols-3 gap-1.5">
                {categories.map((cat) => (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => setNewExCategoryId(cat.id)}
                    className={`px-2 py-1.5 rounded-lg text-xs font-semibold transition ${
                      newExCategoryId === cat.id
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-950 border border-slate-800 text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {cat.name}
                  </button>
                ))}
              </div>
            </div>

            {/* Bodyweight Toggle */}
            <div className="flex items-center gap-2 pt-1">
              <input
                type="checkbox"
                id="bwCheck"
                checked={newExIsBodyweight}
                onChange={(e) => setNewExIsBodyweight(e.target.checked)}
                className="rounded bg-slate-950 border-slate-700 text-blue-600 focus:ring-0"
              />
              <label htmlFor="bwCheck" className="text-xs text-slate-300 select-none">
                Упражнение с собственным весом (подтягивания, брусья)
              </label>
            </div>

            {/* Default Rest Time */}
            <div className="flex items-center justify-between text-xs pt-1">
              <span className="text-slate-400">Отдых: {newExRestSeconds} сек</span>
              <div className="flex gap-1">
                {[60, 90, 120, 180].map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => setNewExRestSeconds(s)}
                    className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      newExRestSeconds === s ? 'bg-blue-600 text-white' : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    {s}с
                  </button>
                ))}
              </div>
            </div>

            {/* Step Kg */}
            {!newExIsBodyweight && (
              <div className="flex items-center justify-between text-xs pt-1">
                <span className="text-slate-400">Шаг веса: {newExStepKg} кг</span>
                <div className="flex gap-1">
                  {[1.25, 2.5, 5.0].map((st) => (
                    <button
                      key={st}
                      type="button"
                      onClick={() => setNewExStepKg(st)}
                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        newExStepKg === st ? 'bg-blue-600 text-white' : 'bg-slate-800 text-slate-400'
                      }`}
                    >
                      {st}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Target Reps */}
            <div className="flex items-center justify-between text-xs pt-1">
              <span className="text-slate-400">Целевые повторы: {newExTargetReps}</span>
              <div className="flex gap-1">
                {[6, 8, 10, 12].map((r) => (
                  <button
                    key={r}
                    type="button"
                    onClick={() => setNewExTargetReps(r)}
                    className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      newExTargetReps === r ? 'bg-blue-600 text-white' : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    {r}
                  </button>
                ))}
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => setIsCreateDialogOpen(false)}
                className="flex-1 h-9 rounded-lg bg-slate-800 text-slate-300 text-xs font-semibold hover:bg-slate-700 transition"
              >
                Отмена
              </button>
              <button
                type="submit"
                className="flex-1 h-9 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold transition shadow"
              >
                Создать
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
