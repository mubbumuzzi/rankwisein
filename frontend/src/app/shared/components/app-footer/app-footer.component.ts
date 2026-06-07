import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  CONTACT_EMAIL,
  FOUNDER_LINKEDIN_URL,
  INSTAGRAM_URL,
  WHATSAPP_COMMUNITY_URL,
} from '../../../core/constants/site.constants';

@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  template: `
    <footer class="border-t border-slate-200 bg-rw-primary text-slate-300">
      <div class="mx-auto max-w-6xl px-4 py-12 sm:px-6">
        <div class="grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
          <div class="sm:col-span-2 lg:col-span-1">
            <p class="text-lg font-bold text-white">RankWise</p>
            <p class="mt-2 text-sm">Your Rank. Your Right College.</p>
            <p class="mt-4 text-sm leading-relaxed">
              Data-driven TG EAPCET college recommendations and practical counselling support.
            </p>
          </div>
          <div>
            <p class="text-sm font-semibold uppercase tracking-wider text-slate-400">Explore</p>
            <ul class="mt-4 space-y-2 text-sm">
              <li><a routerLink="/" class="hover:text-white">Home</a></li>
              <li>
                <a routerLink="/" [queryParams]="{ check: '1' }" class="hover:text-white"
                  >Check My Colleges</a
                >
              </li>
              <li><a routerLink="/about" class="hover:text-white">About RankWise</a></li>
              <li><a routerLink="/" fragment="faq" class="hover:text-white">FAQ</a></li>
            </ul>
          </div>
          <div>
            <p class="text-sm font-semibold uppercase tracking-wider text-slate-400">Connect</p>
            <ul class="mt-4 space-y-2 text-sm">
              <li>
                <a [href]="whatsappUrl" target="_blank" rel="noopener noreferrer" class="hover:text-white"
                  >WhatsApp Community</a
                >
              </li>
              <li>
                <a [href]="emailHref" class="hover:text-white">{{ contactEmail }}</a>
              </li>
              <li>
                <a [href]="instagramUrl" target="_blank" rel="noopener noreferrer" class="hover:text-white"
                  >Instagram</a
                >
              </li>
              <li>
                <a [href]="linkedinUrl" target="_blank" rel="noopener noreferrer" class="hover:text-white"
                  >LinkedIn</a
                >
              </li>
            </ul>
          </div>
          <div>
            <p class="text-sm font-semibold uppercase tracking-wider text-slate-400">Legal</p>
            <ul class="mt-4 space-y-2 text-sm">
              <li><a routerLink="/privacy" class="hover:text-white">Privacy Policy</a></li>
              <li><a routerLink="/terms" class="hover:text-white">Terms of Use</a></li>
            </ul>
          </div>
        </div>
        <p class="mt-10 border-t border-white/10 pt-6 text-center text-xs text-slate-500">
          © {{ year }} RankWise. All rights reserved.
        </p>
      </div>
    </footer>
  `,
})
export class AppFooterComponent {
  readonly year = new Date().getFullYear();
  readonly whatsappUrl = WHATSAPP_COMMUNITY_URL;
  readonly instagramUrl = INSTAGRAM_URL;
  readonly linkedinUrl = FOUNDER_LINKEDIN_URL;
  readonly contactEmail = CONTACT_EMAIL;
  readonly emailHref = `mailto:${CONTACT_EMAIL}`;
}
