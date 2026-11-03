import { CheckCircle2, Clock, Package, RefreshCw, Truck, XCircle } from "lucide-react";
import type { OrderStatus, PaymentMethod, PaymentStatus } from "../types";

export const getOrderStatusLabel = (status?: OrderStatus) => {
  const labels: Record<OrderStatus, string> = {
    PENDING: "Chờ xử lý",
    PROCESSING: "Đang xử lý",
    SHIPPED: "Đang giao hàng",
    DELIVERED: "Đã giao hàng",
    CANCELED: "Đã hủy",
  };
  return status ? labels[status] : "-";
};

export const getOrderStatusIcon = (status?: OrderStatus) => {
  switch (status) {
    case "PENDING": return <Clock className="h-3.5 w-3.5" />;
    case "PROCESSING": return <RefreshCw className="h-3.5 w-3.5" />;
    case "SHIPPED": return <Truck className="h-3.5 w-3.5" />;
    case "DELIVERED": return <CheckCircle2 className="h-3.5 w-3.5" />;
    case "CANCELED": return <XCircle className="h-3.5 w-3.5" />;
    default: return <Package className="h-3.5 w-3.5" />;
  }
};

export const getOrderStatusColor = (status?: OrderStatus) => {
  switch (status) {
    case "PENDING": return "bg-amber-50 text-amber-700 border-amber-200";
    case "PROCESSING": return "bg-blue-50 text-blue-700 border-blue-200";
    case "SHIPPED": return "bg-indigo-50 text-indigo-700 border-indigo-200";
    case "DELIVERED": return "bg-emerald-50 text-emerald-700 border-emerald-200";
    case "CANCELED": return "bg-red-50 text-red-700 border-red-200";
    default: return "bg-gray-50 text-gray-700 border-gray-200";
  }
};

export const getPaymentStatusLabel = (status?: PaymentStatus) => {
  const labels: Record<PaymentStatus, string> = {
    UNPAID: "Chưa thanh toán",
    PAID: "Đã thanh toán",
    REFUNDED: "Đã hoàn tiền",
  };
  return status ? labels[status] : "-";
};

export const getPaymentStatusColor = (status?: PaymentStatus) => {
  switch (status) {
    case "UNPAID": return "bg-orange-50 text-orange-700 border-orange-200";
    case "PAID": return "bg-emerald-50 text-emerald-700 border-emerald-200";
    case "REFUNDED": return "bg-purple-50 text-purple-700 border-purple-200";
    default: return "bg-gray-50 text-gray-700 border-gray-200";
  }
};

export const getPaymentMethodLabel = (method?: PaymentMethod) => {
  const labels: Record<PaymentMethod, string> = {
    COD: "Thanh toán khi nhận hàng",
    BANK_TRANSFER: "Chuyển khoản ngân hàng",
  };
  return method ? labels[method] : "-";
};

export const getPaymentMethodColor = (method?: PaymentMethod) => {
  switch (method) {
    case "COD": return "bg-slate-50 text-slate-700 border-slate-200";
    case "BANK_TRANSFER": return "bg-sky-50 text-sky-700 border-sky-200";
    default: return "bg-gray-50 text-gray-700 border-gray-200";
  }
};

export const formatDate = (dateStr?: string) => {
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

// Status filter tabs config
export const STATUS_TABS = [
  { key: "ALL", label: "Tất cả", icon: Package },
  { key: "PENDING", label: "Chờ xử lý", icon: Clock },
  { key: "PROCESSING", label: "Đang xử lý", icon: RefreshCw },
  { key: "SHIPPED", label: "Đã gửi", icon: Truck },
  { key: "DELIVERED", label: "Đã giao", icon: CheckCircle2 },
  { key: "CANCELED", label: "Đã hủy", icon: XCircle },
];

export const PAYMENT_TABS = [
  { key: "ALL", label: "Tất cả" },
  { key: "UNPAID", label: "Chưa TT" },
  { key: "PAID", label: "Đã TT" },
  { key: "REFUNDED", label: "Hoàn tiền" },
];

