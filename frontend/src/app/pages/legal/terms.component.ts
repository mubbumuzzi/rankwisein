import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CONTACT_EMAIL } from '../../core/constants/site.constants';

@Component({
  selector: 'app-terms',
  imports: [RouterLink],
  template: `
    <div class="mx-auto max-w-3xl px-4 py-12 sm:px-6 sm:py-16">
      <h1 class="text-3xl font-bold text-rw-primary">Terms of Use</h1>
      <p class="mt-2 text-sm text-slate-500">Last updated: {{ year }}</p>
      <div class="prose prose-slate mt-8 max-w-none space-y-4 text-slate-600">
        <p>
          RankWise provides college recommendations based on historical cutoff data. Results are for
          guidance only and do not guarantee admission or seat allotment.
        </p>
        <p>
          You are responsible for verifying information with official TG EAPCET counselling sources before
          making decisions.
        </p>
        <p>
          Use of this site constitutes acceptance of these terms. Contact:
          <a class="text-rw-accent hover:underline" [href]="'mailto:' + email">{{ email }}</a>.
        </p>
      </div>
      <a routerLink="/" class="mt-10 inline-block text-sm font-semibold text-rw-accent hover:underline"
        >← Back to home</a
      >
    </div>
  `,
})
export class TermsComponent {
  readonly year = new Date().getFullYear();
  readonly email = CONTACT_EMAIL;
}
