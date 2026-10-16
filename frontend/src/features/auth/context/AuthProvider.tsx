import React, {
  useState,
  useEffect,
  useRef,
  ReactNode,
} from "react";
import { UserResponse } from "@/features/users/types";
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from "@/constants/config";
import { UsersApi } from "@/features/users/api/users";
import { AuthContext, type AuthContextType } from "./auth-context";

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const authEpoch = useRef(0);

  const isAuthenticated = !!user;

  useEffect(() => {
    let cancelled = false;
    const epoch = authEpoch.current;
    // Restore user session on page refresh
    const restoreSession = async () => {
      const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
      const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);

      if (accessToken && refreshToken) {
        try {
          // Fetch user info to restore session
          const response = await UsersApi.getMyInfo();
          const userData = response.result ?? null;
          if (!cancelled && epoch === authEpoch.current) setUser(userData);
        } catch (error) {
          console.error("Failed to restore session:", error);
          // Clear invalid tokens
          if (!cancelled && epoch === authEpoch.current) {
            localStorage.removeItem(ACCESS_TOKEN_KEY);
            localStorage.removeItem(REFRESH_TOKEN_KEY);
          }
        }
      }
      if (!cancelled && epoch === authEpoch.current) setIsLoading(false);
    };

    void restoreSession();
    return () => { cancelled = true; };
  }, []);

  const login = React.useCallback((
    userData: UserResponse,
    accessToken: string,
    refreshToken: string
  ) => {
    authEpoch.current += 1;
    setUser(userData);
    setIsLoading(false);
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }, []);

  const logout = React.useCallback(() => {
    authEpoch.current += 1;
    setUser(null);
    setIsLoading(false);
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }, []);

  const updateUser = React.useCallback((userData: UserResponse) => {
    setUser(userData);
  }, []);

  const value: AuthContextType = React.useMemo(() => ({
    user,
    isAuthenticated,
    isLoading,
    login,
    logout,
    updateUser,
  }), [user, isAuthenticated, isLoading, login, logout, updateUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

