package com.vidacotidiana.shared.domain;

/**
 * Resource does not exist, or exists but is not accessible to the caller.
 * By design these two cases are never distinguished in the response, to
 * avoid leaking existence of a resource the caller has no access to
 * (see the NotFound response description in Documentacion/openapi/openapi.yaml
 * and the non-enumeration principle in SEC-001, applied consistently here to
 * AC-004: "otro usuario recibe 404 ... sin revelar existencia").
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
