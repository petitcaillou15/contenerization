import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./posts/feed').then(m => m.FeedComponent) },
  { path: 'login', loadComponent: () => import('./core/login').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./core/register').then(m => m.RegisterComponent) },
  { path: 'posts/:id', loadComponent: () => import('./posts/post-detail').then(m => m.PostDetailComponent) },
  { path: 'profile/:id', loadComponent: () => import('./users/profile').then(m => m.ProfileComponent) },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () => import('./notifications/notifications').then(m => m.NotificationsComponent),
  },
  { path: '**', redirectTo: '' },
];
