import { Routes } from '@angular/router';
import { adminAuthGuard } from './core/guards/admin-auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/landing/landing.component').then((m) => m.LandingComponent),
  },
  {
    path: 'check',
    loadComponent: () =>
      import('./pages/predict/predict.component').then((m) => m.PredictComponent),
  },
  {
    path: 'college-cutoffs',
    loadComponent: () =>
      import('./pages/college-cutoffs/college-cutoffs.component').then(
        (m) => m.CollegeCutoffsComponent
      ),
  },
  {
    path: 'results',
    loadComponent: () =>
      import('./pages/results/results.component').then((m) => m.ResultsComponent),
  },
  {
    path: 'about',
    loadComponent: () => import('./pages/about/about.component').then((m) => m.AboutComponent),
  },
  {
    path: 'privacy',
    loadComponent: () => import('./pages/legal/privacy.component').then((m) => m.PrivacyComponent),
  },
  {
    path: 'terms',
    loadComponent: () => import('./pages/legal/terms.component').then((m) => m.TermsComponent),
  },
  {
    path: 'admin/login',
    loadComponent: () =>
      import('./pages/admin/login/admin-login.component').then((m) => m.AdminLoginComponent),
  },
  {
    path: 'admin/import',
    canActivate: [adminAuthGuard],
    loadComponent: () =>
      import('./pages/admin/import/admin-import.component').then((m) => m.AdminImportComponent),
  },
  {
    path: 'admin/leads',
    canActivate: [adminAuthGuard],
    loadComponent: () =>
      import('./pages/admin/leads/admin-leads.component').then((m) => m.AdminLeadsComponent),
  },
  {
    path: 'admin/chat',
    canActivate: [adminAuthGuard],
    loadComponent: () =>
      import('./pages/admin/chat/admin-chat.component').then((m) => m.AdminChatComponent),
  },
  { path: 'admin', redirectTo: 'admin/import', pathMatch: 'full' },
  { path: '**', redirectTo: '' },
];
