import { useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { AuthService } from "../api/auth";
import { useAuth } from "./useAuth";

export function useLogout() {
  const { logout } = useAuth();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useCallback(async () => {
    let serverLogoutSucceeded = true;
    try {
      await AuthService.logout();
    } catch (error) {
      console.error("Logout error:", error);
      serverLogoutSucceeded = false;
    } finally {
      logout();
      queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== "sessionCart" });
      await queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
      navigate("/");
    }
    return serverLogoutSucceeded;
  }, [logout, navigate, queryClient]);
}
