import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

// navbar
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';

// Auth (même import que ton freelancer-dashboard)
import { AuthService } from '../../../core/auth/auth';

import { PortfolioPayload, PortfolioService } from '../../../services/portfolio.service';

@Component({
  standalone: true,
  selector: 'app-freelancer-portfolio',
  templateUrl: './freelancer-portfolio.component.html',
  styleUrls: ['./freelancer-portfolio.component.scss'],
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatProgressBarModule,
    MatSnackBarModule,

    // navbar + routing
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatMenuModule,
    MatSlideToggleModule,
    MatBadgeModule,
    MatDividerModule,
  ]
})
export default class FreelancerPortfolioComponent implements OnInit {
  private readonly portfolioService = inject(PortfolioService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly portfolio = signal<PortfolioPayload | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly paidMilestones = computed(() => this.portfolio()?.paidMilestones ?? []);
  readonly totalEarned = computed(() => this.portfolio()?.totalAmountEarned ?? 0);

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);

    this.portfolioService
      .getPortfolio()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          if (!res?.success || !res.data) {
            this.error.set(res?.message || 'Impossible de charger votre portfolio.');
            return;
          }
          this.portfolio.set(res.data);
        },
        error: err => {
          this.loading.set(false);
          const message =
            err?.error?.message || err?.message || 'Impossible de charger votre portfolio.';
          this.error.set(message);
          this.snackBar.open(message, 'Fermer', { duration: 2600 });
        }
      });
  }

  formatAmount(amount: number, currency?: string | null) {
    const cur = currency || 'EUR';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: cur,
      maximumFractionDigits: 2
    }).format(amount ?? 0);
  }

  // === NAVBAR ACTIONS ===
  toggleDark(enabled: boolean) {
    document.documentElement.classList.toggle('freelancer-dark-mode', enabled);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
