package app.store.service;

import app.store.dto.request.ServiceImageRequest;
import app.store.dto.request.ServiceImageUpdateRequest;
import app.store.dto.response.ServiceImageResponse;
import app.store.entity.ProfessionalService;
import app.store.entity.ServiceImage;
import app.store.exception.AppException;
import app.store.exception.ErrorCode;
import app.store.mapper.ServiceImageMapper;
import app.store.repository.ServiceImageRepository;
import app.store.repository.ServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ServiceImageService {

    ServiceImageRepository serviceImageRepository;
    ServiceImageMapper serviceImageMapper;
    ServiceRepository serviceRepository;
    CloudinaryService cloudinaryService;

    @PreAuthorize("hasAuthority('SERVICE_IMAGE_CREATE')")
    public ServiceImageResponse createServiceImage(MultipartFile file, ServiceImageRequest request) {
        ProfessionalService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));

        String imageUrl = cloudinaryService.uploadImage(file, "store/services");

        ServiceImage serviceImage = serviceImageMapper.toServiceImage(request);
        serviceImage.setService(service);
        serviceImage.setImageUrl(imageUrl);

        return serviceImageMapper.toServiceImageResponse(serviceImageRepository.save(serviceImage));
    }

    public List<ServiceImageResponse> getImagesByServiceId(Long serviceId) {
        return serviceImageRepository.findByServiceId(serviceId).stream()
                .map(serviceImageMapper::toServiceImageResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('SERVICE_IMAGE_UPDATE')")
    public ServiceImageResponse updateServiceImage(Long imageId, ServiceImageUpdateRequest request) {
        ServiceImage serviceImage = serviceImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_IMAGE_NOT_EXISTED));

        serviceImageMapper.updateServiceImage(serviceImage, request);
        return serviceImageMapper.toServiceImageResponse(serviceImageRepository.save(serviceImage));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SERVICE_IMAGE_DELETE')")
    public void deleteServiceImage(Long imageId) {
        ServiceImage serviceImage = serviceImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_IMAGE_NOT_EXISTED));

        cloudinaryService.deleteImage(serviceImage.getImageUrl());
        serviceImageRepository.delete(serviceImage);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SERVICE_IMAGE_SET_PRIMARY')")
    public void setPrimaryImage(Long serviceId, Long imageId) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new AppException(ErrorCode.SERVICE_NOT_EXISTED);
        }

        ServiceImage serviceImage = serviceImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_IMAGE_NOT_EXISTED));

        if (!serviceImage.getService().getId().equals(serviceId)) {
            throw new RuntimeException("Image " + imageId + " does not belong to service " + serviceId);
        }

        serviceImageRepository.resetAllPrimaryImagesForService(serviceId);
        serviceImageRepository.setNewPrimaryImage(imageId, serviceId);
    }
}
