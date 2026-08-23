import { useRef } from 'react'
import { EmojiPickerButton } from '../../../core/ui/pickers/EmojiPickerButton'
import { IconPicker } from '../../../core/ui/pickers/IconPicker'
import { STICKER_FIELD_ICON } from '../../../core/ui/pickers/pickerCatalog'
import { StickerPicker } from '../../../core/ui/pickers/StickerPicker'
import styles from '../../../core/ui/dialogs/DialogShell.module.css'

interface ReminderFormFieldsProps {
  title: string
  onTitleChange: (value: string) => void
  description: string
  onDescriptionChange: (value: string) => void
  dueAtLocal: string
  onDueAtLocalChange: (value: string) => void
  iconId?: string
  onIconChange: (value: string | undefined) => void
  stickerId?: string
  onStickerChange: (value: string | undefined) => void
  onCancel?: () => void
  onSubmit?: () => void
}

export function ReminderFormFields({
  title,
  onTitleChange,
  description,
  onDescriptionChange,
  dueAtLocal,
  onDueAtLocalChange,
  iconId,
  onIconChange,
  stickerId,
  onStickerChange,
  onCancel,
  onSubmit,
}: ReminderFormFieldsProps) {
  const titleRef = useRef<HTMLInputElement | null>(null)
  const descriptionRef = useRef<HTMLTextAreaElement | null>(null)

  return (
    <form 
      className={styles.formContainer} 
      onSubmit={(e) => {
        e.preventDefault()
        if (onSubmit) onSubmit()
      }}
    >
      {/* TÍTULO */}
      <div className={styles.field}>
        <label htmlFor="reminder-title" className={styles.fieldLabel}>
          Título
        </label>
        <div className={styles.inputWithEndAction}>
          <input
            id="reminder-title"
            ref={titleRef}
            className={styles.textInput}
            value={title}
            onChange={(event) => onTitleChange(event.target.value)}
            placeholder="¿Qué quieres recordar?"
            required
            aria-required="true"
          />
          <div className={styles.endAction}>
            <EmojiPickerButton
              targetRef={titleRef}
              value={title}
              onChange={onTitleChange}
              dialogLabel="Insertar emoji en el título"
              aria-label="Insertar emoji en el título"
            />
          </div>
        </div>
      </div>

      {/* DESCRIPCIÓN */}
      <div className={styles.field}>
        <label htmlFor="reminder-description" className={styles.fieldLabel}>
          Descripción
        </label>
        <div className={styles.inputWithEndAction}>
          <textarea
            id="reminder-description"
            ref={descriptionRef}
            className={styles.textArea}
            value={description}
            onChange={(event) => onDescriptionChange(event.target.value)}
            rows={3}
            placeholder="Añade notas o detalles adicionales..."
          />
          <div className={styles.endActionTop}>
            <EmojiPickerButton
              targetRef={descriptionRef}
              value={description}
              onChange={onDescriptionChange}
              dialogLabel="Insertar emoji en la descripción"
              aria-label="Insertar emoji en la descripción"
            />
          </div>
        </div>
      </div>

      {/* FECHA Y HORA */}
      <div className={styles.field}>
        <label htmlFor="reminder-dueat" className={styles.fieldLabel}>
          Fecha y hora <span className={styles.optionalTag}>(Opcional)</span>
        </label>
        <input
          id="reminder-dueat"
          type="datetime-local"
          className={styles.textInput}
          value={dueAtLocal}
          onChange={(event) => onDueAtLocalChange(event.target.value)}
        />
      </div>

      {/* SECCIÓN PERSONALIZACIÓN (STICKERS E ICONOS) */}
      <div className={styles.pickerSection} role="group" aria-label="Personalización visual">
        <div className={styles.field}>
          <span className={styles.fieldLabel}>
            <STICKER_FIELD_ICON width={14} height={14} /> Sticker
          </span>
          <div className={styles.pickerWrapper}>
            <StickerPicker value={stickerId} onChange={onStickerChange} />
          </div>
        </div>

        <div className={styles.field}>
          <span className={styles.fieldLabel}>Icono</span>
          <div className={styles.pickerWrapper}>
            <IconPicker value={iconId} onChange={onIconChange} />
          </div>
        </div>
      </div>

      {/* BOTONES DE ACCIÓN */}
      <div className={styles.formActions}>
        {onCancel && (
          <button 
            type="button" 
            className={styles.cancelBtn}
            onClick={onCancel}
          >
            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}