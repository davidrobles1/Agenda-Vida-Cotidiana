import { Button } from 'react-aria-components'
import {
  ArrowDownToLine,
  ArrowUpToLine,
  ChevronDown,
  ChevronUp,
  Copy,
  Lock,
  LockOpen,
  Minus,
  Plus,
  Redo2,
  RotateCcw,
  Undo2,
} from 'lucide-react'
import { createVisionBoard, type CreateVisionBoardElementInput, type VisionBoard, type VisionBoardElement, type VisionBoardThemeId } from './api'
import { CreateVisionBoardDialog } from './CreateVisionBoardDialog'
import { VisionBoardAnimationAction, type EntranceAnimationId } from './VisionBoardAnimationAction'
import type { SaveStatus } from './VisionBoardCanvas'
import { VisionBoardDeleteConfirm } from './VisionBoardDeleteConfirm'
import { VisionBoardElementEditor } from './VisionBoardElementEditor'
import { VisionBoardElementLibrary } from './VisionBoardElementLibrary'
import { VisionBoardExportAction } from './VisionBoardExportAction'
import { VisionBoardSaveIndicator } from './VisionBoardSaveIndicator'
import { VisionBoardSwitcher } from './VisionBoardSwitcher'
import { VisionBoardTemplatesAction } from './VisionBoardTemplatesAction'
import { VisionBoardThemeSwitcher } from './VisionBoardThemeSwitcher'
import type { VisionBoardTemplate } from './visionBoardTemplates'
import styles from './VisionBoardToolbar.module.css'

type LayerChangeKind = 'front' | 'back' | 'raise' | 'lower'

interface VisionBoardToolbarProps {
  /** FASE 14: the board + its elements as VisionBoardCanvas already holds
      them — VisionBoardExportAction reads straight from this, no second
      copy of the board's data. */
  board: VisionBoard
  /** FASE 17 */
  boards: VisionBoard[]
  switchingBoard: boolean
  onSwitchBoard: (id: string) => void
  /** FASE 24: same "this component doesn't own board-level state, only
      forwards" shape as `onSwitchBoard` above. */
  onBoardCreated: (board: VisionBoard) => void
  /** BLOQUE A: deletes *this* board for real — rejects (and leaves the
      confirm dialog open with the error inline) if the request fails, same
      "owned/forwarded" split as every other board-level action above. */
  onDeleteBoard: () => Promise<void>
  /** FASE 18 */
  saveStatus: SaveStatus
  onRetrySave: () => void
  /** FASE 14: every element on the board — distinct from `selectedElements`
      below (only the current selection), since export renders the whole
      board regardless of what's selected. */
  elements: VisionBoardElement[]
  /** FASE 16 */
  theme: VisionBoardThemeId
  themeSaving: boolean
  onThemeChange: (theme: VisionBoardThemeId) => void
  onCreate: (input: CreateVisionBoardElementInput) => void
  /** FASE 11: every currently selected element (was a single `| null` —
      Editar/Capas only make sense for exactly one, so they render only
      when this has length 1; Duplicar/Eliminar work off the whole array).
      Owned by VisionBoardCanvas, same as onCreate. */
  selectedElements: VisionBoardElement[]
  selectedElementSaving: boolean
  onSaveElement: (data: Record<string, unknown>) => Promise<void>
  /** 2026-08-22 (pedido explícito del usuario, catálogo estilo Canva):
      mismo enabled-solo-con-una-selección que Editar/Capas. */
  onApplyEntranceAnimation: (animationId: EntranceAnimationId) => void
  /** FASE 8: same enabled/shown-only-with-a-single-selection rule as
      Editar. */
  onLayerChange: (kind: LayerChangeKind) => void
  /** FASE 11: works on the whole selection, one or many. */
  onDuplicate: () => void
  onDeleteSelected: () => Promise<void>
  /** FASE 17: lifted so the canvas's Delete/Backspace shortcut can open
      the same confirm dialog the Eliminar button opens. */
  deleteConfirmOpen: boolean
  onDeleteConfirmOpenChange: (open: boolean) => void
  /** FASE 17: bloquear/desbloquear — works on the whole selection, same as
      Duplicar/Eliminar. */
  onToggleLock: () => void
  /** FASE 12: independent of selection — Templates always adds on top of
      whatever's already on the board. */
  onApplyTemplate: (template: VisionBoardTemplate) => Promise<void>
  /** FASE 9: enabled purely by stack occupancy — independent of selection,
      unlike Editar/LayerActions above. */
  canUndo: boolean
  canRedo: boolean
  onUndo: () => void
  onRedo: () => void
  /** FASE 10 */
  zoom: number
  onZoomIn: () => void
  onZoomOut: () => void
  onZoomReset: () => void
  /** BLOQUE E (post-MVP): lifted open-state for the popovers the context
      menu also needs to be able to open — see VisionBoardCanvas.tsx's own
      doc comment on this same group of props. */
  templatesOpen: boolean
  onTemplatesOpenChange: (open: boolean) => void
  elementLibraryOpen: boolean
  onElementLibraryOpenChange: (open: boolean) => void
  themeSwitcherOpen: boolean
  onThemeSwitcherOpenChange: (open: boolean) => void
  createBoardOpen: boolean
  onCreateBoardOpenChange: (open: boolean) => void
  elementEditorOpen: boolean
  onElementEditorOpenChange: (open: boolean) => void
}

