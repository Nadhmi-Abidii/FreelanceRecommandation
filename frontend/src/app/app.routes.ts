// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth-guard';

export const routes: Routes = [
  // 🔓 Routes publiques
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then(m => m.default),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register.component').then(m => m.default),
  },

  // 🔒 Toutes les autres routes sont protégées
  {
    path: '',
    canActivate: [authGuard],
    children: [
      // home (page racine) – protégée maintenant
      {
        path: '',
        loadComponent: () =>
          import('./features/home/home.component').then(m => m.default),
        pathMatch: 'full',
      },

      // dashboard & profile
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.default),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then(m => m.default),
      },

      // ADMIN
      {
        path: 'admin/domaines',
        loadComponent: () =>
          import('./features/admin/domaines/domain-management.component').then(m => m.default),
      },
      {
        path: 'admin',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/admin/admin-dashboard.component').then(m => m.default),
      },
      {
        path: 'admin/users',
        loadComponent: () =>
          import('./features/admin/user-management/admin-users.component').then(m => m.default),
      },

      //feedback 
      {
  path: 'feedback',
  children: [
    {
      path: '',
      loadComponent: () =>
        import('./features/feedback/mission-feedback/mission-feedback.component').then(m => m.default),
    },
    {
      path: 'missions/:missionId',
      loadComponent: () =>
        import('./features/feedback/mission-feedback/mission-feedback.component').then(m => m.default),
    },
  ],
},

      // FREELANCER
      {
        path: 'freelancer',
        children: [
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
          {
            path: 'register',
            loadComponent: () =>
              import('./features/auth/register.component').then(m => m.default),
            data: { defaultType: 'freelancer', freelancerOnly: true },
          },
          {
            path: 'dashboard',
            loadComponent: () =>
              import('./features/freelancer/freelancer-dashboard.component').then(m => m.default),
          },
          {
            path: 'candidatures',
            loadComponent: () =>
              import('./features/freelancer/applications/freelancer-candidatures.component').then(m => m.default),
          },
          {
            path: 'missions/:id',
            loadComponent: () =>
              import('./features/freelancer/mission/freelancer-mission-detail.component').then(m => m.default),
          },
          {
            path: 'missions',
            loadComponent: () =>
              import('./features/freelancer/missions/freelancer-missions.component').then(m => m.default),
          },
          {
            path: 'portfolio',
            loadComponent: () =>
              import('./features/freelancer/portfolio/freelancer-portfolio.component').then(m => m.default),
          },
        ],
      },

      // CLIENT / missions
      {
        path: 'client/missions/:missionId/candidatures',
        loadComponent: () =>
          import('./features/freelancer/mission-candidates/mission-candidates.component').then(m => m.default),
      },

      // MISSIONS
      {
        path: 'missions/create',
        loadComponent: () =>
          import('./features/missions/create-mission.component').then(m => m.default),
      },
      {
        path: 'missions/new',
        redirectTo: 'missions/create',
        pathMatch: 'full',
      },
      {
        path: 'missions',
        loadComponent: () =>
          import('./features/missions/missions-list.component').then(m => m.default),
      },
      {
        path: 'missions/:missionId/milestones',
        loadComponent: () =>
          import('./features/client/milestones/client-mission-milestones.component').then(m => m.default),
      },

      // Wallets
      {
        path: 'client/wallet',
        loadComponent: () =>
          import('./features/client/wallet/client-wallet.component').then(m => m.default),
      },

      // CLIENT discussions / candidatures
      {
        path: 'discussions',
        loadComponent: () =>
          import('./features/client/discussions/client-discussions.component').then(m => m.default),
      },
      {
        path: 'candidatures',
        loadComponent: () =>
          import('./features/client/candidatures/client-candidatures.component').then(m => m.default),
      },

      // Conversations
      {
        path: 'messages',
        loadComponent: () =>
          import('./features/messaging/conversation-list/conversation-list.component').then(m => m.default),
      },
      {
        path: 'messages/:conversationId',
        loadComponent: () =>
          import('./features/messaging/conversation-detail/conversation-detail.component').then(m => m.default),
      },
    ],
  },

  // wildcard
  {
    path: '**',
    redirectTo: '',
  },
];
