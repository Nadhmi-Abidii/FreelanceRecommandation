import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { AuthService } from '../../../core/auth/auth';
import {
  CreateUserInput,
  ManagedRole,
  ManagedStatus,
  ManagedUser,
  UsersService
} from '../../../core/users/users.service';

import AdminUserDialogComponent, {
  AdminUserDialogResult
} from '../dialogs/admin-user-dialog/admin-user-dialog.component';

const ROLE_LABELS: Record<ManagedRole, string> = {
  ROLE_ADMIN: 'Administrateur',
  ROLE_CLIENT: 'Client',
  ROLE_FREELANCER: 'Freelance'
};

const ROLE_COLORS: Record<ManagedRole, string> = {
  ROLE_ADMIN: '#a855f7',
  ROLE_CLIENT: '#3b82f6',
  ROLE_FREELANCER: '#10b981'
};

const ROLE_CLASS: Record<ManagedRole, string> = {
  ROLE_ADMIN: 'admin',
  ROLE_CLIENT: 'client',
  ROLE_FREELANCER: 'freelancer'
};

@Component({
  standalone: true,
  selector: 'app-admin-users',
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './admin-users.component.html',
  styleUrls: [
    '../../dashboard/dashboard.component.scss',
    '../admin-dashboard.component.scss',
    // './admin-users.component.scss'
  ]
})
export default class AdminUsersComponent {
  private readonly auth = inject(AuthService);
  private readonly usersService = inject(UsersService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  private readonly adminFallbackName = 'Administrateur';

  readonly adminName = computed(() => {
    const user = this.auth.currentUser();
    if (!user) return this.adminFallbackName;
    const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return fullName.length > 0 ? fullName : this.adminFallbackName;
  });

  readonly dark = signal(false);
  toggleDark(value: boolean) {
    this.dark.set(value);
    document.documentElement.classList.toggle('dash-dark', value);
  }

  readonly roleColors = ROLE_COLORS;

  readonly users = signal<ManagedUser[]>([]);
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading.set(true);
    this.error.set(null);
    this.usersService.getAllUsers().subscribe({
      next: list => {
        this.users.set(list);
        this.loading.set(false);
      },
      error: err => {
        const message =
          err?.error?.message || err?.message || 'Impossible de récupérer les utilisateurs.';
        this.error.set(message);
        this.loading.set(false);
      }
    });
  }

  /** Ouvre le popup d'ajout utilisateur */
  openCreateDialog() {
    const ref = this.dialog.open<AdminUserDialogComponent, null, AdminUserDialogResult>(
      AdminUserDialogComponent,
      {
        width: '520px',
        panelClass: 'tw-dialog'
      }
    );

    ref.afterClosed().subscribe((result?: CreateUserInput) => {
      if (!result) return; // dialog annulé

      this.creating.set(true);
      this.usersService.createUser(result).subscribe({
        next: created => {
          this.users.update(list => [created, ...list]);
          this.snack.open('Utilisateur créé avec succès.', 'Fermer', { duration: 2500 });
          this.creating.set(false);
        },
        error: err => {
          const message = err?.error?.message || err?.message || 'Création impossible.';
          this.snack.open(message, 'Fermer', { duration: 3500 });
          this.creating.set(false);
        }
      });
    });
  }

  readonly totalUsers = computed(() => this.users().length);
  readonly inactiveUsers = computed(() =>
    this.users().filter(u => u.status === 'Inactif').length
  );

  readonly roleDistribution = computed(() => {
    const base: Record<ManagedRole, number> = {
      ROLE_ADMIN: 0,
      ROLE_CLIENT: 0,
      ROLE_FREELANCER: 0
    };
    return this.users().reduce((acc, user) => {
      acc[user.role] = (acc[user.role] ?? 0) + 1;
      return acc;
    }, { ...base });
  });

  readonly sortedUsers = computed(() =>
    [...this.users()].sort(
      (a, b) => new Date(b.joined).getTime() - new Date(a.joined).getTime()
    )
  );

  readonly stats = computed(() => {
    const distribution = this.roleDistribution();
    return [
      {
        key: 'total',
        label: 'Utilisateurs',
        value: this.totalUsers(),
        icon: 'group',
        color: '#7c3aed'
      },
      {
        key: 'clients',
        label: 'Clients',
        value: distribution.ROLE_CLIENT,
        icon: 'business_center',
        color: ROLE_COLORS.ROLE_CLIENT
      },
      {
        key: 'freelancers',
        label: 'Freelances',
        value: distribution.ROLE_FREELANCER,
        icon: 'travel_explore',
        color: ROLE_COLORS.ROLE_FREELANCER
      },
      {
        key: 'admins',
        label: 'Admins',
        value: distribution.ROLE_ADMIN,
        icon: 'shield_person',
        color: ROLE_COLORS.ROLE_ADMIN
      },
      {
        key: 'inactive',
        label: 'Inactifs',
        value: this.inactiveUsers(),
        icon: 'pause_circle',
        color: '#f97316'
      }
    ];
  });

  readonly rolePieStyle = computed(() => {
    const distribution = this.roleDistribution();
    const total = Math.max(1, this.totalUsers());
    let start = 0;
    const segments = (Object.keys(distribution) as ManagedRole[]).map(role => {
      const from = start;
      start += ((distribution[role] ?? 0) / total) * 360;
      return `${ROLE_COLORS[role]} ${from}deg ${start}deg`;
    });
    return { background: `conic-gradient(${segments.join(',')})` };
  });

  roleLabel(role: ManagedRole) {
    return ROLE_LABELS[role];
  }

  roleChipClass(role: ManagedRole) {
    return `role-${ROLE_CLASS[role]}`;
  }

  statusClass(status: ManagedStatus) {
    return status === 'Actif' ? 'is-active' : 'is-inactive';
  }

  formatJoinDate(value: string) {
    const dt = new Date(value);
    return Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(dt);
  }
}
