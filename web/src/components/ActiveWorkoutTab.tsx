import React, { useState, useEffect, useMemo } from 'react';
import { AppDatabase } from '../db';
import type { Exercise, ProgressConfig, ProgressionResult, SetEntry, WorkoutSessionWithSets } from '../types';
import { ProgressionEngine } from '../progression';
import { AudioNotificationService } from '../sound';
import { 
  Play, CheckCircle2, PlusCircle, 
  Sparkles, Pause, SkipForward, Trash2, Delete
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
  const [isNumericKeypadOpen, setIsNumericKeypadOpen] = useState(false);
  const [rawWeightStr, setRawWeightStr] = useState('80');

  // Rest timer
  const [isTimerRunning, setIsTimerRunning] = useState(false);
  const [isTimerPaused, setIsTimerPaused] = useState(false);
  const [timerSecondsLeft, setTimerSecondsLeft] = useState(90);
  const [totalTimerSeconds, setTotalTimerSeconds] = useState(90);

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
      setWeightKg(initWeight);
      setRawWeightStr(initWeight.toString());
      setReps(autoPopulated.reps || 8);
      setRir(autoPopulated.rir || 2);
    } else {
      setWeightKg(selectedExercise?.isBodyweight ? 0 : 50);
      setRawWeightStr(selectedExercise?.isBodyweight ? '0' : '50');
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
          if (prev === 4) {
            AudioNotificationService.playBeep(440, 0.1, 1);
          } else if (prev === 3) {
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
    setRawWeightStr(next.toString());
  };

  const handleKeypadPress = (val: string) => {
    if (val === 'C') {
      setRawWeightStr('0');
      setWeightKg(0);
    } else if (val === 'DEL') {
      const nextStr = rawWeightStr.length > 1 ? rawWeightStr.slice(0, -1) : '0';
      setRawWeightStr(nextStr);
      setWeightKg(parseFloat(nextStr) || 0);
    } else if (val === '.') {
      if (!rawWeightStr.includes('.')) {
        const nextStr = rawWeightStr + '.';
        setRawWeightStr(nextStr);
      }
    } else {
      const nextStr = rawWeightStr === '0' ? val : rawWeightStr + val;
      setRawWeightStr(nextStr);
      setWeightKg(parseFloat(nextStr) || 0);
    }
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

    // Start auto rest timer (90s)
    const restTime = selectedExercise?.defaultRestTimeSeconds || 90;
    setTotalTimerSeconds(restTime);
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

  return (
    <div className="space-y-4 pb-20 max-w-xl mx-auto">
      {/* Toast */}
      {toastMessage && (
        <div className="fixed top-16 left-1/2 -translate-x-1/2 z-50 bg-blue-600 text-white px-4 py-2 rounded-full shadow-lg text-sm font-medium animate-bounce">
          {toastMessage}
        </div>
      )}

      {/* Top Session Bar */}
      <div className="bg-slate-800/80 border border-slate-700/60 backdrop-blur rounded-2xl p-4 flex items-center justify-between shadow-sm">
        <div>
          <div className="text-xs font-semibold uppercase tracking-wider text-blue-400">
            {activeSession ? 'Активная сессия' : 'Сессия не начата'}
          </div>
          <div className="text-lg font-bold text-white">
            {activeSession ? (
              <span>Подходов: {totalSets} • Объём: {totalVolume.toFixed(1)} кг</span>
            ) : (
              <span>Готовы к тренировке?</span>
            )}
          </div>
        </div>

        {activeSession ? (
          <button
            onClick={handleCompleteWorkout}
            className="touch-target bg-emerald-600 hover:bg-emerald-500 text-white font-medium px-4 py-2 rounded-xl flex items-center gap-2 shadow-md transition"
          >
            <CheckCircle2 size={18} />
            Завершить
          </button>
        ) : (
          <button
            onClick={handleStartWorkout}
            className="touch-target bg-blue-600 hover:bg-blue-500 text-white font-medium px-4 py-2 rounded-xl flex items-center gap-2 shadow-md transition"
          >
            <Play size={18} />
            Начать
          </button>
        )}
      </div>

      {/* Rest Timer Banner */}
      {isTimerRunning && (
        <div className="bg-gradient-to-r from-blue-900/60 to-indigo-900/60 border border-blue-500/40 rounded-2xl p-4 text-white flex items-center justify-between shadow-md">
          <div className="flex items-center gap-3">
            <div className="relative w-12 h-12 flex items-center justify-center">
              <svg className="w-12 h-12 -rotate-90">
                <circle cx="24" cy="24" r="20" className="stroke-slate-700" strokeWidth="4" fill="none" />
                <circle
                  cx="24"
                  cy="24"
                  r="20"
                  className="stroke-blue-400 transition-all duration-1000"
                  strokeWidth="4"
                  fill="none"
                  strokeDasharray={125.6}
                  strokeDashoffset={125.6 * (1 - timerSecondsLeft / totalTimerSeconds)}
                  strokeLinecap="round"
                />
              </svg>
              <span className="absolute text-xs font-bold">{timerSecondsLeft}s</span>
            </div>
            <div>
              <div className="text-xs text-blue-300 font-medium">Таймер отдыха</div>
              <div className="text-sm font-bold">
                {Math.floor(timerSecondsLeft / 60)}:{(timerSecondsLeft % 60).toString().padStart(2, '0')}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-1">
            <button
              onClick={() => setTimerSecondsLeft((p) => p + 30)}
              className="touch-target px-2.5 py-1 text-xs bg-slate-700/80 hover:bg-slate-600 rounded-lg text-slate-200"
            >
              +30с
            </button>
            <button
              onClick={() => setIsTimerPaused(!isTimerPaused)}
              className="touch-target p-2 text-slate-200 hover:text-white"
            >
              {isTimerPaused ? <Play size={18} /> : <Pause size={18} />}
            </button>
            <button
              onClick={() => setIsTimerRunning(false)}
              className="touch-target p-2 text-slate-400 hover:text-red-400"
            >
              <SkipForward size={18} />
            </button>
          </div>
        </div>
      )}

      {/* Exercise Selection */}
      <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-4 space-y-3">
        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
          Выбор упражнения
        </label>
        <select
          value={selectedExerciseId}
          onChange={(e) => setSelectedExerciseId(Number(e.target.value))}
          className="w-full bg-slate-900 border border-slate-700 rounded-xl px-4 py-3 text-white text-base focus:outline-none focus:border-blue-500 transition"
        >
          {exercises.map((ex) => (
            <option key={ex.id} value={ex.id}>
              {ex.name} {ex.isBodyweight ? '(Собственный вес)' : ''}
            </option>
          ))}
        </select>

        {/* Progression Hint Banner (No-AI deterministic formula) */}
        {progressionResult && (
          <div className="bg-indigo-950/40 border border-indigo-500/30 rounded-xl p-3 flex items-start gap-2.5 text-xs text-indigo-200">
            <Sparkles className="text-indigo-400 shrink-0 mt-0.5" size={16} />
            <div>
              <span className="font-semibold text-indigo-300">Алгоритмическая прогрессия: </span>
              {progressionResult.explanationRu}
            </div>
          </div>
        )}
      </div>

      {/* Set Input Box (Weight + Reps + RIR) */}
      <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-4 space-y-5">
        {/* Weight Section */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Рабочий вес (кг)
            </span>
            <button
              onClick={() => setIsNumericKeypadOpen(!isNumericKeypadOpen)}
              className="text-xs text-blue-400 hover:text-blue-300 font-medium"
            >
              {isNumericKeypadOpen ? 'Скрыть клавиатуру' : 'Ввод цифрами'}
            </button>
          </div>

          <div className="flex items-center justify-between gap-3 bg-slate-900/90 border border-slate-700 rounded-xl p-3">
            <button
              onClick={() => handleIncrementWeight(-2.5)}
              className="touch-target w-12 h-12 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-lg font-bold transition"
            >
              -2.5
            </button>
            <div className="text-center">
              <span className="text-3xl font-extrabold text-white tracking-tight">{weightKg}</span>
              <span className="text-sm text-slate-400 ml-1">кг</span>
            </div>
            <button
              onClick={() => handleIncrementWeight(2.5)}
              className="touch-target w-12 h-12 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-lg font-bold transition"
            >
              +2.5
            </button>
          </div>

          {/* Quick Increment Buttons (+1, +2.5, +5, +10, +20 kg) */}
          <div className="grid grid-cols-5 gap-2 mt-2">
            {[1, 2.5, 5, 10, 20].map((inc) => (
              <button
                key={inc}
                onClick={() => handleIncrementWeight(inc)}
                className="touch-target bg-slate-800 hover:bg-blue-600 hover:text-white border border-slate-700 text-slate-200 text-sm font-semibold rounded-xl transition"
              >
                +{inc}
              </button>
            ))}
          </div>

          {/* Collapsible Numeric Keypad */}
          {isNumericKeypadOpen && (
            <div className="mt-3 bg-slate-900 border border-slate-700 rounded-xl p-3 grid grid-cols-3 gap-2">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '0', 'DEL'].map((k) => (
                <button
                  key={k}
                  onClick={() => handleKeypadPress(k)}
                  className="touch-target h-12 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-lg text-lg transition"
                >
                  {k === 'DEL' ? <Delete size={20} className="mx-auto" /> : k}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Reps Stepper */}
        <div>
          <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
            Количество повторений
          </div>
          <div className="flex items-center justify-between gap-3 bg-slate-900/90 border border-slate-700 rounded-xl p-3">
            <button
              onClick={() => setReps((r) => Math.max(1, r - 1))}
              className="touch-target w-12 h-12 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xl font-bold transition"
            >
              -1
            </button>
            <div className="text-center">
              <span className="text-3xl font-extrabold text-white tracking-tight">{reps}</span>
              <span className="text-sm text-slate-400 ml-1">раз</span>
            </div>
            <button
              onClick={() => setReps((r) => r + 1)}
              className="touch-target w-12 h-12 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xl font-bold transition"
            >
              +1
            </button>
          </div>
        </div>

        {/* Discrete RIR Slider (0 to 5) */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Интенсивность RIR (запас до отказа)
            </span>
            <span className="text-sm font-bold text-blue-400">
              RIR: {rir} {rir === 0 ? '(Отказ)' : rir === 1 ? '(Предел)' : rir <= 3 ? '(Рабочий)' : '(Легко)'}
            </span>
          </div>

          <div className="bg-slate-900/90 border border-slate-700 rounded-xl p-4 space-y-3">
            <input
              type="range"
              min="0"
              max="5"
              step="1"
              value={rir}
              onChange={(e) => setRir(Number(e.target.value))}
              className="w-full h-3 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-blue-500"
            />
            <div className="flex justify-between text-xs text-slate-400 px-1 font-medium">
              <span className={rir === 0 ? 'text-red-400 font-bold' : ''}>0 (Отказ)</span>
              <span className={rir === 1 ? 'text-orange-400 font-bold' : ''}>1</span>
              <span className={rir === 2 ? 'text-yellow-400 font-bold' : ''}>2</span>
              <span className={rir === 3 ? 'text-green-400 font-bold' : ''}>3</span>
              <span className={rir === 4 ? 'text-blue-400 font-bold' : ''}>4</span>
              <span className={rir === 5 ? 'text-indigo-400 font-bold' : ''}>5+</span>
            </div>
          </div>
        </div>

        {/* Confirm Save Set Button (<= 4 clicks budget, >= 48px touch target) */}
        <button
          onClick={handleSaveSet}
          className="touch-target w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-4 rounded-xl text-lg shadow-lg shadow-blue-600/30 flex items-center justify-center gap-2 transition active:scale-[0.99]"
        >
          <PlusCircle size={22} />
          Зафиксировать подход #{currentExerciseSets.length + 1}
        </button>
      </div>

      {/* Completed Sets in current workout */}
      {currentExerciseSets.length > 0 && (
        <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-4 space-y-3">
          <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Выполненные подходы ({selectedExercise?.name})
          </div>

          <div className="divide-y divide-slate-700">
            {currentExerciseSets.map((set) => (
              <div key={set.id} className="py-2.5 flex items-center justify-between text-sm">
                <div className="flex items-center gap-3">
                  <span className="w-7 h-7 bg-slate-700 text-slate-300 rounded-full flex items-center justify-center font-bold text-xs">
                    #{set.setNumber}
                  </span>
                  <div>
                    <span className="font-bold text-white">{set.weightKg} кг</span> ×{' '}
                    <span className="font-bold text-white">{set.reps} повт.</span>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs px-2 py-0.5 bg-slate-700/80 text-blue-300 rounded-md font-medium">
                    RIR {set.rir}
                  </span>
                  <button
                    onClick={() => handleDeleteSet(set.id)}
                    className="text-slate-400 hover:text-red-400 p-1"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
