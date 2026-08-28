import { useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { useModeContext } from '../../core/user/ModeContext'
import type { Mode } from '../../core/user/modes'
import { useProfile } from '../../core/user/useVocabulary'
import {
  PROFILE_ORDER,
  PROFILE_VOCABULARY,
  writeProfile,
  type ProfessionalProfile,
} from '../../core/user/vocabulary'
import styles from './SettingsPage.module.css'

/**
 * ADR-015 / FR-016 / UC-15 — first real screen in features/settings (was
 * `.gitkeep`-only). Scope deliberately minimal per CLAUDE.md's
 * over-architecture check: the only thing FR-016 asks for is activating a
 * mode the user didn't enable at registration. Deactivating an already-
 * enabled mode is explicit ADR-015 TBD (not implemented — no button for it),
 * and nothing else belongs on this screen yet.
 */
export function SettingsPage() {
  const { loading, personalEnabled, laboralEnabled, backendSupportsModes, activateMode } = useModeContext()
  const [activating, setActivating] = useState<Mode | null>(null)
  const profile = useProfile()

  async function handleActivate(mode: Mode) {
    setActivating(mode)
    try {
      await activateMode(mode)
    } finally {
      setActivating(null)
    }
  }

  return (
    <AppShell title="Ajustes" subtitle="Activa los modos con los que quieres usar Agenda.">
      <div className={styles.page}>
        {!backendSupportsModes && (
          <p role="status" className={styles.mockNotice}>
            ◌ El backend todavía no confirma estos modos de forma permanente — esta pantalla funciona con datos
            reales cuando estén disponibles y, mientras tanto, con un estado local que se pierde al recargar (ver
            core/user/modes.ts).
          </p>
        )}

        {!loading && (
          <div className={styles.modesList}>
            <ModeRow
              label="Personal"
              description="Tu vida cotidiana, tu hogar, tu familia."
              enabled={personalEnabled}
              activating={activating === 'PERSONAL'}
              onActivate={() => handleActivate('PERSONAL')}
            />
            <ModeRow
              label="Laboral"
              description="Tu consultorio, tu oficina, tu negocio."
              enabled={laboralEnabled}
              activating={activating === 'LABORAL'}
              onActivate={() => handleActivate('LABORAL')}
            />
          </div>
        )}

        {/* UX-014/UX-015 (design-system.md §12, ADR-016(d)): perfil
            profesional. Solo cambia cómo se nombran Proyecto y Persona en el
            modo Laboral — mismas tablas, mismos endpoints, mismas pantallas.
            Se muestra únicamente si Laboral está activo: sin ese modo, el
            vocabulario no afecta a ninguna pantalla visible. */}
        {!loading && laboralEnabled && (
          <section className={styles.profileSection} aria-labelledby="profile-heading">
            <h2 id="profile-heading" className={styles.sectionTitle}>
              Perfil
            </h2>
            <p className={styles.sectionHint}>
              Elige cómo quieres que Agenda nombre tu trabajo. Solo cambia las palabras: tus datos, pantallas y
              funciones son exactamente los mismos en cualquier perfil.
            </p>

            <label className={styles.profileField}>
              <span className={styles.profileLabel}>Perfil profesional</span>
              <select
                className={styles.profileSelect}
                value={profile}
                onChange={(e) => writeProfile(e.target.value as ProfessionalProfile)}
              >
                {PROFILE_ORDER.map((id) => (
                  <option key={id} value={id}>
                    {PROFILE_VOCABULARY[id].label}
                  </option>
                ))}
              </select>
            </label>

            <p className={styles.profilePreview} role="status">
              Con este perfil verás{' '}
              <strong>{PROFILE_VOCABULARY[profile].projectPlural}</strong> en vez de "Proyectos" y{' '}
              <strong>{PROFILE_VOCABULARY[profile].personPlural}</strong> en vez de "Personas".
            </p>
          </section>
        )}
      </div>
    </AppShell>
  )
}

interface ModeRowProps {
  label: string
  description: string
  enabled: boolean
  activating: boolean
  onActivate: () => void
}

function ModeRow({ label, description, enabled, activating, onActivate }: ModeRowProps) {
  return (
    <div className={styles.modeRow} data-testid={`mode-row-${label.toLowerCase()}`}>
      <div>
        <strong>{label}</strong>
        <p>{description}</p>
      </div>
      {enabled ? (
        <span className="badge badge-success">Activado</span>
      ) : (
        <button type="button" onClick={onActivate} disabled={activating}>
          {activating ? 'Activando…' : 'Activar'}
        </button>
      )}
    </div>
  )
}
