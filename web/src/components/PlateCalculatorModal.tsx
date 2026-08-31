import React, { useState, useMemo } from 'react';
import { X, Dumbbell, ArrowRight } from 'lucide-react';

interface PlateSpec {
  weightKg: number;
  bgClass: string;
  textClass: string;
  heightPercent: number;
}

const STANDARD_PLATES: PlateSpec[] = [
  { weightKg: 25.0, bgClass: 'bg-red-600 border-red-700', textClass: 'text-white', heightPercent: 100 },
  { weightKg: 20.0, bgClass: 'bg-blue-600 border-blue-700', textClass: 'text-white', heightPercent: 95 },
  { weightKg: 15.0, bgClass: 'bg-yellow-400 border-yellow-500', textClass: 'text-black', heightPercent: 85 },
  { weightKg: 10.0, bgClass: 'bg-emerald-600 border-emerald-700', textClass: 'text-white', heightPercent: 75 },
  { weightKg: 5.0, bgClass: 'bg-slate-200 border-slate-300', textClass: 'text-black', heightPercent: 65 },
  { weightKg: 2.5, bgClass: 'bg-slate-900 border-slate-700', textClass: 'text-white', heightPercent: 55 },
  { weightKg: 1.25, bgClass: 'bg-slate-400 border-slate-500', textClass: 'text-black', heightPercent: 45 },
  { weightKg: 0.5, bgClass: 'bg-slate-500 border-slate-600', textClass: 'text-white', heightPercent: 35 }
];

interface Props {
  initialWeight: number;
  isOpen: boolean;
  onClose: () => void;
  onApplyWeight?: (weight: number) => void;
}

