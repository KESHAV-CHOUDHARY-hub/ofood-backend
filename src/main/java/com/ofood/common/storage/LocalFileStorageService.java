package com.ofood.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${app.storage.local.root:/tmp/ofood/uploads}") String rootPath) {
        this.rootLocation = Paths.get(rootPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local storage root directory", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String domain, UUID entityId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type. Only JPEG, PNG, and WEBP are allowed.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.jpg");
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Sorry! Filename contains invalid path sequence " + originalFilename);
        }

        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i);
        }

        String generatedFilename = UUID.randomUUID().toString() + extension;
        Path domainPath = this.rootLocation.resolve(domain).resolve(entityId.toString()).normalize();
        
        try {
            Files.createDirectories(domainPath);
            Path targetLocation = domainPath.resolve(generatedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + domain + "/" + entityId.toString() + "/" + generatedFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + generatedFilename + ". Please try again!", ex);
        }
    }

    @Override
    public void deleteFile(String storageReference) {
        if (!owns(storageReference)) {
            log.debug("LocalFileStorageService ignoring unowned reference: {}", storageReference);
            return;
        }

        try {
            String relativePath = storageReference.substring("/uploads/".length());
            Path targetLocation = this.rootLocation.resolve(relativePath).normalize();
            
            // Security check against path traversal
            if (!targetLocation.startsWith(this.rootLocation)) {
                log.warn("Path traversal attempt in deleteFile: {}", storageReference);
                return;
            }

            Files.deleteIfExists(targetLocation);
            log.debug("Deleted local file: {}", targetLocation);
        } catch (IOException ex) {
            log.error("Could not delete file: " + storageReference, ex);
        }
    }

    @Override
    public boolean owns(String storageReference) {
        return storageReference != null && storageReference.startsWith("/uploads/");
    }
}
