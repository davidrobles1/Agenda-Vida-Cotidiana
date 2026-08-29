import { Button } from 'react-aria-components'
import {
  ArrowDownToLine,
  ArrowUpToLine,
  ChevronDown,
  ChevronUp,
  Copy,
  FolderX,
  Lock,
  LockOpen,
  Minus,
  Plus,
  Redo2,
  RotateCcw,
  Undo2,
} from 'lucide-react'
import {
  createVisionBoard,
  type CreateVisionBoardElementInput,
  type VisionBoard,
  type VisionBoardElement,
  type VisionBoardThemeId,
} from './api'
import { CreateVisionBoardDialog } from './CreateVisionBoardDialog'
import {
  VisionBoardAnimationAction,
  type EntranceAnimationId,
} from './VisionBoardAnimationAction'
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
  board: VisionBoard
  boards: VisionBoard[]
  switchingBoard: boolean
  onSwitchBoard: (id: string) => void
  onBoardCreated: (board: VisionBoard) => void
  onDeleteBoard: () => Promise<void>
  saveStatus: SaveStatus
  onRetrySave: () => void
  elements: VisionBoardElement[]
  theme: VisionBoardThemeId
  themeSaving: boolean
  onThemeChange: (theme: VisionBoardThemeId) => void
  onCreate: (input: CreateVisionBoardElementInput) => void
  /** Petición 1.2: varias fotos a la vez — ver VisionBoardElementLibrary. */
  onCreateManyImages?: (files: File[]) => void
  selectedElements: VisionBoardElement[]
  selectedElementSaving: boolean
  onSaveElement: (data: Record<string, unknown>) => Promise<void>
  onApplyEntranceAnimation: (animationId: EntranceAnimationId) => void
  onLayerChange: (kind: LayerChangeKind) => void
  onDuplicate: () => void
  onDeleteSelected: () => Promise<void>
  deleteConfirmOpen: boolean
  onDeleteConfirmOpenChange: (open: boolean) => void
  onToggleLock: () => void
  onApplyTemplate: (template: VisionBoardTemplate) => Promise<void>
  canUndo: boolean
  canRedo: boolean
  onUndo: () => void
  onRedo: () => void
  zoom: number
  onZoomIn: () => void
  onZoomOut: () => void
  onZoomReset: () => void
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
  onCreateManyImages,
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
  const singleSelected =
    selectedElements.length === 1 ? selectedElements[0] : null

  const showSingleElementActions = selectedElements.length <= 1

  const singleSelectedLocked = singleSelected?.locked ?? false

  const deletableCount = selectedElements.filter(
    (el) => !el.locked,
  ).length

  const allSelectedLocked =
    selectedElements.length > 0 &&
    selectedElements.every((el) => el.locked)

  return (
    <div
      className={styles.toolbar}
      role="toolbar"
      aria-label="Herramientas del Vision Board"
    >
      {/* =====================================================
          BOARD
          ===================================================== */}

      <VisionBoardSwitcher
        boards={boards}
        currentBoardId={board.id}
        switching={switchingBoard}
        onSwitch={onSwitchBoard}
      />

      <CreateVisionBoardDialog
        onCreate={createVisionBoard}
        onCreated={onBoardCreated}
        triggerClassName={styles.actionButton}
        isOpen={createBoardOpen}
        onOpenChange={onCreateBoardOpenChange}
      />

      {/* Hallazgo real (Playwright, 2026-08-23): sin texto, este botón y el
          de "Eliminar" un elemento mostraban EL MISMO icono de papelera,
          con consecuencias radicalmente distintas (borrar el tablero
          entero vs. un elemento). Icono propio para el de tablero. */}
      <VisionBoardDeleteConfirm
        count={1}
        disabled={false}
        onConfirm={onDeleteBoard}
        triggerIcon={<FolderX width={16} height={16} />}
        triggerLabel="Eliminar Vision Board"
        heading="Eliminar Vision Board"
        body={
          boards.length > 1
            ? `"${board.name}" se eliminará permanentemente, junto con todos sus elementos. Se cambiará automáticamente a otro Vision Board.`
            : `"${board.name}" se eliminará permanentemente, junto con todos sus elementos. Es tu único Vision Board — después de eliminarlo podrás crear uno nuevo.`
        }
      />

      <VisionBoardSaveIndicator
        status={saveStatus}
        onRetry={onRetrySave}
      />

      {/* =====================================================
          SELECTION
          ===================================================== */}

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

          <LayerActions
            disabled={!singleSelected || singleSelectedLocked}
            onLayerChange={onLayerChange}
          />

          <VisionBoardAnimationAction
            disabled={!singleSelected || singleSelectedLocked}
            current={
              typeof singleSelected?.data.entrance === 'string'
                ? (singleSelected.data.entrance as EntranceAnimationId)
                : undefined
            }
            onApply={onApplyEntranceAnimation}
          />
        </>
      )}

      {/* Barra icon-only (2026-08-23): se quitó el texto visible de cada
          botón para que la barra lateral ocupe poco ancho. `aria-label` es
          obligatorio en todos — sin él, un botón que solo contiene un SVG
          `aria-hidden` queda sin nombre accesible (violación WCAG 4.1.2,
          el mismo hallazgo real que ya documenta VisionBoardContextMenu.tsx
          sobre su propio botón ancla). */}
      <Button
        className={styles.actionButton}
        isDisabled={selectedElements.length === 0}
        onPress={onDuplicate}
        aria-label="Duplicar"
      >
        <Copy width={16} height={16} />
      </Button>

      <Button
        className={styles.actionButton}
        isDisabled={selectedElements.length === 0}
        onPress={onToggleLock}
        aria-label={allSelectedLocked ? 'Desbloquear' : 'Bloquear'}
      >
        {allSelectedLocked ? (
          <LockOpen width={16} height={16} />
        ) : (
          <Lock width={16} height={16} />
        )}
      </Button>

      <VisionBoardDeleteConfirm
        count={deletableCount}
        onConfirm={onDeleteSelected}
        isOpen={deleteConfirmOpen}
        onOpenChange={onDeleteConfirmOpenChange}
      />

      {/* =====================================================
          CREATIVE
          ===================================================== */}

      <VisionBoardTemplatesAction
        onApply={onApplyTemplate}
        isOpen={templatesOpen}
        onOpenChange={onTemplatesOpenChange}
      />

      <VisionBoardElementLibrary
        onCreate={onCreate}
        onCreateManyImages={onCreateManyImages}
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

      <VisionBoardExportAction
        board={board}
        elements={elements}
      />

      {/* =====================================================
          HISTORY
          ===================================================== */}

      <HistoryActions
        canUndo={canUndo}
        canRedo={canRedo}
        onUndo={onUndo}
        onRedo={onRedo}
      />

      {/* =====================================================
          ZOOM
          ===================================================== */}

      <ZoomControls
        zoom={zoom}
        onZoomIn={onZoomIn}
        onZoomOut={onZoomOut}
        onZoomReset={onZoomReset}
      />
    </div>
  )
}

