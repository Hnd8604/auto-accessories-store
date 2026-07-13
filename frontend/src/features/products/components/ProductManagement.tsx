import { useEffect, useRef, useState, useCallback } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { ProductsApi } from "@/features/products/api/products";
import { ProductImagesApi } from "@/features/products/api/productImages";
import { CategoriesApi } from "@/features/categories/api/categories";
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
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
  Plus,
  MoreHorizontal,
  Eye,
  Edit,
  Trash2,
  Loader2,
  Images,
  Upload,
  Star,
  X,
  ImagePlus,
} from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { ProductImageManagement } from "@/features/products/components/ProductImageManagement";
import type { ProductRequest, ProductResponse } from "../types";

// ---- Image upload types ----
interface UploadImageItem {
  id: string;
  file: File;
  previewUrl: string;
  isPrimary: boolean;
}

const productSchema = z.object({
  name: z.string().min(1, { message: "Tên sản phẩm không được để trống." }),
  description: z.string().optional(),
  unitPrice: z.number().min(0, { message: "Giá phải lớn hơn hoặc bằng 0." }),
  categoryId: z.number().min(1, { message: "Vui lòng chọn danh mục." }),
  brandId: z.number().optional().nullable(),
  stockQuantity: z
    .number()
    .min(0, { message: "Số lượng tồn kho phải lớn hơn hoặc bằng 0." })
    .optional(),
});

type ProductFormData = z.infer<typeof productSchema>;

interface ProductManagementProps {
  categoryOptions?: Array<{ id: number; name: string }>;
  brandOptions?: Array<{ id: number; name: string }>;
}

