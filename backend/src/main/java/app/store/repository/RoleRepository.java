package app.store.repository;

import app.store.entity.Role;
import app.store.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
//    Optional<Role> findByName(String name);
//    List<Role> findByNameIn(Set<String> names);

}