/* =========================================================
   LAYER ACTIONS
   ========================================================= */

function LayerActions({
  disabled,
  onLayerChange,
}: {
  disabled: boolean
  onLayerChange: (kind: LayerChangeKind) => void
}) {
  return (
    <div
      className={styles.buttonGroup}
      role="group"
      aria-label="Capas"
    >
      <Button
        className={styles.iconButton}
        isDisabled={disabled}
        onPress={() => onLayerChange('back')}
        aria-label="Enviar al fondo"
      >
        <ArrowDownToLine width={16} height={16} />
      </Button>

      <Button
        className={styles.iconButton}
        isDisabled={disabled}
        onPress={() => onLayerChange('lower')}
        aria-label="Bajar una capa"
      >
        <ChevronDown width={16} height={16} />
      </Button>

      <Button
        className={styles.iconButton}
        isDisabled={disabled}
        onPress={() => onLayerChange('raise')}
        aria-label="Subir una capa"
      >
        <ChevronUp width={16} height={16} />
      </Button>

      <Button
        className={styles.iconButton}
        isDisabled={disabled}
        onPress={() => onLayerChange('front')}
        aria-label="Traer al frente"
      >
        <ArrowUpToLine width={16} height={16} />
      </Button>
    </div>
  )
}

/* =========================================================
   HISTORY
   ========================================================= */

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
    <div
      className={styles.buttonGroup}
      role="group"
      aria-label="Deshacer / rehacer"
    >
      <Button
        className={styles.iconButton}
        isDisabled={!canUndo}
        onPress={onUndo}
        aria-label="Deshacer"
      >
        <Undo2 width={16} height={16} />
      </Button>

      <Button
        className={styles.iconButton}
        isDisabled={!canRedo}
        onPress={onRedo}
        aria-label="Rehacer"
      >
        <Redo2 width={16} height={16} />
      </Button>
    </div>
  )
}

/* =========================================================
   ZOOM
   ========================================================= */

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
    <div
      className={styles.buttonGroup}
      role="group"
      aria-label="Zoom"
    >
      <Button
        className={styles.iconButton}
        onPress={onZoomOut}
        aria-label="Alejar"
      >
        <Minus width={16} height={16} />
      </Button>

      <span className={styles.zoomValue}>
        {Math.round(zoom * 100)}%
      </span>

      <Button
        className={styles.iconButton}
        onPress={onZoomIn}
        aria-label="Acercar"
      >
        <Plus width={16} height={16} />
      </Button>

      <Button
        className={styles.iconButton}
        onPress={onZoomReset}
        aria-label="Restablecer zoom"
      >
        <RotateCcw width={16} height={16} />
      </Button>
    </div>
  )
}