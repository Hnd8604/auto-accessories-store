import { type ReactNode, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CartsApi } from "@/features/cart/api";
import { SessionCartsApi } from "@/features/cart/api/session-carts";
import { useAuth } from "@/features/auth/hooks/useAuth";
import type { CartItemRequest } from "@/features/cart/types";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/utils/errors";
import { CartContext } from "./cart-context";

const EMPTY_SESSION_CART: Record<number, number> = {};

export const CartProvider = ({ children }: { children: ReactNode }) => {
  const { user, isAuthenticated } = useAuth();
  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Fetch session cart - always fetch to check if there's data to merge on login
  const { data: sessionCartData, isLoading: sessionLoading, isError: sessionError, refetch: refetchSession } = useQuery({
    queryKey: ["sessionCart"],
    queryFn: SessionCartsApi.view,
    // Fetch on mount and when authentication changes
    staleTime: 0,
  });

  // Fetch user's cart for logged-in users
  const { data: cartData, isLoading: cartLoading, isError: cartError, refetch: refetchCart } = useQuery({
    queryKey: ["cart", user?.id],
    queryFn: CartsApi.getMyCart,
    enabled: !!user?.id,
  });

  const sessionCart = sessionCartData ?? EMPTY_SESSION_CART;
  const cart = cartData?.result ?? null;
  const cartId = cart?.id ?? null;
  const isLoading = isAuthenticated ? cartLoading : sessionLoading;
  const isError = isAuthenticated ? cartError : sessionError;
  const retry = () => { void (isAuthenticated ? refetchCart() : refetchSession()); };
  const cartItems = useMemo(() => cart?.items || [], [cart]);
  
  // Calculate item count: count unique products, not total quantity
  const itemCount = useMemo(() => 
    isAuthenticated 
      ? cartItems.length
      : Object.keys(sessionCart).length,
    [isAuthenticated, cartItems.length, sessionCart]
  );
  
  const totalPrice = useMemo(() => 
    cartItems.reduce((sum, item) => sum + item.totalPrice, 0),
    [cartItems]
  );

  // Add item mutation
  const addItemMutation = useMutation({
    mutationFn: (payload: CartItemRequest) => CartsApi.addItem(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cart", user?.id] });
      toast({
        title: "Thành công",
        description: "Đã thêm sản phẩm vào giỏ hàng",
      });
    },
    onError: (error) => {
      toast({
        title: "Lỗi",
        description: error?.message || "Không thể thêm sản phẩm vào giỏ hàng",
        variant: "destructive",
      });
    },
  });

  // Remove item mutation
  const removeItemMutation = useMutation({
    mutationFn: ({ itemId }: { itemId: number }) =>
      cartId ? CartsApi.removeItem(cartId, itemId) : Promise.reject(new Error("Cart ID not found")),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cart", user?.id] });
      toast({
        title: "Đã xóa",
        description: "Đã xóa sản phẩm khỏi giỏ hàng",
      });
    },
  });

  // Update quantity mutation
  const updateQuantityMutation = useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: number; quantity: number }) =>
      CartsApi.updateItem(itemId, { quantity }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cart", user?.id] });
    },
  });

  const addToCart = async (productId: number, quantity = 1) => {
    // If user is not logged in, add to session cart
    if (!isAuthenticated || !user?.id) {
      try {
        await SessionCartsApi.add(productId, quantity);
        await queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
        
        toast({
          title: "Thành công",
          description: "Đã thêm sản phẩm vào giỏ hàng",
        });
      } catch (error: unknown) {
        console.error("Failed to add to session cart:", error);
        toast({
          title: "Lỗi",
          description: getErrorMessage(error, "Không thể thêm sản phẩm vào giỏ hàng"),
          variant: "destructive",
        });
      }
      return;
    }

    // If user is logged in, add to user cart
    try {
      await addItemMutation.mutateAsync({
        productId,
        quantity,
      });
    } catch (error) {
      console.error("Failed to add item:", error);
    }
  };

  const removeFromCart = async (itemId: number) => {
    await removeItemMutation.mutateAsync({ itemId });
  };

  const updateQuantity = async (itemId: number, quantity: number) => {
    await updateQuantityMutation.mutateAsync({ itemId, quantity });
  };

  const clearCart = () => {
    queryClient.invalidateQueries({ queryKey: ["cart", user?.id] });
  };

  const contextValue = {
    cart,
    cartId,
    sessionCart,
    isLoading,
    isError,
    retry,
    isAuthenticated,
    addToCart,
    removeFromCart,
    updateQuantity,
    clearCart,
    itemCount,
    totalPrice,
  };

  return (
    <CartContext.Provider value={contextValue}>
      {children}
    </CartContext.Provider>
  );
};

