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
  const [isDragging, setIsDragging] = useState(false);
  const dragStartX = useRef(0);
  const dragStartValue = useRef(value);
  const lastTickValue = useRef(value);

  const clampValue = useCallback((val: number) => {
    const factor = 1 / step;
    const rounded = Math.round(val * factor) / factor;
    const clamped = Math.max(min, Math.min(max, rounded));
    return Number(clamped.toFixed(step < 1 ? 1 : 0));
  }, [min, max, step]);

  const handlePointerDown = (e: React.PointerEvent) => {
    setIsDragging(true);
    dragStartX.current = e.clientX;
    dragStartValue.current = value;
    lastTickValue.current = value;
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!isDragging) return;
    const deltaX = e.clientX - dragStartX.current;
    // 1 step per ~14px of horizontal drag
    const pixelsPerStep = 14;
    const deltaSteps = deltaX / pixelsPerStep;
    const newValue = clampValue(dragStartValue.current + deltaSteps * step);

    if (newValue !== value) {
      onChange(newValue);
      if (Math.abs(newValue - lastTickValue.current) >= step) {
        AudioNotificationService.playClickTick();
        lastTickValue.current = newValue;
      }
    }
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    setIsDragging(false);
    try {
      (e.target as HTMLElement).releasePointerCapture(e.pointerId);
    } catch {
      // ignore
    }
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY < 0 ? step : -step;
    const next = clampValue(value + delta);
    if (next !== value) {
      onChange(next);
      AudioNotificationService.playClickTick();
    }
  };

  // Generate graduation lines for the 3D cylinder
  // We simulate a cylindrical drum with 31 ticks visible (-15 to +15 relative to center)
  const ticks = [];
  const visibleRange = 16;
  for (let i = -visibleRange; i <= visibleRange; i++) {
    const tickVal = value + i * step;
    const isMajor = Math.round(tickVal / (step * 5)) === tickVal / (step * 5);
    const isMid = Math.round(tickVal / (step * 2)) === tickVal / (step * 2);
    
    // Cylindrical projection factor (-1 to 1)
    const norm = i / visibleRange;
    const angle = norm * (Math.PI / 2.2); // curve angle
    const cosVal = Math.cos(angle);
    const opacity = Math.max(0.1, cosVal ** 1.8);
    const height = isMajor ? 36 : isMid ? 24 : 14;
    const posX = 50 + Math.sin(angle) * 46; // percentage position 4% to 96%

    ticks.push({
      index: i,
      val: tickVal,
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
              onClick={() => {
                const next = clampValue(value + qs);
                onChange(next);
                AudioNotificationService.playClickTick();
              }}
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
      <div
        ref={containerRef}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        onWheel={handleWheel}
        className={`relative h-20 bg-gradient-to-b from-neutral-900 via-neutral-950 to-neutral-900 border border-neutral-800 rounded-xl overflow-hidden cursor-ew-resize select-none touch-none shadow-inner ${
          isDragging ? 'ring-2 ring-sky-400' : ''
        }`}
        title="Потяните влево или вправо, чтобы прокрутить колесо"
      >
        {/* Top/Bottom Metallic Rim Shading */}
        <div className="absolute inset-x-0 top-0 h-2 bg-gradient-to-b from-neutral-700/30 to-transparent pointer-events-none" />
        <div className="absolute inset-x-0 bottom-0 h-2 bg-gradient-to-t from-neutral-700/30 to-transparent pointer-events-none" />

        {/* Left/Right Cylinder Vignette (Simulating side round curvature) */}
        <div className="absolute inset-y-0 left-0 w-12 bg-gradient-to-r from-black via-black/80 to-transparent z-10 pointer-events-none" />
        <div className="absolute inset-y-0 right-0 w-12 bg-gradient-to-l from-black via-black/80 to-transparent z-10 pointer-events-none" />

        {/* Graduation Tick Marks Container */}
        <div className="absolute inset-0 flex items-center">
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
              {t.isMajor && t.val >= min && t.val <= max && (
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
    </div>
  );
};