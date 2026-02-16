import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MissionService } from '../../services/mission.service';

type MissionStatus = 'toutes' | 'en_attente' | 'en_cours' | 'en_validation' | 'terminee' | 'annulee';

interface MissionUI {
  id: number;
  titre: string;
  rawStatus: string;
  statut: MissionStatus;
  statutLibelle: string;
  statutBadgeClass: string;
}

@Component({
  selector: 'app-missions-list',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, ReactiveFormsModule],
  templateUrl: './missions-list.component.html',
  styleUrls: ['./missions-list.component.scss']
})
export default class MissionsListComponent {
  private service = inject(MissionService);
  private fb = inject(FormBuilder);

  selectedStatus: MissionStatus = 'toutes';
  loading = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);
  info = signal<string | null>(null);

  // Données UI + raw pour les modals
  private all: MissionUI[] = [];
  private rawById = new Map<number, any>();

  // --- Etat des modals ---
  showViewModal = signal(false);
  showCancelModal = signal(false);
  selectedId = signal<number | null>(null);

  // --- Form d’édition dans "Voir" ---
  viewForm: FormGroup = this.fb.group({
    title: ['',[Validators.required, Validators.minLength(3)]],
    description: ['',[Validators.required, Validators.minLength(10)]],
    budgetMin: [null],
    budgetType: [''],
    estimatedDuration: [null],
    niveauExperience: [''],
    typeTravail: [''],
    isUrgent: [false],
    skillsRequired: ['']
  });

  // ===== Helpers: read token from localStorage =====
  private get authToken(): string | null {
    const direct = localStorage.getItem('token');
    if (direct) return direct;
    try {
      const auth = JSON.parse(localStorage.getItem('auth') ?? 'null');
      return auth?.token ?? null;
    } catch { return null; }
  }

  // Filters
  get filteredMissions(): MissionUI[] {
    if (this.selectedStatus === 'toutes') return this.all;
    return this.all.filter(m => m.statut === this.selectedStatus);
  }

  filters: { label: string; value: MissionStatus }[] = [
    { label: 'Toutes', value: 'toutes' },
    { label: 'En attente', value: 'en_attente' },
    { label: 'En cours', value: 'en_cours' },
    { label: 'En attente de validation', value: 'en_validation' },
    { label: 'Terminée', value: 'terminee' },
    { label: 'Annulée', value: 'annulee' }
  ];

  readonly totalText = computed(() => `${this.filteredMissions.length}`);

  ngOnInit() { this.fetchMineOnly(); }

  onSelectStatus(status: MissionStatus) { this.selectedStatus = status; }

  // --- Fetch only my missions ---
  private fetchMineOnly() {
    this.loading.set(true);
    this.error.set(null);
    this.info.set(null);

    const token = this.authToken;
    if (!token) {
      this.loading.set(false);
      this.error.set('Veuillez vous connecter pour voir vos missions.');
      this.all = [];
      return;
    }

    this.service.getMyMissions(token).subscribe({
      next: res => {
        this.loading.set(false);
        if (!res?.success) {
          this.error.set(res?.message || 'Erreur inconnue');
          this.all = [];
          this.rawById.clear();
          return;
        }
        const list: any[] = Array.isArray(res.data) ? res.data : [];
        this.rawById.clear();
        this.all = list.map(m => {
          const ui = this.toUI(m);
          if (ui.id != null) this.rawById.set(ui.id, m);
          return ui;
        });
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.message || err?.message || 'Erreur réseau');
        this.all = [];
        this.rawById.clear();
      }
    });
  }

  // mapping status -> UI
  private toUI(m: any): MissionUI {
    const st = String(m?.status || '').toUpperCase();
    let statut: MissionStatus = 'en_attente';
    let lib = 'En attente';
    let badge = 'badge-warning';

    switch (st) {
      case 'DRAFT':             statut = 'en_attente';     lib = 'En attente';                 badge = 'badge-warning'; break;
      case 'PUBLISHED':         statut = 'en_validation';  lib = 'En attente de validation';   badge = 'badge-secondary'; break;
      case 'IN_PROGRESS':       statut = 'en_cours';       lib = 'En cours';                   badge = 'badge-info'; break;
      case 'PENDING_CLOSURE':   statut = 'en_validation';  lib = 'En attente de clôture';      badge = 'badge-info'; break;
      case 'COMPLETED':         statut = 'terminee';       lib = 'Terminée';                   badge = 'badge-success'; break;
      case 'CANCELLED':         statut = 'annulee';        lib = 'Annulée';                    badge = 'badge-danger'; break;
      case 'PAUSED':            statut = 'en_attente';     lib = 'En attente';                 badge = 'badge-secondary'; break;
    }

    return {
      id: m?.id ?? m?.missionId ?? null,
      titre: m?.title ?? '—',
      rawStatus: st,
      statut,
      statutLibelle: lib,
      statutBadgeClass: badge
    };
  }

  // ====== MODAL: VOIR (éditable) ======
  openView(m: MissionUI) {
    this.selectedId.set(m.id);
    const raw = this.rawById.get(m.id);
    // pré-remplir le form
    this.viewForm.reset({
      title: raw?.title ?? '',
      description: raw?.description ?? '',
      budgetMin: raw?.budgetMin ?? null,
      budgetType: raw?.budgetType ?? '',
      estimatedDuration: raw?.estimatedDuration ?? null,
      niveauExperience: raw?.niveauExperience ?? '',
      typeTravail: raw?.typeTravail ?? '',
      isUrgent: !!raw?.isUrgent,
      skillsRequired: raw?.skillsRequired ?? ''
    });
    // Le formulaire n'est éditable que si la mission est en attente (DRAFT/PAUSED)
    if (this.isEditableStatus(raw)) {
      this.viewForm.enable({ emitEvent: false });
    } else {
      this.viewForm.disable({ emitEvent: false });
    }
    this.showViewModal.set(true);
    this.info.set(null);
    this.error.set(null);
  }

  closeView() {
    this.showViewModal.set(false);
    this.selectedId.set(null);
    this.viewForm.reset();
  }

  get selectedRaw() {
    const id = this.selectedId();
    return id ? this.rawById.get(id) ?? null : null;
  }

  get canEditSelected(): boolean {
    const id = this.selectedId();
    if (!id) {
      return false;
    }
    const raw = this.rawById.get(id);
    return this.isEditableStatus(raw);
  }

  saveEdit() {
    const id = this.selectedId();
    const token = this.authToken;
    if (!id || !token) return;

    if (this.viewForm.invalid) {
      this.viewForm.markAllAsTouched();
      return;
    }

    const payload = this.viewForm.value;
    this.saving.set(true);
    this.error.set(null);
    this.info.set(null);

    this.service.updateMission(id, payload, token).subscribe({
      next: res => {
        this.saving.set(false);
        if (!res?.success) {
          this.error.set(res?.message || 'Échec de la mise à jour.');
          return;
        }
        this.info.set('Mission mise à jour avec succès.');
        // refresh cache list + raw
        this.fetchMineOnly();
        // fermer après petit délai UX
        setTimeout(() => this.closeView(), 300);
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message || err?.message || 'Erreur réseau');
      }
    });
  }

  // ====== MODAL: ANNULER ======
  openCancel(m: MissionUI) {
    this.selectedId.set(m.id);
    this.showCancelModal.set(true);
  }

  closeCancel() {
    this.showCancelModal.set(false);
    this.selectedId.set(null);
  }

  closeMission(mission: MissionUI) {
    const token = this.authToken;
    if (!token) {
      this.error.set('Authentification requise pour clôturer.');
      return;
    }
    this.saving.set(true);
    this.service.closeMission(mission.id, token).subscribe({
      next: res => {
        this.saving.set(false);
        if (!res?.success) {
          this.error.set(res?.message || 'Clôture impossible.');
          return;
        }
        this.all = this.all.map(m =>
          m.id === mission.id
            ? { ...m, rawStatus: 'COMPLETED', statut: 'terminee', statutLibelle: 'Terminée', statutBadgeClass: 'badge-success' }
            : m
        );
        this.info.set('Mission clôturée avec succès.');
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message || err?.message || 'Erreur lors de la clôture.');
      }
    });
  }

  confirmCancel() {
    const id = this.selectedId();
    const token = this.authToken;
    if (!id || !token) { this.closeCancel(); return; }

    this.loading.set(true);
    this.service.updateMissionStatus(id, 'CANCELLED', token).subscribe({
      next: _ => {
        this.loading.set(false);
        this.closeCancel();
        this.fetchMineOnly(); // refresh list
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.message || err?.message || 'Erreur réseau');
        this.closeCancel();
      }
    });
  }

  // ====== Réactivation ======
  reactivate(mission: MissionUI) {
    const token = this.authToken;
    const id = mission.id;
    if (!id || !token) {
      this.error.set('Impossible de réactiver sans authentification.');
      return;
    }
    const raw = this.rawById.get(id);
    const currentStatus = (raw?.status ?? '').toString().toUpperCase();
    if (currentStatus !== 'CANCELLED') {
      this.error.set('Réactivation disponible uniquement pour une mission annulée.');
      return;
    }

    if (!confirm('Réactiver cette mission et la remettre en ligne ?')) {
      return;
    }

    this.loading.set(true);
    // On repasse en PUBLISHED pour rouvrir les candidatures.
    this.service.updateMissionStatus(id, 'PUBLISHED', token).subscribe({
      next: res => {
        this.loading.set(false);
        if (!res?.success) {
          this.error.set(res?.message || 'Réactivation impossible.');
          return;
        }
        this.info.set('Mission réactivée.');
        this.fetchMineOnly();
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.message || err?.message || 'Erreur réseau.');
      }
    });
  }

  // ------- Helpers -------
  private isEditableStatus(raw: any): boolean {
    const status = (raw?.status ?? '').toString().toUpperCase();
    return status === 'DRAFT' || status === 'PAUSED';
  }
}
