import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
  type AxiosError,
} from "axios";
import {
  API_BASE_URL,
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
  type RequestOptions,
} from "@/constants/config";

type RefreshFunction = (
  refreshToken: string
) => Promise<{ accessToken: string; refreshToken?: string } | null>;

class AuthHttpClient {
  private axiosInstance: AxiosInstance;
  private isRefreshing: boolean = false;
  private pendingQueue: Array<(token: string | null) => void> = [];
  private doRefresh: RefreshFunction;

  constructor(refreshFn: RefreshFunction) {
    this.doRefresh = refreshFn;

    // Create axios instance
    this.axiosInstance = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        "Content-Type": "application/json",
      },
    });

    // Request interceptor - add auth token
    this.axiosInstance.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = this.getAccessToken();
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        // Let axios handle FormData content-type automatically
        if (config.data instanceof FormData) {
          delete config.headers["Content-Type"];
        }

        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor - handle 401 and token refresh
    this.axiosInstance.interceptors.response.use(
      (response: AxiosResponse) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & {
          _retry?: boolean;
        };

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          const newToken = await this.refreshTokenIfNeeded();
          if (newToken) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return this.axiosInstance(originalRequest);
          }
        }

        return Promise.reject(error);
      }
    );
  }

  private getAccessToken(): string | null {
    return typeof window !== "undefined"
      ? localStorage.getItem(ACCESS_TOKEN_KEY)
      : null;
  }

  private getRefreshToken(): string | null {
    return typeof window !== "undefined"
      ? localStorage.getItem(REFRESH_TOKEN_KEY)
      : null;
  }

  private setTokens(accessToken: string, refreshToken?: string) {
    if (typeof window === "undefined") return;
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    if (refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    }
  }

  private clearTokens() {
    if (typeof window === "undefined") return;
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }

  private async refreshTokenIfNeeded(): Promise<string | null> {
    const currentToken = this.getAccessToken();
    if (currentToken) return currentToken;

    if (this.isRefreshing) {
      return new Promise<string | null>((resolve) =>
        this.pendingQueue.push(resolve)
      );
    }

    this.isRefreshing = true;
    try {
      const refreshToken = this.getRefreshToken();
      if (!refreshToken) {
        this.clearTokens();
        this.flushQueue(null);
        return null;
      }
      const res = await this.doRefresh(refreshToken);
      if (res?.accessToken) {
        this.setTokens(res.accessToken, res.refreshToken);
        this.flushQueue(res.accessToken);
        return res.accessToken;
      }
      this.clearTokens();
      this.flushQueue(null);
      return null;
    } finally {
      this.isRefreshing = false;
    }
  }

  private flushQueue(token: string | null) {
    this.pendingQueue.forEach((resolve) => resolve(token));
    this.pendingQueue = [];
  }

  async request<T = any>(
    path: string,
    options: RequestOptions = {}
  ): Promise<T> {
    const config: AxiosRequestConfig = {
      url: path,
      method: options.method || "GET",
      headers: options.headers,
      data: options.body,
      signal: options.signal,
    };

    try {
      const response = await this.axiosInstance.request<T>(config);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage =
          error.response?.data?.message ||
          error.message ||
          `HTTP ${error.response?.status}`;
        throw new Error(errorMessage);
      }
      throw error;
    }
  }
}

// Simple HTTP client for public APIs (no authentication)
class SimpleHttpClient {
  private axiosInstance: AxiosInstance;

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        "Content-Type": "application/json",
      },
      withCredentials: true, // Enable sending cookies for session cart management
    });
  }

  async request<T = any>(
    path: string,
    options: RequestOptions = {}
  ): Promise<T> {
    const config: AxiosRequestConfig = {
      url: path,
      method: options.method || "GET",
      headers: options.headers,
      data: options.body,
      signal: options.signal,
    };

    try {
      const response = await this.axiosInstance.request<T>(config);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage =
          error.response?.data?.message ||
          error.message ||
          `HTTP ${error.response?.status}`;
        throw new Error(errorMessage);
      }
      throw error;
    }
  }
}

// Exports
export default AuthHttpClient;
export const simpleHttp = new SimpleHttpClient();
