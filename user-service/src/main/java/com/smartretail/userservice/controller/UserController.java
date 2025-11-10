package com.smartretail.userservice.controller;

import com.smartretail.userservice.dto.UserDtos;
import com.smartretail.userservice.dto.UserManagementDtos;
import com.smartretail.userservice.service.UserDomainService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserDomainService userDomainService;

	public UserController(UserDomainService userDomainService) {
		this.userDomainService = userDomainService;
	}

	@GetMapping("/health")
	public String health() {
		return "User Service is running!";
	}

	@GetMapping("/test")
	public String test() {
		return "User Service test endpoint - " + System.currentTimeMillis();
	}

	@GetMapping("/info")
	public String info() {
		return "User Service - Port: 8082, Status: UP";
	}

	// ===== Profile =====
	@GetMapping("/me")
	public ResponseEntity<UserDtos.UserInfo> me(Authentication auth) {
		return ResponseEntity.ok(userDomainService.getMe(auth.getName()));
	}

	@PutMapping("/me")
	public ResponseEntity<UserDtos.UserInfo> updateMe(Authentication auth, @Valid @RequestBody UserManagementDtos.UpdateProfileRequest request) {
		return ResponseEntity.ok(userDomainService.updateMe(auth.getName(), request));
	}

	@PutMapping("/me/change-password")
	public ResponseEntity<Void> changePassword(Authentication auth, @Valid @RequestBody UserManagementDtos.ChangePasswordRequest request) {
		userDomainService.changePassword(auth.getName(), request);
		return ResponseEntity.ok().build();
	}

	// ===== Admin management =====
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
	public ResponseEntity<Page<UserDtos.UserInfo>> list(@RequestParam(required = false) String q, Pageable pageable) {
		return ResponseEntity.ok(userDomainService.listUsers(q, pageable));
	}

	@GetMapping("/role/{role}")
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
	public ResponseEntity<List<UserDtos.UserInfo>> listByRole(@PathVariable String role) {
		return ResponseEntity.ok(userDomainService.listUsersByRole(role));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
	public ResponseEntity<UserDtos.UserInfo> get(@PathVariable Long id) {
		return ResponseEntity.ok(userDomainService.getUser(id));
	}

	@GetMapping("/manager/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
	public ResponseEntity<UserDtos.UserInfo> getManager(@PathVariable Long id) {
		return ResponseEntity.ok(userDomainService.getManagerById(id));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
	public ResponseEntity<UserDtos.UserInfo> create(@Valid @RequestBody UserManagementDtos.AdminCreateUserRequest request) {
		return ResponseEntity.ok(userDomainService.adminCreate(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
	public ResponseEntity<UserDtos.UserInfo> update(@PathVariable Long id, @Valid @RequestBody UserManagementDtos.AdminUpdateUserRequest request) {
		return ResponseEntity.ok(userDomainService.adminUpdate(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		userDomainService.adminDelete(id);
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
	public ResponseEntity<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserManagementDtos.UpdateStatusRequest request) {
		userDomainService.updateStatus(id, request);
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/{id}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> updateRole(@PathVariable Long id, @Valid @RequestBody UserManagementDtos.UpdateRoleRequest request) {
		userDomainService.updateRole(id, request);
		return ResponseEntity.ok().build();
	}

	// ===== Forgot / Reset password =====
	@PostMapping("/forgot-password")
	public ResponseEntity<Void> forgotPassword(@Valid @RequestBody UserManagementDtos.ForgotPasswordRequest request) {
		userDomainService.forgotPassword(request);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Void> resetPassword(@Valid @RequestBody UserManagementDtos.ResetPasswordRequest request) {
		userDomainService.resetPassword(request);
		return ResponseEntity.ok().build();
	}

	// ===== Existing endpoints =====
	@PostMapping("/register")
	public ResponseEntity<UserDtos.UserInfo> register(@Valid @RequestBody UserDtos.RegisterRequest request) {
		return ResponseEntity.ok(userDomainService.register(request));
	}

	@PostMapping("/verify")
	public ResponseEntity<UserDtos.UserInfo> verify(@Valid @RequestBody UserDtos.VerifyRequest request) {
		return ResponseEntity.ok(userDomainService.verify(request));
	}

	@PostMapping("/activate")
	public ResponseEntity<Void> activate(@Valid @RequestBody UserDtos.ActivateRequest request) {
		userDomainService.activate(request);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/resend-otp")
	public ResponseEntity<Void> resendOtp(@Valid @RequestBody UserDtos.ResendOtpRequest request) {
		userDomainService.resendOtp(request);
		return ResponseEntity.ok().build();
	}
}
