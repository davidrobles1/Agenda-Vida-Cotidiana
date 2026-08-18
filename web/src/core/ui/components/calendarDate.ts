/** UX-010: shared by CalendarView.tsx/CalendarPage.tsx — split out of
    CalendarView.tsx because co-exporting a plain function from a component
    file breaks React Fast Refresh (real oxlint warning, not just noise). */
export function dateKey(year: number, month: number, day: number): string {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}
