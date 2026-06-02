import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="sticky top-0 z-50 border-b border-slate-200/80 bg-white/90 backdrop-blur-md">
      <div class="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <a routerLink="/" class="flex items-center gap-2 no-underline">
          <span
            class="flex h-9 w-9 items-center justify-center rounded-lg bg-rw-primary text-sm font-bold text-white"
            >RW</span
          >
          <div class="leading-tight">
            <span class="block text-lg font-bold text-rw-primary">RankWise</span>
            <span class="hidden text-xs text-slate-500 sm:block">Your Rank. Your Right College.</span>
          </div>
        </a>
        <nav class="flex items-center gap-2">
          <a
            routerLink="/"
            routerLinkActive="text-rw-accent"
            [routerLinkActiveOptions]="{ exact: true }"
            class="rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-rw-primary"
            >Home</a
          >
          <a
            routerLink="/admin/login"
            class="hidden rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-rw-primary sm:inline-block"
            >Admin</a
          >
          <a
            href="https://wa.me/919505922694?text=Hi%20RankWise%2C%20I%20need%20personalized%20counselling%20for%20TS%20EAMCET."
            target="_blank"
            rel="noopener noreferrer"
            class="hidden rounded-lg px-3 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-50 sm:inline-flex sm:items-center sm:gap-2"
            aria-label="Personalized counselling on WhatsApp"
          >
            <svg viewBox="0 0 32 32" class="h-4 w-4 fill-current" aria-hidden="true">
              <path
                d="M19.11 17.46c-.26-.13-1.52-.75-1.76-.84-.24-.09-.42-.13-.6.13-.18.26-.69.84-.85 1.01-.16.18-.31.2-.57.07-.26-.13-1.1-.4-2.1-1.28-.78-.69-1.31-1.55-1.47-1.81-.15-.26-.02-.4.12-.53.12-.12.26-.31.4-.46.13-.15.18-.26.26-.44.09-.18.04-.33-.02-.46-.06-.13-.6-1.45-.82-1.99-.22-.53-.44-.46-.6-.47l-.51-.01c-.18 0-.46.07-.7.33-.24.26-.92.9-.92 2.19 0 1.28.94 2.52 1.07 2.7.13.18 1.86 2.84 4.5 3.99.63.27 1.12.43 1.5.55.63.2 1.2.17 1.65.1.5-.07 1.52-.62 1.74-1.22.22-.6.22-1.12.16-1.22-.06-.11-.24-.18-.5-.31z"
              />
              <path
                d="M26.67 5.33C24.05 2.72 20.57 1.28 16.9 1.28 9.31 1.28 3.14 7.45 3.14 15.04c0 2.42.63 4.78 1.84 6.85l-1.96 7.18 7.34-1.93c2.02 1.1 4.3 1.68 6.57 1.68h.01c7.59 0 13.76-6.17 13.76-13.76 0-3.67-1.43-7.15-4.03-9.73zm-9.76 21.11h-.01c-2.05 0-4.06-.55-5.82-1.6l-.42-.25-4.36 1.15 1.16-4.25-.27-.44a11.35 11.35 0 0 1-1.74-6.05C5.45 8.72 10.6 3.57 16.9 3.57c3.05 0 5.92 1.19 8.08 3.35a11.35 11.35 0 0 1 3.35 8.08c0 6.3-5.13 11.44-11.42 11.44z"
              />
            </svg>
            WhatsApp
          </a>
          <a
            routerLink="/check"
            routerLinkActive="text-rw-accent"
            class="rounded-lg bg-rw-accent px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-rw-accent-hover"
            >Check My Colleges</a
          >
        </nav>
      </div>
    </header>
  `,
})
export class AppHeaderComponent {}
