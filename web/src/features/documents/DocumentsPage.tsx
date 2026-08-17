import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { IconFolder } from '../../core/ui/icons'
import { documentCategories, documents } from '../../core/mock/mockData'
import styles from './DocumentsPage.module.css'

/** UX-006: scaffolding-only module — mock data (core/mock/mockData.ts), zero endpoints, zero logic. */
export function DocumentsPage() {
  return (
    <AppShell title="Documentos" subtitle="Organiza tus documentos importantes.">
      <div className={styles.categoryGrid}>
        {documentCategories.map((category) => (
          <div key={category.id} className={styles.categoryCard}>
            <span className={styles.categoryIcon}>
              <IconFolder width={18} height={18} />
            </span>
            <span className={styles.categoryName}>{category.name}</span>
            <span className={styles.categoryCount}>{category.count} documentos</span>
          </div>
        ))}
      </div>

      <ListSectionCard title="Recientes">
        {documents.map((doc) => (
          <ListItemRow key={doc.id} title={doc.name} subtitle={`${doc.category} · ${doc.uploadedAt}`} icon={IconFolder} tone="info" />
        ))}
      </ListSectionCard>
    </AppShell>
  )
}
