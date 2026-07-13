import { ReactNode, useState } from "react";
import { UseFormReturn } from "react-hook-form";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { Plus, Eye, Edit, Trash2 } from "lucide-react";
import {
  DataTable,
  Column,
  Pagination,
  SearchBar,
  ConfirmDialog,
  FormDialog,
  PageHeader,
} from "@/components/shared";

export interface ManagementConfig<T, TForm> {
  // Resource info
  resourceName: string;
  resourceNamePlural: string;

  // Data
  data: T[];
  isLoading: boolean;
  error?: any;

  // Pagination
  currentPage?: number;
  totalPages?: number;
  totalItems?: number;
  onPageChange?: (page: number) => void;

  // Search
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  searchPlaceholder?: string;

  // Table
  columns: Column<T>[];
  getRowKey: (item: T) => string | number;

  // Forms
  form: UseFormReturn<TForm>;
  renderFormFields: (form: UseFormReturn<TForm>) => ReactNode;

  // Actions
  onCreate?: (data: TForm) => void;
  onUpdate?: (id: string | number, data: TForm) => void;
  onDelete?: (id: string | number) => void;
  onView?: (item: T) => void;

  // Custom actions
  customActions?: Array<{
    label: string;
    icon?: React.ComponentType<{ className?: string }>;
    onClick: (item: T) => void;
    variant?: "default" | "destructive";
  }>;

  // Detail view
  renderDetailView?: (item: T) => ReactNode;

  // Edit logic
  onEditClick?: (item: T) => void;

  // Misc
  emptyMessage?: string;
  hideCreate?: boolean;
  hideEdit?: boolean;
  hideDelete?: boolean;
  hideView?: boolean;
}

export function CRUDManagement<T, TForm = any>({
  resourceName,
  resourceNamePlural,
  data,
  isLoading,
  error,
  currentPage,
  totalPages,
  totalItems,
  onPageChange,
  searchValue,
  onSearchChange,
  searchPlaceholder,
  columns,
  getRowKey,
  form,
  renderFormFields,
  onCreate,
  onUpdate,
  onDelete,
  onView,
  customActions = [],
  renderDetailView,
  onEditClick,
  emptyMessage,
  hideCreate = false,
  hideEdit = false,
  hideDelete = false,
  hideView = false,
}: ManagementConfig<T, TForm>) {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState<T | null>(null);

  // Build actions
  const actions = [
    ...(!hideView && onView
      ? [
          {
            label: "Xem chi tiết",
            icon: Eye,
            onClick: (item: T) => {
              setSelectedItem(item);
              if (renderDetailView) {
                setIsDetailOpen(true);
              } else if (onView) {
                onView(item);
              }
            },
          },
        ]
      : []),
    ...(!hideEdit && onUpdate
      ? [
          {
            label: "Chỉnh sửa",
            icon: Edit,
            onClick: (item: T) => {
              setSelectedItem(item);
              if (onEditClick) {
                onEditClick(item);
              }
              setIsEditOpen(true);
            },
          },
        ]
      : []),
    ...customActions,
    ...(!hideDelete && onDelete
      ? [
          {
            label: "Xóa",
            icon: Trash2,
            onClick: (item: T) => {
              setSelectedItem(item);
              setIsDeleteOpen(true);
            },
            variant: "destructive" as const,
          },
        ]
      : []),
  ];

  const handleCreateSubmit = (data: TForm) => {
    if (onCreate) {
      onCreate(data);
      setIsCreateOpen(false);
      form.reset();
    }
  };

  const handleEditSubmit = (data: TForm) => {
    if (onUpdate && selectedItem) {
      const id = getRowKey(selectedItem);
      onUpdate(id, data);
      setIsEditOpen(false);
      setSelectedItem(null);
      form.reset();
    }
  };

  const handleDelete = () => {
    if (onDelete && selectedItem) {
      const id = getRowKey(selectedItem);
      onDelete(id);
      setIsDeleteOpen(false);
      setSelectedItem(null);
    }
  };

  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <p className="text-center text-red-500">
            Có lỗi xảy ra khi tải dữ liệu: {error.message}
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Quản lý {resourceNamePlural}</CardTitle>
            <CardDescription>
              Quản lý thông tin {resourceNamePlural.toLowerCase()} trong hệ thống
            </CardDescription>
          </div>
          {!hideCreate && onCreate && (
            <Button onClick={() => setIsCreateOpen(true)}>
              <Plus className="h-4 w-4 mr-2" />
              Thêm {resourceName}
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {onSearchChange && (
          <div className="mb-4">
            <SearchBar
              value={searchValue || ""}
              onChange={onSearchChange}
              placeholder={searchPlaceholder || `Tìm kiếm ${resourceName.toLowerCase()}...`}
              className="max-w-sm"
            />
          </div>
        )}

        <DataTable
          columns={columns}
          data={data}
          isLoading={isLoading}
          emptyMessage={emptyMessage || `Không có ${resourceName.toLowerCase()} nào`}
          actions={actions.length > 0 ? actions : undefined}
          getRowKey={getRowKey}
        />

        {totalPages !== undefined &&
          totalItems !== undefined &&
          onPageChange && (
            <Pagination
              currentPage={currentPage || 0}
              totalPages={totalPages}
              totalItems={totalItems}
              onPageChange={onPageChange}
              itemLabel={resourceName.toLowerCase()}
            />
          )}
      </CardContent>

      {/* Create Dialog */}
      {onCreate && (
        <FormDialog
          open={isCreateOpen}
          onOpenChange={setIsCreateOpen}
          title={`Thêm ${resourceName} mới`}
          description={`Nhập thông tin ${resourceName.toLowerCase()} cần thêm`}
        >
          <Form {...form}>
            <form
              onSubmit={form.handleSubmit(handleCreateSubmit)}
              className="space-y-4"
            >
              {renderFormFields(form)}
              <div className="flex justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsCreateOpen(false)}
                >
                  Hủy
                </Button>
                <Button type="submit">Thêm</Button>
              </div>
            </form>
          </Form>
        </FormDialog>
      )}

      {/* Edit Dialog */}
      {onUpdate && (
        <FormDialog
          open={isEditOpen}
          onOpenChange={setIsEditOpen}
          title={`Chỉnh sửa ${resourceName}`}
          description={`Cập nhật thông tin ${resourceName.toLowerCase()}`}
        >
          <Form {...form}>
            <form
              onSubmit={form.handleSubmit(handleEditSubmit)}
              className="space-y-4"
            >
              {renderFormFields(form)}
              <div className="flex justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsEditOpen(false)}
                >
                  Hủy
                </Button>
                <Button type="submit">Cập nhật</Button>
              </div>
            </form>
          </Form>
        </FormDialog>
      )}

      {/* Detail Dialog */}
      {renderDetailView && (
        <FormDialog
          open={isDetailOpen}
          onOpenChange={setIsDetailOpen}
          title={`Chi tiết ${resourceName}`}
        >
          {selectedItem && renderDetailView(selectedItem)}
        </FormDialog>
      )}

      {/* Delete Confirmation */}
      {onDelete && (
        <ConfirmDialog
          open={isDeleteOpen}
          onOpenChange={setIsDeleteOpen}
          title={`Xóa ${resourceName}`}
          description={`Bạn có chắc chắn muốn xóa ${resourceName.toLowerCase()} này? Hành động này không thể hoàn tác.`}
          onConfirm={handleDelete}
          confirmText="Xóa"
          cancelText="Hủy"
          variant="destructive"
        />
      )}
    </Card>
  );
}
