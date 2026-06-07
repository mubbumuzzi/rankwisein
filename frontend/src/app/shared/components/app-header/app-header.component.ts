import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { INSTAGRAM_URL } from '../../../core/constants/site.constants';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="sticky top-0 z-50 border-b border-slate-200/80 bg-white/90 backdrop-blur-md">
      <div class="mx-auto flex h-16 max-w-6xl items-center gap-2 px-3 sm:gap-3 sm:px-6">
        <a routerLink="/" class="flex min-w-0 shrink items-center gap-2 no-underline sm:shrink-0">
          <span
            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-rw-primary text-sm font-bold text-white"
            >RW</span
          >
          <div class="hidden min-w-0 leading-tight sm:block">
            <span class="block truncate text-lg font-bold text-rw-primary">RankWise</span>
            <span class="hidden text-xs text-slate-500 md:block">Your Rank. Your Right College.</span>
          </div>
        </a>

        <nav class="ml-auto flex shrink-0 items-center gap-1 sm:gap-2" aria-label="Main navigation">
          <a
            routerLink="/"
            routerLinkActive="text-rw-accent"
            [routerLinkActiveOptions]="{ exact: true }"
            class="hidden rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 md:inline-block"
            >Home</a
          >
          <a
            routerLink="/college-cutoffs"
            routerLinkActive="text-rw-accent"
            class="hidden rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 md:inline-block"
            >College Cutoffs</a
          >
          <a
            [href]="instagramUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="hidden rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 lg:inline-block"
            >Instagram</a
          >
          <a
            routerLink="/admin/login"
            class="hidden rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 lg:inline-block"
            >Admin</a
          >
          <a
            routerLink="/"
            [queryParams]="{ check: '1' }"
            class="rounded-lg bg-rw-accent px-2.5 py-2 text-xs font-semibold text-white shadow-sm hover:bg-rw-accent-hover sm:px-4 sm:text-sm"
          >
            <span class="sm:hidden">Check</span>
            <span class="hidden sm:inline">Check My Colleges</span>
          </a>
          <button
            type="button"
            class="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 md:hidden"
            [attr.aria-expanded]="menuOpen()"
            aria-controls="mobile-nav"
            [attr.aria-label]="menuOpen() ? 'Close menu' : 'Open menu'"
            (click)="toggleMenu()"
          >
            @if (menuOpen()) {
              <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            } @else {
              <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            }
          </button>
        </nav>
      </div>

      @if (menuOpen()) {
        <nav
          id="mobile-nav"
          class="border-t border-slate-200 bg-white px-4 py-3 md:hidden"
          (click)="closeMenu()"
        >
          <a
            routerLink="/"
            class="block rounded-lg px-3 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-100"
            >Home</a
          >
          <a
            routerLink="/college-cutoffs"
            class="block rounded-lg px-3 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-100"
            >College Cutoffs</a
          >
          <a
            [href]="instagramUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="block rounded-lg px-3 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-100"
            >Instagram</a
          >
          <a
            routerLink="/admin/login"
            class="block rounded-lg px-3 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-100"
            >Admin</a
          >
        </nav>
      }
    </header>
  `,
})
export class AppHeaderComponent {
  readonly instagramUrl = INSTAGRAM_URL;
  readonly menuOpen = signal(false);

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }
}
