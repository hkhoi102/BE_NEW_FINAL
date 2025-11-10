package com.smartretail.orderservice.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.smartretail.orderservice.client.CustomerServiceClient;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private CustomerServiceClient customerServiceClient;

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Gửi thông báo cho khách hàng khi hàng đã chuẩn bị sẵn và có thể đến nhận tại cửa hàng
     */
    public void sendOrderReadyForPickupNotification(Long orderId, String orderCode, Long customerId, String authHeader) {
        try {
            log.info("=== SENDING ORDER READY FOR PICKUP NOTIFICATION ===");
            log.info("Order ID: {}, Order Code: {}, Customer ID: {}", orderId, orderCode, customerId);

            // Lấy thông tin khách hàng từ customer service
            Map<String, Object> customerInfo = getCustomerInfo(customerId, authHeader);
            if (customerInfo != null) {
                String customerName = (String) customerInfo.getOrDefault("name", "Khách hàng");
                String customerPhone = (String) customerInfo.getOrDefault("phone", "");
                String customerEmail = (String) customerInfo.getOrDefault("email", "");

                log.info("Customer Name: {}", customerName);
                log.info("Customer Phone: {}", customerPhone);
                log.info("Customer Email: {}", customerEmail);

                // Gửi notification qua Email (chỉ cần email, không cần số điện thoại)
                if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                    sendEmailNotification(customerEmail, customerName, orderCode, orderId);
                }

                // Gửi notification qua SMS (nếu có số điện thoại)
                if (customerPhone != null && !customerPhone.trim().isEmpty()) {
                    sendSMSNotification(customerPhone, orderCode, orderId);
                }

                log.info("=== NOTIFICATION SENT SUCCESSFULLY ===");
            } else {
                log.warn("Could not retrieve customer information for customer ID: {}", customerId);
            }
        } catch (Exception e) {
            log.error("Failed to send order ready notification: {}", e.getMessage(), e);
            // Không throw exception để không ảnh hưởng đến flow chính của cập nhật đơn hàng
        }
    }

    /**
     * Gửi SMS notification
     */
    private void sendSMSNotification(String phoneNumber, String orderCode, Long orderId) {
        try {
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                String message = String.format(
                    "Chào bạn,\n\nĐơn hàng %s đã được chuẩn bị xong. Bạn vui lòng đến cửa hàng để nhận hàng.\n\nTrân trọng!",
                    orderCode
                );

                log.info("=== SENDING SMS ===");
                log.info("To: {}", phoneNumber);
                log.info("Message: {}", message);

                // TODO: Tích hợp với SMS gateway thực tế (Twilio, AWS SNS, etc.)
                // Hiện tại chỉ log message

                log.info("=== SMS SENT (SIMULATED) ===");
            }
        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage());
        }
    }

    /**
     * Gửi Email notification
     */
    private void sendEmailNotification(String email, String customerName, String orderCode, Long orderId) {
        if (email != null && !email.trim().isEmpty()) {
            String subject = String.format("Đơn hàng %s đã sẵn sàng nhận", orderCode);
            String body = String.format(
                "Chào %s,\n\n" +
                "Đơn hàng %s (ID: %d) của bạn đã được chuẩn bị xong.\n\n" +
                "Bạn vui lòng đến cửa hàng để nhận hàng.\n\n" +
                "Cảm ơn bạn đã mua sắm tại cửa hàng chúng tôi!\n\n" +
                "Trân trọng.",
                customerName, orderCode, orderId
            );

            log.info("=== SENDING EMAIL ===");
            log.info("To: {}", email);
            log.info("Subject: {}", subject);
            log.info("Body: {}", body);

            // Gửi email thật qua JavaMailSender
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("=== EMAIL SENT SUCCESSFULLY ===");
            } catch (Exception e) {
                log.error("Failed to send email: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Lấy thông tin khách hàng từ customer service
     */
    private Map<String, Object> getCustomerInfo(Long customerId, String authHeader) {
        try {
            Map<String, Object> customerInfo = customerServiceClient.getCustomerById(customerId, authHeader != null ? authHeader : "");
            return customerInfo;
        } catch (Exception e) {
            log.error("Failed to get customer info for ID {}: {}", customerId, e.getMessage());
            return null;
        }
    }
}

