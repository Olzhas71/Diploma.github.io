import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { ru } from './ru';
import { kk } from './kk';
import { en } from './en';

export const SUPPORTED_LANGUAGES = [
  { code: 'ru', label: 'Русский', flag: '🇷🇺' },
  { code: 'kk', label: 'Қазақша', flag: '🇰🇿' },
  { code: 'en', label: 'English', flag: '🇬🇧' },
] as const;

export type LanguageCode = (typeof SUPPORTED_LANGUAGES)[number]['code'];

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      ru: { translation: ru },
      kk: { translation: kk },
      en: { translation: en },
    },
    fallbackLng: 'ru',
    supportedLngs: SUPPORTED_LANGUAGES.map((l) => l.code),
    interpolation: { escapeValue: false }, // React already escapes
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'parking-lang',
      caches: ['localStorage'],
    },
  });

export default i18n;
