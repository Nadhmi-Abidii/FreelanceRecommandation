import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';

import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AuthService } from '../../core/auth/auth';

import { HttpClient, HttpClientModule, HttpHeaders, HttpParams } from '@angular/common/http';
import { forkJoin, map } from 'rxjs';

import ConfirmDialogComponent, { ConfirmDialogData } from './dialogs/confirm-dialog/confirm-dialog.component';
import EditUserDialogComponent, { EditUserDialogData, EditUserDialogResult } from './dialogs/edit-user-dialog/edit-user-dialog.component';

/* ===================== Types ===================== */
type ManagedRole = 'ROLE_CLIENT' | 'ROLE_FREELANCER' | 'ROLE_ADMIN';
type ManagedStatus = 'Actif' | 'Inactif';

export type ManagedUser = {
  id: number;
  name: string;
  email: string;
  role: ManagedRole;
  status: ManagedStatus;
  missions: number;
  joined: string; // ISO date
};

type MessageResponse<T> = { success: boolean; message: string; data: T };
type Page<T> = { content: T[]; totalElements: number; totalPages: number; size: number; number: number };

type ClientDto = {
  id: number; firstName?: string; lastName?: string; email: string;
  createdAt?: string; isActive?: boolean; missionsCount?: number;
};

type FreelancerDto = {
  id: number; firstName?: string; lastName?: string; email: string;
  createdAt?: string; isActive?: boolean; missionsCount?: number;
};

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
  selector: 'app-admin-dashboard',
  imports: [
    CommonModule, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatBadgeModule, MatMenuModule,
    MatSlideToggleModule, MatCardModule, MatChipsModule, MatDividerModule, MatTooltipModule,
    MatDialogModule, MatSnackBarModule, HttpClientModule
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['../dashboard/dashboard.component.scss', './admin-dashboard.component.scss']
})
export default class AdminDashboardComponent {
  /* Services */
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);


  /* Config */
  private readonly baseUrl = 'http://localhost:9020';

  /* Header */
  private readonly adminFallbackName = 'Administrateur';
  readonly adminName = computed(() => {
    const user = this.auth.currentUser();
    if (!user) return this.adminFallbackName;
    const full = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return full || this.adminFallbackName;
  });

  readonly dark = signal(false);
  toggleDark(value: boolean) {
    this.dark.set(value);
    document.documentElement.classList.toggle('dash-dark', value);
  }

  /* Data */
  private readonly users = signal<ManagedUser[]>([]);
  constructor() { this.loadUsers(); }

  /* ===================== API helpers ===================== */
  private getAuthHeaders(): HttpHeaders {
    const token =
      (globalThis.localStorage && localStorage.getItem('token')) ||
      (this.auth as any)?.getToken?.() || '';
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }

  private roleBasePath(role: ManagedRole) {
    if (role === 'ROLE_CLIENT') return 'clients';
    if (role === 'ROLE_FREELANCER') return 'freelancers';
    return 'users'; // au cas où tu ajoutes un endpoint admin
  }

  /* List initiale */
  public loadUsers() {
    const params = new HttpParams().set('page', 0).set('size', 100);
    const headers = this.getAuthHeaders();

    const clients$ = this.http
      .get<MessageResponse<Page<ClientDto>>>(`${this.baseUrl}/clients`, { params, headers })
      .pipe(map(res => res.data?.content ?? []));

    const freelancers$ = this.http
      .get<MessageResponse<Page<FreelancerDto>>>(`${this.baseUrl}/freelancers`, { params, headers })
      .pipe(map(res => res.data?.content ?? []));

    forkJoin({ clients: clients$, freelancers: freelancers$ })
      .pipe(map(({ clients, freelancers }) => {
        const mappedClients: ManagedUser[] = clients.map(c => ({
          id: c.id,
          name: [c.firstName, c.lastName].filter(Boolean).join(' ').trim() || 'Client',
          email: c.email,
          role: 'ROLE_CLIENT',
          status: c.isActive === false ? 'Inactif' : 'Actif',
          missions: c.missionsCount ?? 0,
          joined: c.createdAt ?? new Date().toISOString()
        }));
        const mappedFreelancers: ManagedUser[] = freelancers.map(f => ({
          id: f.id,
          name: [f.firstName, f.lastName].filter(Boolean).join(' ').trim() || 'Freelance',
          email: f.email,
          role: 'ROLE_FREELANCER',
          status: f.isActive === false ? 'Inactif' : 'Actif',
          missions: f.missionsCount ?? 0,
          joined: f.createdAt ?? new Date().toISOString()
        }));
        return [...mappedClients, ...mappedFreelancers];
      }))
      .subscribe({
        next: list => this.users.set(list),
        error: () => this.users.set([])
      });
  }


  /* ===================== Template bindings (existing) ===================== */
  readonly sortedUsers = computed(() =>
    [...this.users()].sort((a, b) => new Date(b.joined).getTime() - new Date(a.joined).getTime())
  );
  readonly highlightedUsers = computed(() => this.sortedUsers().slice(0, 5));
  readonly totalUsers = computed(() => this.users().length);
  readonly inactiveUsers = computed(() => this.users().filter(u => u.status === 'Inactif').length);
  readonly roleColors = ROLE_COLORS;
  readonly roleDistribution = computed(() => {
    const initial: Record<ManagedRole, number> = { ROLE_ADMIN: 0, ROLE_CLIENT: 0, ROLE_FREELANCER: 0 };
    return this.users().reduce((acc, u) => { acc[u.role] = (acc[u.role] ?? 0) + 1; return acc; }, { ...initial });
  });
  readonly stats = computed(() => {
    const d = this.roleDistribution();
    return [
      { key: 'total', label: 'Utilisateurs', value: this.totalUsers(), icon: 'group', color: '#7c3aed' },
      { key: 'clients', label: 'Clients', value: d.ROLE_CLIENT, icon: 'business_center', color: ROLE_COLORS.ROLE_CLIENT },
      { key: 'freelancers', label: 'Freelances', value: d.ROLE_FREELANCER, icon: 'travel_explore', color: ROLE_COLORS.ROLE_FREELANCER },
      { key: 'admins', label: 'Admins', value: d.ROLE_ADMIN, icon: 'shield', color: ROLE_COLORS.ROLE_ADMIN },
      { key: 'inactive', label: 'Inactifs', value: this.inactiveUsers(), icon: 'pause_circle', color: '#f97316' }
    ];
  });
  readonly rolePieStyle = computed(() => {
    const d = this.roleDistribution();
    const total = Math.max(1, this.totalUsers());
    let acc = 0;
    const segments = (Object.keys(d) as ManagedRole[]).map(role => {
      const start = acc;
      acc += ((d[role] ?? 0) / total) * 360;
      return `${ROLE_COLORS[role]} ${start}deg ${acc}deg`;
    });
    return { background: `conic-gradient(${segments.join(',')})` };
  });

  roleLabel(role: ManagedRole) { return ROLE_LABELS[role]; }
  roleChipClass(role: ManagedRole) { return `role-${ROLE_CLASS[role]}`; }
  statusClass(status: ManagedStatus) { return status === 'Actif' ? 'is-active' : 'is-inactive'; }
  formatJoinDate(v: string) {
    const dt = new Date(v);
    return Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }).format(dt);
  }
  totalLabel() { return `${this.totalUsers()} utilisateurs`; }
  inactivePercent() {
    const total = Math.max(1, this.totalUsers());
    return (this.inactiveUsers() / total) * 100;
  }

  /* ===================== NEW: Dialog actions ===================== */

  /** Called by (click)="openEditDialog(user)" in template */
  openEditDialog(user: ManagedUser) {
    const ref = this.dialog.open<EditUserDialogComponent, EditUserDialogData, EditUserDialogResult>(
      EditUserDialogComponent,
      {
        width: '560px',
        panelClass: 'tw-dialog',
        data: { user }
      }
    );

    ref.afterClosed().subscribe(result => {
      if (!result) return; // cancel
      this.updateUser(user, result);
    });
  }

  /** Called by (click)="openDeleteDialog(user)" in template */
  openDeleteDialog(user: ManagedUser) {
    const data: ConfirmDialogData = {
      title: 'Supprimer l’utilisateur',
      message: `Confirmer la suppression définitive de « ${user.name} » ? Cette action est irréversible.`,
      confirmText: 'Supprimer',
      cancelText: 'Annuler',
      warn: true
    };

    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '520px',
      panelClass: 'tw-dialog',
      data
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.deleteUser(user);
    });
  }

  /* ===================== HTTP actions for edit/delete ===================== */

  private updateUser(user: ManagedUser, result: EditUserDialogResult) {
    const headers = this.getAuthHeaders();
    const base = this.roleBasePath(user.role);
    const id = user.id;

    // Mapper le payload selon ce que tes endpoints acceptent.
    // Ici: /clients/{id} et /freelancers/{id} acceptent des champs simples.
    const [firstName, ...rest] = (result.name ?? '').trim().split(' ');
    const lastName = rest.join(' ').trim();

    const payload: any = {
      email: result.email,
      isActive: result.status === 'Actif',
    };
    if (base === 'clients' || base === 'freelancers') {
      payload.firstName = firstName || null;
      payload.lastName = lastName || null;
    }

    this.http.put<MessageResponse<any>>(`${this.baseUrl}/${base}/${id}`, payload, { headers })
      .subscribe({
        next: (res) => {
          // maj locale
          this.users.update(list => list.map(u =>
            u.id === id ? ({
              ...u,
              name: result.name,
              email: result.email,
              role: result.role,
              status: result.status
            }) : u
          ));
          this.snack.open('Utilisateur modifié.', 'Fermer', { duration: 2500 });
        },
        error: (e) => {
          const msg = e?.error?.message || e?.message || 'Erreur API';
          this.snack.open(msg, 'Fermer', { duration: 3500 });
        }
      });
  }

  private deleteUser(user: ManagedUser) {
    const headers = this.getAuthHeaders();
    const base = this.roleBasePath(user.role);
    const id = user.id;

    this.http.delete<MessageResponse<any>>(`${this.baseUrl}/${base}/${id}`, { headers })
      .subscribe({
        next: () => {
          // retire du tableau
          this.users.update(list => list.filter(u => u.id !== id));
          this.snack.open('Utilisateur supprimé.', 'Fermer', { duration: 2500 });
        },
        error: (e) => {
          const msg = e?.error?.message || e?.message || 'Erreur API';
          this.snack.open(msg, 'Fermer', { duration: 3500 });
        }
      });
  }

  logout() {
  this.auth.logout();          // supprime token + reset user/role
  this.router.navigate(['/login']);
}

}
