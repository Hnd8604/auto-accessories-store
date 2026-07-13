import { UserResponse } from "../../users/types";

export const ROLES = {
  ADMIN: "ADMIN",
  USER: "USER",
} as const;

export type RoleName = (typeof ROLES)[keyof typeof ROLES];

/**
 * Check if user has a specific role
 */
export const hasRole = (
  user: UserResponse | null | undefined,
  roleName: string
): boolean => {
  if (!user?.roles || user.roles.length === 0) return false;
  return user.roles.some(role => role.name?.toUpperCase() === roleName.toUpperCase());
};

/**
 * Check if user is admin
 */
export const isAdmin = (user: UserResponse | null | undefined): boolean => {
  return hasRole(user, ROLES.ADMIN);
};

/**
 * Check if user is regular user
 */
export const isUser = (user: UserResponse | null | undefined): boolean => {
  return hasRole(user, ROLES.USER);
};

/**
 * Get user role names
 */
export const getUserRoles = (
  user: UserResponse | null | undefined
): string[] => {
  return user?.roles?.map(role => role.name).filter(Boolean) || [];
};

/**
 * Get user primary role name (first role)
 */
export const getUserRole = (
  user: UserResponse | null | undefined
): string | null => {
  return user?.roles?.[0]?.name || null;
};
