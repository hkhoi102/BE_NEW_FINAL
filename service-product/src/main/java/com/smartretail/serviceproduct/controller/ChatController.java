package com.smartretail.serviceproduct.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class ChatController {

	@Value("${ai.service.url:http://localhost:8000}")
	private String aiServiceBaseUrl;

	private RestTemplate restTemplate() {
		RestTemplate rt = new RestTemplate();
		return rt;
	}

	@PostMapping("/chat")
	public ResponseEntity<?> chat(@RequestBody Map<String, Object> payload) {
		String url = aiServiceBaseUrl + "/chat";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
		try {
			ResponseEntity<Map> response = restTemplate().exchange(url, HttpMethod.POST, request, Map.class);
			return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
		} catch (RestClientException ex) {
			Map<String, Object> err = new HashMap<>();
			err.put("error", "AI service unavailable");
			err.put("detail", ex.getMessage());
			return ResponseEntity.status(502).body(err);
		}
	}

	@PostMapping("/ingest")
	public ResponseEntity<?> ingest(@RequestBody(required = false) Map<String, Object> payload) {
		String url = aiServiceBaseUrl + "/ingest";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload == null ? new HashMap<>() : payload, headers);
		try {
			ResponseEntity<Map> response = restTemplate().exchange(url, HttpMethod.POST, request, Map.class);
			return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
		} catch (RestClientException ex) {
			Map<String, Object> err = new HashMap<>();
			err.put("error", "AI service unavailable");
			err.put("detail", ex.getMessage());
			return ResponseEntity.status(502).body(err);
		}
	}
}


