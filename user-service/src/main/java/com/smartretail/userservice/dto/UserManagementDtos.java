package com.smartretail.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserManagementDtos {
    public static class UpdateProfileRequest {
        @NotBlank
        private String fullName;
        @NotBlank
        @Pattern(regexp = "^[0-9]{8,15}$")
        private String phoneNumber;
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank
        @Size(min = 6)
        private String newPassword;
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class AdminCreateUserRequest {
        @NotBlank private String fullName;
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 6) private String password;
        @NotBlank @Pattern(regexp = "^[0-9]{8,15}$") private String phoneNumber;
        @NotBlank private String role; // USER, ADMIN, MANAGER
        private Long defaultStockLocationId;
        private Long defaultWarehouseId;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Long getDefaultStockLocationId() { return defaultStockLocationId; }
        public void setDefaultStockLocationId(Long defaultStockLocationId) { this.defaultStockLocationId = defaultStockLocationId; }
        public Long getDefaultWarehouseId() { return defaultWarehouseId; }
        public void setDefaultWarehouseId(Long defaultWarehouseId) { this.defaultWarehouseId = defaultWarehouseId; }
    }

    public static class AdminUpdateUserRequest {
        @NotBlank private String fullName;
        @NotBlank @Email private String email;
        @NotBlank @Pattern(regexp = "^[0-9]{8,15}$") private String phoneNumber;
        @NotBlank private String role;
        private Long defaultStockLocationId;
        private Long defaultWarehouseId;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Long getDefaultStockLocationId() { return defaultStockLocationId; }
        public void setDefaultStockLocationId(Long defaultStockLocationId) { this.defaultStockLocationId = defaultStockLocationId; }
        public Long getDefaultWarehouseId() { return defaultWarehouseId; }
        public void setDefaultWarehouseId(Long defaultWarehouseId) { this.defaultWarehouseId = defaultWarehouseId; }
    }

    public static class UpdateStatusRequest {
        private boolean active;
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class UpdateRoleRequest {
        @NotBlank private String role;
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class ForgotPasswordRequest {
        @NotBlank @Email private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        @NotBlank @Email private String email;
        @NotBlank private String otp;
        @NotBlank @Size(min = 6) private String newPassword;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