export const ProductManagement = ({
  categoryOptions = [],
  brandOptions = [],
}: ProductManagementProps) => {
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false);
  const [isImageManagementOpen, setIsImageManagementOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ProductResponse | null>(
    null
  );
  const [viewingProduct, setViewingProduct] = useState<ProductResponse | null>(
    null
  );
  const [managingImagesProduct, setManagingImagesProduct] =
    useState<ProductResponse | null>(null);
  const [currentPage, setCurrentPage] = useState(0);

  // Image upload state (for create product)
  const [uploadImages, setUploadImages] = useState<UploadImageItem[]>([]);
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const [isUploadingImages, setIsUploadingImages] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const pageSize = 10;

  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Form setup
  const form = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: "",
      description: "",
      unitPrice: 0,
      categoryId: 0,
      brandId: undefined,
      stockQuantity: 0,
    },
  });

  const selectedCategoryId = form.watch("categoryId");
  const { data: categoryBrandsData, isLoading: isLoadingCategoryBrands } = useQuery({
    queryKey: ["category-brands", selectedCategoryId],
    queryFn: () => CategoriesApi.getBrandsByCategory(selectedCategoryId),
    enabled: selectedCategoryId > 0,
  });

  const categoryBrandOptions = categoryBrandsData?.result || [];

  useEffect(() => {
    if (!selectedCategoryId) {
      form.setValue("brandId", undefined);
      return;
    }
    const currentBrandId = form.getValues("brandId");
    if (
      currentBrandId &&
      !categoryBrandOptions.some((brand) => brand.id === currentBrandId)
    ) {
      form.setValue("brandId", undefined);
    }
  }, [categoryBrandOptions, form, selectedCategoryId]);

  // Query: Get products with pagination
  const {
    data: productsData,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["products", currentPage, pageSize],
    queryFn: () => ProductsApi.getAll({ page: currentPage, size: pageSize }),
  });



  // ---- Image upload helpers ----
  const addFiles = useCallback((files: FileList | File[]) => {
    const newItems: UploadImageItem[] = Array.from(files)
      .filter((f) => f.type.startsWith("image/"))
      .map((file) => ({
        id: `${Date.now()}-${Math.random()}`,
        file,
        previewUrl: URL.createObjectURL(file),
        isPrimary: false,
      }));

    setUploadImages((prev) => {
      const merged = [...prev, ...newItems];
      // If no primary yet, auto-set first one
      if (merged.length > 0 && !merged.some((i) => i.isPrimary)) {
        merged[0].isPrimary = true;
      }
      return merged;
    });
  }, []);

  const removeImage = (id: string) => {
    setUploadImages((prev) => {
      const remaining = prev.filter((i) => i.id !== id);
      // Re-assign primary if the removed one was primary
      if (remaining.length > 0 && !remaining.some((i) => i.isPrimary)) {
        remaining[0].isPrimary = true;
      }
      return remaining;
    });
  };

  const setPrimaryImage = (id: string) => {
    setUploadImages((prev) =>
      prev.map((i) => ({ ...i, isPrimary: i.id === id }))
    );
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) addFiles(e.target.files);
    e.target.value = "";
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingOver(true);
  };

  const handleDragLeave = () => setIsDraggingOver(false);

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingOver(false);
    if (e.dataTransfer.files) addFiles(e.dataTransfer.files);
  };

  const resetUploadImages = () => {
    uploadImages.forEach((i) => URL.revokeObjectURL(i.previewUrl));
    setUploadImages([]);
  };

  // Mutation: Create product
  const createMutation = useMutation({
    mutationFn: ProductsApi.create,
    onSuccess: async (response) => {
      const newProductId = response.result?.id;

      // Upload images if any
      if (newProductId && uploadImages.length > 0) {
        setIsUploadingImages(true);
        try {
          await Promise.all(
            uploadImages.map((item) =>
              ProductImagesApi.create(item.file, newProductId, item.isPrimary)
            )
          );
        } catch {
          toast({
            variant: "destructive",
            title: "Lỗi tải ảnh",
            description: "Sản phẩm đã tạo nhưng một số ảnh tải lên thất bại.",
          });
        } finally {
          setIsUploadingImages(false);
        }
      }

      toast({
        title: "Thành công!",
        description: "Sản phẩm đã được tạo thành công.",
      });
      queryClient.invalidateQueries({ queryKey: ["products"] });

      setIsCreateDialogOpen(false);
      form.reset();
      resetUploadImages();
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Lỗi tạo sản phẩm",
        description: error.message || "Đã có lỗi xảy ra khi tạo sản phẩm.",
      });
    },
  });

  // Mutation: Update product
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: ProductRequest }) =>
      ProductsApi.update(id, data),
    onSuccess: () => {
      toast({
        title: "Thành công!",
        description: "Sản phẩm đã được cập nhật thành công.",
      });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      setIsEditDialogOpen(false);
      setEditingProduct(null);
      form.reset();
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Lỗi cập nhật sản phẩm",
        description: error.message || "Đã có lỗi xảy ra khi cập nhật sản phẩm.",
      });
    },
  });

  // Mutation: Delete product
  const deleteMutation = useMutation({
    mutationFn: ProductsApi.delete,
    onSuccess: () => {
      toast({
        title: "Thành công!",
        description: "Sản phẩm đã được xóa thành công.",
      });
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Lỗi xóa sản phẩm",
        description: error.message || "Đã có lỗi xảy ra khi xóa sản phẩm.",
      });
    },
  });

  // Handle form submission
  const onSubmit = (data: ProductFormData) => {
    const productRequest: ProductRequest = {
      name: data.name,
      description: data.description || "",
      unitPrice: data.unitPrice,
      categoryId: data.categoryId,
      brandId: data.brandId || undefined,
      stockQuantity: data.stockQuantity || 0,
    };

    if (editingProduct) {
      updateMutation.mutate({ id: editingProduct.id, data: productRequest });
    } else {
      createMutation.mutate(productRequest);
    }
  };

  // Handle edit button click
  const handleEdit = (product: ProductResponse) => {
    setEditingProduct(product);
    form.reset({
      name: product.name,
      description: product.description || "",
      unitPrice: product.unitPrice,
      categoryId:
        categoryOptions.find((c) => c.name === product.categoryName)?.id || 0,
      brandId: product.brandName ? brandOptions.find((b) => b.name === product.brandName)?.id || undefined : undefined,
      stockQuantity: product.stockQuantity || 0,
    });
    setIsEditDialogOpen(true);
  };

  // Handle delete button click
  const handleDelete = (productId: number) => {
    if (confirm("Bạn có chắc chắn muốn xóa sản phẩm này?")) {
      deleteMutation.mutate(productId);
    }
  };

  // Handle view detail button click
  const handleViewDetail = (product: ProductResponse) => {
    setViewingProduct(product);
    setIsDetailDialogOpen(true);
  };

  // Handle manage images button click
  const handleManageImages = (product: ProductResponse) => {
    setManagingImagesProduct(product);
    setIsImageManagementOpen(true);
  };

  // Get products from response
  const products = productsData?.result?.content || [];
  const totalPages = productsData?.result?.totalPages || 0;

  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-center text-red-600">
            <p>Lỗi tải dữ liệu: {error?.message || 'Unknown error'}</p>
            <Button
              variant="outline"
              onClick={() =>
                queryClient.invalidateQueries({ queryKey: ["products"] })
              }
              className="mt-2"
            >
              Thử lại
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold">Quản lý sản phẩm</h2>
        <Button onClick={() => setIsCreateDialogOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Thêm sản phẩm
        </Button>
      </div>

      {/* Products Table */}
      <Card>
        <CardHeader>
          <CardTitle>Danh sách sản phẩm</CardTitle>
          <CardDescription>
            Quản lý sản phẩm dịch vụ nội thất ô tô ({products.length} sản phẩm)
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex justify-center items-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
              <span className="ml-2">Đang tải...</span>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Hình ảnh</TableHead>
                  <TableHead>Sản phẩm</TableHead>
                  <TableHead>Danh mục</TableHead>
                  <TableHead>Thương hiệu</TableHead>
                  <TableHead>Giá</TableHead>
                  <TableHead>Tồn kho</TableHead>
                  <TableHead>Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {products.map((product) => (
                  <TableRow key={product.id}>
                    <TableCell>
                      <div className="w-12 h-12 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
                        <img
                          src={
                            product.primaryImageUrl || "/placeholder.svg"
                          }
                          alt={product.name}
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            const target = e.target as HTMLImageElement;
                            target.src = "/placeholder.svg";
                          }}
                        />
                      </div>
                    </TableCell>
                    <TableCell>
                      <div>
                        <div className="font-medium">{product.name}</div>
                        <div className="text-sm text-muted-foreground">
                          ID: {product.id}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">
                        {product.categoryName || "N/A"}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">
                        {product.brandName || "N/A"}
                      </Badge>
                    </TableCell>
                    <TableCell className="font-medium">
                      {product.unitPrice.toLocaleString()} VND
                    </TableCell>
                    <TableCell>
                      <span
                        className={
                          product.stockQuantity === 0 ? "text-red-600" : ""
                        }
                      >
                        {product.stockQuantity || 0}
                      </span>
                    </TableCell>
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" className="h-8 w-8 p-0">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem
                            onClick={() => handleViewDetail(product)}
                          >
                            <Eye className="h-4 w-4 mr-2" />
                            Xem chi tiết
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => handleManageImages(product)}
                          >
                            <Images className="h-4 w-4 mr-2" />
                            Quản lý hình ảnh
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => handleEdit(product)}>
                            <Edit className="h-4 w-4 mr-2" />
                            Chỉnh sửa
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            className="text-red-600"
                            onClick={() => handleDelete(product.id)}
                          >
                            <Trash2 className="h-4 w-4 mr-2" />
                            Xóa
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
                {products.length === 0 && (
                  <TableRow>
                    <TableCell
                      colSpan={7}
                      className="text-center py-8 text-muted-foreground"
                    >
                      Chưa có sản phẩm nào. Hãy thêm sản phẩm đầu tiên!
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-between items-center mt-4">
              <p className="text-sm text-muted-foreground">
                Trang {currentPage + 1} / {totalPages}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                >
                  Trước
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() =>
                    setCurrentPage(Math.min(totalPages - 1, currentPage + 1))
                  }
                >
                  Tiếp
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Create/Edit Product Dialog */}
      <Dialog
        open={isCreateDialogOpen || isEditDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            setIsCreateDialogOpen(false);
            setIsEditDialogOpen(false);
            setEditingProduct(null);
            form.reset();
            resetUploadImages();
          }
        }}
      >
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingProduct ? "Chỉnh sửa sản phẩm" : "Thêm sản phẩm mới"}
            </DialogTitle>
            <DialogDescription>
              {editingProduct
                ? "Cập nhật thông tin sản phẩm"
                : "Tạo sản phẩm mới cho hệ thống"}
            </DialogDescription>
          </DialogHeader>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="name"
                  render={({ field }) => (
                    <FormItem className="md:col-span-2">
                      <FormLabel>Tên sản phẩm</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập tên sản phẩm" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="categoryId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Danh mục</FormLabel>
                      <Select
                        value={field.value?.toString() || ""}
                        onValueChange={(value) =>
                          field.onChange(Number.parseInt(value))
                        }
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Chọn danh mục" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {categoryOptions.map((category) => (
                            <SelectItem
                              key={category.id}
                              value={category.id.toString()}
                            >
                              {category.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="brandId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Thương hiệu <span className="text-gray-500 text-sm">(Tùy chọn)</span></FormLabel>
                      <Select
                        value={field.value?.toString() || ""}
                        onValueChange={(value) =>
                          field.onChange(value ? Number.parseInt(value) : undefined)
                        }
                        disabled={!selectedCategoryId}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder={selectedCategoryId ? "Chọn thương hiệu" : "Chọn danh mục trước"} />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {isLoadingCategoryBrands && (
                            <SelectItem value="loading" disabled>
                              Đang tải...
                            </SelectItem>
                          )}
                          {categoryBrandOptions.map((brand) => (
                            <SelectItem
                              key={brand.id}
                              value={brand.id.toString()}
                            >
                              {brand.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="unitPrice"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Giá (VND)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="0"
                          value={field.value || ""}
                          onChange={(e) => {
                            const value = e.target.value;
                            field.onChange(value === "" ? 0 : Number.parseInt(value));
                          }}
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
                  name="stockQuantity"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Tồn kho</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="0"
                          value={field.value || ""}
                          onChange={(e) => {
                            const value = e.target.value;
                            field.onChange(value === "" ? 0 : Number.parseInt(value));
                          }}
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
                  name="description"
                  render={({ field }) => (
                    <FormItem className="md:col-span-2">
                      <FormLabel>Mô tả</FormLabel>
                      <FormControl>
                        <Textarea
                          placeholder="Mô tả chi tiết sản phẩm..."
                          className="min-h-[100px]"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* Image Upload Section (only for create) */}
              {!editingProduct && (
                <div className="md:col-span-2 space-y-3">
                  <FormLabel>Hình ảnh sản phẩm</FormLabel>

                  {/* Drop zone */}
                  <div
                    onDragOver={handleDragOver}
                    onDragLeave={handleDragLeave}
                    onDrop={handleDrop}
                    onClick={() => fileInputRef.current?.click()}
                    className={`relative flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed cursor-pointer transition-colors py-6 ${
                      isDraggingOver
                        ? "border-primary bg-primary/5"
                        : "border-gray-300 hover:border-primary/60 hover:bg-gray-50"
                    }`}
                  >
                    <Upload className="h-7 w-7 text-gray-400" />
                    <p className="text-sm text-gray-500">
                      Kéo thả ảnh vào đây hoặc{" "}
                      <span className="text-primary font-medium">chọn file</span>
                    </p>
                    <p className="text-xs text-gray-400">
                      PNG, JPG, WEBP (có thể chọn nhiều ảnh)
                    </p>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/*"
                      multiple
                      className="hidden"
                      onChange={handleFileInputChange}
                    />
                  </div>

                  {/* Image preview grid */}
                  {uploadImages.length > 0 && (
                    <div className="grid grid-cols-3 sm:grid-cols-4 gap-3 mt-2">
                      {uploadImages.map((item) => (
                        <div
                          key={item.id}
                          className={`relative group rounded-lg overflow-hidden border-2 transition-all ${
                            item.isPrimary
                              ? "border-yellow-400 shadow-md"
                              : "border-gray-200 hover:border-gray-300"
                          }`}
                        >
                          <img
                            src={item.previewUrl}
                            alt="preview"
                            className="w-full h-24 object-cover"
                          />

                          {/* Primary badge */}
                          {item.isPrimary && (
                            <span className="absolute top-1 left-1 bg-yellow-400 text-yellow-900 text-[10px] font-bold px-1.5 py-0.5 rounded-full flex items-center gap-0.5">
                              <Star className="h-2.5 w-2.5 fill-yellow-900" />
                              Chính
                            </span>
                          )}

                          {/* Overlay actions */}
                          <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1.5">
                            {!item.isPrimary && (
                              <button
                                type="button"
                                title="Đặt làm ảnh chính"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setPrimaryImage(item.id);
                                }}
                                className="bg-yellow-400 hover:bg-yellow-300 text-yellow-900 rounded-full p-1.5 transition-colors"
                              >
                                <Star className="h-3.5 w-3.5 fill-yellow-900" />
                              </button>
                            )}
                            <button
                              type="button"
                              title="Xóa ảnh"
                              onClick={(e) => {
                                e.stopPropagation();
                                removeImage(item.id);
                              }}
                              className="bg-red-500 hover:bg-red-400 text-white rounded-full p-1.5 transition-colors"
                            >
                              <X className="h-3.5 w-3.5" />
                            </button>
                          </div>
                        </div>
                      ))}

                      {/* Add more button */}
                      <button
                        type="button"
                        onClick={() => fileInputRef.current?.click()}
                        className="flex flex-col items-center justify-center gap-1 rounded-lg border-2 border-dashed border-gray-300 hover:border-primary/60 hover:bg-gray-50 h-24 transition-colors text-gray-400 hover:text-primary"
                      >
                        <ImagePlus className="h-5 w-5" />
                        <span className="text-[11px]">Thêm</span>
                      </button>
                    </div>
                  )}

                  {uploadImages.length > 0 && (
                    <p className="text-xs text-gray-400">
                      {uploadImages.length} ảnh đã chọn · Click{" "}
                      <Star className="inline h-3 w-3 fill-yellow-400 text-yellow-400" />{" "}
                      để đặt ảnh chính
                    </p>
                  )}
                </div>
              )}

              <div className="flex justify-end gap-2 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setIsCreateDialogOpen(false);
                    setIsEditDialogOpen(false);
                    setEditingProduct(null);
                    form.reset();
                    resetUploadImages();
                  }}
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  disabled={
                    createMutation.isPending ||
                    updateMutation.isPending ||
                    isUploadingImages
                  }
                >
                  {(createMutation.isPending ||
                    updateMutation.isPending ||
                    isUploadingImages) && (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  )}
                  {isUploadingImages
                    ? "Đang tải ảnh..."
                    : editingProduct
                    ? "Cập nhật"
                    : "Tạo sản phẩm"}
                </Button>
              </div>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* Product Detail Dialog */}
      <Dialog
        open={isDetailDialogOpen}
        onOpenChange={(open) => {
          setIsDetailDialogOpen(open);
          if (!open) setViewingProduct(null);
        }}
      >
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Chi tiết sản phẩm</DialogTitle>
            <DialogDescription>
              Thông tin chi tiết về sản phẩm
            </DialogDescription>
          </DialogHeader>

          {viewingProduct && (
            <div className="space-y-6">
              {/* Product Image */}
              <div className="flex justify-center">
                <div className="w-64 h-64 bg-gray-100 rounded-lg flex items-center justify-center">
                  <img
                    src={
                      viewingProduct.primaryImageUrl ||
                      "/placeholder.svg"
                    }
                    alt={viewingProduct.name}
                    className="w-full h-full object-cover rounded-lg"
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.src = "/placeholder.svg";
                    }}
                  />
                </div>
              </div>

              {/* Product Information */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-4">
                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Tên sản phẩm
                    </label>
                    <p className="text-lg font-semibold">
                      {viewingProduct.name}
                    </p>
                  </div>

                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Giá
                    </label>
                    <p className="text-lg font-semibold text-green-600">
                      {viewingProduct.unitPrice?.toLocaleString("vi-VN")} VNĐ
                    </p>
                  </div>

                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Danh mục
                    </label>
                    <p className="text-base">
                      {viewingProduct.categoryName || "Chưa có danh mục"}
                    </p>
                  </div>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Thương hiệu
                    </label>
                    <p className="text-base">
                      {viewingProduct.brandName || "Chưa có thương hiệu"}
                    </p>
                  </div>

                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Số lượng tồn kho
                    </label>
                    <p className="text-base">
                      <Badge
                        variant={
                          (viewingProduct.stockQuantity || 0) > 10
                            ? "default"
                            : (viewingProduct.stockQuantity || 0) > 0
                            ? "secondary"
                            : "destructive"
                        }
                      >
                        {viewingProduct.stockQuantity || 0}
                      </Badge>
                    </p>
                  </div>

                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      ID sản phẩm
                    </label>
                    <p className="text-base text-gray-600">
                      #{viewingProduct.id}
                    </p>
                  </div>
                </div>
              </div>

              {/* Product Description */}
              {viewingProduct.description && (
                <div>
                  <label className="text-sm font-medium text-gray-500">
                    Mô tả sản phẩm
                  </label>
                  <div className="mt-2 p-4 bg-gray-50 rounded-lg max-h-40 overflow-y-auto">
                    <p className="text-base text-gray-700 whitespace-pre-wrap">
                      {viewingProduct.description}
                    </p>
                  </div>
                </div>
              )}

              {/* Action buttons */}
              <div className="flex justify-end gap-2 pt-4 border-t">
                <Button
                  variant="outline"
                  onClick={() => {
                    setIsDetailDialogOpen(false);
                    setViewingProduct(null);
                  }}
                >
                  Đóng
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    setIsDetailDialogOpen(false);
                    handleManageImages(viewingProduct);
                  }}
                >
                  <Images className="w-4 h-4 mr-2" />
                  Quản lý hình ảnh
                </Button>
                <Button
                  onClick={() => {
                    setIsDetailDialogOpen(false);
                    handleEdit(viewingProduct);
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

      {/* Product Image Management */}
      {managingImagesProduct && (
        <ProductImageManagement
          productId={managingImagesProduct.id}
          productName={managingImagesProduct.name}
          isOpen={isImageManagementOpen}
          onOpenChange={(open) => {
            setIsImageManagementOpen(open);
            if (!open) {
              setManagingImagesProduct(null);
              // Refetch products to update primaryImageUrl in the table
              queryClient.invalidateQueries({ queryKey: ["products"] });
            }
          }}
        />
      )}
    </div>
  );
};

export default ProductManagement;
