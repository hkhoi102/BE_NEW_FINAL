package com.smartretail.inventoryservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EntityScan(basePackages = {"com.smartretail.inventoryservice.model"})
@EnableDiscoveryClient
@EnableFeignClients
public class InventoryServiceApplication {

	public static void main(String[] args) {
		// Load .env file if exists
		Dotenv dotenv = Dotenv.configure()
			.directory("./")  // Look for .env in root directory
			.ignoreIfMissing()  // Don't fail if .env doesn't exist
			.load();

		// Set environment variables from .env file
		dotenv.entries().forEach(entry ->
			System.setProperty(entry.getKey(), entry.getValue())
		);

		SpringApplication.run(InventoryServiceApplication.class, args);
	}
}
