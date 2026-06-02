import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  template: `
    <div class="flex flex-col items-center justify-center gap-3 py-12" role="status">
      <div
        class="h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-rw-accent"
      ></div>
      @if (message()) {
        <p class="text-sm text-slate-600">{{ message() }}</p>
      }
    </div>
  `,
})
export class LoadingSpinnerComponent {
  readonly message = input('Loading…');
}
