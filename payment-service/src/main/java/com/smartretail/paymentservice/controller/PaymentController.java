package com.smartretail.paymentservice.controller;

import com.smartretail.paymentservice.config.SepayConfig;
import com.smartretail.paymentservice.dto.PaymentDtos.Ack;
import com.smartretail.paymentservice.dto.PaymentDtos.CreatePaymentRequest;
import com.smartretail.paymentservice.dto.PaymentDtos.CreatePaymentResponse;
import com.smartretail.paymentservice.dto.PaymentDtos.WebhookEvent;
import com.smartretail.paymentservice.service.SepayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final SepayClient sepayClient;
    private final SepayConfig sepayConfig;


    public PaymentController(SepayClient sepayClient, SepayConfig sepayConfig) {
        this.sepayClient = sepayClient;
        this.sepayConfig = sepayConfig;
    }

	@PostMapping("/sepay/intent")
	public ResponseEntity<CreatePaymentResponse> createIntent(@RequestBody CreatePaymentRequest request) {
		CreatePaymentResponse resp = sepayClient.createBankTransferIntent(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(resp);
	}

	@PostMapping("/sepay/webhook")
	public ResponseEntity<Ack> sepayWebhook(@RequestBody WebhookEvent event, @RequestHeader(value = "X-Signature", required = false) String signature) {
		// TODO: verify signature if sepayConfig.isWebhookVerify()

		// Xử lý webhook từ Sepay
		if ("payment.succeeded".equals(event.type) && event.data != null) {
			String referenceId = event.data.referenceId;
			Long orderId = event.data.orderId;

			if (referenceId != null && orderId != null) {
				// Chỉ log thông tin webhook, không gọi Order Service
				System.out.println("Payment webhook received for order: " + orderId + ", referenceId: " + referenceId);
				System.out.println("Frontend should call PUT /api/orders/" + orderId + "/payment-status to update payment status");
			}
		}

		return ResponseEntity.ok(Ack.ok());
	}

	@GetMapping("/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("OK");
	}

	// Proxy: Get latest SePay transactions (requires header X-Sepay-ApiKey or uses configured key)
	@GetMapping("/sepay/transactions")
	public ResponseEntity<?> getSepayTransactions(@RequestParam(defaultValue = "20") int limit,
	                                              @RequestHeader(value = "X-Sepay-ApiKey", required = false) String apiKey,
	                                              @RequestParam(required = false) String accountNumber) {
		String acc = accountNumber != null ? accountNumber : sepayConfig.getAccountNumber();
		if (acc == null || acc.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of(
				"success", false,
				"message", "Missing account number"
			));
		}
		try {
			Map resp = sepayClient.getLatestTransactions(acc, limit, apiKey);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			return ResponseEntity.status(502).body(Map.of(
				"success", false,
				"message", "Failed to fetch transactions: " + e.getMessage()
			));
		}
	}

	// Check if a transaction exists matching transfer content (and optional amount)
	@GetMapping("/sepay/match")
	public ResponseEntity<?> matchTransaction(@RequestParam("content") String content,
	                                          @RequestParam(required = false) String amount,
	                                          @RequestParam(defaultValue = "20") int limit,
	                                          @RequestHeader(value = "X-Sepay-ApiKey", required = false) String apiKey,
	                                          @RequestParam(required = false) String accountNumber) {
		String acc = accountNumber != null ? accountNumber : sepayConfig.getAccountNumber();
		if (acc == null || acc.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing account number"));
		}
		// Only accept content that is exactly `ORDER` + alphanumeric code
		if (content == null || !content.matches("^ORDER[0-9A-Za-z]+$")) {
			return ResponseEntity.ok(Map.of(
				"success", false,
				"message", "Invalid content. Must be ORDER+code (letters/digits)."
			));
		}
		try {
			Map resp = sepayClient.getLatestTransactions(acc, limit, apiKey);
			Object txsObj = resp != null ? resp.get("transactions") : null;
			if (!(txsObj instanceof java.util.List<?>)) {
				return ResponseEntity.ok(Map.of("success", false, "message", "No transactions"));
			}
			java.util.List<?> txs = (java.util.List<?>) txsObj;
			// Exact token must appear as standalone
			Pattern exactToken = Pattern.compile("(?<![A-Za-z0-9])" + Pattern.quote(content) + "(?![A-Za-z0-9])");
			for (Object o : txs) {
				if (!(o instanceof Map)) continue;
				Map<?,?> m = (Map<?,?>) o;
				Object descObj = m.get("transaction_content");
				String desc = descObj != null ? descObj.toString() : "";
				Object inObj = m.get("amount_in");
				String inStr = inObj != null ? inObj.toString() : "0";
				boolean contentMatch = !desc.isEmpty() && exactToken.matcher(desc).find();
				boolean amountMatch = true;
				if (amount != null && !amount.isBlank()) {
					try {
						java.math.BigDecimal expect = new java.math.BigDecimal(amount);
						java.math.BigDecimal actual = new java.math.BigDecimal(inStr.replace(",", ""));
						amountMatch = expect.compareTo(actual) == 0;
					} catch (Exception ignore) { amountMatch = false; }
				}
				if (contentMatch && amountMatch) {
					return ResponseEntity.ok(Map.of(
						"success", true,
						"message", "Matched",
						"transaction", m
					));
				}
			}
			return ResponseEntity.ok(Map.of("success", false, "message", "No matching transaction"));
		} catch (Exception e) {
			return ResponseEntity.status(502).body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	// Endpoint để Frontend kiểm tra trạng thái thanh toán
	@GetMapping("/status/{orderId}")
	public ResponseEntity<?> getPaymentStatus(@PathVariable Long orderId) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Payment status check endpoint - Frontend should call Order Service directly");
		response.put("orderId", orderId);
		response.put("suggestedAction", "Call PUT /api/orders/" + orderId + "/payment-status to update payment status");
		return ResponseEntity.ok(response);
	}
}


