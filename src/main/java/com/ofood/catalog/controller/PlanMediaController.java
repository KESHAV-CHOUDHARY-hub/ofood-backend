package com.ofood.catalog.controller;

import com.ofood.catalog.dto.PlanResponse;
import com.ofood.catalog.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans/{id}/media")
public class PlanMediaController {

    private final PlanService planService;

    public PlanMediaController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping(value = "/primary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Upload primary image", description = "Uploads a primary image for a plan. Replaces the existing primary image. Allowed types: JPEG, PNG, WEBP.")
    public ResponseEntity<PlanResponse> uploadPrimaryImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(planService.uploadPrimaryImage(id, file));
    }

    @PostMapping(value = "/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Upload gallery images", description = "Uploads multiple gallery images for a plan. Allowed types: JPEG, PNG, WEBP.")
    public ResponseEntity<PlanResponse> uploadGalleryImages(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(planService.uploadGalleryImages(id, files));
    }

    @DeleteMapping("/primary")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Remove primary image", description = "Removes the primary image for a plan.")
    public ResponseEntity<PlanResponse> removePrimaryImage(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.removePrimaryImage(id));
    }

    @DeleteMapping("/gallery")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Remove gallery image", description = "Removes a specific gallery image from a plan by its imageUrl reference.")
    public ResponseEntity<PlanResponse> removeGalleryImage(
            @PathVariable UUID id,
            @RequestParam("imageUrl") String imageUrl) {
        return ResponseEntity.ok(planService.removeGalleryImage(id, imageUrl));
    }
}