export const PlateCalculatorModal: React.FC<Props> = ({
  initialWeight,
  isOpen,
  onClose,
  onApplyWeight
}) => {
  const [targetWeight, setTargetWeight] = useState<number>(initialWeight > 0 ? initialWeight : 60);
  const [barWeight, setBarWeight] = useState<number>(20);

  const { plates, totalPerSide, remainder } = useMemo(() => {
    if (targetWeight <= barWeight) {
      return { plates: [], totalPerSide: 0, remainder: 0 };
    }

    let remainingPerSide = (targetWeight - barWeight) / 2.0;
    const result: { spec: PlateSpec; count: number }[] = [];

    for (const spec of STANDARD_PLATES) {
      if (remainingPerSide >= spec.weightKg) {
        const count = Math.floor(remainingPerSide / spec.weightKg);
        if (count > 0) {
          result.push({ spec, count });
          remainingPerSide -= count * spec.weightKg;
          remainingPerSide = Math.round(remainingPerSide * 1000) / 1000;
        }
      }
    }

    const total = result.reduce((sum, item) => sum + item.spec.weightKg * item.count, 0);
    return {
      plates: result,
      totalPerSide: total,
      remainder: remainingPerSide * 2
    };
  }, [targetWeight, barWeight]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-3 animate-fade-in">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Dumbbell className="text-blue-500" size={20} />
            <h3 className="text-base font-bold text-white">Калькулятор блинов</h3>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1">
            <X size={18} />
          </button>
        </div>

        {/* Content */}
        <div className="p-4 overflow-y-auto space-y-4">
          {/* Target Weight display */}
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 text-center space-y-2">
            <div className="text-xs text-slate-400 font-medium">Целевой вес штанги</div>
            <div className="flex items-baseline justify-center gap-1.5">
              <span className="text-4xl font-black text-blue-400">{targetWeight.toFixed(1)}</span>
              <span className="text-base font-bold text-slate-400">кг</span>
            </div>

            {/* Quick adjust chips */}
            <div className="flex flex-wrap justify-center gap-1.5 pt-1">
              {[-10, -2.5, -1.25, +1.25, +2.5, +10].map((delta) => (
                <button
                  key={delta}
                  onClick={() => setTargetWeight((w) => Math.max(0, w + delta))}
                  className={`px-2.5 py-1 rounded-lg text-xs font-bold transition ${
                    delta > 0
                      ? 'bg-blue-900/40 text-blue-300 hover:bg-blue-800/60 border border-blue-700/40'
                      : 'bg-slate-800 text-slate-300 hover:bg-slate-700 border border-slate-700'
                  }`}
                >
                  {delta > 0 ? `+${delta}` : delta}
                </button>
              ))}
            </div>
          </div>

          {/* Bar weight selector */}
          <div className="space-y-1.5">
            <div className="text-xs font-bold text-slate-300">Вес грифа:</div>
            <div className="grid grid-cols-4 gap-1.5">
              {[
                { weight: 20, label: '20 кг' },
                { weight: 15, label: '15 кг' },
                { weight: 10, label: '10 кг' },
                { weight: 0, label: '0 кг' }
              ].map((item) => (
                <button
                  key={item.weight}
                  onClick={() => setBarWeight(item.weight)}
                  className={`py-1.5 px-2 rounded-lg text-xs font-bold border transition ${
                    barWeight === item.weight
                      ? 'bg-blue-600 text-white border-blue-500'
                      : 'bg-slate-800 text-slate-300 border-slate-700 hover:border-slate-600'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          {/* Sleeve visualization */}
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-3.5 space-y-3">
            <div className="text-xs font-bold text-slate-300 flex justify-between">
              <span>Надеть на каждую сторону:</span>
              <span className="text-blue-400 font-extrabold">{totalPerSide.toFixed(2)} кг</span>
            </div>

            {/* Sleeve bar */}
            <div className="h-20 bg-slate-900 border border-slate-800 rounded-lg flex items-center px-3 gap-1 overflow-x-auto">
              {/* Collar */}
              <div className="w-2.5 h-16 bg-slate-500 rounded-sm shrink-0" />

              {/* Plates */}
              {plates.length === 0 ? (
                <div className="text-xs text-slate-500 pl-3">Только пустой гриф ({barWeight} кг)</div>
              ) : (
                plates.map((item) =>
                  Array.from({ length: item.count }).map((_, idx) => (
                    <div
                      key={`${item.spec.weightKg}-${idx}`}
                      className={`w-3.5 rounded-sm border shrink-0 flex items-center justify-center ${item.spec.bgClass}`}
                      style={{ height: `${item.spec.heightPercent}%` }}
                    >
                      <span className={`text-[7px] font-black transform -rotate-90 ${item.spec.textClass}`}>
                        {item.spec.weightKg}
                      </span>
                    </div>
                  ))
                )
              )}

              {/* Sleeve shaft */}
              <div className="h-3 bg-slate-700 rounded-r flex-1 min-w-[20px]" />
            </div>

            {/* Breakdown table */}
            {plates.length > 0 && (
              <div className="space-y-1.5 pt-1">
                {plates.map((item) => (
                  <div key={item.spec.weightKg} className="flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2">
                      <span className={`w-3 h-3 rounded-full border ${item.spec.bgClass}`} />
                      <span className="text-white font-semibold">{item.spec.weightKg} кг</span>
                    </div>
                    <span className="text-slate-400 font-bold">
                      × {item.count} шт. на сторону ({item.count * 2} всего)
                    </span>
                  </div>
                ))}
              </div>
            )}

            {remainder > 0 && (
              <div className="text-[11px] text-amber-400">
                Остаток: не хватает {remainder.toFixed(2)} кг до точного веса
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="p-3 border-t border-slate-800 flex gap-2">
          {onApplyWeight ? (
            <>
              <button
                onClick={onClose}
                className="flex-1 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold rounded-xl text-xs"
              >
                Отмена
              </button>
              <button
                onClick={() => {
                  onApplyWeight(targetWeight);
                  onClose();
                }}
                className="flex-2 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl text-xs flex items-center justify-center gap-1.5"
              >
                <span>Применить {targetWeight.toFixed(1)} кг</span>
                <ArrowRight size={14} />
              </button>
            </>
          ) : (
            <button
              onClick={onClose}
              className="w-full py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl text-xs"
            >
              Закрыть
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
