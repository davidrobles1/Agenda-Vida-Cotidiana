import type { ReactNode } from 'react'
import styles from './ListSectionCard.module.css'

interface ListSectionCardProps {
  title: string
  onSeeAll?: () => void
  action?: ReactNode
  children: ReactNode
}

/** UX-006: "card with header + Ver todas" pattern, reused across Home and every mock module list. */
export function ListSectionCard({ title, onSeeAll, action, children }: ListSectionCardProps) {
  return (
    <section className={styles.card}>
      <div className={styles.header}>
        <h2 className={styles.title}>{title}</h2>
        {action ?? (onSeeAll && (
          <button type="button" className={styles.seeAll} onClick={onSeeAll}>
            Ver todas
          </button>
        ))}
      </div>
      <div className={styles.body}>{children}</div>
    </section>
  )
}