/**
 * FASE 6: creates TEXT/IMAGE/NOTE/STICKER/SHAPE elements via the real
 * `POST /vision-boards/{id}/elements` (Fase 2) — `onCreate` is owned by
 * VisionBoardCanvas, which already holds the optimistic `elements` state,
 * selection, and the saving/error UI Fase 4/5 built; this component only
 * collects the type-specific input and hands off a fully-formed
 * `CreateVisionBoardElementInput`, same "presentation calls back to the
 * owner of the data" shape as ReminderFormFields/NoteFormFields.
 *
 * A plain flex row of React Aria `Button`s, not `Toolbar` from
 * react-aria-components — nothing in this app uses that primitive yet, and
 * a `Toolbar`'s own roving-tabindex/orientation semantics exist for larger
 * button groups than the handful of independent, unrelated actions here;
 * the existing convention (CalendarPage's quickAddSection, AppShell's
 * topBarActions) is already a plain row of Buttons, so this matches that
 * rather than introducing a second pattern for the same job.
 *
 * FASE 13: the five individual create actions (Texto/Nota/Sticker/Imagen/
 * Forma) that used to sit inline here are now one consolidated
 * VisionBoardElementLibrary entry — five separate buttons plus Templates
 * plus Duplicar/Eliminar/Deshacer/Rehacer/Zoom was real clutter, and having
 * both the library *and* the old individual buttons would just duplicate
 * the same entry points twice.
 */
