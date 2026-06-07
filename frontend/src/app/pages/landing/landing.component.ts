import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CheckCollegesLauncherService } from '../../core/services/check-colleges-launcher.service';
import { FAQ_ITEMS } from '../../core/constants/seo.constants';
import {
  FOUNDER_BIO,
  FOUNDER_CREDIT,
  FOUNDER_LINKEDIN_URL,
  INSTAGRAM_URL,
  WHATSAPP_DISPLAY,
  WHATSAPP_URL,
} from '../../core/constants/site.constants';

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.component.html',
})
export class LandingComponent implements OnInit {
  private readonly launcher = inject(CheckCollegesLauncherService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly whatsappUrl = WHATSAPP_URL;
  readonly whatsappDisplay = WHATSAPP_DISPLAY;
  readonly instagramUrl = INSTAGRAM_URL;
  readonly builderCredit = FOUNDER_CREDIT;
  readonly builderBio = FOUNDER_BIO;
  readonly builderLinkedInUrl = FOUNDER_LINKEDIN_URL;

  readonly trustItems = [
    'Previous Year Cutoff Analysis',
    'Safe, Target & Dream Colleges',
    'Personalized WhatsApp Counselling',
  ];

  readonly counsellingBenefits = [
    'Counselling Updates',
    'Certificate Verification Guidance',
    'Web Options Support',
    'College Selection Help',
    'Branch Selection Guidance',
    'Seat Allotment Updates',
  ];

  readonly howItWorks = [
    { step: 1, title: 'Enter Your Rank', icon: '📝', description: 'Share rank, category, and branch interest.' },
    {
      step: 2,
      title: 'Get College Recommendations',
      icon: '🎓',
      description: 'Instant lists from previous year cutoffs.',
    },
    {
      step: 3,
      title: 'Compare Safe, Target & Dream',
      icon: '📊',
      description: 'Understand reach, match, and backup options.',
    },
    {
      step: 4,
      title: 'Get Personalized Guidance',
      icon: '💬',
      description: 'Message us on WhatsApp for one-on-one counselling help.',
    },
  ];

  readonly featureCards = [
    { title: 'College Cutoff Lookup', icon: '🔍', description: 'Search any college and view closing ranks by year, phase, category and gender.' },
    { title: 'Counselling Chatbot', icon: '🤖', description: 'Instant answers on dream, target, safe colleges and counselling steps.' },
    { title: 'College Predictor', icon: '🎯', description: 'Rank-based Dream, Target & Safe shortlists.' },
    { title: 'Branch Analysis', icon: '🔬', description: 'Filter by CSE, ECE, IT, and more.' },
    { title: 'Counselling Guidance', icon: '🧭', description: 'Step-by-step support on WhatsApp.' },
    { title: 'Personalized WhatsApp Help', icon: '💬', description: 'Direct counselling support on WhatsApp.' },
    { title: 'Previous Year Cutoff Analysis', icon: '📅', description: 'Official TG EAPCET cutoff data.' },
    { title: 'College Comparison', icon: '⚖️', description: 'Compare options by closing rank.' },
  ];

  readonly faqItems = FAQ_ITEMS;

  readonly proofItems = [
    { label: '100+ Colleges Covered', icon: '🏫' },
    { label: 'Multiple Branch Analysis', icon: '📐' },
    { label: 'Previous Year Cutoff Data', icon: '📈' },
    { label: 'Safe, Target & Dream Predictions', icon: '✨' },
  ];

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      if (params.get('check') === '1') {
        this.openCheckModal();
        void this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {},
          replaceUrl: true,
        });
      }
    });
  }

  openCheckModal(): void {
    this.launcher.open();
  }
}
