import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { OrdersApi } from "@/features/orders/api";
import { useToast } from "@/hooks/use-toast";
import type { OrderResponse } from "@/features/orders/types";
import type {
  OrderStatus,
  PaymentStatus,
  PaymentMethod,
} from "@/features/orders/types";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Loader2,
  Eye,
  XCircle,
  Package,
  Truck,
  CheckCircle2,
  Clock,
  Ban,
  ShoppingBag,
  CreditCard,
  MapPin,
  Phone,
  User,
  FileText,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";

// ─── Helper functions ───────────────────────────────────────────
const getOrderStatusLabel = (status?: OrderStatus) => {
  const labels: Record<OrderStatus, string> = {
    PENDING: "Chờ xử lý",
    PROCESSING: "Đang xử lý",
    SHIPPED: "Đang giao hàng",
    DELIVERED: "Đã giao hàng",
    CANCELED: "Đã hủy",
  };
  return status ? labels[status] : "-";
};

const getOrderStatusIcon = (status?: OrderStatus) => {
  switch (status) {
    case "PENDING":
      return <Clock className="h-4 w-4" />;
    case "PROCESSING":
      return <Package className="h-4 w-4" />;
    case "SHIPPED":
      return <Truck className="h-4 w-4" />;
    case "DELIVERED":
      return <CheckCircle2 className="h-4 w-4" />;
    case "CANCELED":
      return <Ban className="h-4 w-4" />;
    default:
      return <Clock className="h-4 w-4" />;
  }
};

const getOrderStatusColor = (status?: OrderStatus) => {
  switch (status) {
    case "PENDING":
      return "bg-amber-500/10 text-amber-600 border-amber-500/20";
    case "PROCESSING":
      return "bg-blue-500/10 text-blue-600 border-blue-500/20";
    case "SHIPPED":
      return "bg-purple-500/10 text-purple-600 border-purple-500/20";
    case "DELIVERED":
      return "bg-emerald-500/10 text-emerald-600 border-emerald-500/20";
    case "CANCELED":
      return "bg-red-500/10 text-red-600 border-red-500/20";
    default:
      return "bg-gray-500/10 text-gray-600 border-gray-500/20";
  }
};

const getPaymentStatusLabel = (status?: PaymentStatus) => {
  const labels: Record<PaymentStatus, string> = {
    UNPAID: "Chưa thanh toán",
    PAID: "Đã thanh toán",
    REFUNDED: "Đã hoàn tiền",
  };
  return status ? labels[status] : "-";
};

const getPaymentStatusColor = (status?: PaymentStatus) => {
  switch (status) {
    case "UNPAID":
      return "bg-orange-500/10 text-orange-600 border-orange-500/20";
    case "PAID":
      return "bg-emerald-500/10 text-emerald-600 border-emerald-500/20";
    case "REFUNDED":
      return "bg-red-500/10 text-red-600 border-red-500/20";
    default:
      return "bg-gray-500/10 text-gray-600 border-gray-500/20";
  }
};

const getPaymentMethodLabel = (method?: PaymentMethod) => {
  const labels: Record<PaymentMethod, string> = {
    COD: "Thanh toán khi nhận hàng",
    BANK_TRANSFER: "Chuyển khoản ngân hàng",
  };
  return method ? labels[method] : "-";
};

// ─── Filter tabs ────────────────────────────────────────────────
type FilterTab = "ALL" | OrderStatus;

const FILTER_TABS: { key: FilterTab; label: string; icon: React.ReactNode }[] =
  [
    { key: "ALL", label: "Tất cả", icon: <ShoppingBag className="h-4 w-4" /> },
    { key: "PENDING", label: "Chờ xử lý", icon: <Clock className="h-4 w-4" /> },
    {
      key: "PROCESSING",
      label: "Đang xử lý",
      icon: <Package className="h-4 w-4" />,
    },
    {
      key: "SHIPPED",
      label: "Đang giao",
      icon: <Truck className="h-4 w-4" />,
    },
    {
      key: "DELIVERED",
      label: "Đã giao",
      icon: <CheckCircle2 className="h-4 w-4" />,
    },
    { key: "CANCELED", label: "Đã hủy", icon: <Ban className="h-4 w-4" /> },
  ];

