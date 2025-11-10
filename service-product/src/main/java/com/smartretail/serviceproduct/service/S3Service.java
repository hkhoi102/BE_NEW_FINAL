package com.smartretail.serviceproduct.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region:ap-southeast-1}")
    private String region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.folder:product-images}")
    private String folder;

    private S3Client s3Client;

    /**
     * Khởi tạo S3Client
     */
    private S3Client getS3Client() {
        if (s3Client == null) {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
        }
        return s3Client;
    }

    /**
     * Upload file lên S3
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // Tạo tên file duy nhất
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = generateUniqueFilename(fileExtension);

        // Tạo đường dẫn theo năm/tháng
        String yearMonthPath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String s3Key = folder + "/" + yearMonthPath + "/" + uniqueFilename;

        try {
            // Upload file lên S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            getS3Client().putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Trả về URL của file trên S3
            return getS3Url(s3Key);

        } catch (Exception e) {
            throw new IOException("Lỗi khi upload file lên S3: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa file từ S3
     */
    public boolean deleteFile(String s3Url) {
        try {
            // Extract key từ URL
            String s3Key = extractKeyFromUrl(s3Url);
            if (s3Key == null) {
                return false;
            }

            // Xóa file từ S3
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            getS3Client().deleteObject(deleteObjectRequest);
            return true;

        } catch (Exception e) {
            System.err.println("Lỗi khi xóa file từ S3: " + e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra file có tồn tại trên S3 không
     */
    public boolean fileExists(String s3Url) {
        try {
            String s3Key = extractKeyFromUrl(s3Url);
            if (s3Key == null) {
                return false;
            }

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            getS3Client().headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            System.err.println("Lỗi khi kiểm tra file trên S3: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy URL đầy đủ của file trên S3
     */
    public String getS3Url(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }

    /**
     * Extract key từ S3 URL
     */
    private String extractKeyFromUrl(String s3Url) {
        if (s3Url == null || s3Url.trim().isEmpty()) {
            return null;
        }

        try {
            // URL format: https://bucket-name.s3.region.amazonaws.com/folder/path/file.jpg
            String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            if (s3Url.startsWith(baseUrl)) {
                return s3Url.substring(baseUrl.length());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lấy phần mở rộng của file
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Tạo tên file duy nhất
     */
    private String generateUniqueFilename(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "img_" + timestamp + "_" + uuid + extension;
    }

    /**
     * Test kết nối S3
     */
    public boolean testConnection() {
        try {
            ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
            getS3Client().listBuckets(listBucketsRequest);
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi kết nối S3: " + e.getMessage());
            return false;
        }
    }
}
