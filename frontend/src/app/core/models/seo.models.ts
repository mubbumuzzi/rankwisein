export interface SeoOgConfig {
  title?: string;
  description?: string;
  image?: string;
  type?: 'website' | 'article';
  siteName?: string;
}

export interface SeoConfig {
  title: string;
  description: string;
  keywords?: string;
  canonicalPath: string;
  robots?: string;
  og?: SeoOgConfig;
}

export type JsonLd = Record<string, unknown>;
