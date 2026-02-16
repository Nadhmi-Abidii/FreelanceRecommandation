import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

// Angular Material
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';

type Stat = { key: string; label: string; value: number; icon: string; color: string };

@Component({
  standalone: true,
  selector: 'app-dashboard',
  imports: [
    CommonModule, RouterLink,
    MatToolbarModule, MatButtonModule, MatIconModule, MatBadgeModule, MatMenuModule, MatSlideToggleModule,
    MatCardModule, MatChipsModule, MatListModule, MatDividerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export default class DashboardComponent {
  private _userName = signal('Client');
  userName = computed(() => this._userName());

  dark = signal(false);
  toggleDark(value: boolean) {
    this.dark.set(value);
    document.documentElement.classList.toggle('dash-dark', value);
  }

  private raw = signal({
    pending: 0,
    running: 2,
    done: 9,
    canceled: 8,
  });

  stats = computed<Stat[]>(() => [
    { key: 'total', label: 'Missions totales', value: this.total(), icon: 'leaderboard', color: '#7c3aed' },
    { key: 'pending', label: 'En attente', value: this.raw().pending, icon: 'hourglass_bottom', color: '#eab308' },
    { key: 'running', label: 'En cours', value: this.raw().running, icon: 'work_history', color: '#10b981' },
    { key: 'done', label: 'Terminées', value: this.raw().done, icon: 'verified', color: '#3b82f6' },
    { key: 'canceled', label: 'Annulées', value: this.raw().canceled, icon: 'block', color: '#ef4444' },
  ]);

  total = computed(() =>
    this.raw().pending + this.raw().running + this.raw().done + this.raw().canceled
  );

  pieStyle = computed(() => {
    const total = Math.max(1, this.total());
    const parts = [
      { color: '#eab308', value: this.raw().pending },
      { color: '#10b981', value: this.raw().running },
      { color: '#3b82f6', value: this.raw().done },
      { color: '#ef4444', value: this.raw().canceled },
    ];
    let acc = 0;
    const segs = parts.map(p => {
      const start = acc;
      acc += (p.value / total) * 360;
      return `${p.color} ${start}deg ${acc}deg`;
    });
    return { background: `conic-gradient(${segs.join(',')})` };
  });

  activity = signal<Array<{ title: string; timeAgo: string; status: 'En cours' | 'Terminée' | 'Annulée' }>>([
    { title: 'Mission #4323 – App mobile', timeAgo: 'il y a 2 h', status: 'En cours' },
    { title: 'Mission #4310 – Landing page', timeAgo: 'hier', status: 'Terminée' },
    { title: 'Mission #4304 – Audit SEO', timeAgo: 'il y a 3 j', status: 'Annulée' },
  ]);

  statusClass(status: string) {
    return (status || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, '-');
  }
}
