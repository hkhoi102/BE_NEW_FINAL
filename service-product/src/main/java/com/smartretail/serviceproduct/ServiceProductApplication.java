package com.smartretail.serviceproduct;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ServiceProductApplication {

    public static void main(String[] args) {
        // Load .env file if exists
        // Try multiple locations: current directory, parent directory (for JAR files)
        Dotenv dotenv = null;
        try {
            // First try current directory (when running from IDE or Maven)
            dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
        } catch (Exception e) {
            try {
                // If not found, try parent directory (when running JAR from target/)
                dotenv = Dotenv.configure()
                    .directory("../")
                    .ignoreIfMissing()
                    .load();
            } catch (Exception e2) {
                // If still not found, ignore (use defaults)
                dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            }
        }

        // Set environment variables from .env file
        if (dotenv != null) {
            dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
            );
        }

        SpringApplication.run(ServiceProductApplication.class, args);
    }

}
