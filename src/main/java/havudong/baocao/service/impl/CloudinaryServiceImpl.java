package havudong.baocao.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import havudong.baocao.config.CloudinaryProperties;
import havudong.baocao.dto.CloudinaryUploadResult;
import havudong.baocao.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Formatter;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final CloudinaryProperties properties;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();



    @Override
    public CloudinaryUploadResult upload(MultipartFile file) {
        try {
            // Basic validations
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Empty file");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("File must be an image");
            }
            long maxSize = 5L * 1024L * 1024L; // 5 MB (align with spring.servlet.multipart.max-file-size)
            if (file.getSize() > maxSize) {
                throw new IllegalArgumentException("File size exceeds 5MB limit");
            }

            // Ensure Cloudinary credentials are configured
            if (properties.getApiKey() == null || properties.getApiKey().isBlank()
                    || properties.getApiSecret() == null || properties.getApiSecret().isBlank()
                    || properties.getCloudName() == null || properties.getCloudName().isBlank()) {
                throw new IllegalStateException("Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET");
            }

            long timestamp = Instant.now().getEpochSecond();
            String toSign = "timestamp=" + timestamp;
            String signature = sha1Hex(toSign + properties.getApiSecret());

            String url = String.format("https://api.cloudinary.com/v1_1/%s/image/upload", properties.getCloudName());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }

                @Override
                public long contentLength() {
                    try {
                        return file.getSize();
                    } catch (Exception e) {
                        return -1L;
                    }
                }
            });
            body.add("timestamp", String.valueOf(timestamp));
            body.add("api_key", properties.getApiKey());
            body.add("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Cloudinary upload failed: " + response.getStatusCode());
            }

            JsonNode json = objectMapper.readTree(response.getBody());
            String secureUrl = json.path("secure_url").asText(null);
            String publicId = json.path("public_id").asText(null);

            if (secureUrl == null || publicId == null) {
                throw new RuntimeException("Cloudinary response missing url or public_id");
            }

            return new CloudinaryUploadResult(secureUrl, publicId);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload to Cloudinary", ex);
        }
    }

    private String sha1Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}
