package com.adyen.workshop.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory store for recurring payment tokens.
 * In production, this should be replaced with a secure database.
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
