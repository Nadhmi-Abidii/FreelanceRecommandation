// import { Component, computed, inject, signal } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { RouterLink, RouterLinkActive } from '@angular/router';

// import { MatToolbarModule } from '@angular/material/toolbar';
// import { MatButtonModule } from '@angular/material/button';
// import { MatIconModule } from '@angular/material/icon';
// import { MatBadgeModule } from '@angular/material/badge';
// import { MatMenuModule } from '@angular/material/menu';
// import { MatSlideToggleModule } from '@angular/material/slide-toggle';
// import { MatCardModule } from '@angular/material/card';
// import { MatChipsModule, MatChipInputEvent } from '@angular/material/chips';
// import { MatDividerModule } from '@angular/material/divider';
// import { MatTooltipModule } from '@angular/material/tooltip';
// import { MatFormFieldModule } from '@angular/material/form-field';
// import { MatInputModule } from '@angular/material/input';
// import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

// import { COMMA, ENTER } from '@angular/cdk/keycodes';
// import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

// import { AuthService } from '../../../core/auth/auth';

// interface ManagedDomain {
//   id: number;
//   name: string;
//   description: string;
//   skills: string[];
//   createdAt: string;
//   updatedAt?: string;
// }

// const INITIAL_DOMAINS: ManagedDomain[] = [
//   {
//     id: 1,
//     name: 'Développement web',
//     description: 'Applications web, sites vitrine et plateformes e-commerce.',
//     skills: ['Angular', 'React', 'Node.js', 'UI/UX', 'Tests end-to-end'],
//     createdAt: '2024-02-12T09:45:00.000Z',
//     updatedAt: '2024-05-08T11:00:00.000Z'
//   },
//   {
//     id: 2,
//     name: 'Développement mobile',
//     description: 'Applications iOS, Android et solutions cross-platform.',
//     skills: ['Flutter', 'Swift', 'Kotlin', 'React Native'],
//     createdAt: '2024-03-04T10:18:00.000Z',
//     updatedAt: '2024-06-01T08:30:00.000Z'
//   },
//   {
//     id: 3,
//     name: 'Marketing digital',
//     description: 'Acquisition, contenu et performance sur les canaux digitaux.',
//     skills: ['SEO', 'SEA', 'Copywriting', 'Analytics', 'CRM'],
//     createdAt: '2024-03-28T14:25:00.000Z',
//     updatedAt: '2024-05-18T16:05:00.000Z'
//   },
//   {
//     id: 4,
//     name: 'Data & IA',
//     description: 'Analyse de données, IA appliquée et automatisation.',
//     skills: ['Python', 'Power BI', 'Machine Learning', 'SQL', 'MLOps'],
//     createdAt: '2024-04-09T07:40:00.000Z'
//   }
// ];

// @Component({
//   standalone: true,
//   selector: 'app-admin-domain-management',
//   imports: [
//     CommonModule,
//     RouterLink,
//     RouterLinkActive,
//     MatToolbarModule,
//     MatButtonModule,
//     MatIconModule,
//     MatBadgeModule,
//     MatMenuModule,
//     MatSlideToggleModule,
//     MatCardModule,
//     MatChipsModule,
//     MatDividerModule,
//     MatTooltipModule,
//     MatFormFieldModule,
//     MatInputModule,
//     ReactiveFormsModule,
//     MatSnackBarModule
//   ],
//   templateUrl: './domain-management.component.html',
//   styleUrls: ['../../dashboard/dashboard.component.scss', './domain-management.component.scss']
// })
// export default class AdminDomainManagementComponent {
//   private readonly auth = inject(AuthService);
//   private readonly snackBar = inject(MatSnackBar);
//   private readonly fb = inject(FormBuilder);

//   private readonly domainDateFormatter = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' });
//   private domainIdCounter = INITIAL_DOMAINS.reduce((max, domain) => Math.max(max, domain.id), 0);

//   readonly dark = signal(false);
//   readonly domainForm = this.fb.nonNullable.group({
//     name: ['', [Validators.required, Validators.maxLength(60)]],
//     description: ['', [Validators.maxLength(160)]]
//   });

//   readonly domainNameControl = this.domainForm.controls.name;
//   readonly domainDescriptionControl = this.domainForm.controls.description;

//   readonly domains = signal<ManagedDomain[]>(INITIAL_DOMAINS);
//   readonly domainSkills = signal<string[]>([]);
//   readonly selectedDomainId = signal<number | null>(INITIAL_DOMAINS.length > 0 ? INITIAL_DOMAINS[0].id : null);
//   readonly editingDomainId = signal<number | null>(null);
//   readonly domainSearch = signal('');
//   readonly skillSeparators = [ENTER, COMMA] as const;

//   readonly totalDomains = computed(() => this.domains().length);
//   readonly totalDomainSkills = computed(() =>
//     this.domains().reduce((acc, domain) => acc + domain.skills.length, 0)
//   );

//   private readonly richestDomain = computed(() => {
//     const sorted = [...this.domains()].sort((a, b) => {
//       const diff = b.skills.length - a.skills.length;
//       if (diff !== 0) return diff;
//       return a.name.localeCompare(b.name);
//     });
//     return sorted[0] ?? null;
//   });

//   readonly topDomainLabel = computed(() => {
//     const domain = this.richestDomain();
//     return domain ? `${domain.name} • ${domain.skills.length} compétences` : 'Aucun domaine';
//   });

//   readonly filteredDomains = computed(() => {
//     const query = this.domainSearch().trim().toLowerCase();
//     const domains = [...this.domains()].sort((a, b) => {
//       const aDate = new Date(a.updatedAt ?? a.createdAt).getTime();
//       const bDate = new Date(b.updatedAt ?? b.createdAt).getTime();
//       return bDate - aDate;
//     });

