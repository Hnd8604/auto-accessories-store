import { simpleHttp } from "@/services/axios";

export const SessionCartsApi = {
  // Add product to session cart
  add: async (productId: number, quantity: number = 1) => {
    const response = await simpleHttp.request<Record<number, number>>(
      `/session-carts/add?productId=${productId}&qty=${quantity}`,
      { method: "POST" }
    );
    return response || {};
  },

  // View session cart
  view: async () => {
    const response = await simpleHttp.request<Record<number, number>>("/session-carts");
    return response || {};
  },

  // Remove product from session cart
  remove: async (productId: number) => {
    const response = await simpleHttp.request<Record<number, number>>(
      `/session-carts/remove/${productId}`,
      { method: "DELETE" }
    );
    return response || {};
  },


};
