import React, { useState, useEffect, useMemo } from 'react';
import { AppDatabase } from '../db';
import type { Exercise, ProgressConfig, ProgressionResult, SetEntry, WorkoutSessionWithSets } from '../types';
import { ProgressionEngine } from '../progression';
import { AudioNotificationService } from '../sound';
import { 
  Play, CheckCircle2, PlusCircle, 
  Sparkles, Pause, SkipForward, Trash2, Timer, Zap
} from 'lucide-react';
import confetti from 'canvas-confetti';

interface Props {
  onRefresh: () => void;
}

export const ActiveWorkoutTab: React.FC<Props> = ({ onRefresh }) => {
  const [exercises] = useState<Exercise[]>(() => AppDatabase.getExercises());
  const [selectedExerciseId, setSelectedExerciseId] = useState<number>(() => exercises[0]?.id || 1);
  const [activeSession, setActiveSession] = useState<WorkoutSessionWithSets | null>(() => AppDatabase.getActiveSession());
  
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

  // Auto-populate weight & reps on exercise selection + calculate Progression recommendation
  const { autoPopulated, progressionResult } = useMemo(() => {
    const lastSet = AppDatabase.getLastCompletedSetForExercise(selectedExerciseId);
    let autoPop: SetEntry | null = null;
    let prog: ProgressionResult | null = null;

    if (lastSet) {
      autoPop = lastSet;
      const config: ProgressConfig = AppDatabase.getConfig(selectedExerciseId);
      prog = ProgressionEngine.calculateProgression(lastSet.weightKg, lastSet.reps, lastSet.rir, config);
    }
    return { autoPopulated: autoPop, progressionResult: prog };
  }, [selectedExerciseId, activeSession]);

  // When exercise changes, update inputs with previous values or progression
  useEffect(() => {
    if (autoPopulated) {
      const initWeight = progressionResult ? progressionResult.recommendedWeightKg : autoPopulated.weightKg;
      const initReps = progressionResult ? progressionResult.recommendedReps : autoPopulated.reps;
      setWeightKg(initWeight);
      setReps(initReps || 8);
      setRir(autoPopulated.rir || 2);
    } else {
      setWeightKg(selectedExercise?.isBodyweight ? 0 : 50);
      setReps(8);
      setRir(2);
    }
  }, [selectedExerciseId, autoPopulated, progressionResult, selectedExercise]);

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

      {/* 2. Compact Exercise Selector & One-Line Progression Hint */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-2.5 space-y-1.5 shadow-sm">
        <div className="flex items-center gap-2">
          <select
            value={selectedExerciseId}
            onChange={(e) => setSelectedExerciseId(Number(e.target.value))}
            className="flex-1 bg-slate-950 border border-slate-700 rounded-lg px-2.5 py-1.5 text-white text-xs font-semibold focus:outline-none focus:border-blue-500"
          >
            {exercises.map((ex) => (
              <option key={ex.id} value={ex.id}>
                {ex.name} {ex.isBodyweight ? '(Собственный вес)' : ''}
              </option>
            ))}
          </select>

          {progressionResult && (
            <div className="flex items-center gap-1 bg-indigo-950/80 border border-indigo-500/40 px-2 py-1.5 rounded-lg text-[11px] font-bold text-indigo-300 shrink-0">
              <Zap size={12} className="text-indigo-400" />
              <span>Цель: {progressionResult.recommendedWeightKg} кг × {progressionResult.recommendedReps}</span>
            </div>
          )}
        </div>

        {progressionResult && (
          <div className="text-[11px] text-indigo-200/90 leading-tight px-1 truncate" title={progressionResult.explanationRu}>
            <Sparkles size={11} className="inline mr-1 text-indigo-400" />
            {progressionResult.explanationRu}
          </div>
        )}
      </div>

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
              className="touch-target w-8 h-8 bg-slate-800 hover:bg-slate-700 text-white rounded-md text-xs font-bold"
            >
              -2.5
            </button>
            <span className="text-xl font-black text-white">{weightKg}</span>
            <button
              onClick={() => handleIncrementWeight(2.5)}
              className="touch-target w-8 h-8 bg-slate-800 hover:bg-slate-700 text-white rounded-md text-xs font-bold"
            >
              +2.5
            </button>
          </div>

          {/* Micro Chips */}
          <div className="grid grid-cols-4 gap-1">
            {[1, 2.5, 5, 10].map((inc) => (
              <button
                key={inc}
                onClick={() => handleIncrementWeight(inc)}
                className="touch-target h-7 bg-slate-800 hover:bg-blue-600 hover:text-white border border-slate-700 text-slate-300 text-[10px] font-bold rounded-md transition"
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
            <span className="text-white font-extrabold text-xs">{reps} повт.</span>
          </div>

          {/* Stepper */}
          <div className="flex items-center justify-between gap-1 bg-slate-950 border border-slate-700 rounded-lg p-1">
            <button
              onClick={() => setReps((r) => Math.max(1, r - 1))}
              className="touch-target w-8 h-8 bg-slate-800 hover:bg-slate-700 text-white rounded-md text-sm font-bold"
            >
              -1
            </button>
            <span className="text-xl font-black text-white">{reps}</span>
            <button
              onClick={() => setReps((r) => r + 1)}
              className="touch-target w-8 h-8 bg-slate-800 hover:bg-slate-700 text-white rounded-md text-sm font-bold"
            >
              +1
            </button>
          </div>

          {/* Quick Rep Chips */}
          <div className="grid grid-cols-4 gap-1">
            {[6, 8, 10, 12].map((r) => (
              <button
                key={r}
                onClick={() => setReps(r)}
                className={`touch-target h-7 border text-[10px] font-bold rounded-md transition ${
                  reps === r
                    ? 'bg-blue-600 border-blue-500 text-white'
                    : 'bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700'
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
          <span>RIR (запас повторений)</span>
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

      {/* 6. Compact Horizontal Chips for Completed Sets */}
      {currentExerciseSets.length > 0 && (
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-2 space-y-1.5 shadow-sm">
          <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider px-1">
            Выполнено ({selectedExercise?.name})
          </div>

          <div className="flex flex-wrap gap-1.5">
            {currentExerciseSets.map((set) => (
              <div
                key={set.id}
                className="bg-slate-950 border border-slate-700/80 px-2.5 py-1 rounded-lg flex items-center gap-2 text-xs"
              >
                <span className="font-extrabold text-blue-400">#{set.setNumber}</span>
                <span className="font-bold text-white">{set.weightKg} кг × {set.reps}</span>
                <span className="text-[10px] text-slate-400">RIR {set.rir}</span>
                <button
                  onClick={() => handleDeleteSet(set.id)}
                  className="text-slate-500 hover:text-red-400 p-0.5 ml-0.5"
                >
                  <Trash2 size={12} />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
