package com.vidacotidiana.shared.domain;

/** 429: caller exceeded a rate limit (DEVOPS-001 — invitation creation, SEC-001 abuse mitigation). */
public class RateLimitExceededException extends DomainException {

    public RateLimitExceededException(String message) {
        super("RATE_LIMIT_EXCEEDED", message);
    }
}
