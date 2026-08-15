package com.vidacotidiana;

import com.atlassian.oai.validator.OpenApiInteractionValidator;

/**
 * TEST-API-001: validates MockMvc request/response pairs against the real
 * Documentacion/openapi/openapi.yaml (never a copy) — the working directory
 * during `mvn test` is the backend module root, so the spec is one level up.
 * The validator is built once and shared: it parses the spec on
 * construction, and the spec doesn't change mid-suite.
 */
public final class OpenApiContractSupport {

    private static final String OPENAPI_SPEC_PATH = "../Documentacion/openapi/openapi.yaml";

    public static final OpenApiInteractionValidator VALIDATOR =
            OpenApiInteractionValidator.createFor(OPENAPI_SPEC_PATH).build();

    private OpenApiContractSupport() {
    }
}
