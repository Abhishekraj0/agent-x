package com.abhishekraj0.api.tool;

import java.io.Serializable;

/**
 * Resolution decision when human or operator resolves an UNKNOWN_RESULT state.
 */
public record UnknownResultResolution(
        ResolutionType type,
        String output,
        String reason
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ResolutionType {
        CONFIRMED_SUCCESS,
        CONFIRMED_FAILURE,
        RETRY_AUTHORIZED
    }

    public static UnknownResultResolution confirmedSuccess(String output, String reason) {
        return new UnknownResultResolution(ResolutionType.CONFIRMED_SUCCESS, output, reason);
    }

    public static UnknownResultResolution confirmedFailure(String reason) {
        return new UnknownResultResolution(ResolutionType.CONFIRMED_FAILURE, null, reason);
    }

    public static UnknownResultResolution retryAuthorized(String reason) {
        return new UnknownResultResolution(ResolutionType.RETRY_AUTHORIZED, null, reason);
    }
}
