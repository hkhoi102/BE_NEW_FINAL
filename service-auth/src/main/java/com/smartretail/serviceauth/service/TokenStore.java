package com.smartretail.serviceauth.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
	public static class RefreshEntry {
		public final String email;
		public final Instant expiresAt;
		public RefreshEntry(String email, Instant expiresAt) { this.email = email; this.expiresAt = expiresAt; }
	}

	private final Map<String, RefreshEntry> refreshTokens = new ConcurrentHashMap<>();

	public void store(String refreshToken, String email, Instant expiresAt) {
		refreshTokens.put(refreshToken, new RefreshEntry(email, expiresAt));
	}

	public boolean isValid(String refreshToken, String email) {
		RefreshEntry entry = refreshTokens.get(refreshToken);
		return entry != null && entry.email.equals(email) && Instant.now().isBefore(entry.expiresAt);
	}

	public void revoke(String refreshToken) {
		refreshTokens.remove(refreshToken);
	}
}
