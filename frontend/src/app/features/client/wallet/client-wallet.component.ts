import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

// + modules pour la navbar
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';

// + AuthService pour logout
import { AuthService } from '../../../services/auth.service';

import { WalletService, WalletSnapshot, WalletTransaction } from '../../../services/wallet.service';

@Component({
  standalone: true,
  selector: 'app-client-wallet',
  templateUrl: './client-wallet.component.html',
  styleUrls: ['./client-wallet.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSnackBarModule,

    // navbar + routing
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSlideToggleModule,
    MatMenuModule,
    MatBadgeModule,
    MatDividerModule,
  ]
})
export default class ClientWalletComponent implements OnInit {
  private readonly walletService = inject(WalletService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly wallet = signal<WalletSnapshot | null>(null);
  readonly loading = signal(false);
  readonly recharging = signal(false);
  readonly error = signal<string | null>(null);
  readonly showRecharge = signal(false);

  readonly form = this.fb.nonNullable.group({
    amount: [null as number | null, [Validators.required, Validators.min(1)]]
  });

  readonly transactions = computed<WalletTransaction[]>(() =>
    (this.wallet()?.transactions ?? []).filter(
      tx => (tx.type || '').toUpperCase() === 'DEBIT' || (tx.type || '').toUpperCase() === 'PAYMENT'
    )
  );
  readonly balance = computed(() => this.wallet()?.balance ?? 0);
  readonly currency = computed(() => this.wallet()?.currency ?? 'EUR');

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);

    this.walletService
      .getClientWallet()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: wallet => {
          this.loading.set(false);
          this.wallet.set(wallet);
        },
        error: err => {
          this.loading.set(false);
          const message = err?.error?.message || err?.message || 'Impossible de charger le wallet.';
          this.error.set(message);
        }
      });
  }

  toggleRecharge() {
    this.showRecharge.update(value => !value);
  }

  recharge() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const amount = this.form.controls.amount.value;
    if (!amount || amount <= 0) {
      this.form.controls.amount.setErrors({ min: true });
      return;
    }

    this.recharging.set(true);
    this.walletService
      .recharge(amount)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: wallet => {
          this.recharging.set(false);
          this.wallet.set(wallet);
          this.form.reset({ amount: null });
          this.showRecharge.set(false);
          this.snackBar.open('Recharge effectuée avec succès.', 'Fermer', { duration: 2400 });
        },
        error: err => {
          this.recharging.set(false);
          const message = err?.error?.message || err?.message || 'Recharge impossible.';
          this.snackBar.open(message, 'Fermer', { duration: 2600 });
        }
      });
  }

  formatAmount(amount: number, currency?: string | null) {
    const cur = currency || this.currency() || 'EUR';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: cur,
      maximumFractionDigits: 2
    }).format(amount ?? 0);
  }

  txChipColor(type: WalletTransaction['type']) {
    switch ((type || '').toUpperCase()) {
      case 'RECHARGE':
      case 'CREDIT':
        return 'primary';
      case 'PAYMENT':
      case 'DEBIT':
        return 'accent';
      default:
        return 'default';
    }
  }

  // === NAVBAR ACTIONS ===
  toggleDark(on: boolean) {
    const root = document.documentElement;
    root.classList.toggle('dash-dark', !!on);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
