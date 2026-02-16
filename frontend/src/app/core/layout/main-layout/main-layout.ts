import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../auth/auth';

@Component({
  standalone: true,
  selector: 'app-main-layout',
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule
  ],
  template: `
    <mat-toolbar color="primary" class="topbar">
      <div class="brand" routerLink="/dashboard">FreelancePro</div>

      <nav class="nav">
        <a mat-button routerLink="/dashboard" routerLinkActive="active">Tableau de bord</a>
        <a mat-button routerLink="/missions/new" routerLinkActive="active">Creer une mission</a>
        <a mat-button routerLink="/missions" routerLinkActive="active">Mes missions</a>
        <a mat-button routerLink="/candidatures" routerLinkActive="active">Candidatures</a>
        <a mat-button routerLink="/discussions" routerLinkActive="active">Discussions</a>
        <a mat-button *ngIf="isClient()" routerLink="/client/wallet" routerLinkActive="active">Wallet</a>
        <a mat-button *ngIf="isFreelancer()" routerLink="/freelancer/portfolio" routerLinkActive="active">Portfolio</a>
        <a mat-button *ngIf="isFreelancer()" routerLink="/freelancer/candidatures" routerLinkActive="active">Mes candidatures</a>
        <a mat-button *ngIf="isLoggedIn()" routerLink="/messages" routerLinkActive="active">Messages</a>
      </nav>

      <span class="spacer"></span>
      <button mat-icon-button aria-label="Notifications"><mat-icon>notifications</mat-icon></button>
      <button mat-icon-button aria-label="Profil"><mat-icon>account_circle</mat-icon></button>
    </mat-toolbar>

    <div class="page">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .topbar { position: sticky; top:0; z-index: 10; }
    .brand { font-weight: 700; cursor: pointer; margin-right: 20px; }
    .nav a.active { background: rgba(255,255,255,.15); }
    .spacer { flex: 1 1 auto; }
    .page { padding: 24px; }
    @media (max-width: 900px){
      .nav a { font-size: 12px; padding: 0 6px; }
    }
  `]
})
export default class MainLayout {
  private readonly auth = inject(AuthService);

  readonly isClient = computed(() => (this.auth.role() || '').toUpperCase() === 'ROLE_CLIENT');
  readonly isFreelancer = computed(
    () => (this.auth.role() || '').toUpperCase() === 'ROLE_FREELANCER'
  );
  readonly isLoggedIn = computed(() => !!this.auth.isLoggedIn());
}
