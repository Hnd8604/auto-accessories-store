import { simpleHttp } from "@/services/axios";
import { http } from "@/services/http";
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from "@/constants/config";
import { UsersApi } from "@/features/users/api/users";
import type { ApiResponse } from "@/types";
import type {
  UserCreationRequest,
  UserResponse,
} from "@/features/users/types";
import type {
  AuthenticationRequest,
  AuthenticationResponse,
  ChangePasswordRequest,
} from "@/features/auth/types";

/**
 * Backend khi thì bọc payload trong { result }, khi thì trả thẳng. Helper này
 * gỡ lớp bọc đó mà không cần cast `any`.
 */
function unwrapResult<T>(data: { result?: T } | T): T {
  return (
    data && typeof data === "object" && "result" in data
      ? (data as { result?: T }).result
      : data
  ) as T;
}

async function completeLogin(response: AuthenticationResponse) {
  if (typeof window !== "undefined" && response.accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken || "");
  }

  try {
    const userResponse = await UsersApi.getMyInfo();
    const user = unwrapResult<UserResponse>(userResponse);
    return { ...response, user: user ?? response.user };
  } catch (error) {
    console.error("Failed to fetch user roles:", error);
    return response;
  }
}

export const AuthService = {
  async login(payload: AuthenticationRequest) {
    // Step 1: Authenticate and get tokens
    // Use simpleHttp to send session cookie for cart sync
    const data = await simpleHttp.request<
      { result?: AuthenticationResponse } | AuthenticationResponse
    >("/auth/login", { method: "POST", body: payload });
    const response = unwrapResult<AuthenticationResponse>(data);

    return completeLogin(response);
  },

  async register(payload: UserCreationRequest) {
    return simpleHttp.request<ApiResponse<UserResponse>>("/auth/register", {
      method: "POST",
      body: payload,
    });
  },

  async logout() {
    try {
      const accessToken =
        typeof window !== "undefined"
          ? localStorage.getItem(ACCESS_TOKEN_KEY)
          : null;
      const refreshToken =
        typeof window !== "undefined"
          ? localStorage.getItem(REFRESH_TOKEN_KEY)
          : null;
      await http.request("/auth/logout", {
        method: "POST",
        body: { accessToken, refreshToken },
      });
    } finally {
      if (typeof window !== "undefined") {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
      }
    }
  },

  async changePassword(payload: ChangePasswordRequest) {
    return http.request("/auth/password/change", {
      method: "PUT",
      body: payload,
    });
  },

  async googleLogin(code: string) {
    // Step 1: Send authorization code to backend
    const data = await simpleHttp.request<
      { result?: AuthenticationResponse } | AuthenticationResponse
    >("/auth/google", { method: "POST", body: { code } });
    const response = unwrapResult<AuthenticationResponse>(data);

    return completeLogin(response);
  },
};
