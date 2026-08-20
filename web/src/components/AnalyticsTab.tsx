import React, { useState, useMemo, useRef, useEffect } from 'react';
import { AppDatabase } from '../db';
import type { Exercise } from '../types';
import { ProgressionEngine } from '../progression';
import { TrendingUp, Award, Flame, Dumbbell } from 'lucide-react';

interface DataPoint {
  date: number;
  dateStr: string;
  oneRM: number;
  workingWeight: number;
}

export const AnalyticsTab: React.FC = () => {
  const [exercises] = useState<Exercise[]>(() => AppDatabase.getExercises());
  const [selectedExerciseId, setSelectedExerciseId] = useState<number>(() => exercises[0]?.id || 1);
  const [formula, setFormula] = useState<'epley' | 'brzycki'>('epley');
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const sessions = useMemo(
    () =>
      AppDatabase.getAllSessionsWithSets()
        .filter((s) => s.session.status === 'COMPLETED')
        .sort((a, b) => a.session.date - b.session.date),
    []
  );

  // Build chart data
  const { dataPoints, maxOneRM, totalVolume, totalSessionsCount } = useMemo(() => {
    const points: DataPoint[] = [];
    let max1RM = 0;
    let volume = 0;
    let count = 0;

    sessions.forEach((sw) => {
      const exSets = sw.sets.filter((s) => s.exerciseId === selectedExerciseId);
      if (exSets.length === 0) return;

      count++;
      let sessionMax1RM = 0;
      let sessionMaxWeight = 0;

      exSets.forEach((set) => {
        const setVolume = set.weightKg * set.reps;
        volume += setVolume;
        if (set.weightKg > sessionMaxWeight) sessionMaxWeight = set.weightKg;

        const set1RM =
          formula === 'epley'
            ? ProgressionEngine.calculateEpley(set.weightKg, set.reps)
            : ProgressionEngine.calculateBrzycki(set.weightKg, set.reps);

        if (set1RM > sessionMax1RM) sessionMax1RM = set1RM;
      });

      if (sessionMax1RM > max1RM) max1RM = sessionMax1RM;

      const dateObj = new Date(sw.session.date);
      const dateStr = `${dateObj.getDate().toString().padStart(2, '0')}.${(dateObj.getMonth() + 1)
        .toString()
        .padStart(2, '0')}`;

      points.push({
        date: sw.session.date,
        dateStr,
        oneRM: sessionMax1RM,
        workingWeight: sessionMaxWeight,
      });
    });

    return {
      dataPoints: points,
      maxOneRM: max1RM,
      totalVolume: volume,
      totalSessionsCount: count,
    };
  }, [sessions, selectedExerciseId, formula]);

  // Draw Canvas Chart
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    ctx.scale(dpr, dpr);

    ctx.clearRect(0, 0, width, height);

    if (dataPoints.length === 0) {
      ctx.fillStyle = '#94a3b8';
      ctx.font = '14px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('Нет данных для отображения графика', width / 2, height / 2);
      return;
    }

    const padding = { top: 20, right: 45, bottom: 35, left: 45 };
    const chartW = width - padding.left - padding.right;
    const chartH = height - padding.top - padding.bottom;

    // Ranges
    const maxVal1RM = Math.max(...dataPoints.map((d) => d.oneRM), 10);
    const minVal1RM = Math.max(0, Math.min(...dataPoints.map((d) => d.oneRM)) * 0.85);

    const maxValWeight = Math.max(...dataPoints.map((d) => d.workingWeight), 10);
    const minValWeight = Math.max(0, Math.min(...dataPoints.map((d) => d.workingWeight)) * 0.85);

    // Draw Grid Lines
    const gridLines = 4;
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1;
    ctx.font = '10px sans-serif';
    ctx.fillStyle = '#94a3b8';

    for (let i = 0; i <= gridLines; i++) {
      const y = padding.top + (chartH / gridLines) * i;
      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(padding.left + chartW, y);
      ctx.stroke();

      // Left Y-axis labels (1RM)
      const val1RM = maxVal1RM - ((maxVal1RM - minVal1RM) / gridLines) * i;
      ctx.textAlign = 'right';
      ctx.fillText(`${val1RM.toFixed(0)} кг`, padding.left - 6, y + 3);

      // Right Y-axis labels (Working Weight)
      const valWeight = maxValWeight - ((maxValWeight - minValWeight) / gridLines) * i;
      ctx.textAlign = 'left';
      ctx.fillText(`${valWeight.toFixed(0)} кг`, padding.left + chartW + 6, y + 3);
    }

    // Helper to get X position
    const getX = (index: number) => {
      if (dataPoints.length === 1) return padding.left + chartW / 2;
      return padding.left + (chartW / (dataPoints.length - 1)) * index;
    };

    // Helper to get Y position for 1RM
    const getY1RM = (val: number) => {
      const range = maxVal1RM - minVal1RM || 1;
      return padding.top + chartH - ((val - minVal1RM) / range) * chartH;
    };

    // Helper to get Y position for Working Weight
    const getYWeight = (val: number) => {
      const range = maxValWeight - minValWeight || 1;
      return padding.top + chartH - ((val - minValWeight) / range) * chartH;
    };

    // Draw 1RM Line (Blue)
    if (dataPoints.length > 1) {
      ctx.beginPath();
      ctx.strokeStyle = '#3b82f6';
      ctx.lineWidth = 3;
      dataPoints.forEach((pt, i) => {
        const x = getX(i);
        const y = getY1RM(pt.oneRM);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.stroke();
    }

    // Draw Working Weight Line (Amber / Orange)
    if (dataPoints.length > 1) {
      ctx.beginPath();
      ctx.strokeStyle = '#f59e0b';
      ctx.lineWidth = 2.5;
      dataPoints.forEach((pt, i) => {
        const x = getX(i);
        const y = getYWeight(pt.workingWeight);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.stroke();
    }

    // Draw Dots and Dates
    dataPoints.forEach((pt, i) => {
      const x = getX(i);

      // 1RM Dot
      const y1 = getY1RM(pt.oneRM);
      ctx.fillStyle = '#3b82f6';
      ctx.beginPath();
      ctx.arc(x, y1, 5, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#0f172a';
      ctx.lineWidth = 2;
      ctx.stroke();

      // Working Weight Dot
      const y2 = getYWeight(pt.workingWeight);
      ctx.fillStyle = '#f59e0b';
      ctx.beginPath();
      ctx.arc(x, y2, 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();

      // Date Label
      ctx.textAlign = 'center';
      ctx.fillStyle = '#cbd5e1';
      ctx.font = '11px sans-serif';
      ctx.fillText(pt.dateStr, x, padding.top + chartH + 20);
    });
  }, [dataPoints]);

  return (
    <div className="space-y-4 pb-20 max-w-xl mx-auto">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-white flex items-center gap-2">
          <TrendingUp className="text-blue-400" size={20} />
          Аналитика прогресса
        </h2>
      </div>

      {/* Exercise Selector */}
      <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-4 space-y-3">
        <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
          Выберите упражнение
        </label>
        <select
          value={selectedExerciseId}
          onChange={(e) => setSelectedExerciseId(Number(e.target.value))}
          className="w-full bg-slate-900 border border-slate-700 rounded-xl px-4 py-3 text-white text-base focus:outline-none focus:border-blue-500"
        >
          {exercises.map((ex) => (
            <option key={ex.id} value={ex.id}>
              {ex.name}
            </option>
          ))}
        </select>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-3 gap-2.5">
        <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-3.5 text-center">
          <div className="flex justify-center mb-1 text-blue-400">
            <Award size={20} />
          </div>
          <div className="text-lg font-extrabold text-white">{maxOneRM.toFixed(1)} <span className="text-xs text-slate-400">кг</span></div>
          <div className="text-[11px] text-slate-400 mt-0.5">Макс. 1RM</div>
        </div>

        <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-3.5 text-center">
          <div className="flex justify-center mb-1 text-amber-400">
            <Flame size={20} />
          </div>
          <div className="text-lg font-extrabold text-white">{totalVolume.toFixed(0)} <span className="text-xs text-slate-400">кг</span></div>
          <div className="text-[11px] text-slate-400 mt-0.5">Общий объём</div>
        </div>

        <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-3.5 text-center">
          <div className="flex justify-center mb-1 text-emerald-400">
            <Dumbbell size={20} />
          </div>
          <div className="text-lg font-extrabold text-white">{totalSessionsCount}</div>
          <div className="text-[11px] text-slate-400 mt-0.5">Тренировок</div>
        </div>
      </div>

      {/* Chart Section */}
      <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl p-4 space-y-3 shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Динамика 1RM и рабочего веса
          </span>

          <div className="flex items-center gap-1 bg-slate-900 p-1 rounded-lg border border-slate-700 text-xs">
            <button
              onClick={() => setFormula('epley')}
              className={`px-2 py-0.5 rounded font-medium transition ${
                formula === 'epley' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              Эпли
            </button>
            <button
              onClick={() => setFormula('brzycki')}
              className={`px-2 py-0.5 rounded font-medium transition ${
                formula === 'brzycki' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              Бжицки
            </button>
          </div>
        </div>

        {/* Legend */}
        <div className="flex items-center justify-center gap-6 text-xs text-slate-300 pt-1">
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 bg-blue-500 rounded-full inline-block" />
            <span>1RM ({formula === 'epley' ? 'Эпли' : 'Бжицки'})</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 bg-amber-500 rounded-full inline-block" />
            <span>Рабочий вес</span>
          </div>
        </div>

        {/* Canvas */}
        <div className="w-full h-64 relative">
          <canvas ref={canvasRef} className="w-full h-full" />
        </div>
      </div>
    </div>
  );
};
