import { Component } from '@angular/core';
import { WHATSAPP_COMMUNITY_URL } from '../../../core/constants/site.constants';

@Component({
  selector: 'app-whatsapp-sticky-cta',
  template: `
    <div
      class="fixed bottom-0 left-0 right-0 z-40 border-t border-emerald-200 bg-white/95 px-4 py-3 shadow-[0_-8px_30px_rgba(0,0,0,0.08)] backdrop-blur-md sm:py-4"
    >
      <div class="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 sm:flex-row">
        <p class="text-center text-sm font-medium text-rw-primary sm:text-left">
          Need detailed guidance? Join our free WhatsApp community
        </p>
        <a
          [href]="whatsappUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex w-full shrink-0 items-center justify-center gap-2 rounded-xl bg-emerald-600 px-6 py-3 text-sm font-bold text-white shadow-md transition hover:bg-emerald-700 sm:w-auto"
        >
          Join WhatsApp Community
        </a>
      </div>
    </div>
    <div class="h-24 sm:h-20" aria-hidden="true"></div>
  `,
})
export class WhatsappStickyCtaComponent {
  readonly whatsappUrl = WHATSAPP_COMMUNITY_URL;
}
