package com.borrowhub.service;

import com.borrowhub.exception.BadRequestException;
import com.borrowhub.exception.ResourceNotFoundException;
import com.borrowhub.model.entity.Category;
import com.borrowhub.model.entity.Resource;
import com.borrowhub.model.entity.ResourceUnit;
import com.borrowhub.model.enums.InventoryMode;
import com.borrowhub.payload.request.ResourceRequest;
import com.borrowhub.payload.request.ResourceUnitRequest;
import com.borrowhub.payload.response.ResourceResponse;
import com.borrowhub.payload.response.ResourceUnitResponse;
import com.borrowhub.repository.CategoryRepository;
import com.borrowhub.repository.ResourceRepository;
import com.borrowhub.repository.ResourceUnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceUnitRepository resourceUnitRepository;
    private final CategoryRepository categoryRepository;
    private final AvailabilityService availabilityService;

    public ResourceService(ResourceRepository resourceRepository,
                           ResourceUnitRepository resourceUnitRepository,
                           CategoryRepository categoryRepository,
                           AvailabilityService availabilityService) {
        this.resourceRepository = resourceRepository;
        this.resourceUnitRepository = resourceUnitRepository;
        this.categoryRepository = categoryRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> searchResources(Long categoryId, InventoryMode mode, Boolean activeOnly, String search, Pageable pageable) {
        Page<Resource> page = resourceRepository.searchResources(categoryId, mode, activeOnly, search, pageable);
        return page.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));
        return mapToResponse(resource);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceByCode(String code) {
        Resource resource = resourceRepository.findByResourceCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "resourceCode", code));
        return mapToResponse(resource);
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        if (resourceRepository.findByResourceCode(request.getResourceCode().trim()).isPresent()) {
            throw new BadRequestException("Resource code '" + request.getResourceCode() + "' is already in use");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Resource resource = new Resource();
        resource.setName(request.getName().trim());
        resource.setResourceCode(request.getResourceCode().trim().toUpperCase());
        resource.setDescription(request.getDescription());
        resource.setCategory(category);
        resource.setInventoryMode(request.getInventoryMode() != null ? request.getInventoryMode() : InventoryMode.INDIVIDUAL);
        resource.setTotalQuantity(request.getTotalQuantity() != null ? request.getTotalQuantity() : 0);
        resource.setMaxBorrowDays(request.getMaxBorrowDays() != null ? request.getMaxBorrowDays() : 7);
        resource.setReservable(request.getIsReservable() != null ? request.getIsReservable() : true);
        resource.setActive(request.getIsActive() != null ? request.getIsActive() : true);
        resource.setImageUrl(request.getImageUrl());

        Resource saved = resourceRepository.save(resource);
        return mapToResponse(saved);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        resource.setName(request.getName().trim());
        resource.setDescription(request.getDescription());
        resource.setCategory(category);
        resource.setInventoryMode(request.getInventoryMode() != null ? request.getInventoryMode() : resource.getInventoryMode());
        resource.setTotalQuantity(request.getTotalQuantity() != null ? request.getTotalQuantity() : resource.getTotalQuantity());
        resource.setMaxBorrowDays(request.getMaxBorrowDays() != null ? request.getMaxBorrowDays() : resource.getMaxBorrowDays());
        resource.setReservable(request.getIsReservable() != null ? request.getIsReservable() : resource.isReservable());
        resource.setActive(request.getIsActive() != null ? request.getIsActive() : resource.isActive());
        resource.setImageUrl(request.getImageUrl());

        Resource saved = resourceRepository.save(resource);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));
        resource.setActive(false);
        resourceRepository.save(resource);
    }

    // Units
    @Transactional(readOnly = true)
    public List<ResourceUnitResponse> getUnitsByResourceId(Long resourceId) {
        List<ResourceUnit> units = resourceUnitRepository.findByResourceId(resourceId);
        return units.stream().map(this::mapUnitToResponse).collect(Collectors.toList());
    }

    @Transactional
    public ResourceUnitResponse createResourceUnit(ResourceUnitRequest request) {
        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", request.getResourceId()));

        if (resourceUnitRepository.findByAssetCode(request.getAssetCode().trim()).isPresent()) {
            throw new BadRequestException("Asset code '" + request.getAssetCode() + "' is already in use");
        }

        ResourceUnit unit = new ResourceUnit();
        unit.setResource(resource);
        unit.setAssetCode(request.getAssetCode().trim().toUpperCase());
        unit.setQrCodeData("BORROWHUB://UNIT/" + unit.getAssetCode());
        unit.setSerialNumber(request.getSerialNumber());
        unit.setBrandModel(request.getBrandModel());
        unit.setPhysicalCondition(request.getPhysicalCondition());
        unit.setStatus(request.getStatus());
        unit.setStorageLocation(request.getStorageLocation());
        unit.setAcquisitionDate(LocalDate.now());
        unit.setNotes(request.getNotes());

        ResourceUnit saved = resourceUnitRepository.save(unit);

        // Update totalQuantity on parent resource
        resource.setTotalQuantity(resourceUnitRepository.countByResourceId(resource.getId()));
        resourceRepository.save(resource);

        return mapUnitToResponse(saved);
    }

    @Transactional
    public ResourceUnitResponse updateResourceUnit(Long unitId, ResourceUnitRequest request) {
        ResourceUnit unit = resourceUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("ResourceUnit", "id", unitId));

        unit.setSerialNumber(request.getSerialNumber());
        unit.setBrandModel(request.getBrandModel());
        unit.setPhysicalCondition(request.getPhysicalCondition());
        unit.setStatus(request.getStatus());
        unit.setStorageLocation(request.getStorageLocation());
        unit.setNotes(request.getNotes());

        ResourceUnit saved = resourceUnitRepository.save(unit);
        return mapUnitToResponse(saved);
    }

    private ResourceResponse mapToResponse(Resource resource) {
        ResourceResponse dto = new ResourceResponse();
        dto.setId(resource.getId());
        dto.setName(resource.getName());
        dto.setResourceCode(resource.getResourceCode());
        dto.setDescription(resource.getDescription());
        dto.setCategoryId(resource.getCategory().getId());
        dto.setCategoryName(resource.getCategory().getName());
        dto.setInventoryMode(resource.getInventoryMode());
        dto.setTotalQuantity(resource.getTotalQuantity());
        dto.setAvailableQuantity(availabilityService.getAvailableQuantity(resource.getId()));
        dto.setMaxBorrowDays(resource.getMaxBorrowDays());
        dto.setIsReservable(resource.isReservable());
        dto.setIsActive(resource.isActive());
        dto.setImageUrl(resource.getImageUrl());
        dto.setCreatedAt(resource.getCreatedAt());
        dto.setUpdatedAt(resource.getUpdatedAt());
        return dto;
    }

    private ResourceUnitResponse mapUnitToResponse(ResourceUnit unit) {
        ResourceUnitResponse dto = new ResourceUnitResponse();
        dto.setId(unit.getId());
        dto.setResourceId(unit.getResource().getId());
        dto.setResourceName(unit.getResource().getName());
        dto.setAssetCode(unit.getAssetCode());
        dto.setQrCodeData(unit.getQrCodeData());
        dto.setSerialNumber(unit.getSerialNumber());
        dto.setBrandModel(unit.getBrandModel());
        dto.setPhysicalCondition(unit.getPhysicalCondition());
        dto.setStatus(unit.getStatus());
        dto.setStorageLocation(unit.getStorageLocation());
        dto.setAcquisitionDate(unit.getAcquisitionDate());
        dto.setNotes(unit.getNotes());
        dto.setCreatedAt(unit.getCreatedAt());
        dto.setUpdatedAt(unit.getUpdatedAt());
        return dto;
    }
}
