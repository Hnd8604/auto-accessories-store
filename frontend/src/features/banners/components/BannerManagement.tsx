import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Loader2, Pencil, Trash2, Eye, Upload, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
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
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/hooks/use-toast";
import { BannersApi } from "@/features/banners/api";
import type { BannerResponse, BannerRequest } from "@/features/banners/types";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { MoreHorizontal } from "lucide-react";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";

const bannerSchema = z.object({
  title: z.string().optional(),
  subtitle: z.string().optional(),
  redirectUrl: z.string().url("URL không hợp lệ").optional().or(z.literal("")),
  altText: z.string().optional(),
  buttonText: z.string().optional(),
  displayOrder: z.number().int().min(0).optional(),
  isActive: z.boolean(),
});

type BannerFormData = z.infer<typeof bannerSchema>;

export const BannerManagement = () => {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [selectedBanner, setSelectedBanner] = useState<BannerResponse | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>("");
  const [dragActive, setDragActive] = useState(false);

  const form = useForm<BannerFormData>({
    resolver: zodResolver(bannerSchema),
    defaultValues: {
      title: "",
      subtitle: "",
      redirectUrl: "",
      altText: "",
      buttonText: "",
      displayOrder: 0,
      isActive: true,
    },
  });

  // Fetch banners
  const { data: bannersData, isLoading } = useQuery({
    queryKey: ["banners"],
    queryFn: BannersApi.getAll,
  });

  const banners = bannersData?.result || [];

  // Create mutation
  const createMutation = useMutation({
    mutationFn: ({ file, data }: { file: File; data: BannerRequest }) =>
      BannersApi.create(file, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["banners"] });
      toast({
        title: "Thành công",
        description: "Đã tạo banner mới",
      });
      setIsCreateDialogOpen(false);
      form.reset();
      setSelectedFile(null);
      setPreviewUrl("");
    },
    onError: (error: Error) => {
      toast({
        title: "Lỗi",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data, file }: { id: number; data: BannerRequest; file?: File }) =>
      BannersApi.update(id, data, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["banners"] });
      toast({
        title: "Thành công",
        description: "Đã cập nhật banner",
      });
      setIsEditDialogOpen(false);
      setSelectedBanner(null);
      form.reset();
      setSelectedFile(null);
      setPreviewUrl("");
    },
    onError: (error: Error) => {
      toast({
        title: "Lỗi",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (id: number) => BannersApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["banners"] });
      toast({
        title: "Thành công",
        description: "Đã xóa banner",
      });
      setIsDeleteDialogOpen(false);
      setSelectedBanner(null);
    },
    onError: (error: Error) => {
      toast({
        title: "Lỗi",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const handleCreate = () => {
    form.reset({
      title: "",
      subtitle: "",
      redirectUrl: "",
      altText: "",
      buttonText: "",
      displayOrder: 0,
      isActive: true,
    });
    setSelectedFile(null);
    setPreviewUrl("");
    setIsCreateDialogOpen(true);
  };

  const handleEdit = (banner: BannerResponse) => {
    setSelectedBanner(banner);
    form.reset({
      title: banner.title || "",
      subtitle: banner.subtitle || "",
      redirectUrl: banner.redirectUrl || "",
      altText: banner.altText || "",
      buttonText: banner.buttonText || "",
      displayOrder: banner.displayOrder ?? 0,
      isActive: banner.isActive,
    });
    setSelectedFile(null);
    setPreviewUrl(banner.imageUrl);
    setIsEditDialogOpen(true);
  };

  const handleView = (banner: BannerResponse) => {
    setSelectedBanner(banner);
    setIsViewDialogOpen(true);
  };

  const handleDelete = (banner: BannerResponse) => {
    setSelectedBanner(banner);
    setIsDeleteDialogOpen(true);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewUrl(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith("image/")) {
      setSelectedFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewUrl(reader.result as string);
      };
      reader.readAsDataURL(file);
    } else if (file) {
      toast({
        title: "Lỗi",
        description: "Vui lòng chọn file hình ảnh",
        variant: "destructive",
      });
    }
  };

  const onSubmitCreate = (data: BannerFormData) => {
    if (!selectedFile) {
      toast({
        title: "Lỗi",
        description: "Vui lòng chọn hình ảnh",
        variant: "destructive",
      });
      return;
    }
    createMutation.mutate({ file: selectedFile, data: { ...data, isActive: data.isActive ?? true } });
  };

  const onSubmitEdit = (data: BannerFormData) => {
    if (!selectedBanner) return;
    updateMutation.mutate({
      id: selectedBanner.id,
      data: { ...data, isActive: data.isActive ?? true },
      file: selectedFile || undefined,
    });
  };

  const confirmDelete = () => {
    if (selectedBanner) {
      deleteMutation.mutate(selectedBanner.id);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Quản lý Banner</h2>
        <Button onClick={handleCreate}>
          <Plus className="mr-2 h-4 w-4" />
          Tạo Banner
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Danh sách Banner</CardTitle>
          <CardDescription>
            Quản lý banner hiển thị trên trang chủ
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : banners.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">
              Chưa có banner nào
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Hình ảnh</TableHead>
                  <TableHead>Tiêu đề</TableHead>
                  <TableHead>Thứ tự</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {banners.map((banner) => (
                  <TableRow key={banner.id}>
                    <TableCell className="font-mono text-sm">
                      {banner.id}
                    </TableCell>
                    <TableCell>
                      <img
                        src={banner.imageUrl}
                        alt={banner.altText || banner.title}
                        className="h-16 w-24 object-cover rounded"
                      />
                    </TableCell>
                    <TableCell className="font-medium">
                      {banner.title || "Không có tiêu đề"}
                    </TableCell>
                    <TableCell>{banner.displayOrder ?? 0}</TableCell>
                    <TableCell>
                      {banner.isActive ? (
                        <Badge className="bg-green-500">Hiển thị</Badge>
                      ) : (
                        <Badge variant="secondary">Ẩn</Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" className="h-8 w-8 p-0">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleView(banner)}>
                            <Eye className="mr-2 h-4 w-4" />
                            Xem chi tiết
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => handleEdit(banner)}>
                            <Pencil className="mr-2 h-4 w-4" />
                            Chỉnh sửa
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => handleDelete(banner)}
                            className="text-destructive"
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
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

      {/* Create Dialog */}
      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] flex flex-col">
          <DialogHeader>
            <DialogTitle>Tạo Banner Mới</DialogTitle>
            <DialogDescription>
              Thêm banner mới để hiển thị trên trang chủ
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmitCreate)} className="space-y-4 flex flex-col flex-1 overflow-hidden">
              <div className="overflow-y-auto px-1 -mx-1 space-y-4">
                <div>
                  <FormLabel>Hình ảnh *</FormLabel>
                  <div className="mt-2">
                    {previewUrl ? (
                      <div className="space-y-2">
                        <div className="relative">
                          <img
                            src={previewUrl}
                            alt="Preview"
                            className="max-h-64 w-full object-contain rounded-lg border"
                          />
                          <Button
                            type="button"
                            variant="destructive"
                            size="icon"
                            className="absolute top-2 right-2"
                            onClick={() => {
                              setSelectedFile(null);
                              setPreviewUrl("");
                            }}
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        </div>
                        {selectedFile && (
                          <p className="text-sm text-muted-foreground">
                            {selectedFile.name}
                          </p>
                        )}
                      </div>
                    ) : (
                      <div className="flex items-center justify-center w-full">
                        <label
                          htmlFor="create-file-upload"
                          className={`flex flex-col items-center justify-center w-full h-32 border-2 border-dashed rounded-lg cursor-pointer transition-colors ${
                            dragActive
                              ? "bg-primary/10 border-primary"
                              : "bg-muted/50 hover:bg-muted border-border"
                          }`}
                          onDragEnter={handleDrag}
                          onDragLeave={handleDrag}
                          onDragOver={handleDrag}
                          onDrop={handleDrop}
                        >
                          <div className="flex flex-col items-center justify-center pt-5 pb-6">
                            <Upload className="w-8 h-8 mb-2 text-muted-foreground" />
                            <p className="mb-2 text-sm text-muted-foreground">
                              <span className="font-semibold">Chọn hình ảnh</span> hoặc kéo thả vào đây
                            </p>
                            <p className="text-xs text-muted-foreground">PNG, JPG, WEBP</p>
                          </div>
                          <Input
                            id="create-file-upload"
                            type="file"
                            accept="image/*"
                            className="hidden"
                            onChange={handleFileChange}
                            required
                          />
                        </label>
                      </div>
                    )}
                  </div>
                </div>

                <FormField
                  control={form.control}
                  name="title"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Tiêu đề</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập tiêu đề banner" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="redirectUrl"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>URL chuyển hướng</FormLabel>
                      <FormControl>
                        <Input placeholder="https://example.com" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="altText"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Text thay thế</FormLabel>
                      <FormControl>
                        <Input placeholder="Mô tả hình ảnh" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="displayOrder"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Thứ tự hiển thị</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          {...field}
                          onChange={(e) => field.onChange(Number(e.target.value))}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="isActive"
                  render={({ field }) => (
                    <FormItem className="flex items-center justify-between rounded-lg border p-4">
                      <div className="space-y-0.5">
                        <FormLabel>Hiển thị banner</FormLabel>
                        <FormDescription>
                          Bật để hiển thị banner trên trang chủ
                        </FormDescription>
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
              </div>

              <DialogFooter className="mt-4 pt-4 border-t">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsCreateDialogOpen(false)}
                >
                  Hủy
                </Button>
                <Button type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending && (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  )}
                  Tạo Banner
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] flex flex-col">
          <DialogHeader>
            <DialogTitle>Cập Nhật Banner</DialogTitle>
            <DialogDescription>
              Chỉnh sửa thông tin banner
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmitEdit)} className="space-y-4 flex flex-col flex-1 overflow-hidden">
              <div className="overflow-y-auto px-1 -mx-1 space-y-4">
                <div>
                  <FormLabel>Hình ảnh</FormLabel>
                  <div className="mt-2">
                    {previewUrl ? (
                      <div className="space-y-2">
                        <div className="relative">
                          <img
                            src={previewUrl}
                            alt="Preview"
                            className="max-h-64 w-full object-contain rounded-lg border bg-muted/30"
                          />
                          <Button
                            type="button"
                            variant="destructive"
                            size="icon"
                            className="absolute top-2 right-2"
                            onClick={() => {
                              setSelectedFile(null);
                              setPreviewUrl("");
                            }}
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        </div>
                        {selectedFile && (
                          <p className="text-sm text-muted-foreground">
                            {selectedFile.name}
                          </p>
                        )}
                        {!selectedFile && selectedBanner && (
                           <p className="text-sm text-muted-foreground">
                             Hình ảnh hiện tại. Xóa để tải lên ảnh mới.
                           </p>
                        )}
                      </div>
                    ) : (
                      <div className="flex items-center justify-center w-full">
                        <label
                          htmlFor="edit-file-upload"
                          className={`flex flex-col items-center justify-center w-full h-32 border-2 border-dashed rounded-lg cursor-pointer transition-colors ${
                            dragActive
                              ? "bg-primary/10 border-primary"
                              : "bg-muted/50 hover:bg-muted border-border"
                          }`}
                          onDragEnter={handleDrag}
                          onDragLeave={handleDrag}
                          onDragOver={handleDrag}
                          onDrop={handleDrop}
                        >
                          <div className="flex flex-col items-center justify-center pt-5 pb-6">
                            <Upload className="w-8 h-8 mb-2 text-muted-foreground" />
                            <p className="mb-2 text-sm text-muted-foreground">
                              <span className="font-semibold">Chọn hình ảnh mới</span> hoặc kéo thả vào đây
                            </p>
                            <p className="text-xs text-muted-foreground">PNG, JPG, WEBP</p>
                          </div>
                          <Input
                            id="edit-file-upload"
                            type="file"
                            accept="image/*"
                            className="hidden"
                            onChange={handleFileChange}
                          />
                        </label>
                      </div>
                    )}
                  </div>
                </div>

                <FormField
                  control={form.control}
                  name="title"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Tiêu đề</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập tiêu đề banner" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="subtitle"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Mô tả phụ (Subtitle)</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập mô tả phụ" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="redirectUrl"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>URL chuyển hướng</FormLabel>
                      <FormControl>
                        <Input placeholder="https://example.com" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="buttonText"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Text nút bấm (CTA)</FormLabel>
                      <FormControl>
                        <Input placeholder="Ví dụ: Xem chi tiết" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="altText"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Text thay thế</FormLabel>
                      <FormControl>
                        <Input placeholder="Mô tả hình ảnh" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="displayOrder"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Thứ tự hiển thị</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          {...field}
                          onChange={(e) => field.onChange(Number(e.target.value))}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="isActive"
                  render={({ field }) => (
                    <FormItem className="flex items-center justify-between rounded-lg border p-4">
                      <div className="space-y-0.5">
                        <FormLabel>Hiển thị banner</FormLabel>
                        <FormDescription>
                          Bật để hiển thị banner trên trang chủ
                        </FormDescription>
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
              </div>

              <DialogFooter className="mt-4 pt-4 border-t">
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
                  Cập Nhật
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* View Dialog */}
      {selectedBanner && (
        <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>Chi Tiết Banner</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <img
                  src={selectedBanner.imageUrl}
                  alt={selectedBanner.altText || selectedBanner.title}
                  className="w-full rounded-lg"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground">ID</p>
                  <p className="font-medium">{selectedBanner.id}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Tiêu đề</p>
                  <p className="font-medium">{selectedBanner.title || "N/A"}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Mô tả phụ</p>
                  <p className="font-medium">{selectedBanner.subtitle || "N/A"}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Text nút bấm</p>
                  <p className="font-medium">{selectedBanner.buttonText || "N/A"}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Thứ tự</p>
                  <p className="font-medium">{selectedBanner.displayOrder ?? 0}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Trạng thái</p>
                  <Badge className={selectedBanner.isActive ? "bg-green-500" : ""}>
                    {selectedBanner.isActive ? "Hiển thị" : "Ẩn"}
                  </Badge>
                </div>
                {selectedBanner.redirectUrl && (
                  <div className="col-span-2">
                    <p className="text-sm text-muted-foreground">URL chuyển hướng</p>
                    <a
                      href={selectedBanner.redirectUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-primary hover:underline"
                    >
                      {selectedBanner.redirectUrl}
                    </a>
                  </div>
                )}
              </div>
            </div>
          </DialogContent>
        </Dialog>
      )}

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={isDeleteDialogOpen}
        onOpenChange={setIsDeleteDialogOpen}
        title="Xác nhận xóa"
        description={`Bạn có chắc chắn muốn xóa banner "${selectedBanner?.title || 'này'}"? Hành động này không thể hoàn tác.`}
        onConfirm={confirmDelete}
        variant="destructive"
      />
    </div>
  );
};
