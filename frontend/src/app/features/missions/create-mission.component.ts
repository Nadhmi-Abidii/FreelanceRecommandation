import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MissionService } from '../../services/mission.service';
import { DomainesService } from '../../services/domaines.service';
import { AiDraftResponse, AiService } from '../../services/ai.service';
import { BudgetType, MissionPayload, NiveauExperience, TypeTravail } from '../../models/mission.model';
import { Domaine } from '../../models/domaine.model';

interface MissionForm {
  general: FormGroup<{
    title: FormControl<string>;
    description: FormControl<string>;
    domaineId: FormControl<number | null>;
  }>;
  details: FormGroup<{
    skillsSearch: FormControl<string>;
    skills: FormControl<string[]>;
    budget: FormControl<string>;
    budgetType: FormControl<string>;
  }>;
  work: FormGroup<{
    duration: FormControl<string>;
    experience: FormControl<string>;
    workMode: FormControl<string>;
  }>;
  review: FormGroup<{
    notes: FormControl<string>;
  }>;
}

@Component({
  standalone: true,
  selector: 'app-create-mission',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, RouterLinkActive],
  templateUrl: './create-mission.component.html',
  styleUrls: ['./create-mission.component.scss']
})
export default class CreateMissionComponent implements OnInit {
  private fb = inject(FormBuilder);
  private missionService = inject(MissionService);
  private domainesService = inject(DomainesService);
  private aiService = inject(AiService);
  private router = inject(Router);

  // TODO: remplace par l'utilisateur connecté + domaine réel
  private readonly DEFAULT_CLIENT_ID = 1;
  private readonly DEFAULT_DOMAINE_ID = 1;

  protected readonly steps = [
    { label: 'Informations Générales', description: 'Décrivez les bases de la mission.' },
    { label: 'Détails de la Mission', description: 'Compétences et précisions importantes.' },
    { label: 'Conditions de travail', description: 'Cadre, durée et modalités.' },
    { label: 'Finalisation', description: 'Derniers détails avant publication.' }
  ];

  protected readonly popularSkills = ['JavaScript','HTML','CSS','React','Angular','Vue.js','Node.js','Python','PHP'];

  // mappe sur tes enums backend
  protected readonly experienceLevels = [
    { value: NiveauExperience.BEGINNER,     label: 'Junior' },
    { value: NiveauExperience.INTERMEDIATE, label: 'Intermédiaire' },
    { value: NiveauExperience.ADVANCED,     label: 'Senior' },
    { value: NiveauExperience.EXPERT,       label: 'Expert' }
  ];
  protected readonly workModes = [
    { value: TypeTravail.REMOTE,  label: 'Télétravail' },
    { value: TypeTravail.ON_SITE, label: 'Présentiel' },
    { value: TypeTravail.HYBRID,  label: 'Hybride' }
  ];
  protected readonly budgetTypes = [
    { value: BudgetType.FIXED,      label: 'Forfait' },
    { value: BudgetType.HOURLY,     label: 'Horaire' },
    { value: BudgetType.NEGOTIABLE, label: 'Négociable' }
  ];

  protected currentStep = signal(0);
  protected submitted = signal(false);
  protected isSending = signal(false);
  protected apiMessage = signal<string | null>(null);
  protected domaines = signal<Domaine[]>([]);
  protected domainesLoading = signal(false);
  protected domainesError = signal<string | null>(null);
  protected domainSuggestions = signal<{ id: number; name: string; score?: number | null; reason?: string | null }[]>([]);
  protected suggesting = signal(false);
  protected aiDrafting = signal(false);
  protected aiRewriting = signal(false);

  private readonly budgetTypeMap = new Map(this.budgetTypes.map(t => [t.value, t.label] as const));
  private readonly experienceMap = new Map(this.experienceLevels.map(l => [l.value, l.label] as const));
  private readonly workModeMap = new Map(this.workModes.map(m => [m.value, m.label] as const));

