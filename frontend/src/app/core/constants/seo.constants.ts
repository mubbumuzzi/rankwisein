import { SeoConfig } from '../models/seo.models';

export const SITE_URL = 'https://rankwise.co.in';
export const SITE_NAME = 'RankWise';
export const DEFAULT_OG_IMAGE = `${SITE_URL}/og-image.svg`;

/** Replace with your Google Analytics 4 measurement ID */
export const GA4_MEASUREMENT_ID = 'G-XXXXXXXXXX';

/** Replace with Google Search Console HTML tag verification content value */
export const GSC_VERIFICATION = 'YOUR_GSC_VERIFICATION_CODE';

export const DEFAULT_DESCRIPTION =
  'Get TG EAPCET 2026 college predictions based on your rank. Find safe, target and dream colleges with counselling guidance and admission support.';

export const HOME_TITLE =
  'TG EAPCET 2026 College Predictor | Safe, Target & Dream Colleges | RankWise';

export const PUBLIC_SITEMAP_PATHS: { path: string; changefreq: string; priority: string }[] = [
  { path: '/', changefreq: 'weekly', priority: '1.0' },
  { path: '/check', changefreq: 'weekly', priority: '0.9' },
  { path: '/about', changefreq: 'monthly', priority: '0.7' },
  { path: '/privacy', changefreq: 'yearly', priority: '0.3' },
  { path: '/terms', changefreq: 'yearly', priority: '0.3' },
];

export const SEO_KEYWORDS = [
  'TG EAPCET college predictor',
  'TS EAMCET college predictor',
  'TG EAPCET rank wise colleges',
  'TS EAMCET counselling',
  'TG EAPCET counselling',
  'TG EAPCET web options',
  'engineering colleges Telangana',
  'college predictor Telangana',
  'TS EAMCET rank predictor',
  'safe target dream colleges',
].join(', ');

export const FAQ_ITEMS: { question: string; answer: string }[] = [
  {
    question: 'What colleges can I get with my TG EAPCET rank?',
    answer:
      'Enter your TG EAPCET rank, category, gender, branch preferences, and cutoff year on RankWise. We compare your rank with previous-year closing ranks and show Dream, Target, and Safe engineering colleges in Telangana.',
  },
  {
    question: 'How does the college predictor work?',
    answer:
      'RankWise uses official TG EAPCET / TS EAMCET last-rank cutoff data by phase and year. Your rank is matched against historical closing ranks for each college and branch to classify options as dream, target, or safe.',
  },
  {
    question: 'What are safe, target and dream colleges?',
    answer:
      'Dream colleges are ambitious choices where the closing rank is better than yours. Target colleges are realistic matches near your rank. Safe colleges are backup options where your rank comfortably meets previous cutoffs.',
  },
  {
    question: 'How can I join the RankWise community?',
    answer:
      'Join our free WhatsApp community for TG EAPCET counselling updates, web options help, certificate guidance, branch selection tips, and seat allotment updates throughout admission season.',
  },
  {
    question: 'How does TG EAPCET counselling work?',
    answer:
      'After results, students verify certificates, exercise web options by rank and reservation category, attend certificate verification, and accept allotted seats in multiple phases. RankWise helps you shortlist colleges before each web options round.',
  },
];

const baseOg = {
  image: DEFAULT_OG_IMAGE,
  type: 'website' as const,
  siteName: SITE_NAME,
};

export const ROUTE_SEO: Record<string, SeoConfig> = {
  '': {
    title: HOME_TITLE,
    description: DEFAULT_DESCRIPTION,
    keywords: SEO_KEYWORDS,
    canonicalPath: '/',
    robots: 'index, follow',
    og: {
      ...baseOg,
      title: HOME_TITLE,
      description: DEFAULT_DESCRIPTION,
    },
  },
  check: {
    title: 'Check My Colleges | TG EAPCET Rank Predictor | RankWise',
    description:
      'Free TG EAPCET college list by rank. Get safe, target and dream engineering colleges in Telangana using previous year cutoff data.',
    keywords: SEO_KEYWORDS,
    canonicalPath: '/check',
    robots: 'index, follow',
    og: {
      ...baseOg,
      title: 'Check My Colleges | TG EAPCET Rank Predictor | RankWise',
      description:
        'Free TG EAPCET college list by rank. Get safe, target and dream engineering colleges in Telangana.',
    },
  },
  about: {
    title: 'About RankWise | TG EAPCET Counselling Platform',
    description:
      'Learn how RankWise helps TG EAPCET students choose engineering colleges with cutoff-based predictions and free WhatsApp counselling support.',
    canonicalPath: '/about',
    robots: 'index, follow',
    og: {
      ...baseOg,
      title: 'About RankWise | TG EAPCET Counselling Platform',
      description:
        'Learn how RankWise helps TG EAPCET students choose engineering colleges with cutoff-based predictions.',
    },
  },
  privacy: {
    title: 'Privacy Policy | RankWise',
    description: 'RankWise privacy policy for TG EAPCET college predictor and counselling platform users.',
    canonicalPath: '/privacy',
    robots: 'index, follow',
    og: { ...baseOg, title: 'Privacy Policy | RankWise' },
  },
  terms: {
    title: 'Terms of Use | RankWise',
    description: 'Terms of use for RankWise TG EAPCET college predictor and counselling services.',
    canonicalPath: '/terms',
    robots: 'index, follow',
    og: { ...baseOg, title: 'Terms of Use | RankWise' },
  },
  results: {
    title: 'Your College Recommendations | RankWise',
    description: 'TG EAPCET college recommendations based on your rank.',
    canonicalPath: '/results',
    robots: 'noindex, nofollow',
    og: { ...baseOg, title: 'Your College Recommendations | RankWise' },
  },
  'admin/login': {
    title: 'Admin Login | RankWise',
    description: 'RankWise admin',
    canonicalPath: '/admin/login',
    robots: 'noindex, nofollow',
  },
  'admin/import': {
    title: 'Admin Import | RankWise',
    description: 'RankWise admin',
    canonicalPath: '/admin/import',
    robots: 'noindex, nofollow',
  },
  'admin/leads': {
    title: 'Admin Leads | RankWise',
    description: 'RankWise admin',
    canonicalPath: '/admin/leads',
    robots: 'noindex, nofollow',
  },
};
