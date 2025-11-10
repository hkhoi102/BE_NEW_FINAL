package com.smartretail.paymentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SepayConfig {

	@Value("${sepay.api-url:https://api.sepay.vn}")
	private String apiUrl;

	// Base URL for user APIs (e.g., transactions list)
	@Value("${sepay.userapi-url:https://my.sepay.vn/userapi}")
	private String userApiUrl;

	@Value("${sepay.api-key:}")
	private String apiKey;

	@Value("${sepay.secret:}")
	private String webhookSecret;

	@Value("${sepay.webhook-verify:false}")
	private boolean webhookVerify;

	@Value("${sepay.account-number:035011027000-4}")
	private String accountNumber;

	@Value("${sepay.account-name:SMART RETAIL}")
	private String accountName;

	@Value("${sepay.bank-code:MBBank}")
	private String defaultBankCode;

	@Value("${sepay.qr-base-url:https://gr.sepay.vn/img}")
	private String qrBaseUrl;

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public String getApiUrl() {
		return apiUrl;
	}

	public String getUserApiUrl() {
		return userApiUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getWebhookSecret() {
		return webhookSecret;
	}

	public boolean isWebhookVerify() {
		return webhookVerify;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public String getAccountName() {
		return accountName;
	}

	public String getDefaultBankCode() {
		return defaultBankCode;
	}

	public String getQrBaseUrl() {
		return qrBaseUrl;
	}
}


