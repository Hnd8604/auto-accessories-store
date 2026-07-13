import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { useToast } from "@/hooks/use-toast";
import { useCart } from "@/context/cart-context";
import { useAuth } from "@/context/auth-context";
import { OrdersApi } from "@/features/orders/api";
import type { OrderCreationRequest, PaymentMethod } from "@/features/orders/types";
import { useMutation } from "@tanstack/react-query";
import { ProductImagesApi } from "@/features/products/api";
import { useEffect } from "react";
import { CalendarDays, Clock, Car, Phone, Mail, MapPin, CreditCard, Truck, Loader2, Package, Banknote, QrCode } from "lucide-react";
import { PaymentDialog } from "@/features/orders/components/PaymentDialog";

// Validation schema
const checkoutSchema = z.object({
  nameRecipient: z.string().min(2, "Tên người nhận phải có ít nhất 2 ký tự"),
  phoneRecipient: z.string().min(10, "Số điện thoại không hợp lệ"),
  addressRecipient: z.string().min(10, "Địa chỉ giao hàng phải có ít nhất 10 ký tự"),
  note: z.string().optional(),
});

type CheckoutFormData = z.infer<typeof checkoutSchema>;

interface CheckoutProps {
  isOpen: boolean;
  onClose: () => void;
  onComplete: () => void;
}

