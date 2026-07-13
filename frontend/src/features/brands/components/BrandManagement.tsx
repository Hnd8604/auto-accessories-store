import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { BrandsApi } from "@/features/brands/api/brands";
import { useToast } from "@/hooks/use-toast";
import type { BrandResponse, BrandRequest } from "@/features/products/types";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
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
import { Plus, MoreHorizontal, Eye, Edit, Trash2, Loader2 } from "lucide-react";

// Validation schema
const brandSchema = z.object({
  name: z.string().min(1, "Tên thương hiệu là bắt buộc"),
  description: z.string().optional(),
});

type BrandFormData = z.infer<typeof brandSchema>;

interface BrandManagementProps {
  className?: string;
}

export const BrandManagement: React.FC<BrandManagementProps> = ({
  className,
}) => {
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false);
  const [editingBrand, setEditingBrand] = useState<BrandResponse | null>(null);
  const [viewingBrand, setViewingBrand] = useState<BrandResponse | null>(null);

  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Form setup
  const form = useForm<BrandFormData>({
    resolver: zodResolver(brandSchema),
    defaultValues: {
      name: "",
      description: "",
    },
  });

  // Query: Get all brands
  const {
    data: brandsData,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["brands"],
    queryFn: BrandsApi.getAll,
  });

  // Mutation: Create brand
  const createMutation = useMutation({
    mutationFn: BrandsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["brands"] });
      setIsCreateDialogOpen(false);
      form.reset();
      toast({
        title: "Thành công",
        description: "Thương hiệu đã được tạo thành công",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Lỗi",
        description: error.message || "Có lỗi xảy ra khi tạo thương hiệu",
        variant: "destructive",
      });
    },
  });

  // Mutation: Update brand
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: BrandRequest }) =>
      BrandsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["brands"] });
      setIsEditDialogOpen(false);
      setEditingBrand(null);
      form.reset();
      toast({
        title: "Thành công",
        description: "Thương hiệu đã được cập nhật thành công",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Lỗi",
        description: error.message || "Có lỗi xảy ra khi cập nhật thương hiệu",
        variant: "destructive",
      });
    },
  });

  // Mutation: Delete brand
  const deleteMutation = useMutation({
    mutationFn: BrandsApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["brands"] });
      toast({
        title: "Thành công",
        description: "Thương hiệu đã được xóa thành công",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Lỗi",
        description: error.message || "Có lỗi xảy ra khi xóa thương hiệu",
        variant: "destructive",
      });
    },
  });

  // Handle form submission
  const onSubmit = (data: BrandFormData) => {
    const brandRequest: BrandRequest = {
      name: data.name,
      description: data.description || undefined,
    };

    if (editingBrand) {
      updateMutation.mutate({ id: editingBrand.id, data: brandRequest });
    } else {
      createMutation.mutate(brandRequest);
    }
  };

  // Handle edit button click
  const handleEdit = (brand: BrandResponse) => {
    setEditingBrand(brand);
    form.reset({
      name: brand.name,
      description: brand.description || "",
    });
    setIsEditDialogOpen(true);
  };

  // Handle delete button click
  const handleDelete = (brandId: number) => {
    if (confirm("Bạn có chắc chắn muốn xóa thương hiệu này?")) {
      deleteMutation.mutate(brandId);
    }
  };

  // Handle view detail button click
  const handleViewDetail = (brand: BrandResponse) => {
    setViewingBrand(brand);
    setIsDetailDialogOpen(true);
  };

  // Get brands from response
  const brands = brandsData?.result || [];

  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-center text-red-600">
            Có lỗi xảy ra khi tải danh sách thương hiệu
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className={className}>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Quản lý Thương hiệu</CardTitle>
              <CardDescription>
                Quản lý thương hiệu sản phẩm trong hệ thống
              </CardDescription>
            </div>
            <Button onClick={() => setIsCreateDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Thêm thương hiệu
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center p-8">
              <Loader2 className="h-8 w-8 animate-spin" />
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Tên thương hiệu</TableHead>
                  <TableHead>Mô tả</TableHead>
                  <TableHead className="text-center">Số sản phẩm</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {brands.map((brand) => (
                  <TableRow key={brand.id}>
                    <TableCell className="font-mono text-sm">
                      {brand.id}
                    </TableCell>
                    <TableCell className="font-medium">{brand.name}</TableCell>
                    <TableCell className="max-w-xs truncate">
                      {brand.description || "Không có mô tả"}
                    </TableCell>
                    <TableCell className="text-center">
                      {brand.productCount ?? 0}
                    </TableCell>
                    <TableCell className="text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" className="h-8 w-8 p-0">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleEdit(brand)}>
                            <Edit className="h-4 w-4 mr-2" />
                            Chỉnh sửa
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            className="text-red-600"
                            onClick={() => handleDelete(brand.id)}
                          >
                            <Trash2 className="h-4 w-4 mr-2" />
                            Xóa
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog
        open={isCreateDialogOpen || isEditDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            setIsCreateDialogOpen(false);
            setIsEditDialogOpen(false);
            setEditingBrand(null);
            form.reset();
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {editingBrand ? "Chỉnh sửa thương hiệu" : "Tạo thương hiệu mới"}
            </DialogTitle>
            <DialogDescription>
              {editingBrand
                ? "Cập nhật thông tin thương hiệu"
                : "Nhập thông tin để tạo thương hiệu mới"}
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tên thương hiệu *</FormLabel>
                    <FormControl>
                      <Input 
                        placeholder="Nhập tên thương hiệu..." 
                        value={field.value || ""}
                        onChange={(e) => field.onChange(e.target.value)}
                        onBlur={field.onBlur}
                        name={field.name}
                        ref={field.ref}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Mô tả</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="Mô tả chi tiết thương hiệu..."
                        className="min-h-[100px]"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="flex justify-end gap-2 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setIsCreateDialogOpen(false);
                    setIsEditDialogOpen(false);
                    setEditingBrand(null);
                    form.reset();
                  }}
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  disabled={
                    createMutation.isPending || updateMutation.isPending
                  }
                >
                  {(createMutation.isPending || updateMutation.isPending) && (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  )}
                  {editingBrand ? "Cập nhật" : "Tạo thương hiệu"}
                </Button>
              </div>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* Brand Detail Dialog */}
      <Dialog
        open={isDetailDialogOpen}
        onOpenChange={(open) => {
          setIsDetailDialogOpen(open);
          if (!open) setViewingBrand(null);
        }}
      >
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Chi tiết thương hiệu</DialogTitle>
            <DialogDescription>
              Thông tin chi tiết về thương hiệu
            </DialogDescription>
          </DialogHeader>

          {viewingBrand && (
            <div className="space-y-6">
              {/* Brand Information */}
              <div className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-gray-500">
                    ID thương hiệu
                  </label>
                  <p className="text-base text-gray-600 font-mono">
                    #{viewingBrand.id}
                  </p>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500">
                    Tên thương hiệu
                  </label>
                  <p className="text-lg font-semibold">{viewingBrand.name}</p>
                </div>

                {viewingBrand.description && (
                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Mô tả
                    </label>
                    <div className="mt-2 p-4 bg-gray-50 rounded-lg max-h-40 overflow-y-auto">
                      <p className="text-base text-gray-700 whitespace-pre-wrap">
                        {viewingBrand.description}
                      </p>
                    </div>
                  </div>
                )}
              </div>

              {/* Action buttons */}
              <div className="flex justify-end gap-2 pt-4 border-t">
                <Button
                  variant="outline"
                  onClick={() => {
                    setIsDetailDialogOpen(false);
                    setViewingBrand(null);
                  }}
                >
                  Đóng
                </Button>
                <Button
                  onClick={() => {
                    setIsDetailDialogOpen(false);
                    handleEdit(viewingBrand);
                  }}
                >
                  <Edit className="w-4 h-4 mr-2" />
                  Chỉnh sửa
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};
