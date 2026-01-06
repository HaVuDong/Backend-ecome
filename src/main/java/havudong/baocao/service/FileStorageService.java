package havudong.baocao.service;

import havudong.baocao.exception.BadRequestException;
import havudong.baocao.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    private final Path fileStorageLocation;
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Không thể tạo thư mục lưu file", ex);
        }
    }
    
    public String storeFile(MultipartFile file) {
        // Validate file
        validateFile(file);
        
        // Normalize filename và tạo unique name
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + fileExtension;
        
        try {
            // Check for invalid characters
            if (newFilename.contains("..")) {
                throw new FileStorageException("Tên file không hợp lệ: " + newFilename);
            }
            
            // Copy file to target location
            Path targetLocation = this.fileStorageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Uploaded file: {}", newFilename);
            return newFilename;
            
        } catch (IOException ex) {
            throw new FileStorageException("Không thể lưu file: " + newFilename, ex);
        }
    }
    
    public void deleteFile(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filename);
        } catch (IOException ex) {
            throw new FileStorageException("Không thể xóa file: " + filename, ex);
        }
    }
    
    public Path loadFile(String filename) {
        return this.fileStorageLocation.resolve(filename).normalize();
    }
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File quá lớn. Tối đa 5MB");
        }
        
        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Chỉ chấp nhận file ảnh (JPEG, PNG, GIF, WebP)");
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
}
