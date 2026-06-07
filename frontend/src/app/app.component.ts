import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppHeaderComponent } from './shared/components/app-header/app-header.component';
import { AppFooterComponent } from './shared/components/app-footer/app-footer.component';
import { CounsellingChatbotComponent } from './shared/components/counselling-chatbot/counselling-chatbot.component';
import { WhatsappFabComponent } from './shared/components/whatsapp-fab/whatsapp-fab.component';
import { SeoService } from './core/services/seo.service';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    AppHeaderComponent,
    AppFooterComponent,
    CounsellingChatbotComponent,
    WhatsappFabComponent,
  ],
  template: `
    <a
      href="#main-content"
      class="sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[100] focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-rw-primary focus:shadow-lg"
      >Skip to main content</a
    >
    <app-header />
    <main id="main-content" class="min-h-[calc(100vh-4rem)]">
      <router-outlet />
    </main>
    <app-footer />
    <app-whatsapp-fab />
    <app-counselling-chatbot />
  `,
})
export class AppComponent implements OnInit {
  private readonly seo = inject(SeoService);

  ngOnInit(): void {
    this.seo.initRouteListener();
  }
}
