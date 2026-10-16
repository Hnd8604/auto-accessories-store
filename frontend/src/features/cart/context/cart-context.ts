import { createContext } from "react";
import type { CartResponse } from "../types";

export interface CartContextType {
  cart: CartResponse | null;
  cartId: number | null;
  sessionCart: Record<number, number>;
  isLoading: boolean;
  isError: boolean;
  retry: () => void;
  isAuthenticated: boolean;
  addToCart: (productId: number, quantity?: number) => Promise<void>;
  removeFromCart: (itemId: number) => Promise<void>;
  updateQuantity: (itemId: number, quantity: number) => Promise<void>;
  clearCart: () => void;
  itemCount: number;
  totalPrice: number;
}

export const CartContext = createContext<CartContextType | undefined>(undefined);
