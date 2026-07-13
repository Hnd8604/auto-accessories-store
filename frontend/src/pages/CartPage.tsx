import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Minus, Plus, X, ShoppingCart, ArrowLeft, Image as ImageIcon, Trash2 } from "lucide-react";
import { useCart } from "@/context/cart-context";
import { useState, useEffect, useMemo, useCallback } from "react";
import { Checkout } from "@/features/cart/components/Checkout";
import { useNavigate } from "react-router-dom";
import { ProductImagesApi, ProductsApi } from "@/features/products/api";
import { SessionCartsApi } from "@/features/cart/api/session-carts";
import { useQueryClient } from "@tanstack/react-query";

const formatPrice = (price: number) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(price);
};

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
  const { cart, sessionCart, isAuthenticated, itemCount, updateQuantity, removeFromCart, clearCart } = useCart();
  const queryClient = useQueryClient();
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [productImages, setProductImages] = useState<Record<number, string>>({});
  const [sessionProducts, setSessionProducts] = useState<SessionProduct[]>([]);
  const navigate = useNavigate();

  const cartItems = useMemo(() => cart?.items || [], [cart?.items]);
  const totalPrice = useMemo(() => 
    cartItems.reduce((sum, item) => sum + item.totalPrice, 0),
    [cartItems]
  );

  // Fetch session cart products for guest users
  useEffect(() => {
    const fetchSessionProducts = async () => {
      if (isAuthenticated || Object.keys(sessionCart).length === 0) {
        setSessionProducts([]);
        return;
      }

      const products = await Promise.all(
        Object.entries(sessionCart).map(async ([productIdStr, quantity]) => {
          try {
            const productId = Number.parseInt(productIdStr, 10);
            const productResponse = await ProductsApi.getById(productId);
            const product = productResponse?.result;

            if (product) {
              // Fetch product image
              const imageResponse = await ProductImagesApi.getByProductId(productId);
              const images = imageResponse?.result || [];
              const primaryImage = images.find(img => img.isPrimary);

              return {
                id: productId,
                productId,
                productName: product.name,
                quantity: Number(quantity),
                unitPrice: product.unitPrice,
                totalPrice: product.unitPrice * Number(quantity),
                imageUrl: primaryImage?.imageUrl,
              };
            }
            return null;
          } catch (error) {
            console.error(`Failed to fetch product ${productIdStr}:`, error);
            return null;
          }
        })
      );

      setSessionProducts(products.filter(p => p !== null));
    };

    fetchSessionProducts();
  }, [sessionCart, isAuthenticated]);

  // Fetch product images for logged-in user cart
  useEffect(() => {
    const fetchImages = async () => {
      if (!isAuthenticated || cartItems.length === 0) return;
      
      const images: Record<number, string> = {};
      await Promise.all(
        cartItems.map(async (item) => {
          try {
            const response = await ProductImagesApi.getByProductId(item.productId);
            const productImages = response?.result || [];
            const primaryImage = productImages.find(img => img.isPrimary);
            if (primaryImage?.imageUrl) {
              images[item.productId] = primaryImage.imageUrl;
            }
          } catch (error) {
            console.error(`Failed to fetch image for product ${item.productId}:`, error);
          }
        })
      );
      setProductImages(images);
    };
    
    fetchImages();
  }, [cartItems.length, isAuthenticated]);

  const handleUpdateQuantity = useCallback(async (itemId: number, newQuantity: number) => {
    if (newQuantity === 0) {
      await removeFromCart(itemId);
      return;
    }
    await updateQuantity(itemId, newQuantity);
  }, [removeFromCart, updateQuantity]);

  const handleRemoveItem = useCallback(async (itemId: number) => {
    await removeFromCart(itemId);
  }, [removeFromCart]);

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
    } catch (error) {
      console.error("Failed to update session cart:", error);
    }
  }, [sessionCart, queryClient]);

  const handleSessionRemoveItem = useCallback(async (productId: number) => {
    try {
      await SessionCartsApi.remove(productId);
      // Refetch session cart to update UI
      await queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
    } catch (error) {
      console.error("Failed to remove from session cart:", error);
    }
  }, [queryClient]);

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
        {!isAuthenticated && sessionProducts.length > 0 ? (
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
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Phí vận chuyển</span>
                    <span className="font-medium">Miễn phí</span>
                  </div>
                  <Separator />
                  <div className="flex justify-between text-lg">
                    <span className="font-semibold">Tổng cộng</span>
                    <span className="font-bold text-primary">
                      {sessionProducts.reduce((sum, item) => sum + item.totalPrice, 0).toLocaleString('vi-VN')} ₫
                    </span>
                  </div>
                </div>

                <div className="space-y-3">
                  <Button 
                    variant="luxury" 
                    className="w-full"
                    onClick={() => navigate('/auth')}
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
                  
                  <div className="flex justify-between text-base">
                    <span className="text-muted-foreground">Phí vận chuyển:</span>
                    <span className="font-medium">Miễn phí</span>
                  </div>

                  <Separator />

                  <div className="flex justify-between items-center">
                    <span className="text-lg font-semibold">Tổng cộng:</span>
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

                  <div className="pt-4 border-t">
                    <p className="text-sm text-muted-foreground text-center">
                      Miễn phí vận chuyển cho đơn hàng trên 500.000đ
                    </p>
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
