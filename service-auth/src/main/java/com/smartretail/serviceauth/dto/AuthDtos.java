package com.smartretail.serviceauth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {
	public static class RegisterRequest {
		@NotBlank
		private String fullName;
		@NotBlank
		@Email
		private String email;
		@NotBlank
		@Size(min = 6)
		private String password;
		@NotBlank
		@Pattern(regexp = "^[0-9]{8,15}$", message = "Phone must be digits 8-15 length")
		private String phoneNumber;

		public String getFullName() { return fullName; }
		public void setFullName(String fullName) { this.fullName = fullName; }
		public String getEmail() { return email; }
		public void setEmail(String email) { this.email = email; }
		public String getPassword() { return password; }
		public void setPassword(String password) { this.password = password; }
		public String getPhoneNumber() { return phoneNumber; }
		public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
	}

	public static class LoginRequest {
		@NotBlank
		@Email
		private String email;
		@NotBlank
		private String password;

		public String getEmail() { return email; }
		public void setEmail(String email) { this.email = email; }
		public String getPassword() { return password; }
		public void setPassword(String password) { this.password = password; }
	}

	public static class RefreshRequest {
		@NotBlank
		private String refreshToken;

		public String getRefreshToken() { return refreshToken; }
		public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
	}

	public static class LogoutRequest {
		@NotBlank
		private String refreshToken;

		public String getRefreshToken() { return refreshToken; }
		public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
	}

	public static class AuthResponse {
		private String accessToken;
		private String tokenType = "Bearer";
		private String refreshToken;

		public AuthResponse() {}
		public AuthResponse(String accessToken) { this.accessToken = accessToken; }
		public AuthResponse(String accessToken, String refreshToken) { this.accessToken = accessToken; this.refreshToken = refreshToken; }
		public String getAccessToken() { return accessToken; }
		public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
		public String getTokenType() { return tokenType; }
		public void setTokenType(String tokenType) { this.tokenType = tokenType; }
		public String getRefreshToken() { return refreshToken; }
		public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
	}
}
