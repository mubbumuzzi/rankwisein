import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import {
  CONTACT_EMAIL,
  WHATSAPP_COMMUNITY_URL,
} from '../constants/site.constants';
import {
  FAQ_ITEMS,
  ROUTE_SEO,
  SITE_NAME,
  SITE_URL,
} from '../constants/seo.constants';
import { JsonLd, SeoConfig } from '../models/seo.models';

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly doc = inject(DOCUMENT);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly router = inject(Router);

  private readonly jsonLdScriptClass = 'rankwise-jsonld';

  initRouteListener(): void {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => this.applyForCurrentRoute());
    this.applyForCurrentRoute();
  }

  applyForCurrentRoute(): void {
    const path = this.router.url.split('?')[0].replace(/^\//, '');
    const config = ROUTE_SEO[path] ?? ROUTE_SEO[''];
    this.apply(config);

    if (path === '') {
      this.setJsonLd(this.buildHomeSchemas());
    } else {
      this.clearJsonLd();
    }
  }

  apply(config: SeoConfig): void {
    this.title.setTitle(config.title);
    this.setMetaName('description', config.description);
    this.setMetaName('robots', config.robots ?? 'index, follow');

    if (config.keywords) {
      this.setMetaName('keywords', config.keywords);
    }

    const canonical = `${SITE_URL}${config.canonicalPath}`;
    this.setCanonical(canonical);

    const og = config.og ?? {};
    const ogTitle = og.title ?? config.title;
    const ogDescription = og.description ?? config.description;
    const ogImage = og.image ?? `${SITE_URL}/og-image.svg`;

    this.setMetaProperty('og:title', ogTitle);
    this.setMetaProperty('og:description', ogDescription);
    this.setMetaProperty('og:image', ogImage);
    this.setMetaProperty('og:url', canonical);
    this.setMetaProperty('og:type', og.type ?? 'website');
    this.setMetaProperty('og:site_name', og.siteName ?? SITE_NAME);
    this.setMetaProperty('og:locale', 'en_IN');

    this.setMetaName('twitter:card', 'summary_large_image');
    this.setMetaName('twitter:title', ogTitle);
    this.setMetaName('twitter:description', ogDescription);
    this.setMetaName('twitter:image', ogImage);
  }

  setJsonLd(schemas: JsonLd[]): void {
    this.clearJsonLd();
    schemas.forEach((schema, index) => {
      const script = this.doc.createElement('script');
      script.type = 'application/ld+json';
      script.className = this.jsonLdScriptClass;
      script.id = `${this.jsonLdScriptClass}-${index}`;
      script.text = JSON.stringify(schema);
      this.doc.head.appendChild(script);
    });
  }

  clearJsonLd(): void {
    this.doc.querySelectorAll(`script.${this.jsonLdScriptClass}`).forEach((el) => el.remove());
  }

  buildHomeSchemas(): JsonLd[] {
    return [
      {
        '@context': 'https://schema.org',
        '@type': 'EducationalOrganization',
        name: SITE_NAME,
        url: SITE_URL,
        description:
          'TG EAPCET and TS EAMCET college predictor with safe, target and dream college recommendations for Telangana engineering admissions.',
        email: CONTACT_EMAIL,
        areaServed: { '@type': 'State', name: 'Telangana' },
        sameAs: [WHATSAPP_COMMUNITY_URL],
      },
      {
        '@context': 'https://schema.org',
        '@type': 'WebSite',
        name: SITE_NAME,
        url: SITE_URL,
        description:
          'Free TG EAPCET 2026 college predictor and counselling community for Telangana engineering admissions.',
        publisher: { '@type': 'Organization', name: SITE_NAME },
        potentialAction: {
          '@type': 'SearchAction',
          target: {
            '@type': 'EntryPoint',
            urlTemplate: `${SITE_URL}/check`,
          },
          'query-input': 'required name=search_term_string',
        },
      },
      {
        '@context': 'https://schema.org',
        '@type': 'FAQPage',
        mainEntity: FAQ_ITEMS.map((item) => ({
          '@type': 'Question',
          name: item.question,
          acceptedAnswer: {
            '@type': 'Answer',
            text: item.answer,
          },
        })),
      },
    ];
  }

  private setCanonical(url: string): void {
    let link = this.doc.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
    if (!link) {
      link = this.doc.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.doc.head.appendChild(link);
    }
    link.setAttribute('href', url);
  }

  private setMetaName(name: string, content: string): void {
    if (this.meta.getTag(`name="${name}"`)) {
      this.meta.updateTag({ name, content });
    } else {
      this.meta.addTag({ name, content });
    }
  }

  private setMetaProperty(property: string, content: string): void {
    if (this.meta.getTag(`property="${property}"`)) {
      this.meta.updateTag({ property, content });
    } else {
      this.meta.addTag({ property, content });
    }
  }
}
