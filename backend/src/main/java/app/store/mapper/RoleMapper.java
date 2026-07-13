package app.store.mapper;

import app.store.dto.request.RoleRequest;
import app.store.dto.response.RoleResponse;
import app.store.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true) // Ignore permissions during role creation so we have to set them manually
    Role toRole(RoleRequest request);
    RoleResponse toRoleResponse(Role role);

    @Named("toRoleResponseWithoutPermissions")
    @Mapping(target = "permissions", ignore = true)
    RoleResponse toRoleResponseWithoutPermissions(Role role);

    @Mapping(target = "permissions", ignore = true) // Ignore roles during update
    void updateRole(@MappingTarget Role role, RoleRequest request);
}
