import React, { useState } from 'react';
import { AppDatabase } from '../db';
import type { BodyMeasurement } from '../types';
import { Plus, Trash2, TrendingUp, TrendingDown, Scale, Ruler, X } from 'lucide-react';

interface Props {
  onRefresh?: () => void;
}

export const BodyMeasurementsTab: React.FC<Props> = ({ onRefresh }) => {
  const [measurements, setMeasurements] = useState<BodyMeasurement[]>(() =>
    AppDatabase.getBodyMeasurements()
  );
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  // Form inputs
  const [weightKg, setWeightKg] = useState<string>('');
  const [bodyFat, setBodyFat] = useState<string>('');
  const [chestCm, setChestCm] = useState<string>('');
  const [waistCm, setWaistCm] = useState<string>('');
  const [bicepsCm, setBicepsCm] = useState<string>('');
  const [thighsCm, setThighsCm] = useState<string>('');
  const [calvesCm, setCalvesCm] = useState<string>('');
  const [neckCm, setNeckCm] = useState<string>('');
  const [notes, setNotes] = useState<string>('');

  const latest = measurements[0];
  const previous = measurements[1];
  const weightDelta =
    latest?.weightKg && previous?.weightKg ? latest.weightKg - previous.weightKg : null;

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!weightKg && !chestCm && !waistCm) return;

    AppDatabase.addBodyMeasurement({
      date: Date.now(),
      weightKg: weightKg ? parseFloat(weightKg) : undefined,
      bodyFatPercentage: bodyFat ? parseFloat(bodyFat) : undefined,
      chestCm: chestCm ? parseFloat(chestCm) : undefined,
      waistCm: waistCm ? parseFloat(waistCm) : undefined,
      bicepsCm: bicepsCm ? parseFloat(bicepsCm) : undefined,
      thighsCm: thighsCm ? parseFloat(thighsCm) : undefined,
      calvesCm: calvesCm ? parseFloat(calvesCm) : undefined,
      neckCm: neckCm ? parseFloat(neckCm) : undefined,
      notes: notes.trim() || undefined,
    });

    setMeasurements(AppDatabase.getBodyMeasurements());
    setIsDialogOpen(false);
    onRefresh?.();
    // Reset form
    setWeightKg('');
    setBodyFat('');
    setChestCm('');
    setWaistCm('');
    setBicepsCm('');
    setThighsCm('');
    setCalvesCm('');
    setNeckCm('');
    setNotes('');
  };

  const handleDelete = (id: number) => {
    AppDatabase.deleteBodyMeasurement(id);
    setMeasurements(AppDatabase.getBodyMeasurements());
    onRefresh?.();
  };

  return (
    <div className="space-y-4 max-w-2xl mx-auto pb-8">
      {/* Header Bar */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Scale className="text-blue-500" size={20} />
            <span>Антропометрия и Замеры</span>
          </h2>
          <p className="text-xs text-slate-400">
            Трекинг веса, процента жира и обхватов тела
          </p>
        </div>

        <button
          onClick={() => setIsDialogOpen(true)}
          className="px-3 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-lg shadow-blue-600/30 transition"
        >
          <Plus size={16} />
          <span>Новый замер</span>
        </button>
      </div>

      {/* Latest Stats Summary Card */}
      {latest ? (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-blue-600/20 text-blue-400 flex items-center justify-center border border-blue-500/30">
                <Scale size={22} />
              </div>
              <div>
                <div className="text-xs text-slate-400 font-medium">Текущий вес тела</div>
                <div className="text-2xl font-black text-white">
                  {latest.weightKg ? `${latest.weightKg.toFixed(1)} кг` : 'Не указан'}
                </div>
              </div>
            </div>

            {weightDelta !== null && Math.abs(weightDelta) > 0.01 && (
              <div
                className={`flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-bold border ${
                  weightDelta > 0
                    ? 'bg-amber-900/30 text-amber-300 border-amber-700/40'
                    : 'bg-emerald-900/30 text-emerald-300 border-emerald-700/40'
                }`}
              >
                {weightDelta > 0 ? <TrendingUp size={14} /> : <TrendingDown size={14} />}
                <span>{weightDelta > 0 ? `+${weightDelta.toFixed(1)}` : weightDelta.toFixed(1)} кг</span>
              </div>
            )}
          </div>

          {/* Submetrics Grid */}
          <div className="grid grid-cols-4 gap-2 pt-2 border-t border-slate-800 text-center">
            <div className="bg-slate-950 p-2 rounded-xl border border-slate-800/80">
              <div className="text-[10px] text-slate-400">Жир</div>
              <div className="text-xs font-bold text-white">
                {latest.bodyFatPercentage ? `${latest.bodyFatPercentage}%` : '—'}
              </div>
            </div>
            <div className="bg-slate-950 p-2 rounded-xl border border-slate-800/80">
              <div className="text-[10px] text-slate-400">Талия</div>
              <div className="text-xs font-bold text-white">
                {latest.waistCm ? `${latest.waistCm} см` : '—'}
              </div>
            </div>
            <div className="bg-slate-950 p-2 rounded-xl border border-slate-800/80">
              <div className="text-[10px] text-slate-400">Грудь</div>
              <div className="text-xs font-bold text-white">
                {latest.chestCm ? `${latest.chestCm} см` : '—'}
              </div>
            </div>
            <div className="bg-slate-950 p-2 rounded-xl border border-slate-800/80">
              <div className="text-[10px] text-slate-400">Бицепс</div>
              <div className="text-xs font-bold text-white">
                {latest.bicepsCm ? `${latest.bicepsCm} см` : '—'}
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center space-y-3">
          <div className="w-12 h-12 rounded-full bg-blue-600/20 text-blue-400 flex items-center justify-center mx-auto border border-blue-500/30">
            <Ruler size={24} />
          </div>
          <h3 className="text-base font-bold text-white">Нет замеров тела</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Фиксируйте вес и объемы тела, чтобы отслеживать прогресс мышечной массы и процента жира.
          </p>
          <button
            onClick={() => setIsDialogOpen(true)}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold inline-flex items-center gap-1.5 shadow-lg shadow-blue-600/30"
          >
            <Plus size={16} />
            <span>Сделать первый замер</span>
          </button>
        </div>
      )}

      {/* History List */}
      {measurements.length > 0 && (
        <div className="space-y-2 pt-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider px-1">
            История измерений ({measurements.length})
          </div>

          <div className="space-y-2">
            {measurements.map((m) => (
              <div
                key={m.id}
                className="bg-slate-900/80 border border-slate-800 rounded-xl p-3.5 space-y-2 hover:border-slate-700 transition"
              >
                <div className="flex items-center justify-between">
                  <div className="text-xs font-bold text-blue-400">
                    {new Date(m.date).toLocaleDateString('ru-RU', {
                      day: 'numeric',
                      month: 'long',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </div>
                  <button
                    onClick={() => handleDelete(m.id)}
                    className="text-slate-500 hover:text-red-400 p-1 transition"
                    title="Удалить замер"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>

                <div className="flex flex-wrap gap-1.5">
                  {m.weightKg && (
                    <span className="bg-slate-950 border border-slate-800 text-xs px-2.5 py-1 rounded-lg text-white font-bold">
                      Вес: {m.weightKg} кг
                    </span>
                  )}
                  {m.bodyFatPercentage && (
                    <span className="bg-slate-950 border border-slate-800 text-xs px-2.5 py-1 rounded-lg text-slate-300">
                      Жир: {m.bodyFatPercentage}%
                    </span>
                  )}
                  {m.waistCm && (
                    <span className="bg-slate-950 border border-slate-800 text-xs px-2.5 py-1 rounded-lg text-slate-300">
                      Талия: {m.waistCm} см
                    </span>
                  )}
                  {m.chestCm && (
                    <span className="bg-slate-950 border border-slate-800 text-xs px-2.5 py-1 rounded-lg text-slate-300">
                      Грудь: {m.chestCm} см
                    </span>
                  )}
                  {m.bicepsCm && (
                    <span className="bg-slate-950 border border-slate-800 text-xs px-2.5 py-1 rounded-lg text-slate-300">
                      Бицепс: {m.bicepsCm} см
                    </span>
                  )}
                  {m.thighsCm && (
                    <span className="bg-slate-950 border border-slate-800 text-xs px-2.5 py-1 rounded-lg text-slate-300">
                      Бедро: {m.thighsCm} см
                    </span>
                  )}
                </div>

                {m.notes && (
                  <div className="text-xs text-slate-400 italic bg-slate-950/60 p-2 rounded-lg border border-slate-800/40">
                    {m.notes}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Modal Dialog */}
      {isDialogOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-3 animate-fade-in">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
            <div className="p-4 border-b border-slate-800 flex items-center justify-between">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Scale className="text-blue-500" size={18} />
                <span>Новый замер тела</span>
              </h3>
              <button
                onClick={() => setIsDialogOpen(false)}
                className="text-slate-400 hover:text-white p-1"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSave} className="p-4 overflow-y-auto space-y-3">
              <div className="grid grid-cols-2 gap-2.5">
                <div>
                  <label className="text-[11px] font-bold text-slate-300 block mb-1">
                    Вес (кг)
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    placeholder="75.5"
                    value={weightKg}
                    onChange={(e) => setWeightKg(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="text-[11px] font-bold text-slate-300 block mb-1">
                    Процент жира (%)
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    placeholder="15.0"
                    value={bodyFat}
                    onChange={(e) => setBodyFat(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2.5">
                <div>
                  <label className="text-[11px] font-bold text-slate-300 block mb-1">
                    Талия (см)
                  </label>
                  <input
                    type="number"
                    step="0.5"
                    placeholder="80.0"
                    value={waistCm}
                    onChange={(e) => setWaistCm(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="text-[11px] font-bold text-slate-300 block mb-1">
                    Грудь (см)
                  </label>
                  <input
                    type="number"
                    step="0.5"
                    placeholder="100.0"
                    value={chestCm}
                    onChange={(e) => setChestCm(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2.5">
                <div>
                  <label className="text-[11px] font-bold text-slate-300 block mb-1">
                    Бицепс (см)
                  </label>
                  <input
                    type="number"
                    step="0.5"
                    placeholder="38.0"
                    value={bicepsCm}
                    onChange={(e) => setBicepsCm(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="text-[11px] font-bold text-slate-300 block mb-1">
                    Бедро (см)
                  </label>
                  <input
                    type="number"
                    step="0.5"
                    placeholder="58.0"
                    value={thighsCm}
                    onChange={(e) => setThighsCm(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-300 block mb-1">
                  Заметка
                </label>
                <input
                  type="text"
                  placeholder="Утренний замер натощак"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="pt-2 flex gap-2">
                <button
                  type="button"
                  onClick={() => setIsDialogOpen(false)}
                  className="flex-1 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold rounded-xl text-xs"
                >
                  Отмена
                </button>
                <button
                  type="submit"
                  className="flex-2 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl text-xs shadow-lg shadow-blue-600/30"
                >
                  Сохранить замер
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