export function VisionBoardToolbar({
  board,
  boards,
  switchingBoard,
  onSwitchBoard,
  onBoardCreated,
  onDeleteBoard,
  saveStatus,
  onRetrySave,
  elements,
  theme,
  themeSaving,
  onThemeChange,
  onCreate,
  selectedElements,
  selectedElementSaving,
  onSaveElement,
  onApplyEntranceAnimation,
  onLayerChange,
  onDuplicate,
  onDeleteSelected,
  deleteConfirmOpen,
  onDeleteConfirmOpenChange,
  onToggleLock,
  onApplyTemplate,
  canUndo,
  canRedo,
  onUndo,
  onRedo,
  zoom,
  onZoomIn,
  onZoomOut,
  onZoomReset,
  templatesOpen,
  onTemplatesOpenChange,
  elementLibraryOpen,
  onElementLibraryOpenChange,
  themeSwitcherOpen,
  onThemeSwitcherOpenChange,
  createBoardOpen,
  onCreateBoardOpenChange,
  elementEditorOpen,
  onElementEditorOpenChange,
}: VisionBoardToolbarProps) {
  // FASE 11: Editar/Capas only mean something for exactly one element —
  // for a multi-selection they're hidden outright ("no muestres controles
  // que no tengan sentido para una selección múltiple"), not just disabled
  // (that's the zero-selection case, still handled inside each of these).
  const singleSelected = selectedElements.length === 1 ? selectedElements[0] : null
  const showSingleElementActions = selectedElements.length <= 1
  // FASE 17: a locked element can't be edited or reordered — same
  // "bloqueado significa no tocar sus propiedades" rule the pointer
  // handlers already enforce for move/resize/rotate.
  const singleSelectedLocked = singleSelected?.locked ?? false
  // "no puede eliminarse accidentalmente" — locked elements are excluded
  // from what Eliminar actually deletes; the dialog's own count must match.
  const deletableCount = selectedElements.filter((el) => !el.locked).length
  const allSelectedLocked = selectedElements.length > 0 && selectedElements.every((el) => el.locked)

  return (
    <div className={styles.toolbar} role="toolbar" aria-label="Herramientas del Vision Board">
      <VisionBoardSwitcher
        boards={boards}
        currentBoardId={board.id}
        switching={switchingBoard}
        onSwitch={onSwitchBoard}
      />
      {/* FASE 24: reuses CreateVisionBoardDialog exactly as-is (it was
          already generic enough for "the very first or an additional"
          board) — the real gap was that it was only ever reachable from
          VisionBoardPage's empty state. */}
      <CreateVisionBoardDialog
        onCreate={createVisionBoard}
        onCreated={onBoardCreated}
        triggerClassName={styles.actionButton}
        isOpen={createBoardOpen}
        onOpenChange={onCreateBoardOpenChange}
      />
      {/* BLOQUE A: reuses VisionBoardDeleteConfirm's own alertdialog pattern
          (generalized with optional label/heading/body overrides, see its
          own doc comment) instead of a second Modal/Dialog/Motion
          boilerplate — `count`/`onConfirm` still drive the same
          disabled/loading/error mechanics, just aimed at the whole board
          instead of the current element selection. Distinct label
          ("Eliminar Vision Board", not "Eliminar") so this and the
          elements-delete button never collide by accessible name. */}
      <VisionBoardDeleteConfirm
        count={1}
        disabled={false}
        onConfirm={onDeleteBoard}
        triggerLabel="Eliminar Vision Board"
        heading="Eliminar Vision Board"
        body={
          boards.length > 1
            ? `"${board.name}" se eliminará permanentemente, junto con todos sus elementos. Se cambiará automáticamente a otro Vision Board.`
            : `"${board.name}" se eliminará permanentemente, junto con todos sus elementos. Es tu único Vision Board — después de eliminarlo podrás crear uno nuevo.`
        }
      />
      <VisionBoardSaveIndicator status={saveStatus} onRetry={onRetrySave} />
      {showSingleElementActions && (
        <>
          <VisionBoardElementEditor
            element={singleSelected}
            saving={selectedElementSaving}
            disabled={singleSelectedLocked}
            onSave={onSaveElement}
            isOpen={elementEditorOpen}
            onOpenChange={onElementEditorOpenChange}
          />
          <LayerActions disabled={!singleSelected || singleSelectedLocked} onLayerChange={onLayerChange} />
          <VisionBoardAnimationAction
            disabled={!singleSelected || singleSelectedLocked}
            current={typeof singleSelected?.data.entrance === 'string' ? (singleSelected.data.entrance as EntranceAnimationId) : undefined}
            onApply={onApplyEntranceAnimation}
          />
        </>
      )}
      <Button className={styles.actionButton} isDisabled={selectedElements.length === 0} onPress={onDuplicate}>
        <Copy width={16} height={16} />
        Duplicar
      </Button>
      <Button className={styles.actionButton} isDisabled={selectedElements.length === 0} onPress={onToggleLock}>
        {allSelectedLocked ? <LockOpen width={16} height={16} /> : <Lock width={16} height={16} />}
        {allSelectedLocked ? 'Desbloquear' : 'Bloquear'}
      </Button>
      <VisionBoardDeleteConfirm
        count={deletableCount}
        onConfirm={onDeleteSelected}
        isOpen={deleteConfirmOpen}
        onOpenChange={onDeleteConfirmOpenChange}
      />
      <HistoryActions canUndo={canUndo} canRedo={canRedo} onUndo={onUndo} onRedo={onRedo} />
      <VisionBoardTemplatesAction
        onApply={onApplyTemplate}
        isOpen={templatesOpen}
        onOpenChange={onTemplatesOpenChange}
      />
      <VisionBoardElementLibrary
        onCreate={onCreate}
        isOpen={elementLibraryOpen}
        onOpenChange={onElementLibraryOpenChange}
      />
      <VisionBoardThemeSwitcher
        theme={theme}
        saving={themeSaving}
        onChange={onThemeChange}
        isOpen={themeSwitcherOpen}
        onOpenChange={onThemeSwitcherOpenChange}
      />
      <VisionBoardExportAction board={board} elements={elements} />

      <ZoomControls zoom={zoom} onZoomIn={onZoomIn} onZoomOut={onZoomOut} onZoomReset={onZoomReset} />
    </div>
  )
}

