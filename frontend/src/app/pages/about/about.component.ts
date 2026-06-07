import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  FOUNDER_BIO,
  FOUNDER_CREDIT,
  FOUNDER_LINKEDIN_URL,
  WHATSAPP_DISPLAY,
  WHATSAPP_URL,
} from '../../core/constants/site.constants';

@Component({
  selector: 'app-about',
  imports: [RouterLink],
  template: `
    <div class="mx-auto max-w-3xl px-4 py-12 sm:px-6 sm:py-16">
      <h1 class="text-3xl font-bold text-rw-primary">About RankWise</h1>
      <p class="mt-6 text-lg leading-relaxed text-slate-600">
        Choosing a college is one of the most important decisions after TG EAPCET / TS EAMCET, yet many
        students rely on random advice, incomplete cutoff lists, or social media opinions.
      </p>
      <p class="mt-4 text-slate-600">
        RankWise was built to help students make better counselling decisions using previous-year cutoff
        data, rank analysis, and practical guidance throughout the admission process.
      </p>
      <p class="mt-4 text-slate-600">
        Whether you're looking for safe, target, or dream colleges, RankWise aims to simplify counselling
        and provide clear guidance when it matters most.
      </p>
      <div class="mt-10 rounded-2xl border border-slate-200 bg-slate-50 p-6">
        <h2 class="text-xl font-bold text-rw-primary">{{ builderCredit }}</h2>
        <p class="mt-2 text-sm text-slate-600">{{ founderBio }}</p>
        <a
          [href]="linkedinUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="mt-3 inline-block text-sm font-semibold text-[#0A66C2] hover:underline"
          >LinkedIn →</a
        >
      </div>
      <a
        [href]="whatsappUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="mt-8 inline-flex rounded-xl bg-emerald-600 px-8 py-3 font-semibold text-white hover:bg-emerald-700"
        >WhatsApp us at {{ whatsappDisplay }}</a
      >
      <a routerLink="/" class="mt-6 block text-sm font-semibold text-rw-accent hover:underline">← Back to home</a>
    </div>
  `,
})
export class AboutComponent {
  readonly whatsappUrl = WHATSAPP_URL;
  readonly whatsappDisplay = WHATSAPP_DISPLAY;
  readonly builderCredit = FOUNDER_CREDIT;
  readonly founderBio = FOUNDER_BIO;
  readonly linkedinUrl = FOUNDER_LINKEDIN_URL;
}
