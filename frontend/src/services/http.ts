import axios from "axios";
import { API_BASE_URL } from "@/constants/config";
import AuthHttpClient from "./axios";

type RefreshPayload = { accessToken: string; refreshToken?: string };

async function refreshCall(refreshToken: string): Promise<RefreshPayload | null> {
  try {
    // The refresh request must bypass the authenticated client's interceptor.
    const response = await axios.post<
      { result?: RefreshPayload } | RefreshPayload
    >(`${API_BASE_URL}/auth/refresh`, { refreshToken }, {
      headers: { "Content-Type": "application/json" },
    });
    const data = response.data;
    const payload = "result" in data ? data.result : data;
    return payload && "accessToken" in payload && payload.accessToken
      ? payload
      : null;
  } catch {
    return null;
  }
}

export const http = new AuthHttpClient(refreshCall);

export const refreshAccessToken = () => http.refreshAccessToken();