/** FASE 8: traer al frente / enviar al fondo / subir / bajar — all
    disabled together with nothing selected, same rule as Editar. */
function LayerActions({ disabled, onLayerChange }: { disabled: boolean; onLayerChange: (kind: LayerChangeKind) => void }) {
  return (
    <div className={styles.buttonGroup} role="group" aria-label="Capas">
      <Button className={styles.iconButton} isDisabled={disabled} onPress={() => onLayerChange('back')} aria-label="Enviar al fondo">
        <ArrowDownToLine width={16} height={16} />
      </Button>
      <Button className={styles.iconButton} isDisabled={disabled} onPress={() => onLayerChange('lower')} aria-label="Bajar una capa">
        <ChevronDown width={16} height={16} />
      </Button>
      <Button className={styles.iconButton} isDisabled={disabled} onPress={() => onLayerChange('raise')} aria-label="Subir una capa">
        <ChevronUp width={16} height={16} />
      </Button>
      <Button className={styles.iconButton} isDisabled={disabled} onPress={() => onLayerChange('front')} aria-label="Traer al frente">
        <ArrowUpToLine width={16} height={16} />
      </Button>
    </div>
  )
}

/** FASE 9: Deshacer/Rehacer — same icon-button group shape as LayerActions,
    but enabled purely by stack occupancy rather than the current
    selection. */
function HistoryActions({
  canUndo,
  canRedo,
  onUndo,
  onRedo,
}: {
  canUndo: boolean
  canRedo: boolean
  onUndo: () => void
  onRedo: () => void
}) {
  return (
    <div className={styles.buttonGroup} role="group" aria-label="Deshacer / rehacer">
      <Button className={styles.iconButton} isDisabled={!canUndo} onPress={onUndo} aria-label="Deshacer">
        <Undo2 width={16} height={16} />
      </Button>
      <Button className={styles.iconButton} isDisabled={!canRedo} onPress={onRedo} aria-label="Rehacer">
        <Redo2 width={16} height={16} />
      </Button>
    </div>
  )
}

/** FASE 10: −/percentage/+ plus a reset — percentage is a plain span, not
    a control (nothing to type here, "mostrar el porcentaje actual" only
    asks to display it). */
function ZoomControls({
  zoom,
  onZoomIn,
  onZoomOut,
  onZoomReset,
}: {
  zoom: number
  onZoomIn: () => void
  onZoomOut: () => void
  onZoomReset: () => void
}) {
  return (
    <div className={styles.buttonGroup} role="group" aria-label="Zoom">
      <Button className={styles.iconButton} onPress={onZoomOut} aria-label="Alejar">
        <Minus width={16} height={16} />
      </Button>
      <span className={styles.zoomValue}>{Math.round(zoom * 100)}%</span>
      <Button className={styles.iconButton} onPress={onZoomIn} aria-label="Acercar">
        <Plus width={16} height={16} />
      </Button>
      <Button className={styles.iconButton} onPress={onZoomReset} aria-label="Restablecer zoom">
        <RotateCcw width={16} height={16} />
      </Button>
    </div>
  )
}

