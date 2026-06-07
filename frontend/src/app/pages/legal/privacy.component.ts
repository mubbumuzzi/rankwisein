import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CONTACT_EMAIL } from '../../core/constants/site.constants';

@Component({
  selector: 'app-privacy',
  imports: [RouterLink],
  template: `
    <div class="mx-auto max-w-3xl px-4 py-12 sm:px-6 sm:py-16">
      <h1 class="text-3xl font-bold text-rw-primary">Privacy Policy</h1>
      <p class="mt-2 text-sm text-slate-500">Last updated: {{ year }}</p>
      <div class="prose prose-slate mt-8 max-w-none space-y-4 text-slate-600">
        <p>
          RankWise collects information you voluntarily provide when using our college predictor (such as
          rank, category, gender, branch preferences, and optional mobile number) to improve recommendations
          and counselling follow-up.
        </p>
        <p>
          We do not sell your personal data. Data is stored securely and used only for RankWise services and
          communication you request.
        </p>
        <p>
          For questions or data concerns, contact us at
          <a class="text-rw-accent hover:underline" [href]="'mailto:' + email">{{ email }}</a>.
        </p>
      </div>
      <a routerLink="/" class="mt-10 inline-block text-sm font-semibold text-rw-accent hover:underline"
        >← Back to home</a
      >
    </div>
  `,
})
export class PrivacyComponent {
  readonly year = new Date().getFullYear();
  readonly email = CONTACT_EMAIL;
}
