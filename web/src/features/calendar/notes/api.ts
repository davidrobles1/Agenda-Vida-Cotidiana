import { apiFetch } from '../../../core/api/httpClient'
import type { Note } from './notesData'

/** BE-0xx/WEB-0xx. Mirrors web/src/features/warranties/api.ts's shape. */
interface NotesPage {
  items: Note[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface NoteInput {
  title: string
  description?: string
  iconId?: string
  stickerId?: string
  /** ADR-016 Fase 3a/FR-029 (candidato V4). */
  personId?: string
  projectId?: string
}

export async function listNotes(): Promise<NotesPage> {
  const response = await apiFetch('/notes')
  if (!response.ok) throw new Error(`GET /notes failed: ${response.status}`)
  return response.json()
}

export async function createNote(input: NoteInput): Promise<Note> {
  const response = await apiFetch('/notes', {
    method: 'POST',
    body: JSON.stringify(input),
  })
  if (!response.ok) throw new Error(`POST /notes failed: ${response.status}`)
  return response.json()
}

/**
 * `iconId`/`stickerId` use always-overwrite PATCH semantics on the backend
 * (see Note.java's applyEdit) — omitting them here clears them server-side,
 * which is correct since the caller (useNotes.ts) always resubmits the
 * note's complete current selection.
 */
export async function updateNote(
  id: string,
  input: NoteInput & { version: number },
): Promise<Note> {
  const response = await apiFetch(`/notes/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(input),
  })
  if (!response.ok) throw new Error(`PATCH /notes/${id} failed: ${response.status}`)
  return response.json()
}

export async function deleteNote(id: string): Promise<void> {
  const response = await apiFetch(`/notes/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /notes/${id} failed: ${response.status}`)
}
