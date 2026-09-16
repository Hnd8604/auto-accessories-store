import { simpleHttp } from "@/services/axios";

export const SessionCartsApi = {
  // Add product to session cart.
  // `quantity` là delta cộng dồn vào số lượng hiện có, không phải số lượng tuyệt đối:
  // truyền số âm để giảm, giảm về 0 thì backend bỏ sản phẩm khỏi giỏ.
  // Backend từ chối nếu số lượng sau khi cộng vượt tồn kho.
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
