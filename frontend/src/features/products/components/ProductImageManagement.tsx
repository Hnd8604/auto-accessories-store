import { useState, useRef, useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ProductImagesApi } from "@/features/products/api/productImages";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
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
import { Switch } from "@/components/ui/switch";
import {
  MoreHorizontal,
  Edit,
  Trash2,
  Loader2,
  Upload,
  Star,
  Image as ImageIcon,
} from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import type { ProductImageRequest, ProductImageResponse, ProductImageUpdateRequest } from "../types";

const imageSchema = z.object({
  altText: z.string().optional(),
  isPrimary: z.boolean().default(false),
  sortOrder: z.number().min(0, { message: "Thứ tự phải lớn hơn hoặc bằng 0." }),
});

type ImageFormData = z.infer<typeof imageSchema>;

// Image Preview Component
const ImagePreview = ({
  imageUrl,
  isLoading,
  onLoadStart,
  onLoadEnd,
  onError,
}: {
  imageUrl: string;
  isLoading: boolean;
  onLoadStart: () => void;
  onLoadEnd: () => void;
  onError: () => void;
}) => (
  <div className="space-y-2">
    <label className="text-sm font-medium">Preview</label>
    <div className="w-full max-w-xs mx-auto">
      <div className="aspect-square rounded-lg border-2 border-dashed border-gray-300 overflow-hidden bg-gray-50 relative">
        {isLoading && (
          <div className="absolute inset-0 flex items-center justify-center bg-gray-100 z-10">
            <Loader2 className="h-6 w-6 animate-spin text-gray-500" />
          </div>
        )}
        <img
          src={imageUrl}
          alt="Preview"
          className="w-full h-full object-cover"
          onLoadStart={onLoadStart}
          onError={(e) => {
            const target = e.target as HTMLImageElement;
            onError();
            target.style.display = "none";
            target.nextElementSibling?.classList.remove("hidden");
          }}
          onLoad={onLoadEnd}
        />
        <div className="hidden w-full h-full flex items-center justify-center text-gray-500">
          <div className="text-center">
            <ImageIcon className="h-8 w-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">Không thể tải ảnh</p>
          </div>
        </div>
      </div>
    </div>
  </div>
);

interface ProductImageManagementProps {
  productId: number;
  productName?: string;
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
}

