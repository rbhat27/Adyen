package com.adyen.workshop.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory store for recurring payment tokens.
 * 
 * IMPORTANT: This is a simple implementation for demonstration purposes only.
 * In production, this MUST be replaced with:
 * - A secure database (e.g., PostgreSQL, MySQL)
 * - Encryption at rest for stored tokens
 * - Proper access controls and audit logging
 * - Support for distributed/clustered deployments
 * 
 * The current implementation has these limitations:
 * - Tokens are lost on application restart
 * - Not suitable for multi-instance deployments
 * - No encryption or access controls
 * - No audit trail
 */
@Service
public class RecurringTokenStore {
    private final Logger log = LoggerFactory.getLogger(RecurringTokenStore.class);
    
    // Map of shopperReference -> recurringDetailReference
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();
    
    /**
     * Store a recurring token for a shopper
     * @param shopperReference The unique reference for the shopper
     * @param recurringDetailReference The recurring token to store
     */
    public void storeToken(String shopperReference, String recurringDetailReference) {
        log.info("Storing recurring token for shopper: {}", shopperReference);
        tokenStore.put(shopperReference, recurringDetailReference);
    }
    
    /**
     * Get the recurring token for a shopper
     * @param shopperReference The unique reference for the shopper
     * @return The recurring token or null if not found
     */
    public String getToken(String shopperReference) {
        return tokenStore.get(shopperReference);
    }
    
    /**
     * Delete the recurring token for a shopper
     * @param shopperReference The unique reference for the shopper
     * @return true if the token was deleted, false if not found
     */
    public boolean deleteToken(String shopperReference) {
        log.info("Deleting recurring token for shopper: {}", shopperReference);
        return tokenStore.remove(shopperReference) != null;
    }
    
    /**
     * Check if a token exists for a shopper
     * @param shopperReference The unique reference for the shopper
     * @return true if a token exists, false otherwise
     */
    public boolean hasToken(String shopperReference) {
        return tokenStore.containsKey(shopperReference);
    }
}