//     if (!query) {
//       return domains;
//     }

//     return domains.filter(domain => {
//       const matchesName = domain.name.toLowerCase().includes(query);
//       const matchesDescription = domain.description.toLowerCase().includes(query);
//       const matchesSkill = domain.skills.some(skill => skill.toLowerCase().includes(query));
//       return matchesName || matchesDescription || matchesSkill;
//     });
//   });

//   readonly selectedDomain = computed(() => {
//     const id = this.selectedDomainId();
//     if (id === null) return null;
//     return this.domains().find(domain => domain.id === id) ?? null;
//   });

//   readonly isEditingDomain = computed(() => this.editingDomainId() !== null);

//   readonly adminName = computed(() => {
//     const user = this.auth.currentUser();
//     if (!user) return 'Administrateur';
//     const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
//     return fullName.length > 0 ? fullName : 'Administrateur';
//   });

//   toggleDark(value: boolean) {
//     this.dark.set(value);
//     document.documentElement.classList.toggle('dash-dark', value);
//   }

//   setDomainSearch(value: string) {
//     this.domainSearch.set(value);
//   }

//   clearDomainSearch() {
//     if (this.domainSearch()) {
//       this.domainSearch.set('');
//     }
//   }

//   addSkill(event: MatChipInputEvent) {
//     const value = (event.value ?? '').trim();
//     if (!value) {
//       event.chipInput?.clear();
//       return;
//     }

//     const normalized = value.replace(/\s+/g, ' ');
//     const exists = this.domainSkills().some(skill => skill.toLowerCase() === normalized.toLowerCase());
//     if (!exists) {
//       this.domainSkills.update(skills => [...skills, normalized]);
//     }

//     event.chipInput?.clear();
//   }

//   trackDomain = (_: number, domain: ManagedDomain) => domain.id;
//   trackSkill = (_: number, skill: string) => skill;

//   selectDomain(domain: ManagedDomain) {
//     this.selectedDomainId.set(domain.id);
//   }

//   removeSkill(index: number) {
//     this.domainSkills.update(skills => skills.filter((_, idx) => idx !== index));
//   }

//   submitDomain() {
//     this.domainForm.markAllAsTouched();
//     const raw = this.domainForm.getRawValue();
//     const trimmedName = raw.name.trim();
//     const trimmedDescription = raw.description.trim();

//     if (!trimmedName) {
//       this.domainNameControl.setErrors({ required: true });
//     }

//     if (this.domainForm.invalid || this.domainSkills().length === 0 || !trimmedName) {
//       if (this.domainSkills().length === 0) {
//         this.notify('Ajoutez au moins une compétence au domaine.');
//       }
//       return;
//     }

//     if (this.isEditingDomain()) {
//       const id = this.editingDomainId();
//       if (id === null) {
//         return;
//       }

//       const updatedAt = new Date().toISOString();
//       const skills = [...this.domainSkills()];
//       this.domains.update(domains =>
//         domains.map(domain =>
//           domain.id === id
//             ? { ...domain, name: trimmedName, description: trimmedDescription, skills, updatedAt }
//             : domain
//         )
//       );

//       this.selectedDomainId.set(id);
//       this.notify('Domaine mis à jour avec succès.');
//     } else {
//       const createdAt = new Date().toISOString();
//       const newDomain: ManagedDomain = {
//         id: this.nextDomainId(),
//         name: trimmedName,
//         description: trimmedDescription,
//         skills: [...this.domainSkills()],
//         createdAt
//       };

//       this.domains.update(domains => [newDomain, ...domains]);
//       this.selectedDomainId.set(newDomain.id);
//       this.notify('Nouveau domaine ajouté.');
//     }

//     this.resetDomainForm();
//   }

//   editDomain(domain: ManagedDomain) {
//     this.editingDomainId.set(domain.id);
//     this.domainForm.setValue({
//       name: domain.name,
//       description: domain.description ?? ''
//     });
//     this.domainSkills.set([...domain.skills]);
//     this.selectedDomainId.set(domain.id);
//   }

//   cancelEditDomain() {
//     this.resetDomainForm();
//   }

//   resetDomainForm() {
//     this.domainForm.reset({ name: '', description: '' });
//     this.domainForm.markAsPristine();
//     this.domainForm.markAsUntouched();
//     this.domainSkills.set([]);
//     this.editingDomainId.set(null);
//   }

//   deleteDomain(domain: ManagedDomain) {
//     const confirmed = confirm(`Supprimer le domaine « ${domain.name} » ?`);
//     if (!confirmed) {
//       return;
//     }

//     this.domains.update(domains => domains.filter(item => item.id !== domain.id));

//     if (this.selectedDomainId() === domain.id) {
//       const next = this.domains()[0] ?? null;
//       this.selectedDomainId.set(next ? next.id : null);
//     }

//     if (this.editingDomainId() === domain.id) {
//       this.resetDomainForm();
//     }

//     this.notify('Domaine supprimé avec succès.');
//   }

//   formatDomainDate(value?: string) {
//     if (!value) {
//       return '—';
//     }

//     const date = new Date(value);
//     if (Number.isNaN(date.getTime())) {
//       return '—';
//     }

//     return this.domainDateFormatter.format(date);
//   }

//   private nextDomainId(): number {
//     this.domainIdCounter += 1;
//     return this.domainIdCounter;
//   }

//   private notify(message: string) {
//     this.snackBar.open(message, 'Fermer', {
//       duration: 3000,
//       horizontalPosition: 'end',
//       verticalPosition: 'bottom'
//     });
//   }
// }