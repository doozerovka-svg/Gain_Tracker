export class AudioNotificationService {
  private static audioCtx: AudioContext | null = null;

  private static getAudioContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    if (!this.audioCtx) {
      const AudioCtxClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (AudioCtxClass) {
        this.audioCtx = new AudioCtxClass();
      }
    }
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
    return this.audioCtx;
  }

  static playBeep(freq = 880, duration = 0.2, count = 1) {
    try {
      const ctx = this.getAudioContext();
      if (!ctx) return;

      for (let i = 0; i < count; i++) {
        setTimeout(() => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, ctx.currentTime);
          gain.gain.setValueAtTime(0.3, ctx.currentTime);
          gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + duration);

          osc.connect(gain);
          gain.connect(ctx.destination);

          osc.start(ctx.currentTime);
          osc.stop(ctx.currentTime + duration);
        }, i * (duration + 0.1) * 1000);
      }
    } catch {
      // Audio autoplay policy fallback
    }

    if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
      try {
        navigator.vibrate([200, 100, 200]);
      } catch {
        // vibration not supported
      }
    }
  }

  static playSuccess() {
    this.playBeep(587.33, 0.15, 1); // D5
    setTimeout(() => this.playBeep(880, 0.25, 1), 180); // A5
  }

  static requestNotificationPermission() {
    if (typeof window !== 'undefined' && 'Notification' in window) {
      if (Notification.permission === 'default') {
        Notification.requestPermission();
      }
    }
  }

  static showTimerNotification(title = 'Время отдыха истекло!', body = 'Пора начинать следующий подход.') {
    this.playBeep(987.77, 0.25, 3); // 3 beeps
    if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification(title, {
          body,
          icon: '/favicon.ico',
        });
      } catch {
        // Notification fallback
      }
    }
  }
}
