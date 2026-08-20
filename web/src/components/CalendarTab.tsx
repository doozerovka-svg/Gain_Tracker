import React, { useState, useMemo } from 'react';
import { AppDatabase } from '../db';
import type { WorkoutSessionWithSets, Exercise } from '../types';
import { 
  ChevronLeft, ChevronRight, Copy, Plus, X 
} from 'lucide-react';

interface Props {
  onRefresh: () => void;
  onOpenActiveTab: () => void;
}

export const CalendarTab: React.FC<Props> = ({ onRefresh, onOpenActiveTab }) => {
  const [sessions, setSessions] = useState<WorkoutSessionWithSets[]>(() => AppDatabase.getAllSessionsWithSets());
  const [exercises] = useState<Exercise[]>(() => AppDatabase.getExercises());
  const [currentDate, setCurrentDate] = useState(() => new Date());
  const [selectedDate, setSelectedDate] = useState<Date>(() => new Date());
  const [viewMode, setViewMode] = useState<'MONTH' | 'WEEK'>('MONTH');

  // Clone modal state
  const [cloneModalSession, setCloneModalSession] = useState<WorkoutSessionWithSets | null>(null);
  const [cloneTargetDateStr, setCloneTargetDateStr] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  });
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const exerciseMap = useMemo(() => new Map(exercises.map((e) => [e.id, e.name])), [exercises]);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const refreshData = () => {
    setSessions(AppDatabase.getAllSessionsWithSets());
    onRefresh();
  };

  const handlePrev = () => {
    const d = new Date(currentDate);
    if (viewMode === 'MONTH') {
      d.setMonth(d.getMonth() - 1);
    } else {
      d.setDate(d.getDate() - 7);
    }
    setCurrentDate(d);
  };

  const handleNext = () => {
    const d = new Date(currentDate);
    if (viewMode === 'MONTH') {
      d.setMonth(d.getMonth() + 1);
    } else {
      d.setDate(d.getDate() + 7);
    }
    setCurrentDate(d);
  };

  // Calendar cells generation
  const monthDays = useMemo(() => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    const firstDayOfMonth = new Date(year, month, 1);
    const lastDayOfMonth = new Date(year, month + 1, 0);

    // Monday as first day of week (0=Sun, 1=Mon, ..., 6=Sat)
    let startDay = firstDayOfMonth.getDay() - 1;
    if (startDay === -1) startDay = 6;

    const days: (Date | null)[] = [];
    for (let i = 0; i < startDay; i++) {
      days.push(null);
    }
    for (let d = 1; d <= lastDayOfMonth.getDate(); d++) {
      days.push(new Date(year, month, d));
    }
    return days;
  }, [currentDate]);

  const weekDays = useMemo(() => {
    const current = new Date(selectedDate);
    let dayOfWeek = current.getDay() - 1;
    if (dayOfWeek === -1) dayOfWeek = 6;

    const monday = new Date(current);
    monday.setDate(current.getDate() - dayOfWeek);

    const days: Date[] = [];
    for (let i = 0; i < 7; i++) {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      days.push(d);
    }
    return days;
  }, [selectedDate]);

  // Find sessions for selected date
  const selectedDateSessions = useMemo(() => {
    const selY = selectedDate.getFullYear();
    const selM = selectedDate.getMonth();
    const selD = selectedDate.getDate();

    return sessions.filter((sw) => {
      const sDate = new Date(sw.session.date);
      return sDate.getFullYear() === selY && sDate.getMonth() === selM && sDate.getDate() === selD;
    });
  }, [sessions, selectedDate]);

  const isSameDay = (d1: Date, d2: Date) => {
    return (
      d1.getFullYear() === d2.getFullYear() &&
      d1.getMonth() === d2.getMonth() &&
      d1.getDate() === d2.getDate()
    );
  };

  const getSessionForDate = (date: Date) => {
    return sessions.find((sw) => isSameDay(new Date(sw.session.date), date));
  };

  const handleCloneConfirm = () => {
    if (!cloneModalSession) return;
    const targetTimestamp = new Date(cloneTargetDateStr).getTime();
    AppDatabase.cloneSession(cloneModalSession.session.id, targetTimestamp);
    setCloneModalSession(null);
    refreshData();
    showToast('Сессия успешно скопирована!');
  };

  const monthNamesRu = [
    'Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
    'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'
  ];

  const dayHeadersRu = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];

  return (
    <div className="space-y-4 pb-20 max-w-xl mx-auto">
      {/* Toast */}
      {toastMessage && (
        <div className="fixed top-16 left-1/2 -translate-x-1/2 z-50 bg-blue-600 text-white px-4 py-2 rounded-full shadow-lg text-sm font-medium animate-bounce">
          {toastMessage}
        </div>
      )}

      {/* Header controls */}
      <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-4 flex items-center justify-between shadow-sm">
        <button
          onClick={handlePrev}
          className="touch-target p-2 text-slate-300 hover:text-white hover:bg-slate-700/60 rounded-xl"
        >
          <ChevronLeft size={22} />
        </button>

        <div className="text-center">
          <div className="text-lg font-bold text-white">
            {monthNamesRu[currentDate.getMonth()]} {currentDate.getFullYear()}
          </div>
          <button
            onClick={() => setViewMode(viewMode === 'MONTH' ? 'WEEK' : 'MONTH')}
            className="text-xs text-blue-400 font-medium hover:underline"
          >
            {viewMode === 'MONTH' ? 'Вид: Месяц (переключить на неделю)' : 'Вид: Неделя (переключить на месяц)'}
          </button>
        </div>

        <button
          onClick={handleNext}
          className="touch-target p-2 text-slate-300 hover:text-white hover:bg-slate-700/60 rounded-xl"
        >
          <ChevronRight size={22} />
        </button>
      </div>

      {/* Calendar Grid */}
      <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-4 space-y-2 shadow-sm">
        {/* Day of Week Headers */}
        <div className="grid grid-cols-7 gap-1 text-center text-xs font-semibold text-slate-400 py-1">
          {dayHeadersRu.map((d) => (
            <div key={d}>{d}</div>
          ))}
        </div>

        {/* Days Grid */}
        {viewMode === 'MONTH' ? (
          <div className="grid grid-cols-7 gap-1.5">
            {monthDays.map((date, idx) => {
              if (!date) {
                return <div key={`empty-${idx}`} className="aspect-square" />;
              }

              const isToday = isSameDay(date, new Date());
              const isSelected = isSameDay(date, selectedDate);
              const sessionInfo = getSessionForDate(date);

              return (
                <button
                  key={date.toISOString()}
                  onClick={() => setSelectedDate(date)}
                  className={`aspect-square rounded-xl flex flex-col items-center justify-center relative transition ${
                    isSelected
                      ? 'bg-blue-600 text-white font-bold shadow-md'
                      : isToday
                      ? 'bg-slate-700/80 text-white border-2 border-blue-400 font-bold'
                      : 'bg-slate-900/60 text-slate-300 hover:bg-slate-700/50'
                  }`}
                >
                  <span className="text-sm">{date.getDate()}</span>
                  {sessionInfo && (
                    <span
                      className={`w-1.5 h-1.5 rounded-full mt-0.5 ${
                        sessionInfo.session.status === 'COMPLETED' ? 'bg-emerald-400' : 'bg-amber-400'
                      }`}
                    />
                  )}
                </button>
              );
            })}
          </div>
        ) : (
          <div className="grid grid-cols-7 gap-1.5">
            {weekDays.map((date) => {
              const isToday = isSameDay(date, new Date());
              const isSelected = isSameDay(date, selectedDate);
              const sessionInfo = getSessionForDate(date);

              return (
                <button
                  key={date.toISOString()}
                  onClick={() => setSelectedDate(date)}
                  className={`aspect-square rounded-xl flex flex-col items-center justify-center relative transition ${
                    isSelected
                      ? 'bg-blue-600 text-white font-bold shadow-md'
                      : isToday
                      ? 'bg-slate-700/80 text-white border-2 border-blue-400 font-bold'
                      : 'bg-slate-900/60 text-slate-300 hover:bg-slate-700/50'
                  }`}
                >
                  <span className="text-sm">{date.getDate()}</span>
                  {sessionInfo && (
                    <span
                      className={`w-1.5 h-1.5 rounded-full mt-0.5 ${
                        sessionInfo.session.status === 'COMPLETED' ? 'bg-emerald-400' : 'bg-amber-400'
                      }`}
                    />
                  )}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Selected Date Session Details */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Тренировки на {selectedDate.toLocaleDateString('ru-RU')}
          </span>
          <button
            onClick={() => {
              AppDatabase.startNewSession(selectedDate.getTime(), 'Новая тренировка');
              onOpenActiveTab();
            }}
            className="text-xs text-blue-400 hover:text-blue-300 flex items-center gap-1 font-medium"
          >
            <Plus size={14} /> Создать здесь
          </button>
        </div>

        {selectedDateSessions.length === 0 ? (
          <div className="bg-slate-800/50 border border-slate-700/40 rounded-2xl p-6 text-center text-slate-400 text-sm">
            В этот день тренировок не запланировано
          </div>
        ) : (
          selectedDateSessions.map((sw) => {
            const volume = sw.sets.reduce((sum, s) => sum + s.weightKg * s.reps, 0);
            return (
              <div
                key={sw.session.id}
                className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-4 space-y-3 shadow-sm"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span
                      className={`px-2 py-0.5 rounded-full text-xs font-bold ${
                        sw.session.status === 'COMPLETED'
                          ? 'bg-emerald-950 text-emerald-300 border border-emerald-500/30'
                          : 'bg-amber-950 text-amber-300 border border-amber-500/30'
                      }`}
                    >
                      {sw.session.status === 'COMPLETED' ? 'Завершена' : 'Черновик'}
                    </span>
                    <span className="text-sm font-semibold text-white">
                      {sw.session.notes || 'Тренировочный день'}
                    </span>
                  </div>

                  <button
                    onClick={() => setCloneModalSession(sw)}
                    className="touch-target p-2 text-slate-400 hover:text-blue-400"
                    title="Клонировать тренировку"
                  >
                    <Copy size={18} />
                  </button>
                </div>

                <div className="text-xs text-slate-400 flex items-center gap-4">
                  <span>Подходов: <b className="text-slate-200">{sw.sets.length}</b></span>
                  <span>Объём: <b className="text-slate-200">{volume.toFixed(1)} кг</b></span>
                </div>

                {/* Exercises in this session */}
                <div className="space-y-1.5 pt-1">
                  {sw.sets.map((set) => (
                    <div
                      key={set.id}
                      className="text-xs bg-slate-900/60 px-3 py-1.5 rounded-lg flex items-center justify-between text-slate-300"
                    >
                      <span>{exerciseMap.get(set.exerciseId) || 'Упражнение'}</span>
                      <span className="font-semibold text-white">
                        {set.weightKg} кг × {set.reps} (RIR {set.rir})
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Clone Session Modal Dialog */}
      {cloneModalSession && (
        <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-2xl max-w-sm w-full p-5 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between">
              <div className="font-bold text-lg text-white">Клонирование сессии</div>
              <button onClick={() => setCloneModalSession(null)} className="text-slate-400 hover:text-white">
                <X size={20} />
              </button>
            </div>

            <p className="text-xs text-slate-300 leading-relaxed">
              Все упражнения, порядок и рабочие веса из тренировки за{' '}
              <b>{new Date(cloneModalSession.session.date).toLocaleDateString('ru-RU')}</b> будут
              перенесены в новую сессию.
            </p>

            <div>
              <label className="text-xs font-semibold text-slate-400 block mb-1">
                Дата для новой тренировки:
              </label>
              <input
                type="date"
                value={cloneTargetDateStr}
                onChange={(e) => setCloneTargetDateStr(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white text-sm focus:outline-none focus:border-blue-500"
              />
            </div>

            <div className="flex gap-2 pt-2">
              <button
                onClick={() => setCloneModalSession(null)}
                className="touch-target flex-1 bg-slate-700 hover:bg-slate-600 text-slate-200 font-medium py-2.5 rounded-xl text-sm transition"
              >
                Отмена
              </button>
              <button
                onClick={handleCloneConfirm}
                className="touch-target flex-1 bg-blue-600 hover:bg-blue-500 text-white font-bold py-2.5 rounded-xl text-sm shadow-md transition"
              >
                Клонировать
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
