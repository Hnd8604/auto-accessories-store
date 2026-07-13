package app.store.controller;

import app.store.constant.ResponseMessage;
import app.store.dto.request.user.UserCreationRequest;
import app.store.dto.request.user.UserUpdateRequest;
import app.store.dto.response.auth.ApiResponse;
import app.store.dto.response.user.UserResponse;
import app.store.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import static app.store.utils.SortUtils.buildSort;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "User Management", description = "APIs for managing users including CRUD operations and profile management")
public class UserController {
    UserService UserService;

    @PostMapping
    @Operation(
        summary = "Create a new user",
        description = "Creates a new user account. Only accessible by admin users. Requires validation for username, email, and password."
    )
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(UserService.createUser(request))
                .message(ResponseMessage.CREATE_USER_SUCCESS)
                .build();
    }

    @PutMapping("/{userId}")
    @Operation(
        summary = "Update user",
        description = "Updates an existing user by ID. Only accessible by admin users."
    )
    ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(UserService.updateUser(userId, request))
                .message(ResponseMessage.UPDATE_USER_SUCCESS)
                .build();
    }

    @DeleteMapping("/{userId}")
    @Operation(
        summary = "Delete user",
        description = "Permanently deletes a user by ID. Only accessible by admin users."
    )
    ApiResponse<Void> deleteUser(@PathVariable String userId) {
        UserService.deleteUser(userId);
      return ApiResponse.<Void>builder()
                .message(ResponseMessage.DELETE_USER_SUCCESS)
                .build();
    }

    @GetMapping
    @Operation(
        summary = "Get all users",
        description = "Retrieves all users with pagination and sorting. Only accessible by admin users."
    )
    ApiResponse<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "username,asc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        return ApiResponse.<Page<UserResponse>>builder()
                .result(UserService.getAllUsers(pageable))
                .message(ResponseMessage.GET_ALL_USERS_SUCCESS)
                .build();
    }


    @GetMapping("/{userId}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves detailed information of a user by ID. Only accessible by admin users."
    )
    ApiResponse<UserResponse> getUserById(@PathVariable String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(UserService.getUserById(userId))
                .message(ResponseMessage.GET_USER_SUCCESS)
                .build();
    }

    @GetMapping("/my-info")
    @Operation(
        summary = "Get my profile",
        description = "Retrieves the authenticated user's profile information. Accessible by any authenticated user."
    )
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(UserService.getMyInfo())
                .message(ResponseMessage.GET_MY_INFO_SUCCESS)
                .build();
    }
}
