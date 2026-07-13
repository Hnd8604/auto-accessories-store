package app.store.mapper;

import app.store.dto.request.user.UserCreationRequest;
import app.store.dto.request.user.UserUpdateRequest;
import app.store.dto.response.user.UserResponse;
import app.store.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring",uses = {RoleMapper.class})
public interface UserMapper {

    User toUser(UserCreationRequest request);
    // When mapping User to UserResponse, use the custom mapping for roles to exclude permissions
    @Mapping(source = "roles", target = "roles", qualifiedByName = "toRoleResponseWithoutPermissions")
    UserResponse toUserResponse(User user);

    @Mapping(target = "id", ignore = true) // Ignore the ID field during update
    @Mapping(target = "roles", ignore = true) // Ignore role during update
    @Mapping(target = "cart", ignore = true) // Ignore cart during update
    @Mapping(target = "password", ignore = true) // Ignore password during update
    @Mapping(target = "createdAt", ignore = true) // Ignore createdAt during update
    @Mapping(target = "updatedAt", ignore = true) // Ignore updatedAt during update
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
