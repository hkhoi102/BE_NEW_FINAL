package com.smartretail.servicecustomer.dto;

import jakarta.validation.constraints.*;

public class CustomerDtos {
    public static class CreateRequest {
        @NotNull public Long userId;
        @NotBlank public String name;
        @NotBlank public String phone;
        @Email @NotBlank public String email;
    }

    public static class UpdateRequest {
        @NotBlank public String name;
        @NotBlank public String phone;
        @Email @NotBlank public String email;
        public String address;
    }

    public static class ProvisionRequest {
        @NotNull public Long userId;
        @NotBlank public String name;
        @Email @NotBlank public String email;
        @NotBlank public String phone;
    }

    public static class CustomerInfo {
        public Long id;
        public Long userId;
        public String name;
        public String phone;
        public String email;
        public String address;
        public Integer points;
    }
}


