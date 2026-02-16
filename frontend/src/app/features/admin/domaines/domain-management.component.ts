import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';

import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../../core/auth/auth';
import { DomainesService } from '../../../services/domaines.service';
import { Domaine, DomainePage } from '../../../models/domaine.model';
import { CompetencesService } from '../../../services/competences.service';
import { Competence, CompetenceLevel, CompetencePage } from '../../../models/competence.model';

// ⚠️ IMPORTANT: export default !
@Component({
  standalone: true,
  selector: 'app-admin-domain-management',
  imports: [
    CommonModule, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatBadgeModule, MatMenuModule,
    MatSlideToggleModule, MatCardModule, MatDividerModule, MatTooltipModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, ReactiveFormsModule, MatSnackBarModule,
    MatPaginatorModule, MatProgressSpinnerModule
  ],
  templateUrl: './domain-management.component.html',
  styleUrls: ['./domain-management.component.scss']
})
export default class AdminDomainManagementComponent {
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(DomainesService);
  private readonly competenceApi = inject(CompetencesService);
  private readonly router = inject(Router); // 👈 ajouter ça


  private readonly df = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' });

  readonly dark = signal(false);

  // ================== FORM DOMAINES ==================
  readonly domainForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    description: ['', [Validators.maxLength(160)]]
  });
  readonly domainNameControl = this.domainForm.controls.name;
  readonly domainDescriptionControl = this.domainForm.controls.description;

  // ================== FORM COMPÉTENCES ==================
  readonly competenceLevels: CompetenceLevel[] = [
    'BEGINNER',
    'INTERMEDIATE',
    'ADVANCED',
    'EXPERT'
  ];

  readonly competenceForm = this.fb.nonNullable.group({
    // 🔗 Domaine choisi dans le dropdown
    domaineId: [0, [Validators.min(1)]],
    name: ['', [Validators.required, Validators.maxLength(80)]],
    description: ['', [Validators.maxLength(200)]],
    level: [''],
    yearsOfExperience: [0, [Validators.min(0), Validators.max(60)]],
  });

  readonly competenceDomainControl = this.competenceForm.controls.domaineId;
  readonly competenceNameControl = this.competenceForm.controls.name;
  readonly competenceDescriptionControl = this.competenceForm.controls.description;

  // ============= STATE DOMAINES =============
  readonly selectedDomainId = signal<number | null>(null);
  readonly editingDomainId = signal<number | null>(null);

  readonly domainSearch = signal('');

  // paging domaines
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly total = signal(0);
  readonly loading = signal(false);

  readonly domains = signal<Domaine[]>([]);

  // 🔽 liste des domaines actifs pour le dropdown des compétences
  readonly activeDomains = signal<Domaine[]>([]);

  // ============= STATE COMPÉTENCES =============
  readonly competencePageIndex = signal(0);
  readonly competencePageSize = signal(10);
  readonly competenceTotal = signal(0);
  readonly competenceLoading = signal(false);

  readonly competences = signal<Competence[]>([]);
  readonly selectedCompetenceId = signal<number | null>(null);
  readonly editingCompetenceId = signal<number | null>(null);

  constructor() {
    // auto-load domaines quand la recherche change
    effect(() => {
      const q = this.domainSearch().trim();
      this.pageIndex.set(0);
      this.fetchDomains(q);
    });

    // initial load competences
    this.fetchCompetences();

    // charger les domaines actifs pour le dropdown des compétences
    this.loadActiveDomains();
  }

  // ============= COMPUTED =============
  readonly totalDomains = computed(() => this.total());
  readonly totalDomainSkills = computed(() =>
    this.domains().reduce((acc, d) => acc + (d.skills?.length ?? 0), 0)
  );
  readonly richestDomain = computed(() => {
    const list = [...this.domains()];
    list.sort((a, b) => (b.skills?.length ?? 0) - (a.skills?.length ?? 0) || a.name.localeCompare(b.name));
    return list[0] ?? null;
  });
  readonly topDomainLabel = computed(() => {
    const d = this.richestDomain();
    return d ? `${d.name} • ${(d.skills?.length ?? 0)} compétences` : 'Aucun domaine';
  });

  readonly selectedDomain = computed(() => {
    const id = this.selectedDomainId();
    if (id == null) return null;
    return this.domains().find(d => d.id === id) ?? null;
  });

  readonly isEditingDomain = computed(() => this.editingDomainId() !== null);

  readonly totalCompetences = computed(() => this.competenceTotal());

  readonly selectedCompetence = computed(() => {
    const id = this.selectedCompetenceId();
    if (id == null) return null;
    return this.competences().find(c => c.id === id) ?? null;
  });

  readonly isEditingCompetence = computed(() => this.editingCompetenceId() !== null);

  readonly adminName = computed(() => {
    const user = this.auth.currentUser();
    if (!user) return 'Administrateur';
    const full = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return full || 'Administrateur';
  });

  // ============= UI =============
  toggleDark(value: boolean) {
    this.dark.set(value);
    document.documentElement.classList.toggle('dash-dark', value);
  }

  // search domaines
  setDomainSearch(v: string) { this.domainSearch.set(v); }
  clearDomainSearch() { if (this.domainSearch()) this.domainSearch.set(''); }

  // table helpers
  trackDomain = (_: number, d: Domaine) => d.id;
  trackSkill  = (_: number, s: string) => s;
  trackCompetence = (_: number, c: Competence) => c.id;

  selectDomain(d: Domaine) { this.selectedDomainId.set(d.id); }
  selectCompetence(c: Competence) { this.selectedCompetenceId.set(c.id); }

  // paging domaines
  pageChanged(e: PageEvent) {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.fetchDomains(this.domainSearch().trim());
  }

  // ============= CRUD DOMAINES =============
  submitDomain() {
    this.domainForm.markAllAsTouched();
    const { name, description } = this.domainForm.getRawValue();
    const trimmedName = name.trim();
    if (!trimmedName) this.domainNameControl.setErrors({ required: true });

    if (this.domainForm.invalid) return;

    const payload: Partial<Domaine> = {
      name: trimmedName,
      description: (description ?? '').trim(),
      isActive: true,
    };

    if (this.isEditingDomain()) {
      const id = this.editingDomainId()!;
      this.loading.set(true);
      this.api.update(id, payload).subscribe({
        next: (d) => {
          this.notify('Domaine mis à jour avec succès.');
          this.fetchDomains(this.domainSearch().trim(), true, d.id);
          this.resetDomainForm();
          this.loadActiveDomains();
        },
        error: (e) => this.httpError(e),
        complete: () => this.loading.set(false)
      });
    } else {
      this.loading.set(true);
      this.api.create(payload).subscribe({
        next: (d) => {
          this.notify('Nouveau domaine ajouté.');
          this.fetchDomains(this.domainSearch().trim(), true, d.id);
          this.resetDomainForm();
          this.loadActiveDomains();
        },
        error: (e) => this.httpError(e),
        complete: () => this.loading.set(false)
      });
    }
  }

  editDomain(d: Domaine) {
    this.editingDomainId.set(d.id);
    this.domainForm.setValue({ name: d.name ?? '', description: d.description ?? '' });
    this.domainForm.markAsPristine();
    this.domainForm.markAsUntouched();
  }

  cancelEditDomain() { this.resetDomainForm(); }

  resetDomainForm() {
    this.domainForm.reset({ name: '', description: '' });
    this.domainForm.markAsPristine();
    this.domainForm.markAsUntouched();
    this.editingDomainId.set(null);
  }

  deleteDomain(d: Domaine) {
    const confirmed = confirm(`Supprimer le domaine « ${d.name} » ?`);
    if (!confirmed) return;
    this.loading.set(true);
    this.api.delete(d.id).subscribe({
      next: () => {
        this.notify('Domaine supprimé avec succès.');
        this.fetchDomains(this.domainSearch().trim(), true);
        this.loadActiveDomains();
        if (this.selectedDomainId() === d.id) this.selectedDomainId.set(null);
        if (this.editingDomainId() === d.id) this.resetDomainForm();
      },
      error: (e) => this.httpError(e),
      complete: () => this.loading.set(false)
    });
  }

  toggleActive(d: Domaine) {
    this.loading.set(true);
    const call = d.isActive ? this.api.deactivate(d.id) : this.api.activate(d.id);
    call.subscribe({
      next: (res) => {
        this.notify(res.isActive ? 'Domaine activé.' : 'Domaine désactivé.');
        this.fetchDomains(this.domainSearch().trim(), true, res.id);
        this.loadActiveDomains();
      },
      error: (e) => this.httpError(e),
      complete: () => this.loading.set(false)
    });
  }

  // ============= PAGING COMPÉTENCES =============
  competencePageChanged(e: PageEvent) {
    this.competencePageIndex.set(e.pageIndex);
    this.competencePageSize.set(e.pageSize);
    this.fetchCompetences(true);
  }

  // ============= CRUD COMPÉTENCES =============
  submitCompetence() {
    if (this.competenceForm.invalid) {
      this.competenceForm.markAllAsTouched();
      return;
    }

    const { domaineId, name, description, level, yearsOfExperience } =
      this.competenceForm.getRawValue();

    const trimmedName = (name ?? '').trim();
    const trimmedDescription = (description ?? '').trim();

    if (!trimmedName) {
      this.competenceNameControl.setValue(trimmedName);
      this.competenceNameControl.markAsTouched();
      return;
    }

    if (!domaineId || domaineId <= 0) {
      this.competenceDomainControl.setErrors({ required: true });
      this.competenceDomainControl.markAsTouched();
      return;
    }

    const payload: Partial<Competence> = {
      name: trimmedName,
      description: trimmedDescription || null,
      level: level || null,
      yearsOfExperience: yearsOfExperience ?? null,
      domaineId,
      // isActive: true sera mis côté back par défaut
    };

    const editingId = this.editingCompetenceId();
    this.competenceLoading.set(true);

    const request$ = editingId == null
      ? this.competenceApi.create(payload)
      : this.competenceApi.update(editingId, payload);

    request$.subscribe({
      next: (competence) => {
        this.notify(editingId == null ? 'Compétence ajoutée.' : 'Compétence mise à jour.');
        this.fetchCompetences(true, competence.id);
        this.resetCompetenceForm();
      },
      error: (e) => this.httpError(e),
      complete: () => this.competenceLoading.set(false),
    });
  }

  editCompetence(c: Competence) {
    this.editingCompetenceId.set(c.id);
    this.selectedCompetenceId.set(c.id);

    this.competenceForm.setValue({
      domaineId: c.domaineId ?? 0,
      name: c.name ?? '',
      description: c.description ?? '',
      level: (c.level as CompetenceLevel | string | null) || '',
      yearsOfExperience: c.yearsOfExperience ?? 0,
    });

    this.competenceForm.markAsPristine();
    this.competenceForm.markAsUntouched();
  }

  cancelEditCompetence() {
    this.resetCompetenceForm();
  }

  resetCompetenceForm() {
    this.competenceForm.reset({
      domaineId: 0,
      name: '',
      description: '',
      level: '',
      yearsOfExperience: 0,
    });
    this.competenceForm.markAsPristine();
    this.competenceForm.markAsUntouched();
    this.editingCompetenceId.set(null);
  }

  deleteCompetence(c: Competence) {
    if (!c?.id) return;

    const confirmed = confirm(`Supprimer la compétence « ${c.name} » ?`);
    if (!confirmed) return;

    this.competenceLoading.set(true);
    this.competenceApi.delete(c.id).subscribe({
      next: () => {
        this.notify('Compétence supprimée avec succès.');
        const wasSelected = this.selectedCompetenceId() === c.id;
        const wasEditing = this.editingCompetenceId() === c.id;

        this.fetchCompetences(true);

        if (wasSelected) this.selectedCompetenceId.set(null);
        if (wasEditing) this.resetCompetenceForm();
      },
      error: (e) => this.httpError(e),
      complete: () => this.competenceLoading.set(false),
    });
  }

  /** 🔁 Toggle actif / inactif pour une compétence */
  toggleCompetenceActive(c: Competence) {
    if (!c?.id) return;

    this.competenceLoading.set(true);

    const request$ = c.isActive
      ? this.competenceApi.deactivate(c.id)
      : this.competenceApi.activate(c.id);

    request$.subscribe({
      next: (updated) => {
        this.notify(updated.isActive ? 'Compétence activée.' : 'Compétence désactivée.');
        this.fetchCompetences(true, updated.id);
      },
      error: (e) => this.httpError(e),
      complete: () => this.competenceLoading.set(false),
    });
  }

  formatCompetenceLevel(level?: string | null): string {
    if (!level) return '—';
    const normalized = String(level).toUpperCase();
    switch (normalized) {
      case 'BEGINNER':     return 'Débutant';
      case 'INTERMEDIATE': return 'Intermédiaire';
      case 'ADVANCED':     return 'Avancé';
      case 'EXPERT':       return 'Expert';
      default:             return level;
    }
  }

  // ============= FETCH COMPÉTENCES / DOMAINES =============
  private fetchCompetences(keepPage = false, focusId?: number) {
    this.competenceLoading.set(true);

    const page = keepPage ? this.competencePageIndex() : 0;
    const size = this.competencePageSize();

    const sub = this.competenceApi.listPaged(page, size).subscribe({
      next: (p: CompetencePage) => {
        this.competences.set(p.content);
        this.competenceTotal.set(p.totalElements);
        if (!keepPage) {
          this.competencePageIndex.set(p.number);
        }
        if (focusId) {
          this.selectedCompetenceId.set(focusId);
        }
      },
      error: (e) => this.httpError(e),
      complete: () => this.competenceLoading.set(false),
    });

    return sub;
  }

  private fetchDomains(q: string, keepPage = false, focusId?: number) {
    this.loading.set(true);
    const page = keepPage ? this.pageIndex() : 0;
    const size = this.pageSize();

    const sub = q
      ? this.api.search(q).subscribe({
        next: (list) => {
          const sorted = [...list].sort((a, b) => {
            const ad = new Date(a.updatedAt ?? a.createdAt).getTime();
            const bd = new Date(b.updatedAt ?? b.createdAt).getTime();
            return bd - ad;
          });
          this.total.set(sorted.length);
          const start = page * size;
          this.domains.set(sorted.slice(start, start + size));
          if (focusId) this.selectedDomainId.set(focusId);
        },
        error: (e) => this.httpError(e),
        complete: () => this.loading.set(false)
      })
      : this.api.listPaged(page, size).subscribe({
        next: (p: DomainePage) => {
          this.domains.set(p.content);
          this.total.set(p.totalElements);
          if (!keepPage) this.pageIndex.set(p.number);
          if (focusId) this.selectedDomainId.set(focusId);
        },
        error: (e) => this.httpError(e),
        complete: () => this.loading.set(false)
      });

    return sub;
  }

  private loadActiveDomains() {
    this.api.listActive().subscribe({
      next: (list) => {
        this.activeDomains.set(list.filter(d => d.isActive));
      },
      error: (e) => this.httpError(e),
    });
  }

  // ============= UTILS =============
  formatDomainDate(v?: string) {
    if (!v) return '—';
    const dt = new Date(v);
    return Number.isNaN(dt.getTime()) ? '—' : this.df.format(dt);
  }

  private notify(msg: string) {
    this.snackBar.open(msg, 'Fermer', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'bottom'
    });
  }

  private httpError(e: any) {
    const msg = e?.error?.message || e?.message || 'Erreur réseau';
    this.snackBar.open(msg, 'Fermer', {
      duration: 4000,
      panelClass: ['mat-mdc-snack-bar-label']
    });
  }

  logout() {
  this.auth.logout();          // supprime token + reset user/role
  this.router.navigate(['/login']);
}
}
