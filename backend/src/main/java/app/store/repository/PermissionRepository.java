package app.store.repository;

import app.store.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import app.store.entity.Permission;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
   List<Permission> findByNameIn(Set<String> names);

   @Query("SELECT p FROM Role r JOIN r.permissions p WHERE r = :role")
   List<Permission> getAllByRole(@Param("role") Role role);

}