// ─── Timeline step component ───────────────────────────────────
const TIMELINE_STEPS: {
  status: OrderStatus;
  label: string;
  icon: React.ReactNode;
}[] = [
  { status: "PENDING", label: "Đặt hàng", icon: <Clock className="h-4 w-4" /> },
  {
    status: "PROCESSING",
    label: "Xử lý",
    icon: <Package className="h-4 w-4" />,
  },
  {
    status: "SHIPPED",
    label: "Vận chuyển",
    icon: <Truck className="h-4 w-4" />,
  },
  {
    status: "DELIVERED",
    label: "Hoàn thành",
    icon: <CheckCircle2 className="h-4 w-4" />,
  },
];

const getStepIndex = (status?: OrderStatus) => {
  if (!status || status === "CANCELED") return -1;
  return TIMELINE_STEPS.findIndex((s) => s.status === status);
};

// ─── Main Component ─────────────────────────────────────────────
export const MyOrders = () => {
  const [activeFilter, setActiveFilter] = useState<FilterTab>("ALL");
  const [viewingOrder, setViewingOrder] = useState<OrderResponse | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);
  const [orderToCancel, setOrderToCancel] = useState<string | null>(null);
  const [expandedCards, setExpandedCards] = useState<Set<string>>(new Set());

  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Query
  const {
    data: ordersData,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["myOrders"],
    queryFn: () => OrdersApi.getMyOrders(),
  });

  // Cancel mutation
  const cancelMutation = useMutation({
    mutationFn: OrdersApi.cancel,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myOrders"] });
      toast({
        title: "Đã hủy đơn hàng",
        description: "Đơn hàng của bạn đã được hủy thành công.",
      });
    },
    onError: (error: Error) => {
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: error.message || "Không thể hủy đơn hàng",
      });
    },
  });

  const orders = ordersData?.result || [];
  const filteredOrders =
    activeFilter === "ALL"
      ? orders
      : orders.filter((o) => o.status === activeFilter);

  const toggleExpand = (orderId: string) => {
    setExpandedCards((prev) => {
      const next = new Set(prev);
      if (next.has(orderId)) next.delete(orderId);
      else next.add(orderId);
      return next;
    });
  };

  const handleViewDetail = (order: OrderResponse) => {
    setViewingOrder(order);
    setIsDetailOpen(true);
  };

  const handleCancelOrder = (orderId: string) => {
    setOrderToCancel(orderId);
    setCancelConfirmOpen(true);
  };

  const confirmCancel = () => {
    if (orderToCancel) {
      cancelMutation.mutate(orderToCancel);
      setCancelConfirmOpen(false);
      setOrderToCancel(null);
    }
  };

  // Count per status
  const countByStatus = (status: OrderStatus) =>
    orders.filter((o) => o.status === status).length;

  // ─── Render ─────────────────────────────────────────────────
  return (
    <div className="container mx-auto px-4 py-8 max-w-5xl">
      {/* Page Title */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
          Đơn hàng của tôi
        </h1>
        <p className="text-muted-foreground mt-1">
          Theo dõi và quản lý tất cả đơn hàng đã đặt
        </p>
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-2 overflow-x-auto pb-2 mb-6 scrollbar-hide">
        {FILTER_TABS.map((tab) => {
          const count =
            tab.key === "ALL"
              ? orders.length
              : countByStatus(tab.key as OrderStatus);
          const isActive = activeFilter === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveFilter(tab.key)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 border ${
                isActive
                  ? "bg-primary text-primary-foreground border-primary shadow-md shadow-primary/25"
                  : "bg-card text-muted-foreground border-border hover:bg-accent hover:text-foreground"
              }`}
            >
              {tab.icon}
              {tab.label}
              {count > 0 && (
                <span
                  className={`ml-1 px-1.5 py-0.5 rounded-full text-xs font-semibold ${
                    isActive
                      ? "bg-primary-foreground/20 text-primary-foreground"
                      : "bg-muted text-muted-foreground"
                  }`}
                >
                  {count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-20">
          <Loader2 className="h-10 w-10 animate-spin text-primary mb-4" />
          <p className="text-muted-foreground">Đang tải đơn hàng...</p>
        </div>
      ) : error ? (
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-destructive">
              Lỗi khi tải đơn hàng: {(error as Error).message}
            </p>
          </CardContent>
        </Card>
      ) : filteredOrders.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <div className="w-20 h-20 rounded-full bg-muted/50 flex items-center justify-center mb-4">
            <ShoppingBag className="h-10 w-10 text-muted-foreground/50" />
          </div>
          <h3 className="text-lg font-medium text-foreground mb-1">
            {activeFilter === "ALL"
              ? "Bạn chưa có đơn hàng nào"
              : "Không có đơn hàng nào"}
          </h3>
          <p className="text-muted-foreground text-sm">
            {activeFilter === "ALL"
              ? "Hãy khám phá sản phẩm và đặt đơn hàng đầu tiên!"
              : `Không có đơn hàng nào ở trạng thái "${FILTER_TABS.find((t) => t.key === activeFilter)?.label}"`}
          </p>
          {activeFilter === "ALL" && (
            <Button
              className="mt-4"
              onClick={() => (window.location.href = "/products")}
            >
              Khám phá sản phẩm
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {filteredOrders.map((order) => {
            const isExpanded = expandedCards.has(order.id);
            const stepIndex = getStepIndex(order.status);
            const isCanceled = order.status === "CANCELED";

            return (
              <Card
                key={order.id}
                className="overflow-hidden transition-shadow duration-200 hover:shadow-lg border-border/60"
              >
                {/* Order Card Header */}
                <div className="px-6 py-4 bg-card border-b border-border/50">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="flex items-center gap-2">
                        <span className="text-sm text-muted-foreground">
                          Mã đơn:
                        </span>
                        <span className="font-mono font-semibold text-sm text-foreground">
                          {order.orderCode || order.id.slice(0, 12)}
                        </span>
                      </div>
                      <span className="text-border">|</span>
                      <Badge
                        className={`${getOrderStatusColor(order.status)} border gap-1`}
                      >
                        {getOrderStatusIcon(order.status)}
                        {getOrderStatusLabel(order.status)}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge
                        className={`${getPaymentStatusColor(order.paymentStatus)} border`}
                      >
                        <CreditCard className="h-3 w-3 mr-1" />
                        {getPaymentStatusLabel(order.paymentStatus)}
                      </Badge>
                    </div>
                  </div>
                </div>

                {/* Order Timeline (non-canceled) */}
                {!isCanceled && (
                  <div className="px-6 py-4 bg-muted/20">
                    <div className="flex items-center justify-between relative">
                      {/* Line connecting steps */}
                      <div className="absolute left-0 right-0 top-1/2 -translate-y-1/2 h-0.5 bg-border mx-8" />
                      {TIMELINE_STEPS.map((step, idx) => {
                        const isCompleted = idx <= stepIndex;
                        const isCurrent = idx === stepIndex;
                        return (
                          <div
                            key={step.status}
                            className="flex flex-col items-center relative z-10"
                          >
                            <div
                              className={`w-9 h-9 rounded-full flex items-center justify-center transition-all duration-300 ${
                                isCurrent
                                  ? "bg-primary text-primary-foreground shadow-md shadow-primary/30 scale-110"
                                  : isCompleted
                                    ? "bg-primary/80 text-primary-foreground"
                                    : "bg-muted text-muted-foreground"
                              }`}
                            >
                              {step.icon}
                            </div>
                            <span
                              className={`text-xs mt-1.5 font-medium ${
                                isCurrent
                                  ? "text-primary"
                                  : isCompleted
                                    ? "text-foreground"
                                    : "text-muted-foreground"
                              }`}
                            >
                              {step.label}
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* Order Summary Row */}
                <CardContent className="p-6">
                  {/* Quick info */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
                    <div className="flex items-start gap-2">
                      <User className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                      <div>
                        <p className="text-xs text-muted-foreground">
                          Người nhận
                        </p>
                        <p className="text-sm font-medium truncate">
                          {order.nameRecipient || "-"}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-start gap-2">
                      <Phone className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                      <div>
                        <p className="text-xs text-muted-foreground">SĐT</p>
                        <p className="text-sm font-medium">
                          {order.phoneRecipient || "-"}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-start gap-2">
                      <CreditCard className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                      <div>
                        <p className="text-xs text-muted-foreground">
                          Thanh toán
                        </p>
                        <p className="text-sm font-medium">
                          {getPaymentMethodLabel(order.paymentMethod)}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-start gap-2">
                      <ShoppingBag className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                      <div>
                        <p className="text-xs text-muted-foreground">
                          Tổng tiền
                        </p>
                        <p className="text-sm font-bold text-primary">
                          {order.totalPrice.toLocaleString("vi-VN")} ₫
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Expandable: Order Details */}
                  {order.orderDetails && order.orderDetails.length > 0 && (
                    <div>
                      <button
                        onClick={() => toggleExpand(order.id)}
                        className="flex items-center gap-1 text-sm text-primary hover:text-primary/80 transition-colors mb-2"
                      >
                        {isExpanded ? (
                          <ChevronUp className="h-4 w-4" />
                        ) : (
                          <ChevronDown className="h-4 w-4" />
                        )}
                        {isExpanded
                          ? "Ẩn sản phẩm"
                          : `Xem ${order.orderDetails.length} sản phẩm`}
                      </button>

                      {isExpanded && (
                        <div className="rounded-lg border border-border/50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                          <Table>
                            <TableHeader>
                              <TableRow className="bg-muted/30">
                                <TableHead>Sản phẩm</TableHead>
                                <TableHead className="text-center">SL</TableHead>
                                <TableHead className="text-right">Đơn giá</TableHead>
                                <TableHead className="text-right">Thành tiền</TableHead>
                              </TableRow>
                            </TableHeader>
                            <TableBody>
                              {order.orderDetails.map((detail) => (
                                <TableRow key={detail.id}>
                                  <TableCell>
                                    <div className="flex items-center gap-3">
                                      {detail.productImage ? (
                                        <img
                                          src={detail.productImage}
                                          alt={detail.productName || ''}
                                          className="w-10 h-10 rounded-md object-cover border border-border/50"
                                        />
                                      ) : (
                                        <div className="w-10 h-10 rounded-md bg-muted flex items-center justify-center">
                                          <Package className="h-4 w-4 text-muted-foreground" />
                                        </div>
                                      )}
                                      <span className="text-sm font-medium line-clamp-2">
                                        {detail.productName || detail.productId}
                                      </span>
                                    </div>
                                  </TableCell>
                                  <TableCell className="text-center">
                                    {detail.quantity}
                                  </TableCell>
                                  <TableCell className="text-right text-sm">
                                    {detail.unitPrice != null
                                      ? `${detail.unitPrice.toLocaleString('vi-VN')} ₫`
                                      : '-'}
                                  </TableCell>
                                  <TableCell className="text-right font-medium text-sm">
                                    {detail.unitPrice != null
                                      ? `${(detail.unitPrice * detail.quantity).toLocaleString('vi-VN')} ₫`
                                      : '-'}
                                  </TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Action buttons */}
                  <div className="flex items-center justify-end gap-2 mt-4 pt-4 border-t border-border/50">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleViewDetail(order)}
                      className="gap-1.5"
                    >
                      <Eye className="h-4 w-4" />
                      Chi tiết
                    </Button>
                    {order.status === "PENDING" && (
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => handleCancelOrder(order.id)}
                        disabled={cancelMutation.isPending}
                        className="gap-1.5"
                      >
                        {cancelMutation.isPending ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <XCircle className="h-4 w-4" />
                        )}
                        Hủy đơn
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* ─── Detail Dialog ─────────────────────────────────────── */}
      <Dialog
        open={isDetailOpen}
        onOpenChange={(open) => {
          if (!open) {
            setIsDetailOpen(false);
            setViewingOrder(null);
          }
        }}
      >
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <FileText className="h-5 w-5 text-primary" />
              Chi tiết đơn hàng
            </DialogTitle>
            <DialogDescription>
              Mã đơn:{" "}
              <span className="font-mono font-semibold">
                {viewingOrder?.orderCode || viewingOrder?.id}
              </span>
            </DialogDescription>
          </DialogHeader>
          {viewingOrder && (
            <div className="space-y-6">
              {/* Status badges */}
              <div className="flex flex-wrap gap-2">
                <Badge
                  className={`${getOrderStatusColor(viewingOrder.status)} border gap-1 px-3 py-1`}
                >
                  {getOrderStatusIcon(viewingOrder.status)}
                  {getOrderStatusLabel(viewingOrder.status)}
                </Badge>
                <Badge
                  className={`${getPaymentStatusColor(viewingOrder.paymentStatus)} border px-3 py-1`}
                >
                  <CreditCard className="h-3 w-3 mr-1" />
                  {getPaymentStatusLabel(viewingOrder.paymentStatus)}
                </Badge>
              </div>

              {/* Recipient Info */}
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-base flex items-center gap-2">
                    <MapPin className="h-4 w-4 text-primary" />
                    Thông tin nhận hàng
                  </CardTitle>
                </CardHeader>
                <CardContent className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <p className="text-xs text-muted-foreground mb-0.5">
                      Người nhận
                    </p>
                    <p className="text-sm font-medium">
                      {viewingOrder.nameRecipient || "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground mb-0.5">
                      Số điện thoại
                    </p>
                    <p className="text-sm font-medium">
                      {viewingOrder.phoneRecipient || "-"}
                    </p>
                  </div>
                  <div className="sm:col-span-2">
                    <p className="text-xs text-muted-foreground mb-0.5">
                      Địa chỉ
                    </p>
                    <p className="text-sm font-medium">
                      {viewingOrder.addressRecipient || "-"}
                    </p>
                  </div>
                  <div className="sm:col-span-2">
                    <p className="text-xs text-muted-foreground mb-0.5">
                      Ghi chú
                    </p>
                    <p className="text-sm">
                      {viewingOrder.note || "Không có ghi chú"}
                    </p>
                  </div>
                </CardContent>
              </Card>

              {/* Payment Info */}
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-base flex items-center gap-2">
                    <CreditCard className="h-4 w-4 text-primary" />
                    Thanh toán
                  </CardTitle>
                </CardHeader>
                <CardContent className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <p className="text-xs text-muted-foreground mb-0.5">
                      Phương thức
                    </p>
                    <p className="text-sm font-medium">
                      {getPaymentMethodLabel(viewingOrder.paymentMethod)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground mb-0.5">
                      Tổng tiền
                    </p>
                    <p className="text-xl font-bold text-primary">
                      {viewingOrder.totalPrice.toLocaleString("vi-VN")} ₫
                    </p>
                  </div>
                </CardContent>
              </Card>

              {/* Product Details */}
              {viewingOrder.orderDetails &&
                viewingOrder.orderDetails.length > 0 && (
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-base flex items-center gap-2">
                        <ShoppingBag className="h-4 w-4 text-primary" />
                        Sản phẩm ({viewingOrder.orderDetails.length})
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="p-0">
                      <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead>Sản phẩm</TableHead>
                            <TableHead className="text-center">SL</TableHead>
                            <TableHead className="text-right">Đơn giá</TableHead>
                            <TableHead className="text-right">Thành tiền</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {viewingOrder.orderDetails.map((detail) => (
                            <TableRow key={detail.id}>
                              <TableCell>
                                <div className="flex items-center gap-3">
                                  {detail.productImage ? (
                                    <img
                                      src={detail.productImage}
                                      alt={detail.productName || ''}
                                      className="w-12 h-12 rounded-md object-cover border border-border/50"
                                    />
                                  ) : (
                                    <div className="w-12 h-12 rounded-md bg-muted flex items-center justify-center">
                                      <Package className="h-5 w-5 text-muted-foreground" />
                                    </div>
                                  )}
                                  <span className="text-sm font-medium">
                                    {detail.productName || detail.productId}
                                  </span>
                                </div>
                              </TableCell>
                              <TableCell className="text-center">
                                {detail.quantity}
                              </TableCell>
                              <TableCell className="text-right">
                                {detail.unitPrice != null
                                  ? `${detail.unitPrice.toLocaleString('vi-VN')} ₫`
                                  : '-'}
                              </TableCell>
                              <TableCell className="text-right font-medium">
                                {detail.unitPrice != null
                                  ? `${(detail.unitPrice * detail.quantity).toLocaleString('vi-VN')} ₫`
                                  : '-'}
                              </TableCell>
                            </TableRow>
                          ))}
                          <TableRow className="bg-muted/30">
                            <TableCell colSpan={3} className="text-right font-semibold">
                              Tổng cộng
                            </TableCell>
                            <TableCell className="text-right font-bold text-primary">
                              {viewingOrder.totalPrice.toLocaleString('vi-VN')} ₫
                            </TableCell>
                          </TableRow>
                        </TableBody>
                      </Table>
                    </CardContent>
                  </Card>
                )}
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Cancel Confirmation */}
      <ConfirmDialog
        open={cancelConfirmOpen}
        onOpenChange={setCancelConfirmOpen}
        onConfirm={confirmCancel}
        title="Xác nhận hủy đơn hàng"
        description="Bạn có chắc chắn muốn hủy đơn hàng này? Hành động này không thể hoàn tác."
        confirmText="Hủy đơn"
        cancelText="Không"
      />
    </div>
  );
};
