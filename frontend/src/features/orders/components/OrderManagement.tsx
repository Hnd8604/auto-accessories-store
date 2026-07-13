import { useState } from "react";
import {
  ChevronLeft,
  ChevronRight,
  Package,
  Clock,
  Truck,
  CheckCircle2,
  XCircle,
  Search,
  Filter,
  RefreshCw,
  Calendar,
  CreditCard,
  MapPin,
  Phone,
  User,
  FileText,
  ShoppingBag,
} from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { OrdersApi } from "@/features/orders/api";
import { useToast } from "@/hooks/use-toast";
import type {
  OrderResponse,
  OrderUpdateByAdminRequest,
} from "@/features/orders/types";
import type { OrderStatus, PaymentStatus, PaymentMethod } from "@/features/orders/types";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { MoreHorizontal, Edit, Trash2, Loader2, Eye } from "lucide-react";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";

// Validation schema
const orderUpdateSchema = z.object({
  orderStatus: z.enum([
    "PENDING",
    "PROCESSING",
    "SHIPPED",
    "DELIVERED",
    "CANCELED",
  ]),
  paymentStatus: z.enum(["UNPAID", "PAID", "REFUNDED"]),
});

type OrderUpdateFormData = z.infer<typeof orderUpdateSchema>;

interface OrderManagementProps {
  className?: string;
}

// Helper functions
const getOrderStatusLabel = (status?: OrderStatus) => {
  const labels: Record<OrderStatus, string> = {
    PENDING: "Chờ xử lý",
    PROCESSING: "Đang xử lý",
    SHIPPED: "Đã gửi hàng",
    DELIVERED: "Đã giao hàng",
    CANCELED: "Đã hủy",
  };
  return status ? labels[status] : "-";
};

const getOrderStatusIcon = (status?: OrderStatus) => {
  switch (status) {
    case "PENDING": return <Clock className="h-3.5 w-3.5" />;
    case "PROCESSING": return <RefreshCw className="h-3.5 w-3.5" />;
    case "SHIPPED": return <Truck className="h-3.5 w-3.5" />;
    case "DELIVERED": return <CheckCircle2 className="h-3.5 w-3.5" />;
    case "CANCELED": return <XCircle className="h-3.5 w-3.5" />;
    default: return <Package className="h-3.5 w-3.5" />;
  }
};

const getOrderStatusColor = (status?: OrderStatus) => {
  switch (status) {
    case "PENDING": return "bg-amber-50 text-amber-700 border-amber-200";
    case "PROCESSING": return "bg-blue-50 text-blue-700 border-blue-200";
    case "SHIPPED": return "bg-indigo-50 text-indigo-700 border-indigo-200";
    case "DELIVERED": return "bg-emerald-50 text-emerald-700 border-emerald-200";
    case "CANCELED": return "bg-red-50 text-red-700 border-red-200";
    default: return "bg-gray-50 text-gray-700 border-gray-200";
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
    case "UNPAID": return "bg-orange-50 text-orange-700 border-orange-200";
    case "PAID": return "bg-emerald-50 text-emerald-700 border-emerald-200";
    case "REFUNDED": return "bg-purple-50 text-purple-700 border-purple-200";
    default: return "bg-gray-50 text-gray-700 border-gray-200";
  }
};

const getPaymentMethodLabel = (method?: PaymentMethod) => {
  const labels: Record<PaymentMethod, string> = {
    COD: "Thanh toán khi nhận hàng",
    BANK_TRANSFER: "Chuyển khoản",
  };
  return method ? labels[method] : "-";
};

const getPaymentMethodColor = (method?: PaymentMethod) => {
  switch (method) {
    case "COD": return "bg-slate-50 text-slate-700 border-slate-200";
    case "BANK_TRANSFER": return "bg-sky-50 text-sky-700 border-sky-200";
    default: return "bg-gray-50 text-gray-700 border-gray-200";
  }
};

