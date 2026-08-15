package com.vidacotidiana.shared.domain;

/**
 * 410: the resource existed but is no longer actionable (AC-008/AC-009/
 * AC-017 — an invitation that is no longer PENDING, whether resolved or
 * expired). Distinct from NotFoundException: the caller was authorized to
 * see this resource, it just can't be acted on anymore.
 */
public class GoneException extends DomainException {

    public GoneException(String code, String message) {
        super(code, message);
    }
}
