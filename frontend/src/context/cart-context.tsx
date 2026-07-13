import {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
  useMemo,
} from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { CartsApi } from "@/features/cart/api";
import { SessionCartsApi } from "@/features/cart/api/session-carts";
import { useAuth } from "./auth-context";
import type { CartResponse, CartItemRequest } from "@/features/cart/types";
import { useToast } from "@/hooks/use-toast";

interface CartContextType {
  cart: CartResponse | null;
  cartId: number | null;
  sessionCart: Record<number, number>;
  isLoading: boolean;
  isAuthenticated: boolean;
  addToCart: (productId: number, quantity?: number) => Promise<void>;
  removeFromCart: (itemId: number) => Promise<void>;
  updateQuantity: (itemId: number, quantity: number) => Promise<void>;
  clearCart: () => void;
  itemCount: number;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const CartProvider = ({ children }: { children: ReactNode }) => {
  const { user, isAuthenticated } = useAuth();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [cartId, setCartId] = useState<number | null>(null);
  const [sessionCart, setSessionCart] = useState<Record<number, number>>({});

  // Fetch session cart - always fetch to check if there's data to merge on login
  const { data: sessionCartData } = useQuery({
    queryKey: ["sessionCart"],
    queryFn: async () => {
      try {
        return await SessionCartsApi.view();
      } catch (error) {
        console.error("Failed to fetch session cart:", error);
        return {};
      }
    },
    // Fetch on mount and when authentication changes
    staleTime: 0,
  });

  // Fetch user's cart for logged-in users
  const { data: cartData, isLoading } = useQuery({
    queryKey: ["cart", user?.id],
    queryFn: async () => {
      if (!user?.id) return null;
      try {
        const response = await CartsApi.getMyCart();
        if (response.result?.id) {
          setCartId(response.result.id);
        }
        return response;
      } catch (error) {
        console.error("Failed to fetch cart:", error);
        return null;
      }
    },
    enabled: !!user?.id,
  });

  // Update session cart state when query data changes
  useEffect(() => {
    if (sessionCartData) {
      setSessionCart(sessionCartData);
    }
  }, [sessionCartData]);

  const cart = useMemo(() => cartData?.result || null, [cartData]);
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
    onError: (error: any) => {
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
        // Refetch session cart to get latest data
        const latestCart = await queryClient.fetchQuery({
          queryKey: ["sessionCart"],
          queryFn: SessionCartsApi.view,
        });
        setSessionCart(latestCart || {});
        
        toast({
          title: "Thành công",
          description: "Đã thêm sản phẩm vào giỏ hàng",
        });
      } catch (error: any) {
        console.error("Failed to add to session cart:", error);
        toast({
          title: "Lỗi",
          description: error?.message || "Không thể thêm sản phẩm vào giỏ hàng",
          variant: "destructive",
        });
      }
      return;
    }

    // If user is logged in, add to user cart
    const targetCartId = cartId || 0;
    
    try {
      const response = await addItemMutation.mutateAsync({
        cartId: targetCartId,
        productId,
        quantity,
      });
      
      if (!cartId && response?.result?.cartId) {
        setCartId(response.result.cartId);
      }
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
    setCartId(null);
    queryClient.invalidateQueries({ queryKey: ["cart", user?.id] });
  };

  const contextValue = useMemo(
    () => ({
      cart,
      cartId,
      sessionCart,
      isLoading,
      isAuthenticated,
      addToCart,
      removeFromCart,
      updateQuantity,
      clearCart,
      itemCount,
    }),
    [cart, cartId, sessionCart, isLoading, isAuthenticated, addToCart, removeFromCart, updateQuantity, clearCart, itemCount]
  );

  return (
    <CartContext.Provider value={contextValue}>
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (context === undefined) {
    throw new Error("useCart must be used within a CartProvider");
  }
  return context;
};
