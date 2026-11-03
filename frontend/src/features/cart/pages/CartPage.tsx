import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Minus, Plus, X, ShoppingCart, ArrowLeft, Image as ImageIcon, Trash2 } from "lucide-react";
import { useCart } from "@/features/cart/hooks/useCart";
import { useState, useMemo, useCallback } from "react";
import { Checkout } from "@/features/cart/components/Checkout";
import { useNavigate } from "react-router-dom";
import { ProductsApi } from "@/features/products/api";
import { SessionCartsApi } from "@/features/cart/api/session-carts";
import { useQueries, useQueryClient } from "@tanstack/react-query";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/utils/errors";
import { formatPrice } from "@/utils/formatPrice";
import { useProductPrimaryImages } from "@/features/products/hooks/useProductPrimaryImages";

interface SessionProduct {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  imageUrl?: string;
}

export default function CartPage() {
  const { cart, sessionCart, isAuthenticated, isLoading, isError, retry, itemCount, totalPrice, updateQuantity, removeFromCart, clearCart } = useCart();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const navigate = useNavigate();

  const cartItems = useMemo(() => cart?.items || [], [cart?.items]);
  const sessionEntries = Object.entries(sessionCart);
  const sessionProductQueries = useQueries({
    queries: !isAuthenticated ? sessionEntries.map(([productId]) => ({
      queryKey: ["sessionProduct", productId],
      queryFn: async () => {
        const id = Number(productId);
        const productResponse = await ProductsApi.getById(id);
        const product = productResponse.result;
        if (!product) return null;
        return {
          id,
          productId: id,
          productName: product.name,
          unitPrice: product.unitPrice,
        };
      },
      staleTime: 5 * 60 * 1000,
    })) : [],
  });
  const { imageUrls: sessionProductImages } = useProductPrimaryImages(
    !isAuthenticated ? sessionEntries.map(([productId]) => Number(productId)) : []
  );
  const sessionProducts: SessionProduct[] = sessionEntries.flatMap(([_, quantity], index) => {
    const product = sessionProductQueries[index]?.data;
    return product ? [{
      ...product,
      imageUrl: sessionProductImages[product.productId],
      quantity,
      totalPrice: product.unitPrice * quantity,
    }] : [];
  });

  const { imageUrls: productImages } = useProductPrimaryImages(
    isAuthenticated ? cartItems.map((item) => item.productId) : []
  );
  const isCartDataLoading = isLoading || (!isAuthenticated && sessionProductQueries.some((query) => query.isPending));

  const handleUpdateQuantity = useCallback(async (itemId: number, newQuantity: number) => {
    try {
      if (newQuantity === 0) {
        await removeFromCart(itemId);
      } else {
        await updateQuantity(itemId, newQuantity);
      }
    } catch (error) {
      toast({
        title: "Lỗi",
        description: getErrorMessage(error, "Không thể cập nhật giỏ hàng"),
        variant: "destructive",
      });
      await queryClient.invalidateQueries({ queryKey: ["cart"] });
    }
  }, [removeFromCart, updateQuantity, queryClient, toast]);

  const handleRemoveItem = useCallback(async (itemId: number) => {
    try {
      await removeFromCart(itemId);
    } catch (error) {
      toast({
        title: "Lỗi",
        description: getErrorMessage(error, "Không thể xóa sản phẩm khỏi giỏ hàng"),
        variant: "destructive",
      });
    }
  }, [removeFromCart, toast]);

  // Session cart handlers
  const handleSessionUpdateQuantity = useCallback(async (productId: number, newQuantity: number) => {
    try {
      if (newQuantity === 0) {
        await SessionCartsApi.remove(productId);
      } else {
        // Calculate delta: difference between new and current quantity
        const currentQuantity = sessionCart[productId] || 0;
        const delta = newQuantity - currentQuantity;
        
        if (delta !== 0) {
          await SessionCartsApi.add(productId, delta);
        }
      }
      // Refetch session cart to update UI
      await queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
    } catch (error: unknown) {
      console.error("Failed to update session cart:", error);
      toast({
        title: "Lỗi",
        description: getErrorMessage(error, "Không thể cập nhật số lượng"),
        variant: "destructive",
      });
      // Đồng bộ lại để UI quay về số lượng thật thay vì giữ giá trị vừa bấm
      await queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
    }
  }, [sessionCart, queryClient, toast]);

  const handleSessionRemoveItem = useCallback(async (productId: number) => {
    try {
      await SessionCartsApi.remove(productId);
      // Refetch session cart to update UI
      await queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
    } catch (error: unknown) {
      console.error("Failed to remove from session cart:", error);
      toast({
        title: "Lỗi",
        description: getErrorMessage(error, "Không thể xóa sản phẩm khỏi giỏ hàng"),
        variant: "destructive",
      });
    }
  }, [queryClient, toast]);

  const handleOrderComplete = useCallback(() => {
    clearCart();
    setIsCheckoutOpen(false);
  }, [clearCart]);

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-7xl">
        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-3xl font-bold flex items-center gap-2">
              <ShoppingCart className="h-8 w-8" />
              Giỏ Hàng
            </h1>
            <p className="text-muted-foreground mt-1">
              {itemCount} sản phẩm trong giỏ hàng
            </p>
          </div>
        </div>

        {/* Guest User with Session Cart */}
        {isError || (!isAuthenticated && sessionProductQueries.some((query) => query.isError)) ? (
          <Card className="p-12 text-center space-y-4" role="alert">
            <p>Không thể tải giỏ hàng. Vui lòng thử lại.</p>
            <Button variant="outline" onClick={() => {
              retry();
              void queryClient.invalidateQueries({ queryKey: ["sessionProduct"] });
            }}>Thử lại</Button>
          </Card>
        ) : isCartDataLoading ? (
          <Card className="p-12 text-center" role="status">Đang tải giỏ hàng...</Card>
        ) : !isAuthenticated && sessionProducts.length > 0 ? (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Session Cart Items Section */}
            <div className="lg:col-span-2 space-y-4">
              {sessionProducts.map((item) => (
                <Card key={item.id} className="p-6">
                  <div className="flex gap-6">
                    {/* Product Image */}
                    <div className="relative w-32 h-32 rounded-lg overflow-hidden bg-muted flex-shrink-0">
                      {item.imageUrl ? (
                        <img
                          src={item.imageUrl}
                          alt={item.productName}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <ImageIcon className="h-12 w-12 text-muted-foreground" />
                        </div>
                      )}
                    </div>

                    {/* Product Details */}
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-start mb-4">
                        <div className="flex-1">
                          <h3 className="text-lg font-semibold mb-2 line-clamp-2">
                            {item.productName}
                          </h3>
                          <p className="text-primary font-medium text-lg">
                            {item.unitPrice.toLocaleString('vi-VN')} ₫
                          </p>
                        </div>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleSessionRemoveItem(item.productId)}
                        >
                          <Trash2 className="h-5 w-5 text-destructive" />
                        </Button>
                      </div>

                      {/* Quantity Controls */}
                      <div className="flex items-center gap-4">
                        <div className="flex items-center gap-2">
                          <Button
                            variant="outline"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => handleSessionUpdateQuantity(item.productId, item.quantity - 1)}
                          >
                            <Minus className="h-4 w-4" />
                          </Button>
                          <span className="w-12 text-center font-medium">{item.quantity}</span>
                          <Button
                            variant="outline"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => handleSessionUpdateQuantity(item.productId, item.quantity + 1)}
                          >
                            <Plus className="h-4 w-4" />
                          </Button>
                        </div>
                        <div className="text-right flex-1">
                          <p className="text-sm text-muted-foreground">Tổng giá</p>
                          <p className="text-lg font-semibold text-primary">
                            {item.totalPrice.toLocaleString('vi-VN')} ₫
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                </Card>
              ))}
            </div>

            {/* Order Summary Section for Session Cart */}
            <div className="lg:col-span-1">
              <Card className="p-6 sticky top-8">
                <h2 className="text-xl font-semibold mb-6">Thông Tin Đơn Hàng</h2>
                
                <div className="space-y-4 mb-6">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Số sản phẩm</span>
                    <span className="font-medium">{itemCount}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Tạm tính</span>
                    <span className="font-medium">
                      {sessionProducts.reduce((sum, item) => sum + item.totalPrice, 0).toLocaleString('vi-VN')} ₫
                    </span>
                  </div>
                  <Separator />
                  <div className="flex justify-between text-lg">
                    <span className="font-semibold">Tổng tiền sản phẩm</span>
                    <span className="font-bold text-primary">
                      {sessionProducts.reduce((sum, item) => sum + item.totalPrice, 0).toLocaleString('vi-VN')} ₫
                    </span>
                  </div>
                </div>

                <div className="space-y-3">
                  <Button 
                    variant="luxury" 
                    className="w-full"
                    onClick={() => navigate('/auth', { state: { from: { pathname: '/cart' } } })}
                  >
                    Đăng Nhập Để Thanh Toán
                  </Button>
                  <Button 
                    variant="outline" 
                    className="w-full"
                    onClick={() => navigate('/')}
                  >
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Tiếp Tục Mua Sắm
                  </Button>
                </div>
              </Card>
            </div>
          </div>
        ) : cartItems.length === 0 ? (
          /* Empty Cart State */
          <Card className="p-12">
            <div className="text-center">
              <ShoppingCart className="h-24 w-24 text-muted-foreground mx-auto mb-6" />
              <h2 className="text-2xl font-semibold mb-2">Giỏ hàng của bạn đang trống</h2>
              <p className="text-muted-foreground mb-6">
                Hãy thêm sản phẩm vào giỏ hàng để tiếp tục mua sắm
              </p>
              <Button 
                variant="luxury" 
                size="lg"
                onClick={() => navigate('/')}
              >
                Tiếp Tục Mua Sắm
              </Button>
            </div>
          </Card>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Cart Items Section */}
            <div className="lg:col-span-2 space-y-4">
              {cartItems.map((item) => (
                <Card key={item.id} className="p-6">
                  <div className="flex gap-6">
                    {/* Product Image */}
                    {productImages[item.productId] ? (
                      <img
                        src={productImages[item.productId]}
                        alt={item.productName || `Sản phẩm #${item.productId}`}
                        className="w-32 h-32 object-cover rounded-lg flex-shrink-0"
                      />
                    ) : (
                      <div className="w-32 h-32 bg-muted rounded-lg flex items-center justify-center flex-shrink-0">
                        <ShoppingCart className="h-12 w-12 text-muted-foreground" />
                      </div>
                    )}

                    {/* Product Details */}
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-start gap-4">
                        <h3 className="font-semibold text-lg line-clamp-2">
                          {item.productName || `Sản phẩm #${item.productId}`}
                        </h3>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-muted-foreground hover:text-destructive flex-shrink-0"
                          onClick={() => handleRemoveItem(item.id)}
                        >
                          <X className="h-5 w-5" />
                        </Button>
                      </div>

                      <div className="mt-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                        {/* Price */}
                        <div>
                          <p className="text-sm text-muted-foreground">Đơn giá</p>
                          <p className="text-lg font-bold text-primary">
                            {formatPrice(item.unitPrice)}
                          </p>
                        </div>

                        {/* Quantity Controls */}
                        <div>
                          <p className="text-sm text-muted-foreground mb-2">Số lượng</p>
                          <div className="flex items-center gap-3">
                            <Button
                              variant="outline"
                              size="icon"
                              className="h-10 w-10"
                              onClick={() => handleUpdateQuantity(item.id, item.quantity - 1)}
                            >
                              <Minus className="h-4 w-4" />
                            </Button>
                            <span className="w-12 text-center font-semibold text-lg">
                              {item.quantity}
                            </span>
                            <Button
                              variant="outline"
                              size="icon"
                              className="h-10 w-10"
                              onClick={() => handleUpdateQuantity(item.id, item.quantity + 1)}
                            >
                              <Plus className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>

                        {/* Subtotal */}
                        <div className="text-right">
                          <p className="text-sm text-muted-foreground">Thành tiền</p>
                          <p className="text-xl font-bold text-primary">
                            {formatPrice(item.totalPrice)}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                </Card>
              ))}
            </div>

            {/* Order Summary Section */}
            <div className="lg:col-span-1">
              <Card className="p-6 sticky top-24">
                <h2 className="text-xl font-semibold mb-6">Thông tin đơn hàng</h2>
                
                <div className="space-y-4">
                  <div className="flex justify-between text-base">
                    <span className="text-muted-foreground">Tạm tính:</span>
                    <span className="font-medium">{formatPrice(totalPrice)}</span>
                  </div>
                  
                  <Separator />

                  <div className="flex justify-between items-center">
                    <span className="text-lg font-semibold">Tổng tiền sản phẩm:</span>
                    <span className="text-2xl font-bold text-primary">
                      {formatPrice(totalPrice)}
                    </span>
                  </div>

                  <div className="pt-4 space-y-3">
                    <Button 
                      variant="luxury" 
                      className="w-full" 
                      size="lg"
                      onClick={() => setIsCheckoutOpen(true)}
                    >
                      Thanh Toán
                    </Button>
                    
                    <Button 
                      variant="outline" 
                      className="w-full"
                      onClick={() => navigate('/')}
                    >
                      Tiếp Tục Mua Sắm
                    </Button>
                  </div>

                </div>
              </Card>
            </div>
          </div>
        )}
      </div>

      {/* Checkout Dialog */}
      <Checkout 
        isOpen={isCheckoutOpen}
        onClose={() => setIsCheckoutOpen(false)}
        onComplete={handleOrderComplete}
      />
    </div>
  );
}
