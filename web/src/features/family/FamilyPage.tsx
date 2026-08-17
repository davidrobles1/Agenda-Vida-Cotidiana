import { AppShell } from '../../core/ui/layout/AppShell'
import { familyMembers } from '../../core/mock/mockData'
import styles from './FamilyPage.module.css'

/** UX-006: scaffolding-only module — mock data (core/mock/mockData.ts), zero endpoints, zero logic. */
export function FamilyPage() {
  return (
    <AppShell title="Familia" subtitle="Las personas con las que compartes tu día a día.">
      <div className={styles.list}>
        {familyMembers.map((member) => (
          <div key={member.id} className={styles.card}>
            <span className={styles.avatar}>{member.name.charAt(0)}</span>
            <div className={styles.text}>
              <span className={styles.name}>{member.name}</span>
              <span className={styles.relationship}>{member.relationship}</span>
            </div>
          </div>
        ))}
      </div>
    </AppShell>
  )
}
