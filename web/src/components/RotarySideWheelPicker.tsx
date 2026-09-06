import React, { useRef, useState, useCallback } from 'react';
import { AudioNotificationService } from '../sound';
import { Check } from 'lucide-react';

interface Props {
  value: number;
  onChange: (val: number) => void;
  min?: number;
  max?: number;
  step?: number;
  label: string;
  unit: string;
  quickSteps?: number[];
  onClose: () => void;
}

export const RotarySideWheelPicker: React.FC<Props> = ({
  value,
  onChange,
  min = 0,
  max = 500,
  step = 0.5,
  label,
  unit,
  quickSteps = [-5, -2.5, 2.5, 5],
  onClose,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const isDraggingRef = useRef(false);
  const [isDragging, setIsDragging] = useState(false);
  const [dragOffsetSteps, setDragOffsetSteps] = useState(0);
  const dragStartX = useRef(0);
  const dragStartValue = useRef(value);
  const lastTickValue = useRef(value);

  const clampValue = useCallback((val: number) => {
    const factor = 1 / step;
    const rounded = Math.round(val * factor) / factor;
    const clamped = Math.max(min, Math.min(max, rounded));
    return Number(clamped.toFixed(step < 1 ? 1 : 0));
  }, [min, max, step]);

  const nudge = useCallback((delta: number) => {
    const next = clampValue(value + delta);
    if (next !== value) {
      onChange(next);
      AudioNotificationService.playClickTick();
    }
  }, [value, clampValue, onChange]);

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    isDraggingRef.current = true;
    setIsDragging(true);
    setDragOffsetSteps(0);
    dragStartX.current = e.clientX;
    dragStartValue.current = value;
    lastTickValue.current = value;
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {
      // ignore
    }
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDraggingRef.current) return;
    const deltaX = e.clientX - dragStartX.current;
    // 1 step per 14px of horizontal drag
    const pixelsPerStep = 14;
    const deltaSteps = deltaX / pixelsPerStep;
    setDragOffsetSteps(deltaSteps);

    const newValue = clampValue(dragStartValue.current + deltaSteps * step);
    if (newValue !== value) {
      onChange(newValue);
      if (Math.abs(newValue - lastTickValue.current) >= step) {
        AudioNotificationService.playClickTick();
        lastTickValue.current = newValue;
      }
    }
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (isDraggingRef.current) {
      isDraggingRef.current = false;
      setIsDragging(false);
      setDragOffsetSteps(0);
      try {
        e.currentTarget.releasePointerCapture(e.pointerId);
      } catch {
        // ignore
      }
    }
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY < 0 ? step : -step;
    nudge(delta);
  };

  // Generate graduation lines for the 3D cylinder
  // Decoupled continuous visual value for smooth dragging
  const continuousValue = isDragging ? dragStartValue.current + dragOffsetSteps * step : value;
  const clampedContinuousValue = Math.max(min, Math.min(max, continuousValue));

  const ticks = [];
  const visibleRange = 16;
  const centerIndex = Math.round(clampedContinuousValue / step);

  for (let i = centerIndex - visibleRange; i <= centerIndex + visibleRange; i++) {
    const tickVal = Number((i * step).toFixed(2));
    if (tickVal < min || tickVal > max) continue;

    // IEEE 754 precision safe modulo
    const isMajor = Math.abs(Math.round(tickVal / (step * 5)) - tickVal / (step * 5)) < 0.01;
    const isMid = Math.abs(Math.round(tickVal / (step * 2)) - tickVal / (step * 2)) < 0.01;

    // Physical offset from center
    const offsetSteps = (tickVal - clampedContinuousValue) / step;
    const norm = offsetSteps / visibleRange;
    if (norm < -1 || norm > 1) continue;

    // Cylindrical projection factor (-1 to 1)
    const angle = norm * (Math.PI / 2.2); // curve angle
    const cosVal = Math.cos(angle);
    const opacity = Math.max(0.1, cosVal ** 1.8);
    const height = isMajor ? 36 : isMid ? 24 : 14;
    const posX = 50 + Math.sin(angle) * 46; // percentage position 4% to 96%

    ticks.push({
      index: i,
      val: Number(tickVal.toFixed(step < 1 ? 1 : 0)),
      posX,
      height,
      opacity,
      isMajor,
      cosVal,
    });
  }

  return (
    <div className="bg-neutral-950 border border-sky-500/50 rounded-2xl p-3 space-y-3 shadow-2xl shadow-sky-950/40 animate-fade-in ring-1 ring-sky-500/20 my-2">
      {/* Header with Value Readout */}
      <div className="flex items-center justify-between">
        <div>
          <span className="text-[10px] font-black uppercase tracking-wider text-sky-400">
            {label}
          </span>
          <div className="text-2xl font-black text-white tracking-tight flex items-baseline gap-1">
            <span>{value}</span>
            <span className="text-xs text-neutral-400 font-semibold">{unit}</span>
          </div>
        </div>

        {/* Quick adjustments */}
        <div className="flex items-center gap-1">
          {quickSteps.map((qs) => (
            <button
              key={qs}
              type="button"
              onClick={() => nudge(qs)}
              className="px-2 py-1 bg-neutral-900 hover:bg-sky-600 hover:text-white border border-neutral-800 rounded-lg text-xs font-bold text-neutral-300 transition active:scale-95"
            >
              {qs > 0 ? `+${qs}` : qs}
            </button>
          ))}
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 ml-1 bg-sky-600 hover:bg-sky-500 text-white rounded-lg flex items-center justify-center transition active:scale-95 shadow-md shadow-sky-600/30"
            title="Готово"
          >
            <Check size={16} />
          </button>
        </div>
      </div>

      {/* 3D Knurled Rotary Wheel Dial (Side cylinder view) */}
      <div className="relative">
        <div
          ref={containerRef}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          onLostPointerCapture={handlePointerUp}
          onWheel={handleWheel}
          className={`relative h-20 bg-gradient-to-b from-neutral-900 via-neutral-950 to-neutral-900 border border-neutral-800 rounded-xl overflow-hidden cursor-ew-resize select-none touch-none shadow-inner ${
            isDragging ? 'ring-2 ring-sky-400' : ''
          }`}
          title="Потяните влево или вправо, чтобы прокрутить колесо"
        >
          {/* Top/Bottom Metallic Rim Shading */}
          <div className="absolute inset-x-0 top-0 h-2 bg-gradient-to-b from-neutral-700/30 to-transparent pointer-events-none" />
          <div className="absolute inset-x-0 bottom-0 h-2 bg-gradient-to-t from-neutral-700/30 to-transparent pointer-events-none" />

          {/* Left/Right Cylinder Vignette */}
          <div className="absolute inset-y-0 left-0 w-16 bg-gradient-to-r from-black via-black/80 to-transparent z-10 pointer-events-none" />
          <div className="absolute inset-y-0 right-0 w-16 bg-gradient-to-l from-black via-black/80 to-transparent z-10 pointer-events-none" />

          {/* Graduation Tick Marks Container */}
          <div className="absolute inset-0 flex items-center pointer-events-none">
            {ticks.map((t) => (
              <div
                key={t.index}
                className="absolute top-1/2 -translate-y-1/2 flex flex-col items-center pointer-events-none transition-transform duration-75"
                style={{
                  left: `${t.posX}%`,
                  opacity: t.opacity,
                  transform: `translate(-50%, -50%) scale(${t.cosVal})`,
                }}
              >
                {/* Tick line */}
                <div
                  className={`rounded-full ${
                    t.isMajor
                      ? 'w-1 bg-sky-400 shadow-[0_0_6px_#38bdf8]'
                      : 'w-0.5 bg-neutral-500'
                  }`}
                  style={{ height: `${t.height}px` }}
                />
                {/* Tick Label for major marks */}
                {t.isMajor && (
                  <span className="text-[9px] font-black text-neutral-400 mt-1 font-mono">
                    {t.val}
                  </span>
                )}
              </div>
            ))}
          </div>

          {/* Central Illuminated Aim / Marker Needle */}
          <div className="absolute inset-y-0 left-1/2 -translate-x-1/2 w-0.5 bg-sky-400 shadow-[0_0_10px_#38bdf8] z-20 pointer-events-none">
            {/* Top & Bottom Pointers */}
            <div className="absolute -top-1 left-1/2 -translate-x-1/2 w-2 h-2 bg-sky-400 rotate-45" />
            <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-2 h-2 bg-sky-400 rotate-45" />
          </div>

          {/* Interactive Helper Prompt */}
          <div className="absolute bottom-1 right-2 z-20 text-[9px] font-bold text-neutral-500 pointer-events-none tracking-tight">
            ⇄ КРУТИТЬ ВЛЕВО-ВПРАВО
          </div>
        </div>

        {/* Tactile Side Nudge Chevrons (- / +) */}
        <button
          type="button"
          onClick={() => nudge(-step)}
          className="absolute left-1 top-1/2 -translate-y-1/2 z-30 w-7 h-7 bg-neutral-900/80 hover:bg-sky-600/80 border border-neutral-700/60 rounded-full flex items-center justify-center text-neutral-300 hover:text-white transition active:scale-90 text-sm font-black"
          title={`Уменьшить на ${step}`}
        >
          −
        </button>
        <button
          type="button"
          onClick={() => nudge(step)}
          className="absolute right-1 top-1/2 -translate-y-1/2 z-30 w-7 h-7 bg-neutral-900/80 hover:bg-sky-600/80 border border-neutral-700/60 rounded-full flex items-center justify-center text-neutral-300 hover:text-white transition active:scale-90 text-sm font-black"
          title={`Увеличить на ${step}`}
        >
          +
        </button>
      </div>
    </div>
  );
};