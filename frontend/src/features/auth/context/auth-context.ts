import { createContext } from "react";
import type { UserResponse } from "@/features/users/types";

export interface AuthContextType {
  user: UserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (user: UserResponse, accessToken: string, refreshToken: string) => void;
  logout: () => void;
  updateUser: (user: UserResponse) => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
