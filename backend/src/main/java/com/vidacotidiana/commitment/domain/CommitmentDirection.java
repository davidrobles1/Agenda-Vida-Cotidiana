package com.vidacotidiana.commitment.domain;

/**
 * ADR-016: unifies "Seguimiento" (MINE) and "Esperando" (THEIRS) into a
 * single entity distinguished by direction, instead of two tables — see
 * ADR-016 "Alternativas" for why (a compromise frequently flips direction;
 * two tables would require migrating rows every time that happens).
 */
public enum CommitmentDirection {
    MINE,
    THEIRS
}
