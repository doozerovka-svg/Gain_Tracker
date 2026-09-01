import React, { useState, useEffect, useMemo } from 'react';
import { AppDatabase } from '../db';
import type { Category, Exercise, ProgressConfig, WorkoutSessionWithSets, SetType } from '../types';
import { ProgressionEngine } from '../progression';
import { AudioNotificationService } from '../sound';
import { PlateCalculatorModal } from './PlateCalculatorModal';
import { 
  Play, CheckCircle2, PlusCircle, 
  Sparkles, Pause, SkipForward, Trash2, Timer, Zap,
  Plus, Search, X, ArrowLeftRight, Disc
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
    const lastSetInActive = active?.sets && active.sets.length > 0 ? active.sets[active.sets.length - 1] : null;
    if (lastSetInActive) return lastSetInActive.exerciseId;
    const lastUsed = AppDatabase.getLastUsedExerciseId();
    if (lastUsed) return lastUsed;
    const exList = AppDatabase.getExercises();
    return exList[0]?.id || 1;
  });
  const [activeSession, setActiveSession] = useState<WorkoutSessionWithSets | null>(() => AppDatabase.getActiveSession());
  
  // Exercise filtering & sorting
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [sortMode, setSortMode] = useState<SortMode>('BY_CATEGORY');
  const [isExercisePickerOpen, setIsExercisePickerOpen] = useState<boolean>(false);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState<boolean>(false);
  const [isPlateCalcOpen, setIsPlateCalcOpen] = useState<boolean>(false);
  const [isQuickSwapOpen, setIsQuickSwapOpen] = useState<boolean>(false);

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
  const [selectedSetType, setSelectedSetType] = useState<SetType>('NORMAL');

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
      setType: selectedSetType,
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

    showToast(`Подход #${setNumber} (${selectedSetType === 'WARMUP' ? 'Разминка' : selectedSetType === 'DROP_SET' ? 'Дропсет' : selectedSetType === 'FAILURE' ? 'Отказ' : 'Обычный'}) зафиксирован!`);
  };

  const handleDeleteSet = (setId: number) => {
    AppDatabase.deleteSet(setId);
    setActiveSession(AppDatabase.getActiveSession());
    onRefresh();
    showToast('Подход удален');
  };

  const handleQuickSwap = (newExId: number) => {
    setSelectedExerciseId(newExId);
    setIsQuickSwapOpen(false);
    showToast('Упражнение заменено на лету!');
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
  const totalVolume = activeSession?.sets.filter((s) => s.setType !== 'WARMUP').reduce((sum, s) => sum + s.weightKg * s.reps, 0) || 0;
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
      const volume = exSets.filter((s) => s.setType !== 'WARMUP').reduce((sum, s) => sum + s.weightKg * s.reps, 0);
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

  const setTypeOptions: { value: SetType; label: string; tag: string; color: string }[] = [
    { value: 'NORMAL', label: 'Обычный', tag: 'О', color: 'bg-blue-600/20 text-blue-300 border-blue-500/50' },
    { value: 'WARMUP', label: 'Разминка', tag: 'Р', color: 'bg-emerald-600/20 text-emerald-300 border-emerald-500/50' },
    { value: 'DROP_SET', label: 'Дропсет', tag: 'Д', color: 'bg-orange-600/20 text-orange-300 border-orange-500/50' },
    { value: 'FAILURE', label: 'Отказ', tag: '!', color: 'bg-red-600/20 text-red-300 border-red-500/50' },
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

      {/* 2. Compact Exercise Selector with Quick Swap */}
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
          onClick={() => setIsQuickSwapOpen(true)}
          className="touch-target h-9 px-2.5 bg-slate-800 hover:bg-slate-700 text-blue-400 hover:text-blue-300 border border-slate-700 rounded-lg text-xs font-bold flex items-center gap-1 shrink-0 transition"
          title="Быстрая замена тренажера"
        >
          <ArrowLeftRight size={14} />
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
      <div className="grid grid-cols-2 gap-2.5">
        {/* Weight Column */}
        <div className="bg-neutral-950 border border-neutral-800 rounded-2xl p-3 space-y-2 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between text-[11px] font-bold text-neutral-400 uppercase tracking-wider">
            <span>ВЕС (КГ)</span>
            <div className="flex items-center gap-1.5">
              <button
                onClick={() => setIsPlateCalcOpen(true)}
                className="p-1.5 text-sky-400 hover:text-white bg-sky-950/60 hover:bg-sky-600 rounded-lg border border-sky-500/40 transition"
                title="Калькулятор блинов"
              >
                <Disc size={15} />
              </button>
            </div>
          </div>

          {/* Stepper Display (28px Bold) */}
          <div className="flex items-center justify-between gap-1 bg-black border border-neutral-800 rounded-xl p-1.5">
            <button
              onClick={() => handleIncrementWeight(-2.5)}
              className="touch-target w-11 h-11 bg-neutral-900 hover:bg-neutral-800 text-white font-black rounded-lg flex items-center justify-center text-xs transition active:scale-95"
            >
              -2.5
            </button>
            <span className="font-black text-2xl text-white tracking-tight">{weightKg}</span>
            <button
              onClick={() => handleIncrementWeight(2.5)}
              className="touch-target w-11 h-11 bg-neutral-900 hover:bg-neutral-800 text-white font-black rounded-lg flex items-center justify-center text-xs transition active:scale-95"
            >
              +2.5
            </button>
          </div>

          {/* Quick Increments */}
          <div className="grid grid-cols-4 gap-1 pt-0.5">
            {[1.25, 2.5, 5, 10].map((inc) => (
              <button
                key={inc}
                onClick={() => handleIncrementWeight(inc)}
                className="touch-target h-8 bg-neutral-900 hover:bg-sky-600 hover:text-white border border-neutral-800 rounded-lg text-xs font-bold text-neutral-300 transition"
              >
                +{inc}
              </button>
            ))}
          </div>
        </div>

        {/* Reps Column */}
        <div className="bg-neutral-950 border border-neutral-800 rounded-2xl p-3 space-y-2 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between text-[11px] font-bold text-neutral-400 uppercase tracking-wider">
            <span>ПОВТОРЕНИЯ</span>
          </div>

          {/* Stepper Display (28px Bold) */}
          <div className="flex items-center justify-between gap-1 bg-black border border-neutral-800 rounded-xl p-1.5">
            <button
              onClick={() => setReps(Math.max(1, reps - 1))}
              className="touch-target w-11 h-11 bg-neutral-900 hover:bg-neutral-800 text-white font-black rounded-lg flex items-center justify-center text-base transition active:scale-95"
            >
              -1
            </button>
            <span className="font-black text-2xl text-white tracking-tight">{reps}</span>
            <button
              onClick={() => setReps(reps + 1)}
              className="touch-target w-11 h-11 bg-neutral-900 hover:bg-neutral-800 text-white font-black rounded-lg flex items-center justify-center text-base transition active:scale-95"
            >
              +1
            </button>
          </div>

          {/* Quick Reps */}
          <div className="grid grid-cols-4 gap-1 pt-0.5">
            {[6, 8, 10, 12].map((r) => (
              <button
                key={r}
                onClick={() => setReps(r)}
                className={`touch-target h-8 rounded-lg text-xs font-bold transition ${
                  reps === r
                    ? 'bg-sky-600 text-white shadow-md'
                    : 'bg-neutral-900 text-neutral-300 hover:bg-neutral-800 border border-neutral-800'
                }`}
              >
                {r}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 4. Horizontal RIR Segment Selector (0..5+) */}
      <div className="bg-neutral-950 border border-neutral-800 rounded-2xl p-3 space-y-2 shadow-sm">
        <div className="flex items-center justify-between text-[11px] px-0.5 font-bold text-neutral-400 uppercase">
          <span>RIR (запас сил до отказа)</span>
          <span className="text-xs text-sky-400 font-extrabold">
            {rir === 0 ? '0: Отказ' : rir === 1 ? '1: Предел' : rir === 2 ? '2: Рабочий' : rir === 3 ? '3: Запас' : rir === 4 ? '4: Легко' : '5+: Разминка'}
          </span>
        </div>

        <div className="grid grid-cols-6 gap-1.5">
          {rirOptions.map((opt) => (
            <button
              key={opt.value}
              onClick={() => setRir(opt.value)}
              className={`touch-target h-10 rounded-xl border flex flex-col items-center justify-center transition ${
                rir === opt.value
                  ? `${opt.color} font-black ring-2 ring-sky-400 scale-[1.02]`
                  : 'bg-black border-neutral-800 text-neutral-400 hover:text-neutral-200'
              }`}
            >
              <span className="text-sm font-black leading-none">{opt.label}</span>
              <span className="text-[9px] font-medium leading-tight mt-0.5 opacity-80">{opt.desc}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 4b. Set Type Selector (Normal, Warmup, Dropset, Failure) */}
      <div className="bg-neutral-950 border border-neutral-800 rounded-2xl p-3 space-y-2 shadow-sm">
        <div className="flex items-center justify-between text-[11px] px-0.5 font-bold text-neutral-400 uppercase">
          <span>Тип подхода</span>
        </div>

        <div className="grid grid-cols-4 gap-1.5">
          {setTypeOptions.map((opt) => (
            <button
              key={opt.value}
              onClick={() => setSelectedSetType(opt.value)}
              className={`touch-target h-9 rounded-xl border text-xs font-bold transition flex items-center justify-center ${
                selectedSetType === opt.value
                  ? `${opt.color} ring-2 ring-sky-400`
                  : 'bg-black border-neutral-800 text-neutral-400 hover:text-neutral-200'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* 5. Main Action Button (52px touch target) */}
      <button
        onClick={handleSaveSet}
        className="touch-target w-full h-13 py-3.5 bg-sky-600 hover:bg-sky-500 text-white font-black text-base rounded-2xl shadow-lg shadow-sky-600/30 flex items-center justify-center gap-2 transition active:scale-[0.99]"
      >
        <PlusCircle size={20} />
        <span>Зафиксировать подход #{currentExerciseSets.length + 1}</span>
      </button>

      {/* 6. Completed Sets Grouped by Exercise (Gym Luxury Table) */}
      {groupedWorkoutSets.length > 0 && (
        <div className="space-y-3 pt-2">
          <div className="flex items-center justify-between text-xs px-1">
            <span className="font-bold text-white uppercase tracking-wider">
              Выполненные упражнения ({groupedWorkoutSets.length})
            </span>
            <span className="text-sky-400 font-bold">
              Всего сетов: {totalSets}
            </span>
          </div>

          <div className="space-y-3">
            {groupedWorkoutSets.map((group) => (
              <div
                key={group.exerciseId}
                className={`border rounded-2xl p-3.5 space-y-3 transition shadow-sm ${
                  group.isActive
                    ? 'bg-neutral-950 border-sky-500/60 ring-1 ring-sky-500/30'
                    : 'bg-neutral-950 border-neutral-800 hover:border-neutral-700'
                }`}
              >
                {/* Exercise Header */}
                <div className="flex items-center justify-between gap-2">
                  <div
                    className="flex items-center gap-2.5 cursor-pointer min-w-0"
                    onClick={() => setSelectedExerciseId(group.exerciseId)}
                  >
                    <span className={`w-2.5 h-2.5 rounded-full shrink-0 ${group.isActive ? 'bg-sky-400 ring-2 ring-sky-400/40' : 'bg-neutral-600'}`} />
                    <span className="font-bold text-white text-sm truncate">
                      {group.exerciseName}
                    </span>
                    {group.isActive && (
                      <span className="px-2 py-0.5 bg-sky-600/30 text-sky-300 border border-sky-500/40 text-[10px] font-black rounded-md shrink-0 uppercase">
                        Активно
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2 text-xs text-neutral-400 shrink-0">
                    <span className="font-medium">{group.sets.length} подх.</span>
                    <span>•</span>
                    <span className="text-neutral-200 font-bold">{group.volume.toFixed(0)} кг</span>
                    {!group.isActive && (
                      <button
                        onClick={() => setSelectedExerciseId(group.exerciseId)}
                        className="px-2.5 py-1 bg-neutral-900 hover:bg-sky-600 hover:text-white text-sky-400 border border-neutral-700 rounded-lg text-[11px] font-bold transition ml-1"
                      >
                        Выбрать
                      </button>
                    )}
                  </div>
                </div>

                {/* Gym Luxury Set Table */}
                <div className="bg-black border border-neutral-850 rounded-xl overflow-hidden shadow-inner">
                  {/* Table Header */}
                  <div className="bg-neutral-900/90 px-3 py-1.5 border-b border-neutral-800 flex items-center justify-between text-[10px] font-bold text-neutral-400 uppercase tracking-wider">
                    <span className="w-8">СЕТ</span>
                    <span className="w-16">ТИП</span>
                    <span className="flex-1">ВЕС × ПОВТОРЕНИЯ</span>
                    <span className="w-20 text-right">RIR / 1RM</span>
                    <span className="w-7"></span>
                  </div>

                  {/* Table Rows */}
                  <div className="divide-y divide-neutral-900">
                    {group.sets.map((set) => {
                      const tagLabel = set.setType === 'WARMUP' ? 'Разминка' : set.setType === 'DROP_SET' ? 'Дропсет' : set.setType === 'FAILURE' ? 'Отказ' : 'Обычный';
                      const tagColor = set.setType === 'WARMUP'
                        ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40'
                        : set.setType === 'DROP_SET'
                        ? 'bg-amber-500/20 text-amber-400 border-amber-500/40'
                        : set.setType === 'FAILURE'
                        ? 'bg-red-500/20 text-red-400 border-red-500/40'
                        : 'bg-neutral-800 text-neutral-400 border-neutral-700';

                      const set1RM = set.reps > 0 ? (set.weightKg * (1 + set.reps / 30)).toFixed(0) : 0;

                      return (
                        <div
                          key={set.id}
                          className="px-3 py-2 flex items-center justify-between gap-2 hover:bg-neutral-900/50 transition"
                        >
                          <span className="w-8 flex items-center justify-center w-6 h-6 rounded-full bg-sky-950/80 border border-sky-500/40 text-sky-400 text-xs font-black">
                            #{set.setNumber}
                          </span>
                          <span className="w-16">
                            <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold border ${tagColor}`}>
                              {tagLabel}
                            </span>
                          </span>
                          <span className="flex-1 text-sm font-black text-white">
                            {set.weightKg} кг × {set.reps}
                          </span>
                          <div className="w-20 text-right flex flex-col items-end">
                            <span className="text-xs font-bold text-sky-400">RIR {set.rir}</span>
                            {set.setType !== 'WARMUP' && (
                              <span className="text-[10px] text-neutral-500">{set1RM} кг 1RM</span>
                            )}
                          </div>
                          <button
                            onClick={() => handleDeleteSet(set.id)}
                            className="w-7 text-neutral-500 hover:text-red-400 p-1 flex justify-end transition"
                            title="Удалить подход"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ================= MODAL: QUICK SWAP EXERCISE ================= */}
      {isQuickSwapOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-3 animate-fade-in">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md max-h-[80vh] flex flex-col shadow-2xl overflow-hidden">
            <div className="p-3 border-b border-slate-800 flex items-center justify-between">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <ArrowLeftRight size={16} className="text-blue-400" />
                Быстрая замена тренажера
              </h3>
              <button onClick={() => setIsQuickSwapOpen(false)} className="p-1 text-slate-400 hover:text-white">
                <X size={18} />
              </button>
            </div>
            <div className="p-3 space-y-2 overflow-y-auto">
              <p className="text-xs text-slate-400">
                Тренажер занят? Выберите альтернативу для той же группы мышц без потери прогресса:
              </p>
              {exercises
                .filter((e) => e.id !== selectedExerciseId && (selectedExercise ? e.categoryId === selectedExercise.categoryId : true))
                .map((ex) => (
                  <button
                    key={ex.id}
                    onClick={() => handleQuickSwap(ex.id)}
                    className="w-full text-left p-2.5 bg-slate-950 hover:bg-blue-950/40 border border-slate-800 hover:border-blue-500/40 rounded-xl transition flex items-center justify-between"
                  >
                    <div>
                      <div className="text-xs font-bold text-white">{ex.name}</div>
                      <div className="text-[10px] text-slate-400">{ex.isBodyweight ? 'Свой вес' : 'Со снарядом'}</div>
                    </div>
                    <ArrowLeftRight size={14} className="text-blue-400" />
                  </button>
                ))}
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: PLATE CALCULATOR ================= */}
      {isPlateCalcOpen && (
        <PlateCalculatorModal
          isOpen={isPlateCalcOpen}
          initialWeight={weightKg}
          onClose={() => setIsPlateCalcOpen(false)}
          onApplyWeight={(w) => {
            setWeightKg(w);
            setIsPlateCalcOpen(false);
          }}
        />
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
