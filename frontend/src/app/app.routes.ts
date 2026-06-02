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
    path: 'results',
    loadComponent: () =>
      import('./pages/results/results.component').then((m) => m.ResultsComponent),
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
  { path: 'admin', redirectTo: 'admin/import', pathMatch: 'full' },
  { path: '**', redirectTo: '' },
];
