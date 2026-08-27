package com.ofood.common.storage;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface StorageService {
    
    /**
     * Stores a file and returns a stable storage reference.
     * @param file the file to store
     * @param domain the domain/folder (e.g., "plans")
     * @param entityId the UUID of the entity the file belongs to
     * @return the storage reference string (e.g. URL or S3 key)
     */
    String storeFile(MultipartFile file, String domain, UUID entityId);

    /**
     * Deletes a file given its storage reference.
     * Should gracefully ignore references not managed by this implementation.
     * @param storageReference the storage reference to delete
     */
    void deleteFile(String storageReference);

    /**
     * Checks if this storage implementation owns/manages the given reference.
     * @param storageReference the storage reference to check
     * @return true if owned by this implementation, false otherwise
     */
    boolean owns(String storageReference);
}
