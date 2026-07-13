package app.store.repository;

import app.store.entity.ServiceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceImageRepository extends JpaRepository<ServiceImage, Long> {

    @Query("SELECT i FROM ServiceImage i WHERE i.service.id = :serviceId ORDER BY i.sortOrder ASC NULLS LAST, i.createdAt ASC")
    List<ServiceImage> findByServiceId(@Param("serviceId") Long serviceId);

    @Modifying
    @Query("UPDATE ServiceImage i SET i.isPrimary = false WHERE i.service.id = :serviceId")
    void resetAllPrimaryImagesForService(@Param("serviceId") Long serviceId);

    @Modifying
    @Query("UPDATE ServiceImage i SET i.isPrimary = true WHERE i.id = :imageId AND i.service.id = :serviceId")
    void setNewPrimaryImage(@Param("imageId") Long imageId, @Param("serviceId") Long serviceId);
}
