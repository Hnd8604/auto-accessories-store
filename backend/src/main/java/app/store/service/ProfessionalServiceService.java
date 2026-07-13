package app.store.service;

import app.store.dto.request.ServiceRequest;
import app.store.dto.response.ServiceResponse;
import app.store.entity.ProfessionalService;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.ServiceMapper;
import app.store.repository.ServiceRepository;
import app.store.utils.SlugUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProfessionalServiceService {

    ServiceRepository serviceRepository;
    ServiceMapper serviceMapper;
    SlugUtil slugUtil;
    ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ServiceResponse> getAllServices() {
        return serviceRepository.findAllWithImages().stream()
                .map(service -> {
                    ServiceResponse response = serviceMapper.toServiceResponse(service);
                    response.setFeatures(parseFeatures(service.getFeatures()));
                    if (service.getImages() != null) {
                        service.getImages().stream()
                                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                .findFirst()
                                .ifPresent(img -> response.setPrimaryImageUrl(img.getImageUrl()));
                        if (response.getPrimaryImageUrl() == null && !service.getImages().isEmpty()) {
                            response.setPrimaryImageUrl(service.getImages().get(0).getImageUrl());
                        }
                    }
                    return response;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long id) {
        ProfessionalService service = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));
        return toResponse(service);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceBySlug(String slug) {
        ProfessionalService service = serviceRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));
        return toResponse(service);
    }

    @PreAuthorize("hasAuthority('SERVICE_CREATE')")
    public ServiceResponse createService(ServiceRequest request) {
        ProfessionalService service = serviceMapper.toService(request);
        service.setFeatures(serializeFeatures(request.getFeatures()));

        String baseSlug = slugUtil.toSlug(request.getName());
        service.setSlug(slugUtil.createUniqueSlug(baseSlug, serviceRepository::existsBySlug));

        return toResponse(serviceRepository.save(service));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SERVICE_UPDATE')")
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        ProfessionalService service = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));

        if (!service.getName().equals(request.getName())) {
            String baseSlug = slugUtil.toSlug(request.getName());
            String uniqueSlug = slugUtil.createUniqueSlug(baseSlug, slug ->
                    !slug.equals(service.getSlug()) && serviceRepository.existsBySlug(slug));
            service.setSlug(uniqueSlug);
        }

        serviceMapper.updateService(service, request);
        service.setFeatures(serializeFeatures(request.getFeatures()));

        return toResponse(serviceRepository.save(service));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SERVICE_DELETE')")
    public void deleteService(Long id) {
        ProfessionalService service = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));
        serviceRepository.delete(service);
    }

    private ServiceResponse toResponse(ProfessionalService service) {
        ServiceResponse response = serviceMapper.toServiceResponse(service);
        response.setFeatures(parseFeatures(service.getFeatures()));
        if (service.getImages() != null) {
            service.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .ifPresent(img -> response.setPrimaryImageUrl(img.getImageUrl()));
            if (response.getPrimaryImageUrl() == null && !service.getImages().isEmpty()) {
                response.setPrimaryImageUrl(service.getImages().get(0).getImageUrl());
            }
        }
        return response;
    }

    private String serializeFeatures(List<String> features) {
        if (features == null || features.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(features);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize features", e);
            return "[]";
        }
    }

    private List<String> parseFeatures(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse features JSON: {}", json, e);
            return Collections.emptyList();
        }
    }
}
