export const WHATSAPP_PHONE = '918121871508';

/** Display format for UI (without country code). */
export const WHATSAPP_DISPLAY = '8121871508';

/** Short label for WhatsApp CTA buttons (no phone number shown). */
export const WHATSAPP_CTA_LABEL = 'Personalised EAMCET Counselling';

const DEFAULT_WHATSAPP_MESSAGE =
  'Hi RankWise! I need personalized TG EAPCET counselling help.';

/** Build wa.me link with a pre-filled message (opens chat with you on WhatsApp). */
export function buildWhatsAppUrl(message = DEFAULT_WHATSAPP_MESSAGE): string {
  return `https://wa.me/${WHATSAPP_PHONE}?text=${encodeURIComponent(message)}`;
}

/** Direct WhatsApp chat for personalized TG EAPCET counselling. */
export const WHATSAPP_URL = buildWhatsAppUrl(
  'Hi RankWise! I need personalized TG EAPCET counselling. My rank: ___ | Category: ___ | Branches: ___',
);

/** @deprecated Use WHATSAPP_URL — kept for existing imports during migration. */
export const WHATSAPP_COMMUNITY_URL = WHATSAPP_URL;

export const WHATSAPP_COUNSELING_URL = WHATSAPP_URL;

export const INSTAGRAM_URL = 'https://www.instagram.com/rankwise.co.in';

export const RANKWISE_LINKEDIN_URL = 'https://www.linkedin.com/company/rankwisets/';

export const CONTACT_EMAIL = 'rankwise.help@gmail.com';

export const FOUNDER_NAME = 'Md Mubasheer';
export const FOUNDER_CREDIT = 'Built by Md Mubasheer';
export const FOUNDER_BIO =
  'Backend Engineer | Building data-driven tools that solve real-world problems';
export const FOUNDER_LINKEDIN_URL = 'https://www.linkedin.com/in/md-mubasheer-bbab211aa/';
