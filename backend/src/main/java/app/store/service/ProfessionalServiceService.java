package app.store.service;

import app.store.dto.request.ServiceRequest;
import app.store.dto.response.ServiceResponse;
import app.store.entity.ProfessionalService;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.ServiceMapper;
import app.store.repository.ServiceRepository;
import app.store.utils.SlugUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfessionalServiceService {

    ServiceRepository serviceRepository;
    ServiceMapper serviceMapper;
    SlugUtil slugUtil;

    @Transactional(readOnly = true)
    public List<ServiceResponse> getAllServices() {
        return serviceRepository.findAllWithImages().stream()
                .map(serviceMapper::toServiceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long id) {
        ProfessionalService service = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));
        return serviceMapper.toServiceResponse(service);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceBySlug(String slug) {
        ProfessionalService service = serviceRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));
        return serviceMapper.toServiceResponse(service);
    }

    @PreAuthorize("hasAuthority('SERVICE_CREATE')")
    public ServiceResponse createService(ServiceRequest request) {
        ProfessionalService service = serviceMapper.toService(request);

        String baseSlug = slugUtil.toSlug(request.getName());
        service.setSlug(slugUtil.createUniqueSlug(baseSlug, serviceRepository::existsBySlug));

        return serviceMapper.toServiceResponse(serviceRepository.save(service));
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

        return serviceMapper.toServiceResponse(serviceRepository.save(service));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SERVICE_DELETE')")
    public void deleteService(Long id) {
        ProfessionalService service = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));
        serviceRepository.delete(service);
    }
}
