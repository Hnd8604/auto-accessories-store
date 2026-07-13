import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { ServicesApi } from "@/features/services/api/services";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import {
  Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Form, FormControl, FormField, FormItem, FormLabel, FormMessage,
} from "@/components/ui/form";
import { Plus, MoreHorizontal, Edit, Trash2, Loader2, Images, X, ImageIcon } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { ServiceImageManagement } from "./ServiceImageManagement";
import type { ServiceRequest, ServiceResponse } from "../types";

const serviceSchema = z.object({
  name: z.string().min(1, "Tên dịch vụ không được để trống."),
  shortDescription: z.string().optional(),
  fullDescription: z.string().optional(),
  features: z.array(z.object({ value: z.string() })),
  displayOrder: z.number().min(0).optional().nullable(),
});

type ServiceFormData = z.infer<typeof serviceSchema>;

export const ServiceManagement = () => {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isImageOpen, setIsImageOpen] = useState(false);
  const [selectedService, setSelectedService] = useState<ServiceResponse | null>(null);

  const { toast } = useToast();
  const queryClient = useQueryClient();

  const form = useForm<ServiceFormData>({
    resolver: zodResolver(serviceSchema),
    defaultValues: {
      name: "",
      shortDescription: "",
      fullDescription: "",
      features: [],
      displayOrder: null,
    },
  });

  const { fields, append, remove } = useFieldArray({ control: form.control, name: "features" });

  const { data, isLoading } = useQuery({
    queryKey: ["services"],
    queryFn: () => ServicesApi.getAll(),
  });

  const createMutation = useMutation({
    mutationFn: (payload: ServiceRequest) => ServicesApi.create(payload),
    onSuccess: () => {
      toast({ title: "Thành công!", description: "Dịch vụ đã được tạo." });
      queryClient.invalidateQueries({ queryKey: ["services"] });
      setIsCreateOpen(false);
      form.reset();
    },
    onError: (error: any) => {
      toast({ variant: "destructive", title: "Lỗi", description: error.message || "Không thể tạo dịch vụ." });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ServiceRequest }) =>
      ServicesApi.update(id, payload),
    onSuccess: () => {
      toast({ title: "Thành công!", description: "Dịch vụ đã được cập nhật." });
      queryClient.invalidateQueries({ queryKey: ["services"] });
      setIsEditOpen(false);
      setSelectedService(null);
      form.reset();
    },
    onError: (error: any) => {
      toast({ variant: "destructive", title: "Lỗi", description: error.message || "Không thể cập nhật dịch vụ." });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ServicesApi.delete,
    onSuccess: () => {
      toast({ title: "Thành công!", description: "Dịch vụ đã được xóa." });
      queryClient.invalidateQueries({ queryKey: ["services"] });
    },
    onError: (error: any) => {
      toast({ variant: "destructive", title: "Lỗi", description: error.message || "Không thể xóa dịch vụ." });
    },
  });

  const toPayload = (data: ServiceFormData): ServiceRequest => ({
    name: data.name,
    shortDescription: data.shortDescription || undefined,
    fullDescription: data.fullDescription || undefined,
    features: data.features.map((f) => f.value).filter(Boolean),
    displayOrder: data.displayOrder ?? undefined,
  });

  const onSubmit = (data: ServiceFormData) => {
    const payload = toPayload(data);
    if (selectedService) {
      updateMutation.mutate({ id: selectedService.id, payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const handleEdit = (service: ServiceResponse) => {
    setSelectedService(service);
    form.reset({
      name: service.name,
      shortDescription: service.shortDescription || "",
      fullDescription: service.fullDescription || "",
      features: (service.features || []).map((v) => ({ value: v })),
      displayOrder: service.displayOrder ?? null,
    });
    setIsEditOpen(true);
  };

  const handleDelete = (id: number) => {
    if (confirm("Bạn có chắc chắn muốn xóa dịch vụ này?")) {
      deleteMutation.mutate(id);
    }
  };

  const handleManageImages = (service: ServiceResponse) => {
    setSelectedService(service);
    setIsImageOpen(true);
  };

  const handleOpenCreate = () => {
    setSelectedService(null);
    form.reset({ name: "", shortDescription: "", fullDescription: "", features: [], displayOrder: null });
    setIsCreateOpen(true);
  };

  const services = data?.result || [];

  const serviceForm = (isEdit: boolean) => (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField control={form.control} name="name" render={({ field }) => (
          <FormItem>
            <FormLabel>Tên dịch vụ *</FormLabel>
            <FormControl><Input placeholder="VD: Độ Đèn Bi-LED Chuyên Nghiệp" {...field} /></FormControl>
            <FormMessage />
          </FormItem>
        )} />

        <FormField control={form.control} name="shortDescription" render={({ field }) => (
          <FormItem>
            <FormLabel>Mô tả ngắn</FormLabel>
            <FormControl><Textarea rows={2} placeholder="Mô tả ngắn gọn (hiển thị trên trang chủ)" {...field} /></FormControl>
            <FormMessage />
          </FormItem>
        )} />

        <FormField control={form.control} name="fullDescription" render={({ field }) => (
          <FormItem>
            <FormLabel>Mô tả chi tiết</FormLabel>
            <FormControl><Textarea rows={5} placeholder="Mô tả đầy đủ dịch vụ (hiển thị trang chi tiết)" {...field} /></FormControl>
            <FormMessage />
          </FormItem>
        )} />

        <div>
          <div className="flex items-center justify-between mb-2">
            <FormLabel>Điểm nổi bật</FormLabel>
            <Button type="button" variant="outline" size="sm" onClick={() => append({ value: "" })}>
              <Plus className="h-3 w-3 mr-1" />Thêm
            </Button>
          </div>
          <div className="space-y-2">
            {fields.map((field, index) => (
              <div key={field.id} className="flex gap-2">
                <FormField control={form.control} name={`features.${index}.value`} render={({ field }) => (
                  <FormItem className="flex-1">
                    <FormControl><Input placeholder={`Tính năng ${index + 1}`} {...field} /></FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <Button type="button" variant="ghost" size="icon" onClick={() => remove(index)}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </div>
        </div>

        <FormField control={form.control} name="displayOrder" render={({ field }) => (
          <FormItem>
            <FormLabel>Thứ tự hiển thị</FormLabel>
            <FormControl>
              <Input type="number" placeholder="1, 2, 3..." {...field}
                value={field.value ?? ""}
                onChange={(e) => field.onChange(e.target.value ? Number(e.target.value) : null)} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />

        <div className="flex justify-end gap-2 pt-4">
          <Button type="button" variant="outline" onClick={() => isEdit ? setIsEditOpen(false) : setIsCreateOpen(false)}>Hủy</Button>
          <Button type="submit" disabled={isEdit ? updateMutation.isPending : createMutation.isPending}>
            {(isEdit ? updateMutation.isPending : createMutation.isPending) && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isEdit ? "Cập nhật" : "Tạo dịch vụ"}
          </Button>
        </div>
      </form>
    </Form>
  );

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Quản lý dịch vụ chuyên nghiệp</CardTitle>
          <Button onClick={handleOpenCreate}>
            <Plus className="h-4 w-4 mr-2" />Thêm dịch vụ
          </Button>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex justify-center py-8"><Loader2 className="h-8 w-8 animate-spin" /></div>
          ) : services.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              <ImageIcon className="h-12 w-12 mx-auto mb-4 opacity-40" />
              <p>Chưa có dịch vụ nào. Hãy thêm dịch vụ đầu tiên!</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Ảnh</TableHead>
                  <TableHead>Tên dịch vụ</TableHead>
                  <TableHead>Mô tả ngắn</TableHead>
                  <TableHead>Thứ tự</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {services.map((service) => (
                  <TableRow key={service.id}>
                    <TableCell>
                      {service.primaryImageUrl ? (
                        <img src={service.primaryImageUrl} alt={service.name}
                          className="w-12 h-12 object-cover rounded-lg border" />
                      ) : (
                        <div className="w-12 h-12 bg-muted rounded-lg flex items-center justify-center">
                          <ImageIcon className="h-5 w-5 opacity-40" />
                        </div>
                      )}
                    </TableCell>
                    <TableCell className="font-medium">{service.name}</TableCell>
                    <TableCell className="max-w-[200px] truncate text-muted-foreground text-sm">
                      {service.shortDescription || "—"}
                    </TableCell>
                    <TableCell>{service.displayOrder ?? "—"}</TableCell>
                    <TableCell className="text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" className="h-8 w-8 p-0">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleManageImages(service)}>
                            <Images className="h-4 w-4 mr-2" />Quản lý ảnh
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => handleEdit(service)}>
                            <Edit className="h-4 w-4 mr-2" />Chỉnh sửa
                          </DropdownMenuItem>
                          <DropdownMenuItem className="text-destructive" onClick={() => handleDelete(service.id)}>
                            <Trash2 className="h-4 w-4 mr-2" />Xóa
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
      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Thêm dịch vụ mới</DialogTitle>
            <DialogDescription>Điền thông tin dịch vụ chuyên nghiệp</DialogDescription>
          </DialogHeader>
          {serviceForm(false)}
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={isEditOpen} onOpenChange={setIsEditOpen}>
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Chỉnh sửa dịch vụ</DialogTitle>
            <DialogDescription>Cập nhật thông tin: {selectedService?.name}</DialogDescription>
          </DialogHeader>
          {serviceForm(true)}
        </DialogContent>
      </Dialog>

      {/* Image Management Dialog */}
      {selectedService && (
        <ServiceImageManagement
          serviceId={selectedService.id}
          serviceName={selectedService.name}
          isOpen={isImageOpen}
          onOpenChange={setIsImageOpen}
        />
      )}
    </div>
  );
};
