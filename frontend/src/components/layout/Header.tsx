import { useTranslation } from 'react-i18next'
import './Header.css'

const LANGUAGES = [
  { code: 'pt-BR', label: 'PT' },
  { code: 'en', label: 'EN' },
]

export function Header() {
  const { i18n, t } = useTranslation()

  return (
    <header className="header">
      <span className="header__title">{t('header.title')}</span>
      <div className="header__spacer" />
      <div className="header__actions">
        <div className="lang-switcher">
          {LANGUAGES.map(({ code, label }) => (
            <button
              key={code}
              className={`lang-switcher__btn${i18n.language === code ? ' lang-switcher__btn--active' : ''}`}
              onClick={() => i18n.changeLanguage(code)}
              aria-label={`Switch to ${label}`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>
    </header>
  )
}
