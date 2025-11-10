package com.smartretail.serviceauth.controller;

import com.smartretail.serviceauth.dto.AuthDtos;
import com.smartretail.serviceauth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/health")
	public String health() {
		return "Auth Service is running!";
	}

	@GetMapping("/test")
	public String test() {
		return "Auth Service test endpoint - " + System.currentTimeMillis();
	}

	@GetMapping("/info")
	public String info() {
		return "Auth Service - Port: 8081, Status: UP";
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
		try {
			System.out.println("🔍 AuthController.register called with: " + request);
			AuthDtos.AuthResponse response = authService.register(request);
			System.out.println("🔍 AuthController.register response: " + response);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			// Handle registration errors - return 200 with error message
			System.out.println("🔍 AuthController.register IllegalArgumentException: " + e.getMessage());
			return ResponseEntity.ok(new ErrorResponse(e.getMessage()));
		} catch (Exception e) {
			// Handle other errors - return 200 with error message
			System.out.println("🔍 AuthController.register Exception: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.ok(new ErrorResponse("Lỗi server, vui lòng thử lại sau"));
		}
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
		try {
			AuthDtos.AuthResponse response = authService.login(request);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			// Handle authentication errors - return 200 with error message
			return ResponseEntity.ok(new ErrorResponse("Email hoặc mật khẩu không đúng"));
		} catch (Exception e) {
			// Handle other errors - return 200 with error message
			return ResponseEntity.ok(new ErrorResponse("Lỗi server, vui lòng thử lại sau"));
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody AuthDtos.LogoutRequest request) {
		authService.logout(request);
		return ResponseEntity.ok().build();
	}

	// Error response class
	public static class ErrorResponse {
		private String message;
		private long timestamp;

		public ErrorResponse(String message) {
			this.message = message;
			this.timestamp = System.currentTimeMillis();
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
	}
}
