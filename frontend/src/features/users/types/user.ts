// User Response Types
export interface UserResponse {
  id: string;
  username: string;
  email?: string;
  fullName?: string;
  phoneNumber?: string;
  roles?: import('./role').RoleResponse[];
}

// User Request Types
export interface UserCreationRequest {
  username: string;
  password: string;
  email?: string;
  fullName?: string;
  phoneNumber?: string;
}

export interface UserUpdateRequest extends Partial<UserCreationRequest> {
  roles?: string[];
}
