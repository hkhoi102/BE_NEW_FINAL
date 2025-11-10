package com.smartretail.paymentservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public class PaymentDtos {

	public static class CreatePaymentRequest {
		public Long orderId;
		public BigDecimal amount;
		public String description;
		public String bankCode; // optional preferred bank
	}

	public static class CreatePaymentResponse {
		public String qrContent; // text to render QR or transfer content
		public String accountNumber;
		public String accountName;
		public String bankCode;
		public String transferContent; // unique content to match transaction
		public String referenceId; // our reference to match webhook
	}

	public static class WebhookEvent {
		public String id;
		public String type; // payment.succeeded, payment.failed
		public Data data;
		public String signature; // if provided
		public OffsetDateTime createdAt;
	}

    public static class Data {
        public String referenceId; // our reference
        public Long orderId; // order ID from our system
        public BigDecimal amount;
        public String currency;
        public String description;
        public String transactionId;
        public String bankCode;
        public String accountNumber;
        public OffsetDateTime paidAt;
        public Map<String, Object> raw; // keep full payload if needed
    }

	public static class Ack {
		public String status;
		public String message;
		public static Ack ok() { Ack a = new Ack(); a.status = "ok"; a.message = "received"; return a; }
	}
}


