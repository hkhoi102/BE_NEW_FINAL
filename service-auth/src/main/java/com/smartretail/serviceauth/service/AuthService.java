package com.smartretail.serviceauth.service;

import com.smartretail.serviceauth.client.UserServiceClient;
import com.smartretail.serviceauth.client.dto.UserDtos;
import com.smartretail.serviceauth.dto.AuthDtos;
import com.smartretail.serviceauth.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
	private final UserServiceClient userServiceClient;
	private final JwtTokenProvider jwtTokenProvider;
	private final TokenStore tokenStore;

	public AuthService(UserServiceClient userServiceClient, JwtTokenProvider jwtTokenProvider, TokenStore tokenStore) {
		this.userServiceClient = userServiceClient;
		this.jwtTokenProvider = jwtTokenProvider;
		this.tokenStore = tokenStore;
	}

	@Transactional
	public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
		UserDtos.UserInfo user = userServiceClient.register(request);
		Map<String, Object> claims = buildClaims(user);
		String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), claims);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
		tokenStore.store(refreshToken, user.getEmail(), Instant.now().plusMillis(1209600000));
		return new AuthDtos.AuthResponse(accessToken, refreshToken);
	}

	@Transactional(readOnly = true)
	public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
		UserDtos.UserInfo user = userServiceClient.verify(request);
		Map<String, Object> claims = buildClaims(user);
		String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), claims);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
		tokenStore.store(refreshToken, user.getEmail(), Instant.now().plusMillis(1209600000));
		return new AuthDtos.AuthResponse(accessToken, refreshToken);
	}

	@Transactional(readOnly = true)
	public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
		String refreshToken = request.getRefreshToken();
		var claimsJws = jwtTokenProvider.validateAndParse(refreshToken);
		String email = claimsJws.getBody().getSubject();
		if (!tokenStore.isValid(refreshToken, email)) {
			throw new IllegalArgumentException("Invalid refresh token");
		}
		Map<String, Object> claims = new HashMap<>();
		claims.put("tokenRefreshed", true);
		String accessToken = jwtTokenProvider.generateAccessToken(email, claims);
		return new AuthDtos.AuthResponse(accessToken, refreshToken);
	}

	@Transactional
	public void logout(AuthDtos.LogoutRequest request) {
		tokenStore.revoke(request.getRefreshToken());
	}

	private Map<String, Object> buildClaims(UserDtos.UserInfo user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("role", user.getRole());
		claims.put("uid", user.getId());
		claims.put("name", user.getFullName());
		return claims;
	}
}
