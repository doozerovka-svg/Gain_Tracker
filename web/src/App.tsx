import React, { useState } from 'react';
import type { TabType } from './types';
import { ActiveWorkoutTab } from './components/ActiveWorkoutTab';
import { CalendarTab } from './components/CalendarTab';
import { HistoryTab } from './components/HistoryTab';
import { AnalyticsTab } from './components/AnalyticsTab';
import { BodyMeasurementsTab } from './components/BodyMeasurementsTab';
import { ExportTab } from './components/ExportTab';
import { Dumbbell, Calendar, History, TrendingUp, Ruler, Download, Smartphone } from 'lucide-react';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('active');
  const [refreshKey, setRefreshKey] = useState(0);

  const handleRefresh = () => {
    setRefreshKey((k) => k + 1);
  };

  return (
    <div className="min-h-screen bg-black text-slate-100 flex flex-col justify-between selection:bg-blue-600 selection:text-white">
      {/* Top App Bar (Pure AMOLED Black) */}
      <header className="sticky top-0 z-40 bg-black/90 backdrop-blur border-b border-neutral-900 px-4 py-3 shadow-md">
        <div className="max-w-xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-sky-500 to-blue-600 flex items-center justify-center text-white shadow-md shadow-sky-500/20">
              <Dumbbell size={20} />
            </div>
            <div>
              <h1 className="text-sm font-extrabold tracking-tight text-white uppercase">
                Gain Tracker 2.0
              </h1>
              <span className="text-[10px] font-medium text-sky-400 block -mt-0.5">
                Local-First • Без ИИ • Силовой прогресс
              </span>
            </div>
          </div>

          <a
            href="./workout-tracker.apk?v=2.0"
            download="workout-tracker-v2.0.apk"
            className="touch-target px-3 py-1.5 bg-sky-500/10 hover:bg-sky-500/20 text-sky-400 hover:text-sky-300 border border-sky-500/30 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition"
            title="Скачать APK для Android v2.0"
          >
            <Smartphone size={15} />
            <span>APK v2.0</span>
          </a>
        </div>
      </header>

      {/* Main Content View */}
      <main className="flex-1 px-3 sm:px-4 py-4 max-w-xl mx-auto w-full">
        {activeTab === 'active' && <ActiveWorkoutTab onRefresh={handleRefresh} />}
        {activeTab === 'calendar' && (
          <CalendarTab
            key={refreshKey}
            onRefresh={handleRefresh}
            onOpenActiveTab={() => setActiveTab('active')}
          />
        )}
        {activeTab === 'history' && <HistoryTab key={refreshKey} onRefresh={handleRefresh} />}
        {activeTab === 'analytics' && <AnalyticsTab key={refreshKey} />}
        {activeTab === 'body' && <BodyMeasurementsTab key={refreshKey} onRefresh={handleRefresh} />}
        {activeTab === 'export' && <ExportTab key={refreshKey} />}
      </main>

      {/* Bottom Navigation Bar (Pure AMOLED Black Fixed) */}
      <nav className="fixed bottom-0 left-0 right-0 z-40 bg-black/95 backdrop-blur border-t border-neutral-900 px-1 py-1 shadow-2xl">
        <div className="max-w-xl mx-auto grid grid-cols-6 gap-0.5">
          <button
            onClick={() => setActiveTab('active')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'active' ? 'text-sky-400 font-bold bg-sky-950/40' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <Dumbbell size={18} />
            <span className="text-[9px] mt-1">Зал</span>
          </button>

          <button
            onClick={() => setActiveTab('calendar')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'calendar' ? 'text-sky-400 font-bold bg-sky-950/40' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <Calendar size={18} />
            <span className="text-[9px] mt-1">План</span>
          </button>

          <button
            onClick={() => setActiveTab('history')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'history' ? 'text-sky-400 font-bold bg-sky-950/40' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <History size={18} />
            <span className="text-[9px] mt-1">История</span>
          </button>

          <button
            onClick={() => setActiveTab('analytics')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'analytics' ? 'text-sky-400 font-bold bg-sky-950/40' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <TrendingUp size={18} />
            <span className="text-[9px] mt-1">Ранги</span>
          </button>

          <button
            onClick={() => setActiveTab('body')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'body' ? 'text-sky-400 font-bold bg-sky-950/40' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <Ruler size={18} />
            <span className="text-[9px] mt-1">Замеры</span>
          </button>

          <button
            onClick={() => setActiveTab('export')}
            className={`touch-target flex flex-col items-center justify-center py-1.5 rounded-xl transition ${
              activeTab === 'export' ? 'text-sky-400 font-bold bg-sky-950/40' : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <Download size={18} />
            <span className="text-[9px] mt-1">Экспорт</span>
          </button>
        </div>
      </nav>
    </div>
  );
};

export default App;
