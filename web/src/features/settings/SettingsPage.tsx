import { useState } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { useModeContext } from '../../core/user/ModeContext'
import type { Mode } from '../../core/user/modes'
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
