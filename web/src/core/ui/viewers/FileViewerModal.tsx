import { Dialog, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { AlertTriangle, Loader2 } from 'lucide-react'
import { motionTokens } from '../../motion/tokens'
import { useAuthenticatedFileSrc } from '../../api/useAuthenticatedFileSrc'
import styles from './FileViewerModal.module.css'

const MotionDialog = motion.create(Dialog)

interface FileViewerModalProps {
  isOpen: boolean
  onOpenChange: (open: boolean) => void
  title: string
  /** Ruta relativa (ej. `/documents/{id}/content`) — se resuelve con fetch
      autenticado (useAuthenticatedFileSrc), nunca un `<img src>`/`<iframe
      src>` directo, porque el endpoint exige el mismo Bearer JWT que
      cualquier otro (mismo motivo que visionboard/visionBoardImages.ts).
      `undefined` mientras el modal está cerrado — evita un fetch de más
      antes de que el usuario realmente pida ver el archivo. */
  contentPath: string | undefined
  contentType: string
}

/**
 * Visor genérico de imagen y/o PDF (pedido explícito del usuario para
 * Garantías, reutilizado también por Documentos — "que muestre un
 * visualizador de imágenes y/o pdf solamente"). Los navegadores ya saben
 * renderizar un PDF dentro de un `<iframe>` apuntando a un blob URL
 * `application/pdf` (soporte nativo, ningún visor de PDF de terceros
 * necesario) — mismo principio "usar capacidades nativas del navegador
 * antes que una librería nueva" que visionBoardExport.ts ya siguió para su
 * propio export de PDF.
 */
export function FileViewerModal({ isOpen, onOpenChange, title, contentPath, contentType }: FileViewerModalProps) {
  const { src, loading, error } = useAuthenticatedFileSrc(contentPath)
  const isImage = contentType.startsWith('image/')
  const isPdf = contentType === 'application/pdf'

  return (
    <Modal isOpen={isOpen} onOpenChange={onOpenChange} isDismissable className={styles.overlay}>
      <MotionDialog
        className={styles.panel}
        initial={{ opacity: 0, scale: 0.97 }}
        animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.97 }}
        transition={motionTokens.smooth}
      >
        {({ close }) => (
          <>
            <div className={styles.headerRow}>
              <Heading slot="title" className={styles.heading}>
                {title}
              </Heading>
              <button type="button" className={styles.closeButton} onClick={close} aria-label="Cerrar">
                ×
              </button>
            </div>
            <div className={styles.body}>
              {loading && (
                <div className={styles.stateMessage} role="status">
                  <Loader2 width={28} height={28} className={styles.spinner} aria-hidden="true" />
                  <span>Cargando…</span>
                </div>
              )}
              {!loading && error && (
                <div className={styles.stateMessage} role="alert">
                  <AlertTriangle width={28} height={28} aria-hidden="true" />
                  <span>No se pudo cargar el archivo.</span>
                </div>
              )}
              {!loading && !error && src && isImage && <img src={src} alt={title} className={styles.image} />}
              {!loading && !error && src && isPdf && <iframe src={src} title={title} className={styles.pdfFrame} />}
              {!loading && !error && src && !isImage && !isPdf && (
                <div className={styles.stateMessage}>
                  <span>Este tipo de archivo no se puede previsualizar aquí.</span>
                </div>
              )}
            </div>
          </>
        )}
      </MotionDialog>
    </Modal>
  )
}
