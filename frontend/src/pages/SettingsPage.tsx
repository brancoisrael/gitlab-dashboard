import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '../components/Toast/ToastContext'
import './SettingsPage.css'

type StorageType = 'local' | 's3'

interface StorageConfig {
  type: StorageType
  localPath: string
  endpoint: string
  bucket: string
  region: string
  accessKey: string
  secretKey: string
}

const EMPTY_CONFIG: StorageConfig = {
  type: 'local',
  localPath: '',
  endpoint: '',
  bucket: '',
  region: 'us-east-1',
  accessKey: '',
  secretKey: '',
}

export function SettingsPage() {
  const { t } = useTranslation()
  const { showToast } = useToast()

  const [config, setConfig] = useState<StorageConfig>(EMPTY_CONFIG)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    fetch('/api/settings/storage')
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json() as Promise<StorageConfig>
      })
      .then((data) => setConfig({ ...EMPTY_CONFIG, ...data }))
      .catch(() => showToast(t('common.error'), 'error'))
      .finally(() => setLoading(false))
  }, [showToast, t])

  function handleChange(field: keyof StorageConfig, value: string) {
    setConfig((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setSaving(true)
    try {
      const res = await fetch('/api/settings/storage', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config),
      })
      if (res.ok) {
        showToast(t('settings.storage.saveSuccess'), 'success')
      } else {
        showToast(t('settings.storage.saveError'), 'error')
      }
    } catch {
      showToast(t('settings.storage.saveError'), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="settings-page">
      <h1>{t('settings.title')}</h1>

      <section className="settings-page__section">
        <div className="settings-page__section-header">
          <h2>{t('settings.storage.title')}</h2>
        </div>
        <p className="settings-page__description">{t('settings.storage.description')}</p>

        {loading ? (
          <p className="settings-page__msg">{t('common.loading')}</p>
        ) : (
          <form className="settings-page__form" onSubmit={handleSubmit}>

            {/* Storage type */}
            <div className="settings-field">
              <label className="settings-label">{t('settings.storage.type')}</label>
              <div className="settings-radio-group">
                <label className="settings-radio-option">
                  <input
                    type="radio"
                    name="storageType"
                    value="local"
                    checked={config.type === 'local'}
                    onChange={() => handleChange('type', 'local')}
                  />
                  <span>{t('settings.storage.local')}</span>
                </label>
                <label className="settings-radio-option">
                  <input
                    type="radio"
                    name="storageType"
                    value="s3"
                    checked={config.type === 's3'}
                    onChange={() => handleChange('type', 's3')}
                  />
                  <span>{t('settings.storage.s3')}</span>
                </label>
              </div>
            </div>

            {/* Local path */}
            {config.type === 'local' && (
              <div className="settings-field">
                <label className="settings-label" htmlFor="localPath">
                  {t('settings.storage.localPath')}
                </label>
                <input
                  id="localPath"
                  className="settings-input"
                  type="text"
                  value={config.localPath}
                  onChange={(e) => handleChange('localPath', e.target.value)}
                  placeholder={t('settings.storage.localPathPlaceholder')}
                />
              </div>
            )}

            {/* S3 fields */}
            {config.type === 's3' && (
              <>
                <div className="settings-field">
                  <label className="settings-label" htmlFor="endpoint">
                    {t('settings.storage.endpoint')}
                  </label>
                  <input
                    id="endpoint"
                    className="settings-input"
                    type="text"
                    value={config.endpoint}
                    onChange={(e) => handleChange('endpoint', e.target.value)}
                    placeholder={t('settings.storage.endpointPlaceholder')}
                  />
                </div>

                <div className="settings-row">
                  <div className="settings-field">
                    <label className="settings-label" htmlFor="bucket">
                      {t('settings.storage.bucket')}
                    </label>
                    <input
                      id="bucket"
                      className="settings-input"
                      type="text"
                      value={config.bucket}
                      onChange={(e) => handleChange('bucket', e.target.value)}
                      placeholder={t('settings.storage.bucketPlaceholder')}
                    />
                  </div>
                  <div className="settings-field">
                    <label className="settings-label" htmlFor="region">
                      {t('settings.storage.region')}
                    </label>
                    <input
                      id="region"
                      className="settings-input"
                      type="text"
                      value={config.region}
                      onChange={(e) => handleChange('region', e.target.value)}
                      placeholder={t('settings.storage.regionPlaceholder')}
                    />
                  </div>
                </div>

                <div className="settings-row">
                  <div className="settings-field">
                    <label className="settings-label" htmlFor="accessKey">
                      {t('settings.storage.accessKey')}
                    </label>
                    <input
                      id="accessKey"
                      className="settings-input"
                      type="text"
                      value={config.accessKey}
                      onChange={(e) => handleChange('accessKey', e.target.value)}
                      autoComplete="off"
                    />
                  </div>
                  <div className="settings-field">
                    <label className="settings-label" htmlFor="secretKey">
                      {t('settings.storage.secretKey')}
                    </label>
                    <input
                      id="secretKey"
                      className="settings-input"
                      type="password"
                      value={config.secretKey}
                      onChange={(e) => handleChange('secretKey', e.target.value)}
                      autoComplete="new-password"
                    />
                  </div>
                </div>
              </>
            )}

            <div className="settings-page__actions">
              <button
                type="submit"
                className="settings-page__btn"
                disabled={saving || loading}
              >
                {t('settings.storage.save')}
              </button>
            </div>
          </form>
        )}
      </section>
    </div>
  )
}
