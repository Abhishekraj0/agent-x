package com.abhishekraj0.api.security;

/**
 * Represents permission to execute an action.
 */
public interface Permission {
    boolean isAllowed();
}
