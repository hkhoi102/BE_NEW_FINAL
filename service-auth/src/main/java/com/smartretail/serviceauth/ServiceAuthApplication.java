package com.smartretail.serviceauth;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServiceAuthApplication {

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

		SpringApplication.run(ServiceAuthApplication.class, args);
	}

}
