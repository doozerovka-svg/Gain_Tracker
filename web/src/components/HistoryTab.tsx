import React, { useState, useMemo } from 'react';
import { AppDatabase } from '../db';
import type { WorkoutSessionWithSets, Exercise } from '../types';
import { ChevronDown, ChevronUp, Trash2, Calendar, Dumbbell, X } from 'lucide-react';

interface Props {
  onRefresh: () => void;
}

export const HistoryTab: React.FC<Props> = ({ onRefresh }) => {
  const [sessions, setSessions] = useState<WorkoutSessionWithSets[]>(() =>
    AppDatabase.getAllSessionsWithSets()
      .filter((s) => s.session.status === 'COMPLETED')
      .sort((a, b) => b.session.date - a.session.date)
  );
  const [exercises] = useState<Exercise[]>(() => AppDatabase.getExercises());
  const [expandedSessionIds, setExpandedSessionIds] = useState<Set<number>>(new Set());
  const [deleteSessionId, setDeleteSessionId] = useState<number | null>(null);

  const exerciseMap = useMemo(() => new Map(exercises.map((e) => [e.id, e.name])), [exercises]);

  const toggleExpand = (id: number) => {
    setExpandedSessionIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleDeleteConfirm = () => {
    if (deleteSessionId === null) return;
    AppDatabase.deleteSession(deleteSessionId);
    setSessions(
      AppDatabase.getAllSessionsWithSets()
        .filter((s) => s.session.status === 'COMPLETED')
        .sort((a, b) => b.session.date - a.session.date)
    );
    setDeleteSessionId(null);
    onRefresh();
  };

  return (
    <div className="space-y-4 pb-20 max-w-xl mx-auto">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-white flex items-center gap-2">
          <Calendar className="text-blue-400" size={20} />
          История тренировок
        </h2>
        <span className="text-xs text-slate-400">Всего: {sessions.length} сессий</span>
      </div>

      {sessions.length === 0 ? (
        <div className="bg-slate-800/60 border border-slate-700/50 rounded-2xl p-8 text-center text-slate-400 space-y-2">
          <Dumbbell className="mx-auto text-slate-600 mb-2" size={32} />
          <p className="font-semibold text-slate-300">История тренировок пуста</p>
          <p className="text-xs">Завершите активную тренировку, чтобы она появилась здесь.</p>
        </div>
      ) : (
        sessions.map((sw) => {
          const isExpanded = expandedSessionIds.has(sw.session.id);
          const totalVolume = sw.sets.reduce((sum, s) => sum + s.weightKg * s.reps, 0);
          const uniqueExerciseCount = new Set(sw.sets.map((s) => s.exerciseId)).size;

          // Group sets by exercise
          const setsByExercise = new Map<number, typeof sw.sets>();
          sw.sets.forEach((s) => {
            const list = setsByExercise.get(s.exerciseId) || [];
            list.push(s);
            setsByExercise.set(s.exerciseId, list);
          });

          return (
            <div
              key={sw.session.id}
              className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-4 space-y-3 shadow-sm transition"
            >
              <div className="flex items-start justify-between">
                <div className="cursor-pointer flex-1" onClick={() => toggleExpand(sw.session.id)}>
                  <div className="text-base font-bold text-white flex items-center gap-2">
                    <span>{new Date(sw.session.date).toLocaleDateString('ru-RU', { weekday: 'short', day: 'numeric', month: 'long', year: 'numeric' })}</span>
                  </div>
                  <div className="text-xs text-slate-400 mt-1">
                    {sw.session.notes || 'Силовая тренировка'}
                  </div>
                  <div className="flex items-center gap-3 text-xs text-blue-300 font-medium mt-2">
                    <span>Упражнений: {uniqueExerciseCount}</span>
                    <span>•</span>
                    <span>Подходов: {sw.sets.length}</span>
                    <span>•</span>
                    <span>Объём: {totalVolume.toFixed(1)} кг</span>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    onClick={() => toggleExpand(sw.session.id)}
                    className="touch-target p-2 text-slate-400 hover:text-white"
                  >
                    {isExpanded ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                  </button>
                  <button
                    onClick={() => setDeleteSessionId(sw.session.id)}
                    className="touch-target p-2 text-slate-400 hover:text-red-400"
                    title="Удалить тренировку"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              </div>

              {/* Expanded Set Details */}
              {isExpanded && (
                <div className="pt-3 border-t border-slate-700 space-y-3">
                  {Array.from(setsByExercise.entries()).map(([exId, sets]) => (
                    <div key={exId} className="space-y-1.5">
                      <div className="text-xs font-semibold text-blue-400">
                        {exerciseMap.get(exId) || 'Упражнение'}
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                        {sets.map((set) => (
                          <div
                            key={set.id}
                            className="text-xs bg-slate-900/80 px-3 py-2 rounded-xl flex items-center justify-between text-slate-300"
                          >
                            <span className="font-medium text-slate-400">Подход #{set.setNumber}</span>
                            <span className="font-bold text-white">
                              {set.weightKg} кг × {set.reps}
                            </span>
                            <span className="text-[11px] px-1.5 py-0.5 bg-slate-800 text-blue-300 rounded font-medium">
                              RIR {set.rir}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })
      )}

      {/* Delete Confirmation Modal */}
      {deleteSessionId !== null && (
        <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-2xl max-w-sm w-full p-5 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between">
              <div className="font-bold text-lg text-white">Удалить тренировку?</div>
              <button onClick={() => setDeleteSessionId(null)} className="text-slate-400 hover:text-white">
                <X size={20} />
              </button>
            </div>
            <p className="text-xs text-slate-300">
              Все данные этой тренировки будут удалены безвозвратно.
            </p>
            <div className="flex gap-2 pt-2">
              <button
                onClick={() => setDeleteSessionId(null)}
                className="touch-target flex-1 bg-slate-700 hover:bg-slate-600 text-slate-200 font-medium py-2.5 rounded-xl text-sm"
              >
                Отмена
              </button>
              <button
                onClick={handleDeleteConfirm}
                className="touch-target flex-1 bg-red-600 hover:bg-red-500 text-white font-bold py-2.5 rounded-xl text-sm shadow-md"
              >
                Удалить
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
