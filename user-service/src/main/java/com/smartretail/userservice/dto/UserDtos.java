package com.smartretail.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserDtos {
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

    public static class VerifyRequest {
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

    public static class ActivateRequest {
        @NotBlank
        @Email
        private String email;
        @NotBlank
        private String otp;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class ResendOtpRequest {
        @NotBlank
        @Email
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String role;
        private String phoneNumber;
        private boolean active;
        private Long defaultStockLocationId;
        private Long defaultWarehouseId;

        public UserInfo() {}
        public UserInfo(Long id, String fullName, String email, String role, String phoneNumber, boolean active) {
            this.id = id; this.fullName = fullName; this.email = email; this.role = role; this.phoneNumber = phoneNumber; this.active = active;
        }
        public UserInfo(Long id, String fullName, String email, String role, String phoneNumber, boolean active, Long defaultStockLocationId, Long defaultWarehouseId) {
            this.id = id; this.fullName = fullName; this.email = email; this.role = role; this.phoneNumber = phoneNumber; this.active = active; this.defaultStockLocationId = defaultStockLocationId; this.defaultWarehouseId = defaultWarehouseId;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Long getDefaultStockLocationId() { return defaultStockLocationId; }
        public void setDefaultStockLocationId(Long defaultStockLocationId) { this.defaultStockLocationId = defaultStockLocationId; }
        public Long getDefaultWarehouseId() { return defaultWarehouseId; }
        public void setDefaultWarehouseId(Long defaultWarehouseId) { this.defaultWarehouseId = defaultWarehouseId; }
    }
}
