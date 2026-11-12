package com.borrowhub.controller;

import com.borrowhub.model.enums.InventoryMode;
import com.borrowhub.payload.request.ResourceRequest;
import com.borrowhub.payload.request.ResourceUnitRequest;
import com.borrowhub.payload.response.ApiResponse;
import com.borrowhub.payload.response.ResourceResponse;
import com.borrowhub.payload.response.ResourceUnitResponse;
import com.borrowhub.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ResourceResponse>>> searchResources(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) InventoryMode mode,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ResourceResponse> results = resourceService.searchResources(categoryId, mode, activeOnly, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceById(@PathVariable Long id) {
        ResourceResponse resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(ApiResponse.success(resource));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceByCode(@PathVariable String code) {
        ResourceResponse resource = resourceService.getResourceByCode(code);
        return ResponseEntity.ok(ApiResponse.success(resource));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(
            @Valid @RequestBody ResourceRequest request) {
        ResourceResponse created = resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {
        ResourceResponse updated = resourceService.updateResource(id, request);
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok(ApiResponse.success("Resource deactivated successfully", null));
    }

    // Units
    @GetMapping("/{id}/units")
    public ResponseEntity<ApiResponse<List<ResourceUnitResponse>>> getResourceUnits(@PathVariable Long id) {
        List<ResourceUnitResponse> units = resourceService.getUnitsByResourceId(id);
        return ResponseEntity.ok(ApiResponse.success(units));
    }

    @PostMapping("/{id}/units")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResourceUnitResponse>> createResourceUnit(
            @PathVariable Long id,
            @Valid @RequestBody ResourceUnitRequest request) {
        request.setResourceId(id);
        ResourceUnitResponse created = resourceService.createResourceUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource unit created successfully", created));
    }

    @PutMapping("/units/{unitId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResourceUnitResponse>> updateResourceUnit(
            @PathVariable Long unitId,
            @Valid @RequestBody ResourceUnitRequest request) {
        ResourceUnitResponse updated = resourceService.updateResourceUnit(unitId, request);
        return ResponseEntity.ok(ApiResponse.success("Resource unit updated successfully", updated));
    }
}
