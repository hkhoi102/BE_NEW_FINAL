package com.smartretail.serviceauth.client;

import com.smartretail.serviceauth.dto.AuthDtos;
import com.smartretail.serviceauth.client.dto.UserDtos;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceClient {
	private final RestTemplate restTemplate;
	private static final String BASE_URL = "http://user-service/api/users";

	public UserServiceClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public UserDtos.UserInfo register(AuthDtos.RegisterRequest request) {
		try {
			System.out.println("🔍 UserServiceClient.register called with: " + request);
			System.out.println("🔍 UserServiceClient.register calling: " + BASE_URL + "/register");
			ResponseEntity<UserDtos.UserInfo> response = restTemplate.postForEntity(BASE_URL + "/register", request, UserDtos.UserInfo.class);
			System.out.println("🔍 UserServiceClient.register response: " + response.getBody());
			return response.getBody();
		} catch (HttpClientErrorException e) {
			System.out.println("🔍 UserServiceClient.register HttpClientErrorException: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
			throw new IllegalArgumentException(e.getResponseBodyAsString());
		} catch (Exception e) {
			System.out.println("🔍 UserServiceClient.register Exception: " + e.getMessage());
			e.printStackTrace();
			throw new IllegalArgumentException("Không thể kết nối đến user-service: " + e.getMessage());
		}
	}

	public UserDtos.UserInfo verify(AuthDtos.LoginRequest request) {
		try {
			ResponseEntity<UserDtos.UserInfo> response = restTemplate.postForEntity(BASE_URL + "/verify", request, UserDtos.UserInfo.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			// Extract error message from response
			String errorMessage = "Email hoặc mật khẩu không đúng";
			try {
				// Try to parse error message from response body
				String responseBody = e.getResponseBodyAsString();
				if (responseBody != null && !responseBody.isEmpty()) {
					// You can add JSON parsing here if needed
					errorMessage = responseBody;
				}
			} catch (Exception parseException) {
				// Use default message if parsing fails
			}
			throw new IllegalArgumentException(errorMessage);
		}
	}
}