const formatDate = (dateStr?: string) => {
  if (!dateStr) return "-";
  const date = new Date(dateStr);
  return date.toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const formatPrice = (price: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

// Status filter tabs config
const STATUS_TABS = [
  { key: "ALL", label: "Tất cả", icon: Package },
  { key: "PENDING", label: "Chờ xử lý", icon: Clock },
  { key: "PROCESSING", label: "Đang xử lý", icon: RefreshCw },
  { key: "SHIPPED", label: "Đã gửi", icon: Truck },
  { key: "DELIVERED", label: "Đã giao", icon: CheckCircle2 },
  { key: "CANCELED", label: "Đã hủy", icon: XCircle },
];

const PAYMENT_TABS = [
  { key: "ALL", label: "Tất cả" },
  { key: "UNPAID", label: "Chưa TT" },
  { key: "PAID", label: "Đã TT" },
  { key: "REFUNDED", label: "Hoàn tiền" },
];

export const OrderManagement: React.FC<OrderManagementProps> = ({
  className,
}) => {
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false);
  const [editingOrder, setEditingOrder] = useState<OrderResponse | null>(null);
  const [viewingOrder, setViewingOrder] = useState<OrderResponse | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [orderToDelete, setOrderToDelete] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 20;
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [paymentFilter, setPaymentFilter] = useState<string>("ALL");

  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Form setup
  const form = useForm<OrderUpdateFormData>({
    resolver: zodResolver(orderUpdateSchema),
    defaultValues: {
      orderStatus: "PENDING",
      paymentStatus: "UNPAID",
    },
  });

  // Query: Get all orders
  const {
    data: ordersData,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ["orders", currentPage, pageSize],
    queryFn: () => OrdersApi.getAll({ page: currentPage, size: pageSize }),
  });

  // Mutation: Update order
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: OrderUpdateByAdminRequest }) =>
      OrdersApi.updateByAdmin(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      setIsEditDialogOpen(false);
      setEditingOrder(null);
      form.reset();
      toast({
        title: "Thành công",
        description: "Đơn hàng đã được cập nhật",
      });
    },
    onError: (error: Error) => {
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: error.message || "Không thể cập nhật đơn hàng",
      });
    },
  });

  // Mutation: Delete order
  const deleteMutation = useMutation({
    mutationFn: OrdersApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      toast({
        title: "Thành công",
        description: "Đơn hàng đã được xóa",
      });
    },
    onError: (error: Error) => {
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: error.message || "Không thể xóa đơn hàng",
      });
    },
  });

  // Handlers
  const handleEdit = (order: OrderResponse) => {
    setEditingOrder(order);
    form.reset({
      orderStatus: order.status || "PENDING",
      paymentStatus: order.paymentStatus || "UNPAID",
    });
    setIsEditDialogOpen(true);
  };

  const handleViewDetail = (order: OrderResponse) => {
    setViewingOrder(order);
    setIsDetailDialogOpen(true);
  };

  const handleDelete = (id: string) => {
    setOrderToDelete(id);
    setDeleteConfirmOpen(true);
  };

  const confirmDelete = () => {
    if (orderToDelete) {
      deleteMutation.mutate(orderToDelete);
      setDeleteConfirmOpen(false);
      setOrderToDelete(null);
    }
  };

  const onSubmit = (data: OrderUpdateFormData) => {
    if (editingOrder) {
      updateMutation.mutate({
        id: editingOrder.id,
        data: {
          orderStatus: data.orderStatus,
          paymentStatus: data.paymentStatus,
        },
      });
    }
  };

  const orders = ordersData?.result?.content || [];
  const totalPages = ordersData?.result?.totalPages || 0;
  const totalElements = ordersData?.result?.totalElements || 0;

  const filteredOrders = orders.filter((o) => {
    const matchStatus = statusFilter === "ALL" || o.status === statusFilter;
    const matchPayment = paymentFilter === "ALL" || o.paymentStatus === paymentFilter;
    return matchStatus && matchPayment;
  });

  // Counts for status tabs
  const statusCounts = orders.reduce((acc, o) => {
    if (o.status) acc[o.status] = (acc[o.status] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  if (error) {
    return (
      <Card className={className}>
        <CardContent className="pt-6">
          <p className="text-destructive">
            Lỗi khi tải đơn hàng: {(error as Error).message}
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className={className}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Quản lý đơn hàng</h2>
          <p className="text-muted-foreground text-sm mt-1">
            Theo dõi và quản lý tất cả đơn hàng từ khách hàng
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          className="gap-2"
        >
          <RefreshCw className="h-4 w-4" />
          Làm mới
        </Button>
      </div>

      {/* Status Filter Tabs */}
      <div className="flex flex-wrap items-center gap-2 mb-4">
        {STATUS_TABS.map((tab) => {
          const Icon = tab.icon;
          const count = tab.key === "ALL" ? orders.length : (statusCounts[tab.key] || 0);
          const isActive = statusFilter === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => { setStatusFilter(tab.key); setCurrentPage(0); }}
              className={`
                inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium
                transition-all duration-200 border
                ${isActive
                  ? "bg-primary text-primary-foreground border-primary shadow-sm"
                  : "bg-background text-muted-foreground border-border hover:bg-accent hover:text-accent-foreground"
                }
              `}
            >
              <Icon className="h-3.5 w-3.5" />
              {tab.label}
              <span className={`
                ml-1 px-1.5 py-0.5 rounded-full text-xs font-semibold
                ${isActive
                  ? "bg-primary-foreground/20 text-primary-foreground"
                  : "bg-muted text-muted-foreground"
                }
              `}>
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Payment Filter */}
      <div className="flex items-center gap-2 mb-6">
        <CreditCard className="h-4 w-4 text-muted-foreground" />
        <span className="text-sm text-muted-foreground mr-1">Thanh toán:</span>
        {PAYMENT_TABS.map((tab) => {
          const isActive = paymentFilter === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => { setPaymentFilter(tab.key); setCurrentPage(0); }}
              className={`
                px-2.5 py-1 rounded-md text-xs font-medium transition-all duration-200 border
                ${isActive
                  ? "bg-primary text-primary-foreground border-primary"
                  : "bg-background text-muted-foreground border-border hover:bg-accent"
                }
              `}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Orders Table */}
      <Card className="border shadow-sm">
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <p className="text-sm text-muted-foreground">Đang tải đơn hàng...</p>
            </div>
          ) : filteredOrders.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <Package className="h-12 w-12 text-muted-foreground/50" />
              <p className="text-muted-foreground font-medium">Không có đơn hàng nào</p>
              <p className="text-sm text-muted-foreground">Thử thay đổi bộ lọc để xem thêm</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/40 hover:bg-muted/40">
                    <TableHead className="font-semibold">Mã đơn</TableHead>
                    <TableHead className="font-semibold">Khách hàng</TableHead>
                    <TableHead className="font-semibold">Tổng tiền</TableHead>
                    <TableHead className="font-semibold">Phương thức TT</TableHead>
                    <TableHead className="font-semibold">Trạng thái</TableHead>
                    <TableHead className="font-semibold">Thanh toán</TableHead>
                    <TableHead className="font-semibold">Ngày tạo</TableHead>
                    <TableHead className="text-right font-semibold w-12"></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredOrders.map((order) => (
                    <TableRow
                      key={order.id}
                      className="group cursor-pointer hover:bg-muted/30 transition-colors"
                      onClick={() => handleViewDetail(order)}
                    >
                      <TableCell>
                        <span className="font-mono text-sm font-medium text-primary">
                          {order.orderCode || order.id.slice(0, 10)}
                        </span>
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-col">
                          <span className="font-medium text-sm">
                            {order.nameRecipient || "-"}
                          </span>
                          <span className="text-xs text-muted-foreground">
                            {order.phoneRecipient || ""}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="font-semibold text-sm">
                          {formatPrice(order.totalPrice)}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={`text-xs ${getPaymentMethodColor(order.paymentMethod)}`}
                        >
                          {order.paymentMethod === "COD" ? "COD" : "Chuyển khoản"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={`gap-1.5 text-xs ${getOrderStatusColor(order.status)}`}
                        >
                          {getOrderStatusIcon(order.status)}
                          {getOrderStatusLabel(order.status)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={`text-xs ${getPaymentStatusColor(order.paymentStatus)}`}
                        >
                          {getPaymentStatusLabel(order.paymentStatus)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <span className="text-xs text-muted-foreground">
                          {formatDate(order.createdAt)}
                        </span>
                      </TableCell>
                      <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              className="h-8 w-8 p-0 opacity-0 group-hover:opacity-100 transition-opacity"
                            >
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => handleViewDetail(order)}>
                              <Eye className="h-4 w-4 mr-2" />
                              Xem chi tiết
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => handleEdit(order)}>
                              <Edit className="h-4 w-4 mr-2" />
                              Cập nhật trạng thái
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              className="text-red-600 focus:text-red-600"
                              onClick={() => handleDelete(order.id)}
                            >
                              <Trash2 className="h-4 w-4 mr-2" />
                              Xóa đơn hàng
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}

          {/* Pagination */}
          {(totalPages > 1 || totalElements > 0) && (
            <div className="flex items-center justify-between px-6 py-4 border-t bg-muted/20">
              <p className="text-sm text-muted-foreground">
                Hiển thị <span className="font-medium text-foreground">{filteredOrders.length}</span> / <span className="font-medium text-foreground">{totalElements}</span> đơn hàng
                {totalPages > 1 && <> — Trang <span className="font-medium text-foreground">{currentPage + 1}</span> / {totalPages}</>}
              </p>
              {totalPages > 1 && (
                <div className="flex items-center gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                    disabled={currentPage === 0}
                    className="h-8 px-3"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                    let pageNum = i;
                    if (totalPages > 5) {
                      const start = Math.max(0, Math.min(currentPage - 2, totalPages - 5));
                      pageNum = start + i;
                    }
                    return (
                      <Button
                        key={pageNum}
                        variant={pageNum === currentPage ? "default" : "outline"}
                        size="sm"
                        className="h-8 w-8 p-0"
                        onClick={() => setCurrentPage(pageNum)}
                      >
                        {pageNum + 1}
                      </Button>
                    );
                  })}
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={currentPage >= totalPages - 1}
                    className="h-8 px-3"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Edit Dialog */}
      <Dialog
        open={isEditDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            setIsEditDialogOpen(false);
            setEditingOrder(null);
            form.reset();
          }
        }}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Edit className="h-5 w-5 text-primary" />
              Cập nhật trạng thái
            </DialogTitle>
            <DialogDescription>
              Đơn hàng: <span className="font-mono font-medium">{editingOrder?.orderCode || editingOrder?.id?.slice(0, 12)}</span>
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
              <FormField
                control={form.control}
                name="orderStatus"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-2">
                      <Package className="h-4 w-4 text-muted-foreground" />
                      Trạng thái đơn hàng
                    </FormLabel>
                    <Select
                      onValueChange={field.onChange}
                      defaultValue={field.value}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Chọn trạng thái" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="PENDING">
                          <span className="flex items-center gap-2"><Clock className="h-3.5 w-3.5 text-amber-500" /> Chờ xử lý</span>
                        </SelectItem>
                        <SelectItem value="PROCESSING">
                          <span className="flex items-center gap-2"><RefreshCw className="h-3.5 w-3.5 text-blue-500" /> Đang xử lý</span>
                        </SelectItem>
                        <SelectItem value="SHIPPED">
                          <span className="flex items-center gap-2"><Truck className="h-3.5 w-3.5 text-indigo-500" /> Đã gửi hàng</span>
                        </SelectItem>
                        <SelectItem value="DELIVERED">
                          <span className="flex items-center gap-2"><CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" /> Đã giao hàng</span>
                        </SelectItem>
                        <SelectItem value="CANCELED">
                          <span className="flex items-center gap-2"><XCircle className="h-3.5 w-3.5 text-red-500" /> Đã hủy</span>
                        </SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="paymentStatus"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-2">
                      <CreditCard className="h-4 w-4 text-muted-foreground" />
                      Trạng thái thanh toán
                    </FormLabel>
                    <Select
                      onValueChange={field.onChange}
                      defaultValue={field.value}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Chọn trạng thái thanh toán" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="UNPAID">Chưa thanh toán</SelectItem>
                        <SelectItem value="PAID">Đã thanh toán</SelectItem>
                        <SelectItem value="REFUNDED">Đã hoàn tiền</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <Separator />

              <div className="flex justify-end space-x-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setIsEditDialogOpen(false);
                    setEditingOrder(null);
                    form.reset();
                  }}
                >
                  Hủy
                </Button>
                <Button type="submit" disabled={updateMutation.isPending}>
                  {updateMutation.isPending && (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  )}
                  Cập nhật
                </Button>
              </div>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* Detail Dialog */}
      <Dialog
        open={isDetailDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            setIsDetailDialogOpen(false);
            setViewingOrder(null);
          }
        }}
      >
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto p-0">
          {viewingOrder && (
            <>
              {/* Dialog Header */}
              <div className="sticky top-0 z-10 bg-background border-b px-6 py-4">
                <div className="flex items-center justify-between">
                  <div>
                    <DialogTitle className="text-lg font-bold">
                      Đơn hàng #{viewingOrder.orderCode || viewingOrder.id?.slice(0, 12)}
                    </DialogTitle>
                    <DialogDescription className="mt-0.5">
                      {formatDate(viewingOrder.createdAt)}
                    </DialogDescription>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="outline" className={`gap-1.5 ${getOrderStatusColor(viewingOrder.status)}`}>
                      {getOrderStatusIcon(viewingOrder.status)}
                      {getOrderStatusLabel(viewingOrder.status)}
                    </Badge>
                    <Badge variant="outline" className={getPaymentStatusColor(viewingOrder.paymentStatus)}>
                      {getPaymentStatusLabel(viewingOrder.paymentStatus)}
                    </Badge>
                  </div>
                </div>
              </div>

              <div className="px-6 py-5 space-y-6">
                {/* Customer & Delivery Info */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <Card className="border-dashed">
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm font-medium flex items-center gap-2">
                        <User className="h-4 w-4 text-primary" />
                        Thông tin khách hàng
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3 text-sm">
                      <div className="flex items-start gap-2">
                        <User className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                        <span>{viewingOrder.nameRecipient || "-"}</span>
                      </div>
                      <div className="flex items-start gap-2">
                        <Phone className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                        <span>{viewingOrder.phoneRecipient || "-"}</span>
                      </div>
                      <div className="flex items-start gap-2">
                        <MapPin className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                        <span className="break-words">{viewingOrder.addressRecipient || "-"}</span>
                      </div>
                    </CardContent>
                  </Card>

                  <Card className="border-dashed">
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm font-medium flex items-center gap-2">
                        <CreditCard className="h-4 w-4 text-primary" />
                        Thông tin thanh toán
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3 text-sm">
                      <div className="flex items-center justify-between">
                        <span className="text-muted-foreground">Phương thức:</span>
                        <Badge variant="outline" className={`text-xs ${getPaymentMethodColor(viewingOrder.paymentMethod)}`}>
                          {getPaymentMethodLabel(viewingOrder.paymentMethod)}
                        </Badge>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-muted-foreground">Trạng thái:</span>
                        <Badge variant="outline" className={`text-xs ${getPaymentStatusColor(viewingOrder.paymentStatus)}`}>
                          {getPaymentStatusLabel(viewingOrder.paymentStatus)}
                        </Badge>
                      </div>
                      {viewingOrder.note && (
                        <div className="pt-2 border-t">
                          <div className="flex items-start gap-2">
                            <FileText className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                            <span className="text-muted-foreground italic">{viewingOrder.note}</span>
                          </div>
                        </div>
                      )}
                      <div className="pt-3 border-t">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold">Tổng tiền:</span>
                          <span className="text-xl font-bold text-primary">
                            {formatPrice(viewingOrder.totalPrice)}
                          </span>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </div>

                {/* Product Details */}
                {viewingOrder.orderDetails && viewingOrder.orderDetails.length > 0 && (
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm font-medium flex items-center gap-2">
                        <ShoppingBag className="h-4 w-4 text-primary" />
                        Sản phẩm đã đặt ({viewingOrder.orderDetails.length})
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="p-0">
                      <Table>
                        <TableHeader>
                          <TableRow className="bg-muted/30">
                            <TableHead>Sản phẩm</TableHead>
                            <TableHead className="text-center w-20">SL</TableHead>
                            <TableHead className="text-right w-32">Đơn giá</TableHead>
                            <TableHead className="text-right w-32">Thành tiền</TableHead>
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
                                      alt={detail.productName || ""}
                                      className="w-12 h-12 rounded-lg object-cover border border-border/50 flex-shrink-0"
                                    />
                                  ) : (
                                    <div className="w-12 h-12 rounded-lg bg-muted flex items-center justify-center flex-shrink-0">
                                      <Package className="h-5 w-5 text-muted-foreground" />
                                    </div>
                                  )}
                                  <div>
                                    <p className="text-sm font-medium line-clamp-1">
                                      {detail.productName || `SP #${detail.productId}`}
                                    </p>
                                    <p className="text-xs text-muted-foreground">
                                      ID: {detail.productId}
                                    </p>
                                  </div>
                                </div>
                              </TableCell>
                              <TableCell className="text-center font-medium">
                                {detail.quantity}
                              </TableCell>
                              <TableCell className="text-right text-sm">
                                {detail.unitPrice != null
                                  ? formatPrice(detail.unitPrice)
                                  : "-"}
                              </TableCell>
                              <TableCell className="text-right font-medium">
                                {detail.unitPrice != null
                                  ? formatPrice(detail.unitPrice * detail.quantity)
                                  : "-"}
                              </TableCell>
                            </TableRow>
                          ))}
                          <TableRow className="bg-muted/30 border-t-2">
                            <TableCell colSpan={3} className="text-right font-semibold">
                              Tổng cộng
                            </TableCell>
                            <TableCell className="text-right font-bold text-primary text-base">
                              {formatPrice(viewingOrder.totalPrice)}
                            </TableCell>
                          </TableRow>
                        </TableBody>
                      </Table>
                    </CardContent>
                  </Card>
                )}

                {/* Action Buttons */}
                <div className="flex justify-end gap-2 pt-2">
                  <Button
                    variant="outline"
                    onClick={() => {
                      setIsDetailDialogOpen(false);
                      setViewingOrder(null);
                    }}
                  >
                    Đóng
                  </Button>
                  <Button onClick={() => {
                    setIsDetailDialogOpen(false);
                    handleEdit(viewingOrder);
                  }}>
                    <Edit className="h-4 w-4 mr-2" />
                    Cập nhật trạng thái
                  </Button>
                </div>
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteConfirmOpen}
        onOpenChange={setDeleteConfirmOpen}
        onConfirm={confirmDelete}
        title="Xác nhận xóa đơn hàng"
        description="Bạn có chắc chắn muốn xóa đơn hàng này? Hành động này không thể hoàn tác."
        confirmText="Xóa"
        cancelText="Hủy"
        variant="destructive"
      />
    </div>
  );
};