export const Checkout = ({ isOpen, onClose, onComplete }: CheckoutProps) => {
  const { toast } = useToast();
  const { cart, itemCount, clearCart } = useCart();
  const { user } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [productImages, setProductImages] = useState<Record<number, string>>({});
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("COD");
  const [paymentDialogOpen, setPaymentDialogOpen] = useState(false);
  const [createdOrderId, setCreatedOrderId] = useState<string | null>(null);

  const form = useForm<CheckoutFormData>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: {
      nameRecipient: user?.fullName || "",
      phoneRecipient: user?.phone || "",
      addressRecipient: "",
      note: "",
    },
  });

  // Create order mutation
  const createOrderMutation = useMutation({
    mutationFn: (payload: OrderCreationRequest) => OrdersApi.createFromCart(payload),
    onSuccess: (response) => {
      const orderId = response.result?.id;
      const orderPaymentMethod = response.result?.paymentMethod;

      if (orderPaymentMethod === "BANK_TRANSFER" && orderId) {
        // Mở dialog thanh toán QR
        setCreatedOrderId(orderId);
        setPaymentDialogOpen(true);
        toast({
          title: "Đơn hàng đã được tạo!",
          description: `Mã đơn: ${response.result?.orderCode}. Vui lòng thanh toán để hoàn tất.`,
        });
      } else {
        // COD - xong luôn
        toast({
          title: "Đặt hàng thành công!",
          description: `Đơn hàng #${response.result?.orderCode || orderId} đã được tạo. Chúng tôi sẽ liên hệ với bạn sớm.`,
        });
        form.reset();
        clearCart();
        onComplete();
        onClose();
      }
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Đặt hàng thất bại",
        description: error.message || "Có lỗi xảy ra. Vui lòng thử lại.",
      });
    },
  });

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(price);
  };

  const cartItems = cart?.items || [];

  // Fetch product images when cart items change
  useEffect(() => {
    const fetchImages = async () => {
      const images: Record<number, string> = {};
      for (const item of cartItems) {
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
      }
      setProductImages(images);
    };

    if (cartItems.length > 0) {
      fetchImages();
    }
  }, [cartItems]);
  const totalPrice = cart?.totalPrice || 0;
  const shippingFee = totalPrice >= 20000000 ? 0 : 30000;
  const finalTotal = totalPrice + shippingFee;

  const onSubmit = async (data: CheckoutFormData) => {
    if (!user?.id) {
      toast({
        variant: "destructive",
        title: "Chưa đăng nhập",
        description: "Vui lòng đăng nhập để đặt hàng.",
      });
      return;
    }

    setIsSubmitting(true);
    try {
      const orderPayload: OrderCreationRequest = {
        userId: user.id,
        nameRecipient: data.nameRecipient,
        phoneRecipient: data.phoneRecipient,
        addressRecipient: data.addressRecipient,
        note: data.note,
        paymentMethod: paymentMethod,
        orderDetails: cartItems.map(item => ({
          productId: item.productId,
          quantity: item.quantity,
        })),
      };

      await createOrderMutation.mutateAsync(orderPayload);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePaymentSuccess = () => {
    form.reset();
    clearCart();
    onComplete();
    // Close the checkout after a short delay
    setTimeout(() => {
      setPaymentDialogOpen(false);
      onClose();
    }, 2000);
  };

  const handlePaymentDialogClose = () => {
    setPaymentDialogOpen(false);
    // If payment was already created, still close checkout and clear cart
    form.reset();
    clearCart();
    onComplete();
    onClose();
  };

  return (
    <>
      <Dialog open={isOpen && !paymentDialogOpen} onOpenChange={onClose}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-2xl">
              <CreditCard className="h-6 w-6 text-primary" />
              Thanh Toán Đơn Hàng
            </DialogTitle>
            <DialogDescription>
              Hoàn tất thông tin để xác nhận đơn hàng của bạn
            </DialogDescription>
          </DialogHeader>

          <div className="grid lg:grid-cols-2 gap-6">
            {/* Order Summary */}
            <div className="space-y-4">
              <Card className="border-primary/20">
                <CardHeader>
                  <CardTitle className="text-primary">Đơn Hàng Của Bạn</CardTitle>
                  <CardDescription>
                    {itemCount} sản phẩm trong giỏ hàng
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-3">
                  {cartItems.map((item) => (
                    <div key={item.id} className="flex gap-3 p-3 border rounded-lg hover:bg-muted/50 transition-colors">
                      {/* Product Image */}
                      <div className="w-20 h-20 bg-muted rounded-md flex items-center justify-center flex-shrink-0 overflow-hidden">
                        {productImages[item.productId] ? (
                          <img
                            src={productImages[item.productId]}
                            alt={item.productName}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <Package className="h-8 w-8 text-muted-foreground" />
                        )}
                      </div>

                      {/* Product Info */}
                      <div className="flex-1 min-w-0">
                        <h4 className="font-medium text-sm line-clamp-2">
                          {item.productName || `Sản phẩm #${item.productId}`}
                        </h4>
                        <div className="flex justify-between items-center mt-2">
                          <span className="text-sm text-muted-foreground">
                            Số lượng: {item.quantity}
                          </span>
                          <span className="font-bold text-sm text-primary">
                            {formatPrice(item.totalPrice)}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}

                  <Separator />

                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span>Tạm tính:</span>
                      <span>{formatPrice(totalPrice)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="flex items-center gap-2">
                        <Truck className="h-4 w-4" />
                        Phí vận chuyển:
                      </span>
                      <span className={shippingFee === 0 ? "text-green-600" : ""}>
                        {shippingFee === 0 ? "Miễn phí" : formatPrice(shippingFee)}
                      </span>
                    </div>
                    <Separator />
                    <div className="flex justify-between items-center text-lg font-bold">
                      <span>Tổng cộng:</span>
                      <span className="text-primary">{formatPrice(finalTotal)}</span>
                    </div>
                    {shippingFee === 0 && (
                      <p className="text-xs text-green-600">
                        🎉 Bạn được miễn phí vận chuyển!
                      </p>
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Checkout Form */}
            <div className="space-y-4">
              <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                  {/* Delivery Information */}
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-lg">
                        <Truck className="h-5 w-5 text-primary" />
                        Thông Tin Giao Hàng
                      </CardTitle>
                      <CardDescription>Hoàn tất thông tin để xác nhận đơn hàng của bạn</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <FormField
                        control={form.control}
                        name="nameRecipient"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Họ và Tên *</FormLabel>
                            <FormControl>
                              <Input
                                placeholder="Nguyễn Văn A"
                                value={field.value}
                                onChange={(e) => field.onChange(e.target.value)}
                                onBlur={field.onBlur}
                                name={field.name}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      <FormField
                        control={form.control}
                        name="phoneRecipient"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel className="flex items-center gap-2">
                              <Phone className="h-4 w-4" />
                              Số Điện Thoại *
                            </FormLabel>
                            <FormControl>
                              <Input
                                placeholder="0123456789"
                                value={field.value}
                                onChange={(e) => field.onChange(e.target.value)}
                                onBlur={field.onBlur}
                                name={field.name}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      <FormField
                        control={form.control}
                        name="addressRecipient"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel className="flex items-center gap-2">
                              <MapPin className="h-4 w-4" />
                              Địa Chỉ Giao Hàng *
                            </FormLabel>
                            <FormControl>
                              <Textarea
                                placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
                                className="min-h-[80px]"
                                value={field.value}
                                onChange={(e) => field.onChange(e.target.value)}
                                onBlur={field.onBlur}
                                name={field.name}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      <FormField
                        control={form.control}
                        name="note"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Ghi Chú</FormLabel>
                            <FormControl>
                              <Textarea
                                placeholder="Ghi chú thêm cho đơn hàng (tùy chọn)"
                                className="min-h-[60px]"
                                value={field.value || ""}
                                onChange={(e) => field.onChange(e.target.value)}
                                onBlur={field.onBlur}
                                name={field.name}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </CardContent>
                  </Card>

                  {/* Payment Method Selection */}
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-lg">
                        <Banknote className="h-5 w-5 text-primary" />
                        Phương Thức Thanh Toán
                      </CardTitle>
                      <CardDescription>Chọn cách thanh toán phù hợp với bạn</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-3">
                      {/* COD Option */}
                      <div
                        onClick={() => setPaymentMethod("COD")}
                        className={`flex items-center gap-4 p-4 border-2 rounded-xl cursor-pointer transition-all ${paymentMethod === "COD"
                            ? "border-primary bg-primary/5 shadow-sm"
                            : "border-muted hover:border-primary/50"
                          }`}
                      >
                        <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${paymentMethod === "COD" ? "border-primary" : "border-muted-foreground/30"
                          }`}>
                          {paymentMethod === "COD" && (
                            <div className="w-3 h-3 rounded-full bg-primary" />
                          )}
                        </div>
                        <Truck className={`h-6 w-6 ${paymentMethod === "COD" ? "text-primary" : "text-muted-foreground"}`} />
                        <div className="flex-1">
                          <p className="font-medium">Thanh toán khi nhận hàng (COD)</p>
                          <p className="text-sm text-muted-foreground">
                            Thanh toán bằng tiền mặt khi nhận được hàng
                          </p>
                        </div>
                      </div>

                      {/* Bank Transfer Option */}
                      <div
                        onClick={() => setPaymentMethod("BANK_TRANSFER")}
                        className={`flex items-center gap-4 p-4 border-2 rounded-xl cursor-pointer transition-all ${paymentMethod === "BANK_TRANSFER"
                            ? "border-primary bg-primary/5 shadow-sm"
                            : "border-muted hover:border-primary/50"
                          }`}
                      >
                        <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${paymentMethod === "BANK_TRANSFER" ? "border-primary" : "border-muted-foreground/30"
                          }`}>
                          {paymentMethod === "BANK_TRANSFER" && (
                            <div className="w-3 h-3 rounded-full bg-primary" />
                          )}
                        </div>
                        <QrCode className={`h-6 w-6 ${paymentMethod === "BANK_TRANSFER" ? "text-primary" : "text-muted-foreground"}`} />
                        <div className="flex-1">
                          <p className="font-medium">Chuyển khoản ngân hàng</p>
                          <p className="text-sm text-muted-foreground">
                            Quét mã QR VietQR để thanh toán nhanh chóng
                          </p>
                        </div>
                        <Badge variant="secondary" className="text-xs">Nhanh</Badge>
                      </div>
                    </CardContent>
                  </Card>

                  <div className="flex gap-3 pt-2">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={onClose}
                      className="flex-1"
                      disabled={isSubmitting}
                    >
                      Quay Lại
                    </Button>
                    <Button
                      type="submit"
                      className="flex-1 bg-primary hover:bg-primary/90 text-white"
                      size="lg"
                      disabled={isSubmitting || cartItems.length === 0}
                    >
                      {isSubmitting ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          Đang xử lý...
                        </>
                      ) : paymentMethod === "BANK_TRANSFER" ? (
                        <>
                          <QrCode className="mr-2 h-4 w-4" />
                          Đặt Hàng & Thanh Toán
                        </>
                      ) : (
                        <>
                          <CreditCard className="mr-2 h-4 w-4" />
                          Xác Nhận Đặt Hàng
                        </>
                      )}
                    </Button>
                  </div>
                </form>
              </Form>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Payment QR Dialog */}
      <PaymentDialog
        isOpen={paymentDialogOpen}
        onClose={handlePaymentDialogClose}
        orderId={createdOrderId}
        onPaymentSuccess={handlePaymentSuccess}
      />
    </>
  );
};