export const ProductImageManagement = ({
  productId,
  productName,
  isOpen,
  onOpenChange,
}: ProductImageManagementProps) => {
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [editingImage, setEditingImage] = useState<ProductImageResponse | null>(
    null
  );
  const [previewImageUrl, setPreviewImageUrl] = useState<string>("");
  const [isPreviewLoading, setIsPreviewLoading] = useState<boolean>(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Form setup
  const form = useForm<ImageFormData>({
    resolver: zodResolver(imageSchema),
    defaultValues: {
      altText: "",
      isPrimary: false,
      sortOrder: 0,
    },
  });

  // Watch for selected file changes for preview
  useEffect(() => {
    if (selectedFile) {
      setIsPreviewLoading(true);
      const reader = new FileReader();
      reader.onload = (e) => {
        setPreviewImageUrl(e.target?.result as string);
        setIsPreviewLoading(false);
      };
      reader.onerror = () => {
        setIsPreviewLoading(false);
        setPreviewImageUrl("");
      };
      reader.readAsDataURL(selectedFile);
    } else {
      setPreviewImageUrl("");
      setIsPreviewLoading(false);
    }
  }, [selectedFile]);

  // Reset preview when dialog closes
  useEffect(() => {
    if (!isAddDialogOpen && !isEditDialogOpen) {
      setPreviewImageUrl("");
      setIsPreviewLoading(false);
      setSelectedFile(null);
    }
  }, [isAddDialogOpen, isEditDialogOpen]);

  // Query: Get product images
  const {
    data: imagesData,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["productImages", productId],
    queryFn: () => ProductImagesApi.getByProductId(productId),
    enabled: isOpen && !!productId,
  });

  // Mutation: Create image
  const createMutation = useMutation({
    mutationFn: ({
      file,
      isPrimary,
      altText,
      sortOrder,
    }: {
      file: File;
      isPrimary: boolean;
      altText?: string;
      sortOrder?: number;
    }) =>
      ProductImagesApi.createWithMetadata(
        file,
        productId,
        isPrimary,
        altText,
        sortOrder
      ),
    onSuccess: (response, variables) => {
      const wasPrimarySet = variables.isPrimary && hasPrimaryImage;
      toast({
        title: "Thành công!",
        description: wasPrimarySet
          ? "Hình ảnh đã được thêm và đặt làm ảnh chính. Ảnh chính trước đó đã trở thành ảnh phụ."
          : "Hình ảnh đã được thêm thành công.",
      });
      queryClient.invalidateQueries({ queryKey: ["productImages", productId] });
      setIsAddDialogOpen(false);
      setSelectedFile(null);
      form.reset();
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Lỗi thêm hình ảnh",
        description: error.message || "Đã có lỗi xảy ra khi thêm hình ảnh.",
      });
    },
  });

  // Mutation: Update image
  const updateMutation = useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: number;
      data: ProductImageUpdateRequest;
    }) => ProductImagesApi.update(id, data),
    onSuccess: (response, variables) => {
      const wasPrimarySet = variables.data.isPrimary && hasPrimaryImage;
      toast({
        title: "Thành công!",
        description: wasPrimarySet
          ? "Hình ảnh đã được cập nhật và đặt làm ảnh chính. Ảnh chính trước đó đã trở thành ảnh phụ."
          : "Hình ảnh đã được cập nhật thành công.",
      });
      queryClient.invalidateQueries({ queryKey: ["productImages", productId] });
      setIsEditDialogOpen(false);
      setEditingImage(null);
      form.reset();
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Lỗi cập nhật hình ảnh",
        description: error.message || "Đã có lỗi xảy ra khi cập nhật hình ảnh.",
      });
    },
  });

  // Mutation: Delete image
  const deleteMutation = useMutation({
    mutationFn: ProductImagesApi.delete,
    onSuccess: () => {
      toast({
        title: "Thành công!",
        description: "Hình ảnh đã được xóa thành công.",
      });
      queryClient.invalidateQueries({ queryKey: ["productImages", productId] });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Lỗi xóa hình ảnh",
        description: error.message || "Đã có lỗi xảy ra khi xóa hình ảnh.",
      });
    },
  });

  // Mutation: Set primary image
  const setPrimaryMutation = useMutation({
    mutationFn: ({ imageId }: { imageId: number }) =>
      ProductImagesApi.setPrimary(productId, imageId),
    onSuccess: () => {
      toast({
        title: "Thành công!",
        description: hasPrimaryImage
          ? "Đã đặt làm hình ảnh chính. Ảnh chính trước đó đã trở thành ảnh phụ."
          : "Đã đặt làm hình ảnh chính thành công.",
      });
      queryClient.invalidateQueries({ queryKey: ["productImages", productId] });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Lỗi đặt hình ảnh chính",
        description:
          error.message || "Đã có lỗi xảy ra khi đặt hình ảnh chính.",
      });
    },
  });

  // Handle form submission
  const onSubmit = async (data: ImageFormData) => {
    console.log("Form data before submit:", data); // Debug log

    if (!editingImage && !selectedFile) {
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: "Vui lòng chọn file hình ảnh.",
      });
      return;
    }

    // If setting as primary, we need to unset other primary images first
    if (data.isPrimary) {
      const currentImages = imagesData?.result || [];
      const otherPrimaryImages = currentImages.filter(
        (img) => img.isPrimary && img.id !== editingImage?.id
      );

      // Unset other primary images first
      for (const img of otherPrimaryImages) {
        try {
          await ProductImagesApi.update(img.id, {
            altText: img.altText || "",
            isPrimary: false,
            sortOrder: img.sortOrder,
          });
        } catch (error) {
          console.error("Error unsetting primary image:", error);
        }
      }
    }

    if (editingImage) {
      const updateRequest: ProductImageUpdateRequest = {
        altText: data.altText || "",
        isPrimary: data.isPrimary,
        sortOrder: data.sortOrder,
      };
      updateMutation.mutate({ id: editingImage.id, data: updateRequest });
    } else if (selectedFile) {
      createMutation.mutate({
        file: selectedFile,
        isPrimary: data.isPrimary,
        altText: data.altText,
        sortOrder: data.sortOrder,
      });
    }
  };

  // Handle edit button click
  const handleEdit = (image: ProductImageResponse) => {
    setEditingImage(image);
    form.reset({
      altText: image.altText || "",
      isPrimary: image.isPrimary,
      sortOrder: image.sortOrder,
    });
    // For edit, show current image as preview
    setPreviewImageUrl(image.imageUrl);
    setSelectedFile(null);
    setIsEditDialogOpen(true);
  };

  // Handle delete button click
  const handleDelete = (imageId: number) => {
    if (confirm("Bạn có chắc chắn muốn xóa hình ảnh này?")) {
      deleteMutation.mutate(imageId);
    }
  };

  // Handle set primary
  const handleSetPrimary = async (imageId: number) => {
    // Unset other primary images first
    const currentImages = imagesData?.result || [];
    const otherPrimaryImages = currentImages.filter(
      (img) => img.isPrimary && img.id !== imageId
    );

    // Unset other primary images
    for (const img of otherPrimaryImages) {
      try {
        await ProductImagesApi.update(img.id, {
          altText: img.altText || "",
          isPrimary: false,
          sortOrder: img.sortOrder,
        });
      } catch (error) {
        console.error("Error unsetting primary image:", error);
      }
    }

    // Then set the new primary image
    setPrimaryMutation.mutate({ imageId });
  };

  // Handle file upload
  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith("image/")) {
        toast({
          variant: "destructive",
          title: "Lỗi",
          description: "Vui lòng chọn file hình ảnh hợp lệ.",
        });
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        toast({
          variant: "destructive",
          title: "Lỗi",
          description: "Kích thước file không được vượt quá 5MB.",
        });
        return;
      }

      setSelectedFile(file);
      const currentImages = imagesData?.result || [];
      form.reset({
        altText: "",
        isPrimary: false,
        sortOrder: currentImages.length, // Auto set sort order
      });
      setIsAddDialogOpen(true);
    }
  };

  const images = imagesData?.result || [];

  // Check if there's already a primary image
  const hasPrimaryImage = images.some(
    (img) => img.isPrimary && img.id !== editingImage?.id
  );
  const currentPrimaryImage = images.find(
    (img) => img.isPrimary && img.id !== editingImage?.id
  );

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Quản lý hình ảnh sản phẩm</DialogTitle>
          <DialogDescription>
            Quản lý hình ảnh cho sản phẩm: {productName}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Header Actions */}
          <div className="flex justify-between items-center">
            <h3 className="text-lg font-semibold">
              Hình ảnh ({images.length})
            </h3>
            <div className="flex gap-2">
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileUpload}
                className="hidden"
              />
              <Button onClick={() => fileInputRef.current?.click()}>
                <Upload className="h-4 w-4 mr-2" />
                Chọn hình ảnh
              </Button>
            </div>
          </div>

          {/* Images Grid */}
          {isLoading ? (
            <div className="flex justify-center items-center py-8">
              <Loader2 className="h-8 w-8 animate-spin" />
              <span className="ml-2">Đang tải...</span>
            </div>
          ) : error ? (
            <div className="text-center text-red-600">
              <p>Lỗi tải hình ảnh: {(error as Error).message}</p>
            </div>
          ) : images.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              <ImageIcon className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Chưa có hình ảnh nào. Hãy thêm hình ảnh đầu tiên!</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {images.map((image) => (
                <Card key={image.id} className="relative">
                  <CardContent className="p-4">
                    <div className="aspect-square relative mb-3">
                      <img
                        src={image.imageUrl}
                        alt={image.altText || "Product image"}
                        className="w-full h-full object-cover rounded-lg"
                        onError={(e) => {
                          const target = e.target as HTMLImageElement;
                          target.src = "/placeholder.svg";
                        }}
                      />
                      {image.isPrimary && (
                        <Badge className="absolute top-2 left-2 bg-yellow-500 hover:bg-yellow-600">
                          <Star className="h-3 w-3 mr-1" />
                          Chính
                        </Badge>
                      )}
                    </div>

                    <div className="space-y-2">
                      <div className="flex justify-between items-start">
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium truncate">
                            {image.altText || "Không có mô tả"}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            Thứ tự: {image.sortOrder}
                          </p>
                        </div>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="h-8 w-8 p-0">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            {!image.isPrimary && (
                              <DropdownMenuItem
                                onClick={() => handleSetPrimary(image.id)}
                                disabled={setPrimaryMutation.isPending}
                              >
                                <Star className="h-4 w-4 mr-2" />
                                Đặt làm chính
                              </DropdownMenuItem>
                            )}
                            <DropdownMenuItem onClick={() => handleEdit(image)}>
                              <Edit className="h-4 w-4 mr-2" />
                              Chỉnh sửa
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              className="text-red-600"
                              onClick={() => handleDelete(image.id)}
                            >
                              <Trash2 className="h-4 w-4 mr-2" />
                              Xóa
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>

        {/* Add Image Dialog */}
        <Dialog
          open={isAddDialogOpen}
          onOpenChange={(open) => {
            setIsAddDialogOpen(open);
            if (!open) {
              setSelectedFile(null);
              form.reset();
            }
          }}
        >
          <DialogContent className="max-w-md max-h-[80vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Thêm hình ảnh mới</DialogTitle>
              <DialogDescription>
                Thêm hình ảnh mới cho sản phẩm
              </DialogDescription>
            </DialogHeader>

            <Form {...form}>
              <form
                onSubmit={form.handleSubmit(onSubmit)}
                className="space-y-4"
              >
                {/* Selected File Info */}
                {selectedFile && (
                  <div className="space-y-2">
                    <label className="text-sm font-medium">
                      File được chọn
                    </label>
                    <div className="p-3 border rounded-lg bg-gray-50">
                      <div className="flex items-center gap-2">
                        <Upload className="h-4 w-4 text-gray-500" />
                        <span className="text-sm font-medium">
                          {selectedFile.name}
                        </span>
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                      </div>
                    </div>
                  </div>
                )}

                {/* Image Preview */}
                {previewImageUrl && (
                  <ImagePreview
                    imageUrl={previewImageUrl}
                    isLoading={isPreviewLoading}
                    onLoadStart={() => setIsPreviewLoading(true)}
                    onLoadEnd={() => setIsPreviewLoading(false)}
                    onError={() => setIsPreviewLoading(false)}
                  />
                )}

                <FormField
                  control={form.control}
                  name="altText"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Mô tả hình ảnh</FormLabel>
                      <FormControl>
                        <Input
                          placeholder="Mô tả ngắn gọn về hình ảnh..."
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="sortOrder"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Thứ tự hiển thị</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="0"
                          {...field}
                          onChange={(e) =>
                            field.onChange(parseInt(e.target.value) || 0)
                          }
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="isPrimary"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                      <div className="space-y-0.5">
                        <FormLabel className="text-base">
                          Hình ảnh chính
                        </FormLabel>
                        <p className="text-sm text-muted-foreground">
                          {hasPrimaryImage
                            ? `Đặt làm hình ảnh chính. Ảnh "${
                                currentPrimaryImage?.altText || "hiện tại"
                              }" sẽ trở thành ảnh phụ.`
                            : "Đặt làm hình ảnh chính của sản phẩm."}
                        </p>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value}
                          onCheckedChange={field.onChange}
                        />
                      </FormControl>
                    </FormItem>
                  )}
                />

                <div className="flex justify-end gap-2 pt-4">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setIsAddDialogOpen(false);
                      setSelectedFile(null);
                      form.reset();
                    }}
                  >
                    Hủy
                  </Button>
                  <Button
                    type="submit"
                    disabled={createMutation.isPending || !selectedFile}
                  >
                    {createMutation.isPending && (
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    )}
                    Thêm hình ảnh
                  </Button>
                </div>
              </form>
            </Form>
          </DialogContent>
        </Dialog>

        {/* Edit Image Dialog */}
        <Dialog
          open={isEditDialogOpen}
          onOpenChange={(open) => {
            setIsEditDialogOpen(open);
            if (!open) {
              setEditingImage(null);
              form.reset();
            }
          }}
        >
          <DialogContent className="max-w-md max-h-[80vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Chỉnh sửa hình ảnh</DialogTitle>
              <DialogDescription>Cập nhật thông tin hình ảnh</DialogDescription>
            </DialogHeader>

            <Form {...form}>
              <form
                onSubmit={form.handleSubmit(onSubmit)}
                className="space-y-4"
              >
                {/* Image Preview */}
                {previewImageUrl && (
                  <ImagePreview
                    imageUrl={previewImageUrl}
                    isLoading={isPreviewLoading}
                    onLoadStart={() => setIsPreviewLoading(true)}
                    onLoadEnd={() => setIsPreviewLoading(false)}
                    onError={() => setIsPreviewLoading(false)}
                  />
                )}

                <FormField
                  control={form.control}
                  name="altText"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Mô tả hình ảnh</FormLabel>
                      <FormControl>
                        <Input
                          placeholder="Mô tả ngắn gọn về hình ảnh..."
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="sortOrder"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Thứ tự hiển thị</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="0"
                          {...field}
                          onChange={(e) =>
                            field.onChange(parseInt(e.target.value) || 0)
                          }
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="isPrimary"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                      <div className="space-y-0.5">
                        <FormLabel className="text-base">
                          Hình ảnh chính
                        </FormLabel>
                        <p className="text-sm text-muted-foreground">
                          {hasPrimaryImage
                            ? `Đặt làm hình ảnh chính. Ảnh "${
                                currentPrimaryImage?.altText || "hiện tại"
                              }" sẽ trở thành ảnh phụ.`
                            : "Đặt làm hình ảnh chính của sản phẩm."}
                        </p>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value}
                          onCheckedChange={field.onChange}
                        />
                      </FormControl>
                    </FormItem>
                  )}
                />

                <div className="flex justify-end gap-2 pt-4">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setIsEditDialogOpen(false)}
                  >
                    Hủy
                  </Button>
                  <Button type="submit" disabled={updateMutation.isPending}>
                    {updateMutation.isPending && (
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    )}
                    Cập nhật
                  </Button>
                </div>
              </form>
            </Form>
          </DialogContent>
        </Dialog>
      </DialogContent>
    </Dialog>
  );
};
