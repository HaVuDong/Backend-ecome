package havudong.baocao.e2e;

import havudong.baocao.dto.AuthResponse;
import havudong.baocao.dto.ProductRequest;
import havudong.baocao.entity.Category;
import havudong.baocao.entity.Product;
import havudong.baocao.entity.User;
import havudong.baocao.repository.CategoryRepository;
import havudong.baocao.repository.ProductRepository;
import havudong.baocao.repository.UserRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class E2EProductUploadTest {

    @LocalServerPort
    private int port;

    private final RestTemplate rest = new RestTemplate();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void upload_product_with_image_to_cloudinary() throws Exception {
        // Skip if Cloudinary not configured
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");
        Assumptions.assumeTrue(cloudName != null && !cloudName.isBlank() && apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank(), "Cloudinary not configured - skipping E2E");

        // Create seller user via register
        String base = "http://localhost:" + port;
        Map<String, Object> reg = Map.of(
                "email", "e2e-seller+" + System.currentTimeMillis() + "@local",
                "password", "password123",
                "fullName", "E2E Seller",
                "role", "SELLER"
        );
        ResponseEntity<AuthResponse> regResp = rest.postForEntity(base + "/api/auth/register", reg, AuthResponse.class);
        // Accept any 2xx (201 Created is returned by controller)
        assertTrue(regResp.getStatusCode().is2xxSuccessful());
        String token = regResp.getBody().getToken();
        assertNotNull(token);

        // Create category with unique name to avoid collisions when tests run repeatedly
        Category cat = new Category();
        cat.setName("E2E Test Category " + System.currentTimeMillis());
        cat = categoryRepository.save(cat);

        // Build product JSON
        ProductRequest request = new ProductRequest();
        request.setCategoryId(cat.getId());
        request.setName("E2E Product");
        request.setDescription("Uploaded by E2E test");
        request.setPrice(BigDecimal.valueOf(123.45));
        request.setStock(5);

        // Prepare a tiny PNG file (1x1) as byte[]
        byte[] png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("product", new org.springframework.http.HttpEntity<>(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request), createJsonHeaders()));
        org.springframework.core.io.ByteArrayResource bar = new org.springframework.core.io.ByteArrayResource(png) {
            @Override
            public String getFilename() {
                return "e2e.png";
            }
        };
        body.add("file", bar);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = rest.postForEntity(base + "/api/products", entity, String.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());

        // Load seller entity from repo and find products by seller
        Long userId = regResp.getBody().getUser().getId();
        User sellerEntity = userRepository.findById(userId).orElseThrow();
        Product p = productRepository.findBySellerAndIsActiveTrue(sellerEntity).stream()
                .filter(x -> "E2E Product".equals(x.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(p);
        assertNotNull(p.getMainImage());
        assertTrue(p.getMainImage().startsWith("https://"));
        assertNotNull(p.getMainImagePublicId());
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
