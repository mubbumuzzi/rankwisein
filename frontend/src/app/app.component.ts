import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppHeaderComponent } from './shared/components/app-header/app-header.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppHeaderComponent],
  template: `
    <app-header />
    <main class="min-h-[calc(100vh-4rem)]">
      <router-outlet />
    </main>
  `,
})
export class AppComponent {}
