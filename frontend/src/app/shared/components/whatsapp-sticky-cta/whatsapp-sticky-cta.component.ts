import { Component, inject } from '@angular/core';
import { WHATSAPP_CTA_LABEL } from '../../../core/constants/site.constants';
import { WhatsappCounsellingLinkService } from '../../../core/services/whatsapp-counselling-link.service';

@Component({
  selector: 'app-whatsapp-sticky-cta',
  template: `
    <div
      class="fixed bottom-0 left-0 right-0 z-40 border-t border-emerald-200 bg-white/95 px-4 py-3 shadow-[0_-8px_30px_rgba(0,0,0,0.08)] backdrop-blur-md sm:py-4"
    >
      <div class="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 sm:flex-row">
        <p class="text-center text-sm font-medium text-rw-primary sm:text-left">
          Need one-on-one help with web options, branches, or seat allotment?
        </p>
        <a
          [href]="whatsappUrl()"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex w-full shrink-0 items-center justify-center gap-2 rounded-xl bg-emerald-600 px-6 py-3 text-sm font-bold text-white shadow-md transition hover:bg-emerald-700 sm:w-auto"
        >
          {{ whatsappCtaLabel }}
        </a>
      </div>
    </div>
    <div class="h-24 sm:h-20" aria-hidden="true"></div>
  `,
})
export class WhatsappStickyCtaComponent {
  private readonly whatsappLink = inject(WhatsappCounsellingLinkService);

  readonly whatsappUrl = this.whatsappLink.url;
  readonly whatsappCtaLabel = WHATSAPP_CTA_LABEL;
}
