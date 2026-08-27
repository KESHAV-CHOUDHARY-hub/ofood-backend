package com.ofood.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.catalog.repository.PlanRepository;
import com.ofood.role.Role;
import com.ofood.role.repository.RoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PlanMediaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ofood")
            .withUsername("ofood")
            .withPassword("ofood");

    private static final String TEST_UPLOADS_DIR = "./test-uploads-" + System.currentTimeMillis();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("ofood.security.jwt.issuer", () -> "http://localhost:8080");
        registry.add("ofood.security.jwt.audience", () -> "ofood-api");
        registry.add("ofood.security.jwt.key-id", () -> "test-kid");
        registry.add("ofood.security.jwt.private-key-path",
                () -> "file:" + System.getProperty("user.dir") + "/src/test/resources/keys/test_private.pem");
        registry.add("ofood.auth.cookie.secure", () -> false);
        registry.add("app.storage.type", () -> "local");
        registry.add("app.storage.local.root", () -> TEST_UPLOADS_DIR);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlanRepository planRepository;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setup() throws Exception {
        planRepository.deleteAll();
        userRepository.deleteAll();

        adminToken = createUserAndLogin("admin3@test.com", "ROLE_ADMIN");
        customerToken = createUserAndLogin("customer3@test.com", "ROLE_CUSTOMER");

        Files.createDirectories(Paths.get(TEST_UPLOADS_DIR));
    }

    @AfterEach
    void teardown() throws IOException {
        Path path = Paths.get(TEST_UPLOADS_DIR);
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }

    private String createUserAndLogin(String email, String roleName) throws Exception {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password@123"));
        user.setFullName(email);
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.getRoles().add(role);
        userRepository.save(user);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createBaseDraft() throws Exception {
        String draftReq = "{\"name\": \"Media Plan\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftReq))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void testPrimaryImageUploadAndReplace() throws Exception {
        String planId = createBaseDraft();

        MockMultipartFile file1 = new MockMultipartFile(
                "file", "test1.jpg", "image/jpeg", "image-content".getBytes());

        // Upload first image
        MvcResult res1 = mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/primary")
                        .file(file1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image", startsWith("/uploads/plans/" + planId)))
                .andReturn();

        String imgUrl1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("image").asText();
        
        // Assert file exists on disk
        Path filePath1 = Paths.get(TEST_UPLOADS_DIR, imgUrl1.substring("/uploads/".length()));
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(filePath1));

        MockMultipartFile file2 = new MockMultipartFile(
                "file", "test2.png", "image/png", "new-content".getBytes());

        // Replace primary image
        MvcResult res2 = mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/primary")
                        .file(file2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image", startsWith("/uploads/plans/" + planId)))
                .andReturn();

        String imgUrl2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("image").asText();
        
        Path filePath2 = Paths.get(TEST_UPLOADS_DIR, imgUrl2.substring("/uploads/".length()));
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(filePath2));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(filePath1), "Old file should be deleted");
    }

    @Test
    void testUploadInvalidFileRejected() throws Exception {
        String planId = createBaseDraft();
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "bad".getBytes());

        mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/primary")
                        .file(badFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void testUploadEmptyFileRejected() throws Exception {
        String planId = createBaseDraft();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/primary")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMultipleGalleryImagesWhereOneIsInvalidStoresNone() throws Exception {
        String planId = createBaseDraft();
        MockMultipartFile okFile = new MockMultipartFile(
                "files", "test1.jpg", "image/jpeg", "image".getBytes());
        MockMultipartFile badFile = new MockMultipartFile(
                "files", "bad.pdf", "application/pdf", "pdf".getBytes());

        mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/gallery")
                        .file(okFile)
                        .file(badFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Validate plan is unmodified
        mockMvc.perform(get("/api/v1/plans/" + planId + "/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gallery").isEmpty());
    }

    @Test
    void testGalleryAppendAndRemove() throws Exception {
        String planId = createBaseDraft();
        MockMultipartFile f1 = new MockMultipartFile(
                "files", "1.jpg", "image/jpeg", "img".getBytes());
        MockMultipartFile f2 = new MockMultipartFile(
                "files", "2.png", "image/png", "img".getBytes());

        // Upload 2 files
        MvcResult res = mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/gallery")
                        .file(f1)
                        .file(f2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gallery", hasSize(2)))
                .andReturn();

        String url1 = objectMapper.readTree(res.getResponse().getContentAsString()).get("gallery").get(0).asText();
        String url2 = objectMapper.readTree(res.getResponse().getContentAsString()).get("gallery").get(1).asText();

        // Add 1 more
        MockMultipartFile f3 = new MockMultipartFile(
                "files", "3.jpg", "image/jpeg", "img".getBytes());
        MvcResult res2 = mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/gallery")
                        .file(f3)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gallery", hasSize(3)))
                .andReturn();

        // Delete url1
        mockMvc.perform(request(HttpMethod.DELETE, "/api/v1/plans/" + planId + "/media/gallery")
                        .param("imageUrl", url1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gallery", hasSize(2)));

        Path p1 = Paths.get(TEST_UPLOADS_DIR, url1.substring("/uploads/".length()));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(p1), "Gallery file should be deleted");
    }

    @Test
    void testExistingExternalImageReferenceNotDeleted() throws Exception {
        String planId = createBaseDraft();
        // Patch with external URL
        mockMvc.perform(patch("/api/v1/plans/" + planId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"image\": \"https://external.com/image.jpg\"}"))
                .andExpect(status().isOk());

        // Replace it
        MockMultipartFile file = new MockMultipartFile(
                "file", "new.jpg", "image/jpeg", "img".getBytes());
        mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/primary")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image", startsWith("/uploads/")));
    }

    @Test
    void testRemoveLastGalleryImageResultsInEmptyArray() throws Exception {
        String planId = createBaseDraft();
        MockMultipartFile f1 = new MockMultipartFile(
                "files", "1.jpg", "image/jpeg", "img".getBytes());

        MvcResult res = mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/gallery")
                        .file(f1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
                
        String url1 = objectMapper.readTree(res.getResponse().getContentAsString()).get("gallery").get(0).asText();

        mockMvc.perform(request(HttpMethod.DELETE, "/api/v1/plans/" + planId + "/media/gallery")
                        .param("imageUrl", url1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gallery", hasSize(0)));
    }

    @Test
    void testUnauthorizedUploadRejected() throws Exception {
        String planId = createBaseDraft();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "image-content".getBytes());

        mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/primary")
                        .file(file)
                        .header("Authorization", "Bearer " + customerToken)) // Not admin
                .andExpect(status().isForbidden());
    }

    @Test
    void testUploadDoesNotChangeStatus() throws Exception {
        String planId = createBaseDraft();
        
        mockMvc.perform(get("/api/v1/plans/" + planId + "/admin")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "img".getBytes());

        mockMvc.perform(multipart("/api/v1/plans/" + planId + "/media/primary")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
}
