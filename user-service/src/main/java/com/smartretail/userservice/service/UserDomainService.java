package com.smartretail.userservice.service;

import com.smartretail.userservice.dto.UserDtos;
import com.smartretail.userservice.dto.UserManagementDtos;
import com.smartretail.userservice.model.User;
import com.smartretail.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

@Service
public class UserDomainService {
	private static final Logger log = LoggerFactory.getLogger(UserDomainService.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final SecureRandom random = new SecureRandom();
	private final EmailService emailService;
	private final RestTemplate restTemplate;

	public UserDomainService(UserRepository userRepository, EmailService emailService) {
		this.userRepository = userRepository;
		this.emailService = emailService;
		this.restTemplate = new RestTemplate();
	}

	// ==== Profile (me) ====
	@Transactional(readOnly = true)
	public UserDtos.UserInfo getMe(String email) {
		User user = userRepository.findByEmail(email).orElseThrow();
		return toInfo(user);
	}

	@Transactional
	public UserDtos.UserInfo updateMe(String email, UserManagementDtos.UpdateProfileRequest req) {
		User current = userRepository.findByEmail(email).orElseThrow();
		Long currentId = current.getId();
		// unique phone check for others
		userRepository.findAll().stream()
				.filter(u -> !u.getId().equals(currentId))
				.filter(u -> req.getPhoneNumber().equals(u.getPhoneNumber()))
				.findAny().ifPresent(u -> { throw new IllegalArgumentException("Phone already exists"); });
		current.setFullName(req.getFullName());
		current.setPhoneNumber(req.getPhoneNumber());
		User saved = userRepository.save(current);
		return toInfo(saved);
	}

	@Transactional
	public void changePassword(String email, UserManagementDtos.ChangePasswordRequest req) {
		User user = userRepository.findByEmail(email).orElseThrow();
		if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
			throw new IllegalArgumentException("Current password incorrect");
		}
		user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
		userRepository.save(user);
	}

	// ==== Admin ====
	@Transactional(readOnly = true)
	public Page<UserDtos.UserInfo> listUsers(String q, Pageable pageable) {
		Page<User> page = (q == null || q.isBlank())
				? userRepository.findAll(pageable)
				: userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(q, q, pageable);
		return page.map(this::toInfo);
	}

	@Transactional(readOnly = true)
	public List<UserDtos.UserInfo> listUsersByRole(String role) {
		List<User> users = userRepository.findByRole(role);
		return users.stream().map(this::toInfo).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public UserDtos.UserInfo getUser(Long id) {
		return toInfo(userRepository.findById(id).orElseThrow());
	}

	@Transactional(readOnly = true)
	public UserDtos.UserInfo getManagerById(Long id) {
		User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
		if (!"MANAGER".equalsIgnoreCase(user.getRole())) {
			throw new IllegalArgumentException("User is not a MANAGER");
		}
		return toInfo(user);
	}

	@Transactional
	public UserDtos.UserInfo adminCreate(UserManagementDtos.AdminCreateUserRequest req) {
		if (userRepository.existsByEmail(req.getEmail())) throw new IllegalArgumentException("Email exists");
		userRepository.findAll().stream().filter(u -> req.getPhoneNumber().equals(u.getPhoneNumber())).findAny()
				.ifPresent(u -> { throw new IllegalArgumentException("Phone exists"); });

		// Validate required fields for MANAGER and ADMIN
		if ("MANAGER".equalsIgnoreCase(req.getRole()) || "ADMIN".equalsIgnoreCase(req.getRole())) {
			if (req.getDefaultStockLocationId() == null) {
				throw new IllegalArgumentException("defaultStockLocationId is required for MANAGER and ADMIN roles");
			}
			if (req.getDefaultWarehouseId() == null) {
				throw new IllegalArgumentException("defaultWarehouseId is required for MANAGER and ADMIN roles");
			}
		}

		User user = new User();
		user.setFullName(req.getFullName());
		user.setEmail(req.getEmail());
		user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
		user.setPhoneNumber(req.getPhoneNumber());
		user.setRole(req.getRole());
		user.setActive(true);
		user.setDefaultStockLocationId(req.getDefaultStockLocationId());
		user.setDefaultWarehouseId(req.getDefaultWarehouseId());
		user = userRepository.save(user);
		// provision customer (best-effort) - same as register method
		try { provisionCustomer(user); } catch (Exception ex) { log.warn("Provision customer failed: {}", ex.getMessage()); }
		return toInfo(user);
	}

	@Transactional
	public UserDtos.UserInfo adminUpdate(Long id, UserManagementDtos.AdminUpdateUserRequest req) {
		User user = userRepository.findById(id).orElseThrow();
		if (!user.getEmail().equals(req.getEmail()) && userRepository.existsByEmail(req.getEmail())) {
			throw new IllegalArgumentException("Email exists");
		}
		userRepository.findAll().stream()
				.filter(u -> !u.getId().equals(id))
				.filter(u -> req.getPhoneNumber().equals(u.getPhoneNumber()))
				.findAny().ifPresent(u -> { throw new IllegalArgumentException("Phone exists"); });

		// Validate required fields for MANAGER and ADMIN
		if ("MANAGER".equalsIgnoreCase(req.getRole()) || "ADMIN".equalsIgnoreCase(req.getRole())) {
			if (req.getDefaultStockLocationId() == null) {
				throw new IllegalArgumentException("defaultStockLocationId is required for MANAGER and ADMIN roles");
			}
			if (req.getDefaultWarehouseId() == null) {
				throw new IllegalArgumentException("defaultWarehouseId is required for MANAGER and ADMIN roles");
			}
		}

		user.setFullName(req.getFullName());
		user.setEmail(req.getEmail());
		user.setPhoneNumber(req.getPhoneNumber());
		user.setRole(req.getRole());
		user.setDefaultStockLocationId(req.getDefaultStockLocationId());
		user.setDefaultWarehouseId(req.getDefaultWarehouseId());
		user = userRepository.save(user);
		return toInfo(user);
	}

	@Transactional
	public void adminDelete(Long id) {
		User user = userRepository.findById(id).orElseThrow();
		user.setActive(false);
		userRepository.save(user);
	}

	@Transactional
	public void updateStatus(Long id, UserManagementDtos.UpdateStatusRequest req) {
		User user = userRepository.findById(id).orElseThrow();
		user.setActive(req.isActive());
		userRepository.save(user);
	}

	@Transactional
	public void updateRole(Long id, UserManagementDtos.UpdateRoleRequest req) {
		User user = userRepository.findById(id).orElseThrow();
		user.setRole(req.getRole());
		userRepository.save(user);
	}

	// ==== Forgot / Reset password ====
	@Transactional
	public void forgotPassword(UserManagementDtos.ForgotPasswordRequest req) {
		User user = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> new IllegalArgumentException("Email not found"));
		issueOtp(user);
		userRepository.save(user);
		emailService.sendOtpEmail(user.getEmail(), user.getOtp());
	}

