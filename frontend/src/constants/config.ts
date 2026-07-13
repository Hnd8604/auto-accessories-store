export const API_BASE_URL: string =
  (import.meta as any)?.env?.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

export const ACCESS_TOKEN_KEY = "access_token";
export const REFRESH_TOKEN_KEY = "refresh_token";

// Google OAuth2 Configuration
export const GOOGLE_CLIENT_ID: string =
  (import.meta as any)?.env?.VITE_GOOGLE_CLIENT_ID || "990160635393-5s2c8e26gt5m459ib69su6l1a5r52vca.apps.googleusercontent.com";
export const GOOGLE_REDIRECT_URI: string =
  (import.meta as any)?.env?.VITE_GOOGLE_REDIRECT_URI || "http://localhost:3000/auth/google/callback";

// Cloudinary Configuration
// Note: These should be moved to environment variables in production
export const CLOUDINARY_CONFIG = {
  cloudName: "dsgftzhzt",
  apiKey: "991924558367536",
  apiSecret: "HR2QU9_z_Bzvk4lxaoAR_U69dTQ",
  uploadPreset: "ml_default", // You need to create an unsigned upload preset in Cloudinary dashboard
  // Alternative: use signed upload via backend API for better security
};

export type HttpHeaders = Record<string, string>;

export interface RequestOptions {
  method?: string;
  headers?: HttpHeaders;
  body?: any;
  signal?: AbortSignal;
}
