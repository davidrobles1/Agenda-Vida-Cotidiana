import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { IconPlus } from '../../core/ui/icons'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import type { CreateVisionBoardInput, VisionBoard, VisionBoardThemeId } from './api'
import { handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import { DEFAULT_VISION_BOARD_THEME, VISION_BOARD_THEME_LIST } from './visionBoardThemes'
import toolbarStyles from './VisionBoardToolbar.module.css'

const MotionDialog = motion.create(Dialog)

/** FASE 3: fixed canvas size for a newly created board — no size-picker UI
    (a numeric width/height input has no natural "good" default a user can
    reason about before ever seeing their board; 1600×1000 is the same
    reasonable default every board created in this project so far — Fase
    22's own backend tests, Fase 24's own new ones below — already uses). */
const DEFAULT_WIDTH = 1600
const DEFAULT_HEIGHT = 1000

interface CreateVisionBoardDialogProps {
  onCreate: (input: CreateVisionBoardInput) => Promise<VisionBoard>
  onCreated: (board: VisionBoard) => void
  /** FASE 24: this component now renders from two places (VisionBoardPage's
      own empty state, unstyled — kept exactly as it always looked — and
      VisionBoardToolbar's new "Nuevo Vision Board" entry, which needs to
      look like every other toolbar pill). Optional so the empty-state call
      site doesn't have to change. */
  triggerClassName?: string
  /** BLOQUE E (post-MVP): same optional-controlled shape as
      VisionBoardDeleteConfirm.tsx's own isOpen/onOpenChange — the context
      menu's "Nuevo Vision Board" entry needs to open this dialog too. */
  isOpen?: boolean
  onOpenChange?: (open: boolean) => void
}

/**
 * Creates the very first (or an additional) Vision Board — same real
 * pattern already established for Crear in this app (React Aria
 * `DialogTrigger` + `Modal` + `Dialog`, Motion animates the `Dialog`
 * itself, never the `Modal` — see reminders/CreateReminderDialog.tsx).
 *
 * FASE 24: this was already written generically enough ("the very first
 * *or an additional* Vision Board," per this doc comment's own original
 * wording) — the real gap wasn't this component, it was that
 * VisionBoardPage.tsx only ever rendered it in the empty-board branch.
 * Reused as-is here, now also rendered from VisionBoardToolbar so it's
 * reachable once a board already exists. Description and Board Theme
 * fields added — `CreateVisionBoardInput` (api.ts) already had `description`/
 * `theme` as optional fields since Fase 16, unused by this form until now;
 * no API/backend change needed. Board Theme options come straight from
 * `visionBoardThemes.ts` (`VISION_BOARD_THEME_LIST`), same single source
 * `VisionBoardThemeSwitcher.tsx` reads — never a second color definition.
 * Width/height stay fixed defaults, per this file's own established
 * reasoning (no size-picker UI).
 */
export function CreateVisionBoardDialog({
  onCreate,
  onCreated,
  triggerClassName,
  isOpen: isOpenProp,
  onOpenChange: onOpenChangeProp,
}: CreateVisionBoardDialogProps) {
  const [internalOpen, setInternalOpen] = useState(false)
  const isOpen = isOpenProp ?? internalOpen
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [theme, setTheme] = useState<VisionBoardThemeId>(DEFAULT_VISION_BOARD_THEME)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function handleOpenChange(open: boolean) {
    if (onOpenChangeProp) {
      onOpenChangeProp(open)
    } else {
      setInternalOpen(open)
    }
    if (!open) {
      setName('')
      setDescription('')
      setTheme(DEFAULT_VISION_BOARD_THEME)
      setError(null)
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const created = await onCreate({
        name: name.trim(),
        description: description.trim() || undefined,
        width: DEFAULT_WIDTH,
        height: DEFAULT_HEIGHT,
        theme,
      })
      onCreated(created)
      handleOpenChange(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo crear el Vision Board.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      {/* Barra icon-only (2026-08-23): sin texto visible este botón se
          quedaba SIN nombre accesible (su único hijo es un SVG
          `aria-hidden`) — violación WCAG 4.1.2, el mismo hallazgo real que
          ya documenta VisionBoardContextMenu.tsx sobre su botón ancla. */}
      <Button className={triggerClassName} aria-label="Nuevo Vision Board">
        <IconPlus width={16} height={16} />
      </Button>
      <Modal isDismissable className={shellStyles.modalOverlay}>
        <MotionDialog
          className={shellStyles.panel}
          initial={{ opacity: 0, scale: 0.96, y: 8 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : 8 }}
          transition={motionTokens.smooth}
        >
          {({ close }) => (
            <div className={shellStyles.panelScroll}>
              <form onSubmit={handleSubmit}>
                <div className={shellStyles.headerRow}>
                  <Heading slot="title" className={shellStyles.heading}>
                    Nuevo Vision Board
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Nombre</span>
                  <input
                    className={shellStyles.textInput}
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    required
                    autoFocus
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Descripción (opcional)</span>
                  <textarea
                    className={shellStyles.textArea}
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    rows={2}
                  />
                </label>

                <div className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Tema</span>
                  <div
                    className={toolbarStyles.shapeVariantGrid}
                    role="radiogroup"
                    aria-label="Tema del Vision Board"
                    onKeyDown={handleRadiogroupKeyDown}
                  >
                    {VISION_BOARD_THEME_LIST.map((option, index) => (
                      <button
                        key={option.id}
                        type="button"
                        role="radio"
                        aria-checked={theme === option.id}
                        tabIndex={radioTabIndex(theme === option.id, index === 0, true)}
                        className={`${toolbarStyles.shapeVariantButton} ${theme === option.id ? toolbarStyles.shapeVariantButtonActive : ''}`}
                        onClick={() => setTheme(option.id)}
                      >
                        <span
                          aria-hidden="true"
                          style={{
                            display: 'inline-block',
                            width: 10,
                            height: 10,
                            borderRadius: '50%',
                            background: option.colors.accent,
                            marginRight: 6,
                          }}
                        />
                        {option.name}
                      </button>
                    ))}
                  </div>
                </div>

                <div className={shellStyles.formActions}>
                  {saving && <span className={shellStyles.savingHint}>Creando…</span>}
                  <button type="button" data-variant="secondary" onClick={close} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={saving}>
                    Crear
                  </button>
                </div>
              </form>
            </div>
          )}
        </MotionDialog>
      </Modal>
    </DialogTrigger>
  )
}
