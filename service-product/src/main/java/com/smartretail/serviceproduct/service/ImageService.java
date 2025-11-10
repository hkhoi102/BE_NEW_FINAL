package com.smartretail.serviceproduct.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ImageService {

    @Autowired
    private S3Service s3Service;

    @Value("${app.upload.path:uploads/images}")
    private String uploadPath;

    @Value("${app.upload.max-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${app.upload.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypes;

    @Value("${app.upload.use-s3:true}")
    private boolean useS3;

    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

    /**
     * Upload và lưu ảnh sản phẩm
     */
    public String uploadProductImage(MultipartFile file) throws IOException {
        // Validate file
        validateImageFile(file);

        if (useS3) {
            // Upload lên S3
            return s3Service.uploadFile(file);
        } else {
            // Upload local (fallback)
            return uploadLocalFile(file);
        }
    }

    /**
     * Upload file local (fallback method)
     */
    private String uploadLocalFile(MultipartFile file) throws IOException {
        // Tạo tên file duy nhất
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = generateUniqueFilename(fileExtension);

        // Tạo thư mục theo năm/tháng
        String yearMonthPath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path targetDir = Paths.get(uploadPath, yearMonthPath);
        Files.createDirectories(targetDir);

        // Lưu file
        Path targetPath = targetDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về đường dẫn tương đối để lưu vào database
        return yearMonthPath + "/" + uniqueFilename;
    }

    /**
     * Xóa ảnh sản phẩm
     */
    public boolean deleteProductImage(String imagePath) {
        if (useS3) {
            // Xóa từ S3
            return s3Service.deleteFile(imagePath);
        } else {
            // Xóa local (fallback)
            return deleteLocalFile(imagePath);
        }
    }

    /**
     * Xóa file local (fallback method)
     */
    private boolean deleteLocalFile(String imagePath) {
        try {
            Path fullPath = Paths.get(uploadPath, imagePath);
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Không thể xóa ảnh: " + e.getMessage());
        }
    }

    /**
     * Lấy đường dẫn đầy đủ của ảnh
     */
    public String getFullImagePath(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }

        if (useS3) {
            // Nếu đã là S3 URL thì trả về luôn
            if (imagePath.startsWith("https://")) {
                return imagePath;
            }
            // Nếu là key thì tạo URL
            return s3Service.getS3Url(imagePath);
        } else {
            // Local path
            return uploadPath + "/" + imagePath;
        }
    }

    /**
     * Validate file ảnh
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Kích thước file quá lớn. Tối đa: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Loại file không được hỗ trợ. Chỉ chấp nhận: " + allowedTypes);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidExtension(originalFilename)) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ. Chỉ chấp nhận: " + String.join(", ", ALLOWED_EXTENSIONS));
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
     * Kiểm tra phần mở rộng file có hợp lệ không
     */
    private boolean hasValidExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (allowedExt.equals(extension)) {
                return true;
            }
        }
        return false;
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
     * Kiểm tra file có phải là ảnh không
     */
    public boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Lấy kích thước file dưới dạng đọc được
     */
    public String getReadableFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