  protected form = this.fb.group<MissionForm>({
    general: this.fb.group({
      title: this.fb.control('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
      description: this.fb.control('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(800)] }),
      domaineId: this.fb.control<number | null>(null, { validators: [Validators.required] })
    }),
    details: this.fb.group({
      skillsSearch: this.fb.control('', { nonNullable: true }),
      skills: this.fb.control<string[]>([], { nonNullable: true }),
      budget: this.fb.control('', { nonNullable: true, validators: [Validators.required] }),
      budgetType: this.fb.control('', { nonNullable: true, validators: [Validators.required] })
    }),
    work: this.fb.group({
      duration: this.fb.control('', { nonNullable: true, validators: [Validators.required] }),
      experience: this.fb.control('', { nonNullable: true, validators: [Validators.required] }),
      workMode: this.fb.control('', { nonNullable: true, validators: [Validators.required] })
    }),
    review: this.fb.group({
      notes: this.fb.control('', { nonNullable: true })
    })
  });

  protected readonly progress = computed(() => ((this.currentStep() + 1) / this.steps.length) * 100);

  ngOnInit(): void {
    this.loadDomaines();
  }

  // —— NAVIGATION —— //
  protected nextStep(): void {
    if (!this.canAdvance()) { this.touchCurrentStep(); return; }
    if (this.currentStep() < this.steps.length - 1) this.currentStep.update(v => v + 1);
  }
  protected previousStep(): void { if (this.currentStep() > 0) this.currentStep.update(v => v - 1); }
  protected goToStep(i: number): void { if (i !== this.currentStep() && i <= this.highestUnlockedStep()) this.currentStep.set(i); }
  protected isActive(i: number)   { return i === this.currentStep(); }
  protected isCompleted(i: number){ return i <  this.currentStep(); }

  // —— UI helpers —— //
  protected toggleSkill(skill: string): void {
    const ctl = this.form.controls.details.controls.skills;
    const cur = ctl.value ?? [];
    ctl.setValue(cur.includes(skill) ? cur.filter(s => s !== skill) : [...cur, skill]);
  }
  protected skillSelected(skill: string): boolean {
    return this.form.controls.details.controls.skills.value?.includes(skill) ?? false;
  }
  protected canAdvance(): boolean {
    const i = this.currentStep();
    if (i === 0) return this.form.controls.general.valid;
    if (i === 1) return this.form.controls.details.valid;
    if (i === 2) return this.form.controls.work.valid;
    return this.form.valid;
  }
  protected highestUnlockedStep(): number {
    let h = 0;
    if (this.form.controls.general.valid) h = 1;
    if (this.form.controls.details.valid) h = 2;
    if (this.form.controls.work.valid)    h = 3;
    if (this.submitted())                 h = this.steps.length - 1;
    return h;
  }
  protected showErrors(c: AbstractControl | null): boolean {
    return !!c && c.invalid && (c.touched || this.submitted());
  }
  protected budgetTypeLabel(v?: string | null) { return v ? (this.budgetTypeMap.get(v as any) ?? '—') : '—'; }
  protected experienceLabel(v?: string | null) { return v ? (this.experienceMap.get(v as any) ?? '—') : '—'; }
  protected workModeLabel(v?: string | null)   { return v ? (this.workModeMap.get(v as any) ?? '—') : '—'; }

  // —— Form submit via (ngSubmit) —— //
  protected finish(): void {
    // When user presses Enter, behave like clicking the main button:
    if (this.currentStep() < this.steps.length - 1) {
      this.nextStep();
      return;
    }
    this.createMission();
  }

  // —— Backend call only on last step button click —— //
  protected createMission(): void {
    this.submitted.set(true);
    if (!this.form.valid) { this.touchCurrentStep(); return; }

    const f = this.form.value;
    const budgetNumber = Number(f.details?.budget ?? 0) || 0;

    const payload: MissionPayload = {
      client:  { id: this.DEFAULT_CLIENT_ID },
      domaine: { id: f.general?.domaineId ?? this.DEFAULT_DOMAINE_ID },
      title: f.general?.title ?? '',
      description: f.general?.description ?? '',
      requirements: f.review?.notes?.trim() ? f.review?.notes : null,
      budgetMin: budgetNumber,
      budgetMax: null,
      budgetType: f.details?.budgetType as unknown as BudgetType,
      typeTravail: f.work?.workMode as unknown as TypeTravail,
      niveauExperience: f.work?.experience as unknown as NiveauExperience,
      deadline: null,
      estimatedDuration: Number(f.work?.duration ?? 0) || 0,
      skillsRequired: (f.details?.skills?.length ? f.details.skills.join(', ') : null),
      isUrgent: false,
      attachments: null
    };

    this.isSending.set(true);
    this.apiMessage.set(null);

    this.missionService.createMission(payload).subscribe({
      next: (res) => {
        this.isSending.set(false);
        this.apiMessage.set(res?.message || 'Mission created successfully');
        // Redirect to dashboard
        this.router.navigateByUrl('/dashboard');
      },
      error: (err) => {
        this.isSending.set(false);
        const msg = err?.error?.message || err?.message || 'Erreur réseau.';
        this.apiMessage.set(msg);
      }
    });
  }

  protected requestSuggestions(): void {
    if (this.suggesting()) return;
    const f = this.form.value;
    this.suggesting.set(true);
    this.domainesService
      .suggest({
        title: f.general?.title ?? '',
        description: f.general?.description ?? '',
        requirements: f.review?.notes ?? '',
        skillsRequired: f.details?.skills?.join(', ') ?? '',
        limit: 3,
        language: 'fr'
      })
      .subscribe({
        next: suggestions => {
          this.suggesting.set(false);
          const mapped = (suggestions ?? []).map(item => ({
            id: item.domaineId,
            name: item.domaineName,
            score: item.score ?? null,
            reason: item.reason ?? null
          }));
          this.domainSuggestions.set(mapped);
          if (mapped.length && !this.form.controls.general.controls.domaineId.value) {
            this.form.controls.general.controls.domaineId.setValue(mapped[0].id);
          }
        },
        error: () => {
          this.suggesting.set(false);
          this.domainSuggestions.set([]);
        }
      });
  }

  protected applySuggestion(id: number): void {
    this.form.controls.general.controls.domaineId.setValue(id);
  }

  protected generateDraft(): void {
    if (this.aiDrafting()) return;
    const f = this.form.value;
    this.aiDrafting.set(true);
    this.aiService
      .draftMission({
        title: f.general?.title ?? '',
        description: f.general?.description ?? '',
        requirements: f.review?.notes ?? '',
        skillsRequired: f.details?.skills?.join(', ') ?? '',
        tone: 'professionnel',
        language: 'fr',
        maxLength: 800
      })
      .subscribe({
        next: draft => {
          this.aiDrafting.set(false);
          this.applyDraft(draft);
        },
        error: () => {
          this.aiDrafting.set(false);
        }
      });
  }

  protected rewriteDescription(): void {
    if (this.aiRewriting()) return;
    const content = this.form.controls.general.controls.description.value ?? '';
    if (!content.trim()) return;
    this.aiRewriting.set(true);
    this.aiService
      .rewrite({
        content,
        intent: 'clarity',
        tone: 'professionnel',
        language: 'fr',
        maxLength: 800
      })
      .subscribe({
        next: res => {
          this.aiRewriting.set(false);
          if (res?.content) {
            this.form.controls.general.controls.description.setValue(res.content);
          }
        },
        error: () => {
          this.aiRewriting.set(false);
        }
      });
  }

  private applyDraft(draft?: AiDraftResponse | null): void {
    if (!draft) return;
    if (draft.title) {
      this.form.controls.general.controls.title.setValue(draft.title);
    }
    if (draft.description) {
      this.form.controls.general.controls.description.setValue(draft.description);
    }
    if (draft.requirements) {
      this.form.controls.review.controls.notes.setValue(draft.requirements);
    }
    if (draft.skillsSuggested) {
      const skills = this.parseSkills(draft.skillsSuggested);
      if (skills.length) {
        this.form.controls.details.controls.skills.setValue(skills);
      }
    }
  }

  private parseSkills(input: string): string[] {
    return input
      .split(/[,\n]/)
      .map(skill => skill.trim())
      .filter(Boolean);
  }

  private loadDomaines(): void {
    this.domainesLoading.set(true);
    this.domainesError.set(null);
    this.domainesService.listActive().subscribe({
      next: list => {
        this.domainesLoading.set(false);
        this.domaines.set(list ?? []);
        const current = this.form.controls.general.controls.domaineId.value;
        if (!current && list?.length) {
          this.form.controls.general.controls.domaineId.setValue(list[0].id);
        }
      },
      error: err => {
        this.domainesLoading.set(false);
        this.domainesError.set(err?.error?.message || err?.message || 'Unable to load domaines.');
      }
    });
  }

  private touchCurrentStep(): void {
    const i = this.currentStep();
    const groups = [this.form.controls.general, this.form.controls.details, this.form.controls.work, this.form.controls.review];
    const g = groups[i];
    if (!g) return;
    Object.values(g.controls).forEach(c => c.markAsTouched());
  }
}
