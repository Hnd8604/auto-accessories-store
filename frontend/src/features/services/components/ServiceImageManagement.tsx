import { useState, useRef, useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ServiceImagesApi } from "@/features/services/api/serviceImages";
import { Card, CardContent } from "@/components/ui/card";
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
import type { ServiceImageResponse, ServiceImageUpdateRequest } from "../types";

const imageSchema = z.object({
  altText: z.string().optional(),
  isPrimary: z.boolean().default(false),
  sortOrder: z.number().min(0),
});

type ImageFormData = z.infer<typeof imageSchema>;

interface ServiceImageManagementProps {
  serviceId: number;
  serviceName?: string;
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
}

export const ServiceImageManagement = ({
  serviceId,
  serviceName,
  isOpen,
  onOpenChange,
}: ServiceImageManagementProps) => {
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [editingImage, setEditingImage] = useState<ServiceImageResponse | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { toast } = useToast();
  const queryClient = useQueryClient();

  const form = useForm<ImageFormData>({
    resolver: zodResolver(imageSchema),
    defaultValues: { altText: "", isPrimary: false, sortOrder: 0 },
  });

  useEffect(() => {
    if (selectedFile) {
      const reader = new FileReader();
      reader.onload = (e) => setPreviewUrl(e.target?.result as string);
      reader.readAsDataURL(selectedFile);
    } else if (!editingImage) {
      setPreviewUrl("");
    }
  }, [selectedFile, editingImage]);

  useEffect(() => {
    if (!isAddDialogOpen && !isEditDialogOpen) {
      setPreviewUrl("");
      setSelectedFile(null);
    }
  }, [isAddDialogOpen, isEditDialogOpen]);

  const { data: imagesData, isLoading, error } = useQuery({
    queryKey: ["serviceImages", serviceId],
    queryFn: () => ServiceImagesApi.getByServiceId(serviceId),
    enabled: isOpen && !!serviceId,
  });

  const createMutation = useMutation({
    mutationFn: ({ file, isPrimary }: { file: File; isPrimary: boolean }) =>
      ServiceImagesApi.create(file, serviceId, isPrimary),
    onSuccess: () => {
      toast({ title: "Thành công!", description: "Hình ảnh đã được thêm." });
      queryClient.invalidateQueries({ queryKey: ["serviceImages", serviceId] });
      queryClient.invalidateQueries({ queryKey: ["services"] });
      setIsAddDialogOpen(false);
      setSelectedFile(null);
      form.reset();
    },
    onError: (error: any) => {
      toast({ variant: "destructive", title: "Lỗi", description: error.message || "Không thể thêm ảnh." });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: ServiceImageUpdateRequest }) =>
      ServiceImagesApi.update(id, data),
    onSuccess: () => {
      toast({ title: "Thành công!", description: "Hình ảnh đã được cập nhật." });
      queryClient.invalidateQueries({ queryKey: ["serviceImages", serviceId] });
      setIsEditDialogOpen(false);
      setEditingImage(null);
      form.reset();
    },
    onError: (error: any) => {
      toast({ variant: "destructive", title: "Lỗi", description: error.message || "Không thể cập nhật ảnh." });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ServiceImagesApi.delete,
    onSuccess: () => {
      toast({ title: "Thành công!", description: "Hình ảnh đã được xóa." });
      queryClient.invalidateQueries({ queryKey: ["serviceImages", serviceId] });
      queryClient.invalidateQueries({ queryKey: ["services"] });
    },
    onError: (error: any) => {
      toast({ variant: "destructive", title: "Lỗi", description: error.message || "Không thể xóa ảnh." });
    },
  });

  const setPrimaryMutation = useMutation({
    mutationFn: (imageId: number) => ServiceImagesApi.setPrimary(serviceId, imageId),
    onSuccess: () => {
      toast({ title: "Thành công!", description: "Đã đặt làm ảnh chính." });
      queryClient.invalidateQueries({ queryKey: ["serviceImages", serviceId] });
      queryClient.invalidateQueries({ queryKey: ["services"] });
    },
    onError: (error: any) => {
      toast({ variant: "destructive", title: "Lỗi", description: error.message || "Không thể đặt ảnh chính." });
    },
  });

  const onSubmit = (data: ImageFormData) => {
    if (editingImage) {
      updateMutation.mutate({
        id: editingImage.id,
        data: { altText: data.altText || "", isPrimary: data.isPrimary, sortOrder: data.sortOrder },
      });
    } else if (selectedFile) {
      createMutation.mutate({ file: selectedFile, isPrimary: data.isPrimary });
    }
  };

  const handleEdit = (image: ServiceImageResponse) => {
    setEditingImage(image);
    form.reset({ altText: image.altText || "", isPrimary: image.isPrimary, sortOrder: image.sortOrder });
    setPreviewUrl(image.imageUrl);
    setSelectedFile(null);
    setIsEditDialogOpen(true);
  };

  const handleDelete = (imageId: number) => {
    if (confirm("Bạn có chắc chắn muốn xóa hình ảnh này?")) {
      deleteMutation.mutate(imageId);
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      toast({ variant: "destructive", title: "Lỗi", description: "Vui lòng chọn file hình ảnh hợp lệ." });
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      toast({ variant: "destructive", title: "Lỗi", description: "Kích thước file không được vượt quá 5MB." });
      return;
    }
    setSelectedFile(file);
    form.reset({ altText: "", isPrimary: false, sortOrder: images.length });
    setIsAddDialogOpen(true);
  };

  const images = imagesData?.result || [];

  const imageFormFields = (isEdit: boolean) => (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        {previewUrl && (
          <div className="w-full aspect-video rounded-lg overflow-hidden border bg-muted">
            <img src={previewUrl} alt="Preview" className="w-full h-full object-cover" />
          </div>
        )}
        <FormField control={form.control} name="altText" render={({ field }) => (
          <FormItem>
            <FormLabel>Mô tả ảnh (caption)</FormLabel>
            <FormControl>
              <Input placeholder="VD: Độ đèn Bi-LED Toyota Camry..." {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
        <FormField control={form.control} name="sortOrder" render={({ field }) => (
          <FormItem>
            <FormLabel>Thứ tự hiển thị</FormLabel>
            <FormControl>
              <Input type="number" placeholder="0" {...field} onChange={(e) => field.onChange(parseInt(e.target.value) || 0)} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
        <FormField control={form.control} name="isPrimary" render={({ field }) => (
          <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
            <div>
              <FormLabel className="text-base">Ảnh đại diện</FormLabel>
              <p className="text-sm text-muted-foreground">Hiển thị trên trang chủ (card dịch vụ)</p>
            </div>
            <FormControl>
              <Switch checked={field.value} onCheckedChange={field.onChange} />
            </FormControl>
          </FormItem>
        )} />
        <div className="flex justify-end gap-2 pt-4">
          <Button type="button" variant="outline" onClick={() => isEdit ? setIsEditDialogOpen(false) : setIsAddDialogOpen(false)}>Hủy</Button>
          <Button type="submit" disabled={isEdit ? updateMutation.isPending : (createMutation.isPending || !selectedFile)}>
            {(isEdit ? updateMutation.isPending : createMutation.isPending) && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isEdit ? "Cập nhật" : "Thêm ảnh"}
          </Button>
        </div>
      </form>
    </Form>
  );

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Quản lý ảnh dịch vụ</DialogTitle>
          <DialogDescription>Ảnh cho dịch vụ: <strong>{serviceName}</strong></DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <span className="font-semibold">Hình ảnh ({images.length})</span>
            <div>
              <input ref={fileInputRef} type="file" accept="image/*" onChange={handleFileUpload} className="hidden" />
              <Button onClick={() => fileInputRef.current?.click()}>
                <Upload className="h-4 w-4 mr-2" />Thêm ảnh
              </Button>
            </div>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-8"><Loader2 className="h-8 w-8 animate-spin" /></div>
          ) : error ? (
            <p className="text-center text-destructive">Lỗi tải ảnh.</p>
          ) : images.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              <ImageIcon className="h-12 w-12 mx-auto mb-4 opacity-40" />
              <p>Chưa có ảnh nào. Hãy thêm ảnh công trình thực tế!</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              {images.map((image) => (
                <Card key={image.id}>
                  <CardContent className="p-3">
                    <div className="aspect-video relative mb-2 rounded overflow-hidden bg-muted">
                      <img src={image.imageUrl} alt={image.altText || "Service image"} className="w-full h-full object-cover"
                        onError={(e) => { (e.target as HTMLImageElement).src = "/placeholder.svg"; }} />
                      {image.isPrimary && (
                        <Badge className="absolute top-1 left-1 bg-yellow-500 hover:bg-yellow-600 text-xs">
                          <Star className="h-3 w-3 mr-1" />Đại diện
                        </Badge>
                      )}
                    </div>
                    <div className="flex justify-between items-start gap-1">
                      <p className="text-xs text-muted-foreground truncate flex-1">{image.altText || "Không có mô tả"}</p>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" className="h-6 w-6 p-0"><MoreHorizontal className="h-3 w-3" /></Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          {!image.isPrimary && (
                            <DropdownMenuItem onClick={() => setPrimaryMutation.mutate(image.id)}>
                              <Star className="h-4 w-4 mr-2" />Đặt đại diện
                            </DropdownMenuItem>
                          )}
                          <DropdownMenuItem onClick={() => handleEdit(image)}>
                            <Edit className="h-4 w-4 mr-2" />Chỉnh sửa
                          </DropdownMenuItem>
                          <DropdownMenuItem className="text-destructive" onClick={() => handleDelete(image.id)}>
                            <Trash2 className="h-4 w-4 mr-2" />Xóa
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>

        <Dialog open={isAddDialogOpen} onOpenChange={(open) => { setIsAddDialogOpen(open); if (!open) { setSelectedFile(null); form.reset(); } }}>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>Thêm ảnh mới</DialogTitle>
              <DialogDescription>Thêm ảnh công trình thực tế cho dịch vụ</DialogDescription>
            </DialogHeader>
            {imageFormFields(false)}
          </DialogContent>
        </Dialog>

        <Dialog open={isEditDialogOpen} onOpenChange={(open) => { setIsEditDialogOpen(open); if (!open) { setEditingImage(null); form.reset(); } }}>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>Chỉnh sửa ảnh</DialogTitle>
              <DialogDescription>Cập nhật thông tin ảnh</DialogDescription>
            </DialogHeader>
            {imageFormFields(true)}
          </DialogContent>
        </Dialog>
      </DialogContent>
    </Dialog>
  );
};
