import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const SITE_URL = process.env.SITE_URL ?? 'https://rankwise.co.in';
const __dirname = dirname(fileURLToPath(import.meta.url));
const outDir = join(__dirname, '..', 'public');

const paths = [
  { loc: '/', changefreq: 'weekly', priority: '1.0' },
  { loc: '/check', changefreq: 'weekly', priority: '0.9' },
  { loc: '/about', changefreq: 'monthly', priority: '0.7' },
  { loc: '/privacy', changefreq: 'yearly', priority: '0.3' },
  { loc: '/terms', changefreq: 'yearly', priority: '0.3' },
];

const lastmod = new Date().toISOString().slice(0, 10);

const urls = paths
  .map(
    (p) => `  <url>
    <loc>${SITE_URL}${p.loc}</loc>
    <lastmod>${lastmod}</lastmod>
    <changefreq>${p.changefreq}</changefreq>
    <priority>${p.priority}</priority>
  </url>`
  )
  .join('\n');

const xml = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>
`;

mkdirSync(outDir, { recursive: true });
writeFileSync(join(outDir, 'sitemap.xml'), xml, 'utf8');
console.log(`Wrote sitemap.xml with ${paths.length} URLs → ${outDir}`);
