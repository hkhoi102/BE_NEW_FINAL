package com.smartretail.serviceauth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {
	private final Key key;
	private final long accessValidityInMs;
	private final long refreshValidityInMs;

	public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
						   @Value("${app.jwt.expiration}") long accessValidityInMs,
						   @Value("${app.jwt.refreshExpiration:1209600000}") long refreshValidityInMs) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes());
		this.accessValidityInMs = accessValidityInMs;
		this.refreshValidityInMs = refreshValidityInMs;
	}

	public String generateAccessToken(String subject, Map<String, Object> claims) {
		return buildToken(subject, claims, accessValidityInMs, "access");
	}

	public String generateRefreshToken(String subject) {
		return buildToken(subject, Map.of("typ", "refresh"), refreshValidityInMs, "refresh");
	}

	private String buildToken(String subject, Map<String, Object> claims, long validity, String type) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + validity);
		JwtBuilder builder = Jwts.builder()
				.setClaims(claims)
				.setSubject(subject)
				.setIssuedAt(now)
				.setExpiration(expiry)
				.signWith(key, SignatureAlgorithm.HS256);
		builder.claim("token_type", type);
		return builder.compact();
	}

	public Jws<Claims> validateAndParse(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token);
	}

	public String getSubject(String token) {
		return validateAndParse(token).getBody().getSubject();
	}
}