	@Transactional
	public void resetPassword(UserManagementDtos.ResetPasswordRequest req) {
		User user = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> new IllegalArgumentException("Email not found"));
		if (user.getOtp() == null || user.getOtpExpiresAt() == null || Instant.now().isAfter(user.getOtpExpiresAt())) {
			throw new IllegalArgumentException("OTP expired");
		}
		if (!req.getOtp().equals(user.getOtp())) {
			throw new IllegalArgumentException("Invalid OTP");
		}
		user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
		user.setOtp(null);
		user.setOtpExpiresAt(null);
		userRepository.save(user);
	}

	// ==== existing methods for register/verify/activate/resendOtp remain below ====
	@Transactional
	public UserDtos.UserInfo register(UserDtos.RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new IllegalArgumentException("Email already exists");
		}
		// basic unique phone check
		userRepository.findAll().stream().filter(u -> request.getPhoneNumber().equals(u.getPhoneNumber())).findAny()
				.ifPresent(u -> { throw new IllegalArgumentException("Phone already exists"); });

		User user = new User();
		user.setFullName(request.getFullName());
		user.setEmail(request.getEmail());
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setPhoneNumber(request.getPhoneNumber());
		user.setActive(false);
		issueOtp(user);
		user = userRepository.save(user);
		// send email OTP
		emailService.sendOtpEmail(user.getEmail(), user.getOtp());
		// provision customer (best-effort)
		try { provisionCustomer(user); } catch (Exception ex) { log.warn("Provision customer failed: {}", ex.getMessage()); }
		return toInfo(user);
	}

	@Transactional(readOnly = true)
	public UserDtos.UserInfo verify(UserDtos.VerifyRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new IllegalArgumentException("Invalid credentials");
		}
		if (!user.isActive()) {
			throw new IllegalArgumentException("Account not activated");
		}
		return toInfo(user);
	}

	@Transactional
	public void activate(UserDtos.ActivateRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new IllegalArgumentException("Email not found"));
		if (user.getOtp() == null || user.getOtpExpiresAt() == null || Instant.now().isAfter(user.getOtpExpiresAt())) {
			throw new IllegalArgumentException("OTP expired");
		}
		if (!request.getOtp().equals(user.getOtp())) {
			throw new IllegalArgumentException("Invalid OTP");
		}
		user.setActive(true);
		user.setOtp(null);
		user.setOtpExpiresAt(null);
		userRepository.save(user);
	}

	@Transactional
	public void resendOtp(UserDtos.ResendOtpRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new IllegalArgumentException("Email not found"));
		if (user.isActive()) {
			throw new IllegalArgumentException("Account already active");
		}
		issueOtp(user);
		userRepository.save(user);
		emailService.sendOtpEmail(user.getEmail(), user.getOtp());
	}

	private void issueOtp(User user) {
		String otp = String.format("%06d", random.nextInt(1_000_000));
		user.setOtp(otp);
		user.setOtpExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
		log.info("[DEV] OTP for {} is {} (expires at {})", user.getEmail(), otp, user.getOtpExpiresAt());
	}

	private UserDtos.UserInfo toInfo(User user) {
		return new UserDtos.UserInfo(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getPhoneNumber(), user.isActive(), user.getDefaultStockLocationId(), user.getDefaultWarehouseId());
	}

	@org.springframework.beans.factory.annotation.Value("${app.api-gateway.url}")
	private String apiGatewayUrl;
	@org.springframework.beans.factory.annotation.Value("${app.customer-service.path}")
	private String customerProvisionPath;

	private void provisionCustomer(User user) {
		java.util.Map<String, Object> body = new java.util.HashMap<>();
		body.put("userId", user.getId());
		body.put("name", user.getFullName());
		body.put("email", user.getEmail());
		body.put("phone", user.getPhoneNumber());
		String url = apiGatewayUrl + customerProvisionPath;
		restTemplate.postForEntity(url, body, Void.class);
	}
}
