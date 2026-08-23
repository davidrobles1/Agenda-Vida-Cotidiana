package com.vidacotidiana.commitment.domain;

/** ADR-016/FR-025. OPEN until resolved (UC-20); no "in progress" state — kept minimal, no product reason to add one yet. */
public enum CommitmentStatus {
    OPEN,
    DONE
}
