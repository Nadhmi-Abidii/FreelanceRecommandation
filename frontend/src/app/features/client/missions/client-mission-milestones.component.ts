import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AuthService } from '../../../core/auth/auth';
import { MilestoneDto, MilestoneService } from '../../../services/milestone.service';
import { MissionService } from '../../../services/mission.service';
import { PaymentService, WalletDto, TransactionDto } from '../../../services/payment.service';
import { FeedbackService } from '../../../services/feedback.service';
import { HttpClientModule } from '@angular/common/http';
import { MissionFeedbackButtonComponent } from '../../feedback/mission-feedback-button/mission-feedback-button.component';
import { MissionFeedbackDialogComponent, MissionFeedbackDialogData, MissionFeedbackDialogResult } from '../../feedback/mission-feedback-button/mission-feedback-dialog.component';

@Component({
  standalone: true,
  selector: 'app-client-mission-milestones',
  templateUrl: './client-mission-milestones.component.html',
  styleUrls: ['./client-mission-milestones.component.scss'],
  imports: [
     CommonModule,
  ReactiveFormsModule,
  RouterLink,
  HttpClientModule,
  MatButtonModule,
  MatCardModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatListModule,
  MatProgressBarModule,
  MatSnackBarModule,
  MatChipsModule,
  MatToolbarModule,
  MatMenuModule,
  MatBadgeModule,
  MatDividerModule,
  MatSlideToggleModule,
  MatDialogModule,
  MissionFeedbackButtonComponent
  ]
})
export default class ClientMissionMilestonesComponent implements OnInit {
  private readonly service = inject(MilestoneService);
  private readonly missionService = inject(MissionService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly payments = inject(PaymentService);
  private readonly feedbacks = inject(FeedbackService);
  private readonly dialog = inject(MatDialog);

  missionId = signal<number | null>(null);
  missionTitle = signal<string>('Mission');
  missionStatus = signal<string | null>(null);
  milestones = signal<MilestoneDto[]>([]);
  loading = signal(false);
  sending = signal(false);
  paying = signal(false);
  closing = signal(false);
  walletLoading = signal(false);
  transactionsLoading = signal(false);
  clientWallet = signal<WalletDto | null>(null);
  walletTransactions = signal<TransactionDto[]>([]);
  error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    dueDate: [''],
    orderIndex: [1, [Validators.required, Validators.min(1)]]
  });

  payoutForm = this.fb.nonNullable.group({
    freelancerId: [null as number | null, [Validators.required, Validators.min(1)]],
    topupAmount: [null as number | null, [Validators.min(1)]]
  });

  readonly total = computed(() => this.milestones().reduce((sum, m) => sum + (m.amount ?? 0), 0));
  readonly allPaid = computed(() =>
    !!this.milestones().length &&
    this.milestones().every(m => {
      const status = (m.status || '').toUpperCase();
      return status === 'PAID' || status === 'COMPLETED';
    })
  );
  readonly canFinalize = computed(() => {
    const status = (this.missionStatus() || '').toUpperCase();
    return status === 'PENDING_CLOSURE' && this.allPaid();
  });

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('missionId') ?? params.get('id'));
      if (id) {
        this.missionId.set(id);
        this.fetchMissionTitle(id);
        this.load(id);
        this.loadWallet();
      }
    });
  }

  private fetchMissionTitle(id: number) {
    this.missionService.getMissionById(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        if (res?.success && res?.data) {
          if (res.data.title) this.missionTitle.set(res.data.title);
          this.missionStatus.set(res.data.status || null);
        }
      }
    });
  }

  get missionInfo() {
    const id = this.missionId();
    if (!id) return null;
    return { id, status: this.missionStatus(), title: this.missionTitle() };
  }

  load(id: number) {
    this.loading.set(true);
    this.service.byMission(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.loading.set(false);
        if (!res.success || !Array.isArray(res.data)) {
          this.error.set(res.message || 'Impossible de charger les jalons.');
          this.milestones.set([]);
          return;
        }
        this.milestones.set(res.data);
        this.error.set(null);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.message || err?.message || 'Erreur reseau.');
        this.milestones.set([]);
      }
    });
  }

  create() {
    const missionId = this.missionId();
    if (!missionId) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload: MilestoneDto = {
      mission: { id: missionId },
      title: this.form.controls.title.value,
      description: this.form.controls.description.value ?? '',
      amount: this.form.controls.amount.value ?? 0,
      dueDate: this.form.controls.dueDate.value || undefined,
      orderIndex: this.form.controls.orderIndex.value ?? 1
    };
    this.sending.set(true);
    this.service.create(missionId, payload).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.sending.set(false);
        if (!res.success || !res.data) {
          this.snackBar.open(res.message || 'Creation impossible', 'Fermer', { duration: 2200 });
          return;
        }
        this.form.reset({ title: '', description: '', amount: null, dueDate: '', orderIndex: 1 });
        this.load(missionId);
        this.snackBar.open('Jalon cree avec succes', 'Fermer', { duration: 2000 });
      },
      error: err => {
        this.sending.set(false);
        this.snackBar.open(err?.error?.message || err?.message || 'Erreur reseau', 'Fermer', { duration: 2400 });
      }
    });
  }

  markDelivered(id: number) {
    this.service.deliver(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        const mid = this.missionId();
        if (mid) this.load(mid);
        this.snackBar.open('Jalon marque comme livre', 'Fermer', { duration: 1800 });
      },
      error: err => {
        this.snackBar.open(err?.error?.message || err?.message || 'Action impossible', 'Fermer', { duration: 2200 });
      }
    });
  }

  revert(id: number) {
    this.service.revert(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        const mid = this.missionId();
        if (mid) this.load(mid);
        this.snackBar.open('Jalon remis en attente', 'Fermer', { duration: 1800 });
      },
      error: err => {
        this.snackBar.open(err?.error?.message || err?.message || 'Action impossible', 'Fermer', { duration: 2200 });
      }
    });
  }

  validate(id: number) {
    this.service.validate(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        const mid = this.missionId();
        if (mid) this.load(mid);
        this.snackBar.open('Jalon valide', 'Fermer', { duration: 1800 });
      },
      error: err => {
        this.snackBar.open(err?.error?.message || err?.message || 'Validation impossible', 'Fermer', { duration: 2000 });
      }
    });
  }

  reject(id: number) {
    const reason = prompt('Pourquoi refuser ce jalon ?') || '';
    this.service.reject(id, reason || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        const mid = this.missionId();
        if (mid) this.load(mid);
        this.snackBar.open('Jalon refuse', 'Fermer', { duration: 1800 });
      },
      error: err => {
        this.snackBar.open(err?.error?.message || err?.message || 'Refus impossible', 'Fermer', { duration: 2000 });
      }
    });
  }

  payMilestone(milestone: MilestoneDto) {
    const clientId = this.auth.currentUser()?.userId;
    if (!clientId) {
      this.snackBar.open('Connectez-vous en tant que client pour payer un jalon.', 'Fermer', { duration: 2200 });
      return;
    }
    if (!milestone?.id) return;

    const freelancerId = this.payoutForm.controls.freelancerId.value;
    if (!freelancerId) {
      this.payoutForm.controls.freelancerId.setErrors({ required: true });
      return;
    }

    this.paying.set(true);
    this.service
      .pay(milestone.id, {
        clientId,
        freelancerId,
        paymentMethod: 'WALLET',
        description: `Paiement du jalon ${milestone.title}`
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.paying.set(false);
          if (!res.success) {
            this.snackBar.open(res.message || 'Paiement impossible', 'Fermer', { duration: 2200 });
            return;
          }
          const mid = this.missionId();
          if (mid) this.load(mid);
          this.snackBar.open('Paiement envoye', 'Fermer', { duration: 2000 });
        },
        error: err => {
          this.paying.set(false);
          this.snackBar.open(err?.error?.message || err?.message || 'Paiement impossible', 'Fermer', { duration: 2400 });
        }
      });
  }

  topUpWallet() {
    const clientId = this.auth.currentUser()?.userId;
    const amount = this.payoutForm.controls.topupAmount.value;
    if (!clientId) {
      this.snackBar.open('Connectez-vous pour recharger votre wallet.', 'Fermer', { duration: 2000 });
      return;
    }
    if (!amount || amount <= 0) {
      this.payoutForm.controls.topupAmount.setErrors({ min: true });
      return;
    }

    this.paying.set(true);
    this.service.topUpClientWallet(clientId, amount).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.paying.set(false);
        if (!res.success) {
          this.snackBar.open(res.message || 'Recharge impossible', 'Fermer', { duration: 2000 });
          return;
        }
        this.snackBar.open('Wallet credite', 'Fermer', { duration: 1800 });
        this.loadWallet();
      },
      error: err => {
        this.paying.set(false);
        this.snackBar.open(err?.error?.message || err?.message || 'Recharge impossible', 'Fermer', { duration: 2200 });
      }
    });
  }

  closeMission() {
    const missionId = this.missionId();
    if (!missionId) {
      this.snackBar.open('Mission manquante pour cloturer.', 'Fermer', { duration: 2000 });
      return;
    }

    this.closing.set(true);
    this.missionService.closeMission(missionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.closing.set(false);
          if (!res?.success) {
            this.snackBar.open(res?.message || 'Cloture impossible', 'Fermer', { duration: 2400 });
            return;
          }
          this.missionStatus.set('COMPLETED');
          this.snackBar.open('Mission cloturee. Merci de noter le freelance.', 'Fermer', { duration: 2600 });
          this.openFeedbackDialog(missionId);
        },
        error: (err: any) => {
          this.closing.set(false);
          this.snackBar.open(err?.error?.message || err?.message || 'Cloture impossible', 'Fermer', { duration: 2600 });
        }
      });
  }

  private openFeedbackDialog(missionId: number) {
    const ref = this.dialog.open<MissionFeedbackDialogComponent, MissionFeedbackDialogData, MissionFeedbackDialogResult>(
      MissionFeedbackDialogComponent,
      {
        width: '480px',
        data: { title: this.missionTitle(), context: 'client' }
      }
    );

    ref.afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        if (!result || !result.rating) return;
        this.feedbacks.createFeedback(missionId, {
          rating: result.rating,
          comment: result.comment,
          direction: 'CLIENT_TO_FREELANCER'
        }).subscribe({
          next: res => {
            if (res?.success) {
              this.snackBar.open('Feedback envoye', 'Fermer', { duration: 2200 });
            }
          },
          error: err => {
            this.snackBar.open(err?.error?.message || err?.message || 'Feedback impossible a envoyer', 'Fermer', { duration: 2400 });
          }
        });
      });
  }

  statusLabel(status?: string | null) {
    switch ((status || '').toUpperCase()) {
      case 'PENDING':
        return 'Cree';
      case 'IN_PROGRESS':
        return 'En cours';
      case 'SUBMITTED':
      case 'PENDING_VALIDATION':
        return 'En validation';
      case 'VALIDATED':
      case 'COMPLETED':
        return 'Valide';
      case 'REJECTED':
        return 'Refuse';
      case 'PAID':
        return 'Paye';
      case 'DELIVERED':
        return 'Livre';
      default:
        return 'Cree';
    }
  }

  private loadWallet() {
    const clientId = this.auth.currentUser()?.userId;
    if (!clientId) return;
    this.walletLoading.set(true);
    this.payments.getClientWallet(clientId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.walletLoading.set(false);
        if (res?.success) {
          this.clientWallet.set(res.data ?? null);
        }
      },
      error: () => this.walletLoading.set(false)
    });

    this.transactionsLoading.set(true);
    this.payments.getClientTransactions(clientId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.transactionsLoading.set(false);
        if (res?.success && Array.isArray(res.data)) {
          this.walletTransactions.set(res.data);
        } else {
          this.walletTransactions.set([]);
        }
      },
      error: () => {
        this.transactionsLoading.set(false);
        this.walletTransactions.set([]);
      }
    });
  }
}
