package com.smartretail.paymentservice.service;

import com.smartretail.paymentservice.config.SepayConfig;
import com.smartretail.paymentservice.dto.PaymentDtos.CreatePaymentRequest;
import com.smartretail.paymentservice.dto.PaymentDtos.CreatePaymentResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
public class SepayClient {

	private final RestTemplate restTemplate;
	private final SepayConfig sepayConfig;

	public SepayClient(RestTemplate restTemplate, SepayConfig sepayConfig) {
		this.restTemplate = restTemplate;
		this.sepayConfig = sepayConfig;
	}

	public CreatePaymentResponse createBankTransferIntent(CreatePaymentRequest req) {
		// Generate unique content for transfer
		String referenceId = "SEPAY-REF-" + UUID.randomUUID();
		// Generate ORDER+code (letters/digits only), no orderId in content
		String uuidToken = UUID.randomUUID().toString().replace("-", "");
		String code = uuidToken.substring(uuidToken.length() - 8).toUpperCase();
		String transferContent = "ORDER" + code;

		CreatePaymentResponse resp = new CreatePaymentResponse();
		resp.referenceId = referenceId;
		resp.transferContent = transferContent;
		resp.accountNumber = sepayConfig.getAccountNumber();
		resp.accountName = sepayConfig.getAccountName();
		resp.bankCode = req.bankCode != null ? req.bankCode : sepayConfig.getDefaultBankCode();

		// Generate Sepay QR URL using config (URL-encoded description)
		String encodedDes = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
		String qrUrl = String.format(
			"%s?bank=%s&acc=%s&template=compact&amount=%s&des=%s",
			sepayConfig.getQrBaseUrl(),
			resp.bankCode,
			resp.accountNumber,
			normalize(req.amount),
			encodedDes
		);
		resp.qrContent = qrUrl;

		return resp;
	}

	public Map getLatestTransactions(String accountNumber, int limit, String apiKey) {
		String url = String.format(
			"%s/transactions/list?account_number=%s&limit=%d",
			sepayConfig.getUserApiUrl(), accountNumber, Math.max(1, Math.min(limit, 50))
		);

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		String effectiveKey = (apiKey != null && !apiKey.isBlank()) ? apiKey : sepayConfig.getApiKey();
		if (effectiveKey != null && !effectiveKey.isBlank()) {
			headers.set("Authorization", "Bearer " + effectiveKey);
		}
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
		return resp.getBody();
	}

	private String normalize(BigDecimal amount) {
		return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
	}
}


