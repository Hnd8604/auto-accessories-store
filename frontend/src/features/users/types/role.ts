import type { PermissionResponse } from './permission';

// Role Response Types
export interface RoleResponse {
  name: string;
  description?: string;
  permissions?: PermissionResponse[];
}

// Role Request Types
export interface RoleRequest {
  name: string;
  description?: string;
  permissions?: string[];
}
