import { Edit, Images } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { formatPrice } from "@/utils/formatPrice";
import type { ProductResponse } from "../types";

interface ProductDetailDialogProps {
  open: boolean;
  product: ProductResponse | null;
  onOpenChange: (open: boolean) => void;
  onManageImages: (product: ProductResponse) => void;
  onEdit: (product: ProductResponse) => void;
}

export function ProductDetailDialog({
  open,
  product,
  onOpenChange,
  onManageImages,
  onEdit,
}: ProductDetailDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Chi tiết sản phẩm</DialogTitle>
          <DialogDescription>Thông tin chi tiết về sản phẩm</DialogDescription>
        </DialogHeader>

        {product && (
          <div className="space-y-6">
            <div className="flex justify-center">
              <div className="w-64 h-64 bg-gray-100 rounded-lg flex items-center justify-center">
                <img
                  src={product.primaryImageUrl || "/placeholder.svg"}
                  alt={product.name}
                  className="w-full h-full object-cover rounded-lg"
                  onError={(event) => {
                    if (!event.currentTarget.src.endsWith("/placeholder.svg")) {
                      event.currentTarget.src = "/placeholder.svg";
                    }
                  }}
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-4">
                <div>
                  <span className="text-sm font-medium text-gray-500">Tên sản phẩm</span>
                  <p className="text-lg font-semibold">{product.name}</p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-500">Giá</span>
                  <p className="text-lg font-semibold text-green-600">{formatPrice(product.unitPrice)}</p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-500">Danh mục</span>
                  <p>{product.categoryName || "Chưa có danh mục"}</p>
                </div>
              </div>
              <div className="space-y-4">
                <div>
                  <span className="text-sm font-medium text-gray-500">Thương hiệu</span>
                  <p>{product.brandName || "Chưa có thương hiệu"}</p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-500">Số lượng tồn kho</span>
                  <p>
                    <Badge variant={(product.stockQuantity || 0) > 10 ? "default" : (product.stockQuantity || 0) > 0 ? "secondary" : "destructive"}>
                      {product.stockQuantity || 0}
                    </Badge>
                  </p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-500">ID sản phẩm</span>
                  <p className="text-gray-600">#{product.id}</p>
                </div>
              </div>
            </div>

            {product.description && (
              <div>
                <span className="text-sm font-medium text-gray-500">Mô tả sản phẩm</span>
                <div className="mt-2 p-4 bg-gray-50 rounded-lg max-h-40 overflow-y-auto">
                  <p className="text-gray-700 whitespace-pre-wrap">{product.description}</p>
                </div>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-4 border-t">
              <Button variant="outline" onClick={() => onOpenChange(false)}>Đóng</Button>
              <Button variant="outline" onClick={() => { onOpenChange(false); onManageImages(product); }}>
                <Images className="w-4 h-4 mr-2" />Quản lý hình ảnh
              </Button>
              <Button onClick={() => { onOpenChange(false); onEdit(product); }}>
                <Edit className="w-4 h-4 mr-2" />Chỉnh sửa
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
