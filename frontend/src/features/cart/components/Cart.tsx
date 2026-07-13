import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ShoppingCart, Minus, Plus, X, Loader2 } from "lucide-react";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { Checkout } from "@/features/cart/components/Checkout";
import { useCart } from "@/context/cart-context";

export const Cart = () => {
  const { cart, itemCount, updateQuantity, removeFromCart, isLoading, clearCart } = useCart();
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(price);
  };

  const cartItems = cart?.items || [];
  const totalPrice = cartItems.reduce((sum, item) => sum + item.totalPrice, 0);

  const handleUpdateQuantity = async (itemId: number, newQuantity: number) => {
    if (newQuantity === 0) {
      await removeFromCart(itemId);
      return;
    }
    await updateQuantity(itemId, newQuantity);
  };

  const handleRemoveItem = async (itemId: number) => {
    await removeFromCart(itemId);
  };

  const handleOrderComplete = () => {
    clearCart();
    setIsCheckoutOpen(false);
  };

  return (
    <>
      <Sheet>
        <SheetTrigger asChild>
          <Button variant="outline" size="sm" className="relative" disabled={isLoading}>
            {isLoading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <>
                <ShoppingCart className="h-4 w-4" />
                {itemCount > 0 && (
                  <Badge 
                    variant="destructive" 
                    className="absolute -top-2 -right-2 h-5 w-5 flex items-center justify-center p-0 text-xs"
                  >
                    {itemCount}
                  </Badge>
                )}
              </>
            )}
            <span className="ml-2 hidden sm:inline">Giỏ Hàng</span>
          </Button>
        </SheetTrigger>
        
        <SheetContent side="right" className="w-full max-w-md">
          <SheetHeader>
            <SheetTitle className="flex items-center gap-2">
              <ShoppingCart className="h-5 w-5" />
              Giỏ Hàng ({itemCount} sản phẩm)
            </SheetTitle>
          </SheetHeader>

          <div className="flex flex-col h-full">
            {cartItems.length === 0 ? (
              <div className="flex-1 flex items-center justify-center">
                <div className="text-center">
                  <ShoppingCart className="h-16 w-16 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground mb-4">Giỏ hàng của bạn đang trống</p>
                  <Button variant="outline">Tiếp Tục Mua Sắm</Button>
                </div>
              </div>
            ) : (
              <>
                {/* Cart Items */}
                <div className="flex-1 overflow-y-auto py-6">
                  <div className="space-y-4">
                    {cartItems.map((item) => (
                      <div key={item.id} className="flex gap-4 p-4 bg-muted/30 rounded-lg">
                        <div className="flex-1">
                          <h3 className="font-medium text-foreground line-clamp-2">
                            {item.productName || `Sản phẩm #${item.productId}`}
                          </h3>
                          <div className="flex items-center justify-between mt-2">
                            <span className="font-bold text-primary">
                              {formatPrice(item.unitPrice)}
                            </span>
                            <div className="flex items-center gap-2">
                              <Button
                                variant="outline"
                                size="icon"
                                className="h-8 w-8"
                                onClick={() => handleUpdateQuantity(item.id, item.quantity - 1)}
                              >
                                <Minus className="h-3 w-3" />
                              </Button>
                              <span className="w-8 text-center text-sm">
                                {item.quantity}
                              </span>
                              <Button
                                variant="outline"
                                size="icon"
                                className="h-8 w-8"
                                onClick={() => handleUpdateQuantity(item.id, item.quantity + 1)}
                              >
                                <Plus className="h-3 w-3" />
                              </Button>
                            </div>
                          </div>
                          <div className="text-xs text-muted-foreground mt-1">
                            Tổng: {formatPrice(item.totalPrice)}
                          </div>
                        </div>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-muted-foreground hover:text-destructive"
                          onClick={() => handleRemoveItem(item.id)}
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Cart Summary */}
                <div className="border-t border-border pt-6 space-y-4">
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-semibold text-foreground">
                      Tổng cộng:
                    </span>
                    <span className="text-xl font-bold text-primary">
                      {formatPrice(totalPrice)}
                    </span>
                  </div>
                  
                  <div className="space-y-2">
                    <Button 
                      variant="luxury" 
                      className="w-full" 
                      size="lg"
                      onClick={() => setIsCheckoutOpen(true)}
                    >
                      Thanh Toán
                    </Button>
                    <Button variant="outline" className="w-full">
                      Xem Giỏ Hàng
                    </Button>
                  </div>

                  <p className="text-xs text-muted-foreground text-center">
                    Miễn phí vận chuyển cho đơn hàng trên 20.000.000 VNĐ
                  </p>
                </div>
              </>
            )}
          </div>
        </SheetContent>
      </Sheet>

      {/* Checkout Modal */}
      <Checkout
        isOpen={isCheckoutOpen}
        onClose={() => setIsCheckoutOpen(false)}
        onComplete={handleOrderComplete}
      />
    </>
  );
};