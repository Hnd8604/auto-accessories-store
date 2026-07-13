import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { BrandsApi } from "@/features/brands/api/brands";
import { useToast } from "@/hooks/use-toast";
import type { BrandResponse, BrandRequest } from "@/features/products/types";
import { CRUDManagement } from "@/components/shared";
import { FormField, FormItem, FormLabel, FormControl, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";

// Validation schema
const brandSchema = z.object({
  name: z.string().min(1, "Tên thương hiệu là bắt buộc"),
  description: z.string().optional(),
});

type BrandFormData = z.infer<typeof brandSchema>;

interface BrandManagementProps {
  className?: string;
}

export const BrandManagement: React.FC<BrandManagementProps> = () => {
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const form = useForm<BrandFormData>({
    resolver: zodResolver(brandSchema),
    defaultValues: {
      name: "",
      description: "",
    },
  });

  // Query: Get all brands
  const { data: brandsData, isLoading, error } = useQuery({
    queryKey: ["brands"],
    queryFn: BrandsApi.getAll,
  });

  // Mutation: Create brand
  const createMutation = useMutation({
    mutationFn: BrandsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["brands"] });
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

  const brands = brandsData?.result || [];

  return (
    <CRUDManagement<BrandResponse, BrandFormData>
      resourceName="Thương hiệu"
      resourceNamePlural="Thương hiệu"
      data={brands}
      isLoading={isLoading}
      error={error}
      columns={[
        {
          key: "name",
          header: "Tên thương hiệu",
        },
        {
          key: "description",
          header: "Mô tả",
          render: (brand) => brand.description || "-",
        },
        {
          key: "slug",
          header: "Slug",
          render: (brand) => brand.slug || "-",
        },
        {
          key: "productCount",
          header: "Số sản phẩm",
          render: (brand) => (
            <Badge variant="secondary">{brand.productCount || 0}</Badge>
          ),
        },
      ]}
      getRowKey={(brand) => brand.id}
      form={form}
      renderFormFields={(form) => (
        <>
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Tên thương hiệu</FormLabel>
                <FormControl>
                  <Input placeholder="Nhập tên thương hiệu" {...field} />
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
                    placeholder="Nhập mô tả thương hiệu"
                    {...field}
                    rows={3}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </>
      )}
      onCreate={(data) => {
        createMutation.mutate({
          name: data.name,
          description: data.description || undefined,
        });
      }}
      onUpdate={(id, data) => {
        updateMutation.mutate({
          id: Number(id),
          data: {
            name: data.name,
            description: data.description || undefined,
          },
        });
      }}
      onDelete={(id) => {
        deleteMutation.mutate(Number(id));
      }}
      onEditClick={(brand) => {
        form.reset({
          name: brand.name,
          description: brand.description || "",
        });
      }}
      renderDetailView={(brand) => (
        <div className="space-y-4">
          <div>
            <h4 className="font-semibold text-sm text-muted-foreground">
              Tên thương hiệu
            </h4>
            <p className="text-base">{brand.name}</p>
          </div>
          {brand.description && (
            <div>
              <h4 className="font-semibold text-sm text-muted-foreground">
                Mô tả
              </h4>
              <p className="text-base">{brand.description}</p>
            </div>
          )}
          {brand.slug && (
            <div>
              <h4 className="font-semibold text-sm text-muted-foreground">
                Slug
              </h4>
              <p className="text-base">{brand.slug}</p>
            </div>
          )}
          <div>
            <h4 className="font-semibold text-sm text-muted-foreground">
              Số sản phẩm
            </h4>
            <Badge variant="secondary">{brand.productCount || 0}</Badge>
          </div>
        </div>
      )}
    />
  );
};
