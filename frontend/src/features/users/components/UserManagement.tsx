import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { UsersApi } from "@/features/users/api/users";
import { RolesApi } from "@/features/users/api/roles";
import { useToast } from "@/hooks/use-toast";
import type { PaginationParams } from "@/types";
import type {
  UserResponse,
  UserCreationRequest,
  UserUpdateRequest,
  RoleResponse,
} from "../types";

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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
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
  User,
  Mail,
  Phone,
  Shield,
} from "lucide-react";

// Validation schemas
const userCreateSchema = z.object({
  username: z.string().min(3, "Tên đăng nhập phải có ít nhất 3 ký tự"),
  password: z.string().min(6, "Mật khẩu phải có ít nhất 6 ký tự"),
  email: z
    .string()
    .refine((val) => val === "" || z.string().email().safeParse(val).success, {
      message: "Email không hợp lệ",
    }),
  fullName: z.string().optional().or(z.literal("")),
  phoneNumber: z.string().optional().or(z.literal("")),
});

const userUpdateSchema = z.object({
  username: z.string().min(3, "Tên đăng nhập phải có ít nhất 3 ký tự"),
  email: z
    .string()
    .refine((val) => val === "" || z.string().email().safeParse(val).success, {
      message: "Email không hợp lệ",
    }),
  fullName: z.string().optional().or(z.literal("")),
  phoneNumber: z.string().optional().or(z.literal("")),
  password: z.string().optional().or(z.literal("")),
  roles: z.array(z.string()).optional(),
});

type UserCreateFormData = z.infer<typeof userCreateSchema>;
type UserUpdateFormData = z.infer<typeof userUpdateSchema>;

interface UserManagementProps {
  className?: string;
}

export const UserManagement: React.FC<UserManagementProps> = ({
  className,
}) => {
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserResponse | null>(null);
  const [viewingUser, setViewingUser] = useState<UserResponse | null>(null);

  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Forms setup
  const createForm = useForm<UserCreateFormData>({
    resolver: zodResolver(userCreateSchema),
    defaultValues: {
      username: "",
      password: "",
      email: "",
      fullName: "",
      phoneNumber: "",
    },
  });

  const updateForm = useForm<UserUpdateFormData>({
    resolver: zodResolver(userUpdateSchema),
    defaultValues: {
      username: "",
      email: "",
      fullName: "",
      phoneNumber: "",
      password: "",
      roles: [],
    },
  });

  // Query: Get all users
  const {
    data: usersData,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["users"],
    queryFn: () => UsersApi.getAll({ page: 0, size: 100 }),
  });

  // Query: Get all roles
  const { data: rolesData, isLoading: rolesLoading } = useQuery({
    queryKey: ["roles"],
    queryFn: RolesApi.getAll,
  });

  // Mutation: Create user
  const createMutation = useMutation({
    mutationFn: UsersApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      setIsCreateDialogOpen(false);
      createForm.reset();
      toast({
        title: "Thành công",
        description: "Người dùng đã được tạo thành công",
      });
    },
    onError: (error: any) => {
      console.error("Error creating user:", error);
      let errorMessage = "Có lỗi xảy ra khi tạo người dùng";

      if (error?.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error?.message) {
        errorMessage = error.message;
      }

      toast({
        title: "Lỗi",
        description: errorMessage,
        variant: "destructive",
      });
    },
  });

  // Mutation: Update user
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UserUpdateRequest }) =>
      UsersApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      setIsEditDialogOpen(false);
      setEditingUser(null);
      updateForm.reset();
      toast({
        title: "Thành công",
        description: "Người dùng đã được cập nhật thành công",
      });
    },
    onError: (error: any) => {
      console.error("Error updating user:", error);
      console.error("Error details:", {
        response: error?.response,
        data: error?.response?.data,
        status: error?.response?.status,
        message: error?.message,
        stack: error?.stack,
      });

      let errorMessage = "Có lỗi xảy ra khi cập nhật người dùng";

      if (error?.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error?.response?.data?.error) {
        errorMessage = error.response.data.error;
      } else if (error?.message) {
        errorMessage = error.message;
      }

      toast({
        title: "Lỗi",
        description: errorMessage,
        variant: "destructive",
      });
    },
  });

  // Mutation: Delete user
  const deleteMutation = useMutation({
    mutationFn: UsersApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      toast({
        title: "Thành công",
        description: "Người dùng đã được xóa thành công",
      });
    },
    onError: (error: any) => {
      console.error("Error deleting user:", error);
      let errorMessage = "Có lỗi xảy ra khi xóa người dùng";

      if (error?.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error?.message) {
        errorMessage = error.message;
      }

      toast({
        title: "Lỗi",
        description: errorMessage,
        variant: "destructive",
      });
    },
  });

  // Handle form submissions
  const onCreateSubmit = (data: UserCreateFormData) => {
    const userRequest: UserCreationRequest = {
      username: data.username,
      password: data.password,
      email: data.email || undefined,
      fullName: data.fullName || undefined,
      phoneNumber: data.phoneNumber || undefined,
    };

    createMutation.mutate(userRequest);
  };

  const onUpdateSubmit = (data: UserUpdateFormData) => {
    if (!editingUser) return;

    const userRequest: UserUpdateRequest = {
      username: data.username,
      email: data.email || undefined,
      fullName: data.fullName || undefined,
      phoneNumber: data.phoneNumber || undefined,
    };

    // Only include password if it's provided
    if (data.password && data.password.trim() !== "") {
      userRequest.password = data.password;
    }

    // Update roles if provided
    if (data.roles && data.roles.length > 0) {
      userRequest.roles = data.roles;
    }

    // Remove any undefined values
    Object.keys(userRequest).forEach((key) => {
      if (userRequest[key as keyof UserUpdateRequest] === undefined) {
        delete userRequest[key as keyof UserUpdateRequest];
      }
    });

    console.log("Final request payload:", userRequest);
    console.log("User ID:", editingUser.id);
    console.log("=== END UPDATE DEBUG ===");

    updateMutation.mutate({ id: editingUser.id, data: userRequest });
  };

  // Handle actions
  const handleEdit = (user: UserResponse) => {
    setEditingUser(user);
    const resetData = {
      username: user.username,
      email: user.email || "",
      fullName: user.fullName || "",
      phoneNumber: user.phoneNumber || "",
      password: "", // Don't pre-fill password
      roles: user.roles?.map(r => r.name) || [],
    };
    updateForm.reset(resetData);
    setIsEditDialogOpen(true);
  };

  const handleDelete = (userId: string) => {
    if (confirm("Bạn có chắc chắn muốn xóa người dùng này?")) {
      deleteMutation.mutate(userId);
    }
  };

  const handleViewDetail = (user: UserResponse) => {
    setViewingUser(user);
    setIsDetailDialogOpen(true);
  };

  // Get users and roles from response
  const usersPage = usersData?.result;
  const users = Array.isArray(usersPage) ? usersPage : (usersPage?.content || []);
  const roles = rolesData?.result || [];

  // Helper function to get user display name
  const getUserDisplayName = (user: UserResponse) => {
    return user.fullName || user.username;
  };

  // Helper function to get role badge
  const getRoleBadge = (user: UserResponse) => {
    if (!user.roles || user.roles.length === 0) {
      return <Badge variant="secondary">Chưa có vai trò</Badge>;
    }

    return (
      <>
        {user.roles.map((role, idx) => {
          const isAdminRole = role.name?.toUpperCase() === "ADMIN";
          return (
            <Badge
              key={idx}
              variant={isAdminRole ? "destructive" : "default"}
              className="mr-1"
            >
              {role.name}
            </Badge>
          );
        })}
      </>
    );
  };
  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-center text-red-600">
            Có lỗi xảy ra khi tải danh sách người dùng
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
              <CardTitle>Quản lý Người dùng</CardTitle>
              <CardDescription>
                Quản lý tài khoản người dùng trong hệ thống
              </CardDescription>
            </div>
            <Button onClick={() => setIsCreateDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Thêm người dùng
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
                  <TableHead>Tên đăng nhập</TableHead>
                  <TableHead>Họ tên</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Số điện thoại</TableHead>
                  <TableHead>Vai trò</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">
                      <div className="flex items-center gap-2">
                        <User className="h-4 w-4 text-muted-foreground" />
                        {user.username}
                      </div>
                    </TableCell>
                    <TableCell>{getUserDisplayName(user)}</TableCell>
                    <TableCell>
                      {user.email ? (
                        <div className="flex items-center gap-2">
                          <Mail className="h-4 w-4 text-muted-foreground" />
                          {user.email}
                        </div>
                      ) : (
                        <span className="text-muted-foreground">Chưa có</span>
                      )}
                    </TableCell>
                    <TableCell>
                      {user.phoneNumber ? (
                        <div className="flex items-center gap-2">
                          <Phone className="h-4 w-4 text-muted-foreground" />
                          {user.phoneNumber}
                        </div>
                      ) : (
                        <span className="text-muted-foreground">Chưa có</span>
                      )}
                    </TableCell>
                    <TableCell>{getRoleBadge(user)}</TableCell>
                    <TableCell className="text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" className="h-8 w-8 p-0">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem
                            onClick={() => handleViewDetail(user)}
                          >
                            <Eye className="h-4 w-4 mr-2" />
                            Xem chi tiết
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => handleEdit(user)}>
                            <Edit className="h-4 w-4 mr-2" />
                            Chỉnh sửa
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            className="text-red-600"
                            onClick={() => handleDelete(user.id)}
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

      {/* Create User Dialog */}
      <Dialog
        open={isCreateDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            setIsCreateDialogOpen(false);
            createForm.reset();
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Tạo người dùng mới</DialogTitle>
            <DialogDescription>
              Nhập thông tin để tạo tài khoản người dùng mới
            </DialogDescription>
          </DialogHeader>
          <Form {...createForm}>
            <form
              onSubmit={createForm.handleSubmit(onCreateSubmit)}
              className="space-y-4"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FormField
                  control={createForm.control}
                  name="username"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Tên đăng nhập *</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập tên đăng nhập..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={createForm.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Mật khẩu *</FormLabel>
                      <FormControl>
                        <Input
                          type="password"
                          placeholder="Nhập mật khẩu..."
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={createForm.control}
                  name="fullName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Họ và tên</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập họ và tên..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={createForm.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Email</FormLabel>
                      <FormControl>
                        <Input
                          type="email"
                          placeholder="Nhập email..."
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={createForm.control}
                  name="phoneNumber"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Số điện thoại</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập số điện thoại..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <div className="flex justify-end gap-2 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setIsCreateDialogOpen(false);
                    createForm.reset();
                  }}
                >
                  Hủy
                </Button>
                <Button type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending && (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  )}
                  Tạo người dùng
                </Button>
              </div>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      {/* Edit User Dialog */}
      <Dialog
        open={isEditDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            setIsEditDialogOpen(false);
            setEditingUser(null);
            updateForm.reset({
              username: "",
              email: "",
              fullName: "",
              phoneNumber: "",
              password: "",
              roles: [],
            });
          }
        }}
      >
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Chỉnh sửa người dùng</DialogTitle>
            <DialogDescription>Cập nhật thông tin người dùng</DialogDescription>
          </DialogHeader>
          <Form {...updateForm}>
            <form
              onSubmit={updateForm.handleSubmit(onUpdateSubmit)}
              className="space-y-4"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FormField
                  control={updateForm.control}
                  name="username"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Tên đăng nhập *</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập tên đăng nhập..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={updateForm.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Email</FormLabel>
                      <FormControl>
                        <Input
                          type="email"
                          placeholder="Nhập email..."
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={updateForm.control}
                  name="fullName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Họ và tên</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập họ và tên..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={updateForm.control}
                  name="phoneNumber"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Số điện thoại</FormLabel>
                      <FormControl>
                        <Input placeholder="Nhập số điện thoại..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={updateForm.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Mật khẩu mới (tùy chọn)</FormLabel>
                      <FormControl>
                        <Input
                          type="password"
                          placeholder="Để trống nếu không đổi..."
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* Role Selection */}
              <div className="space-y-4">
                <FormField
                  control={updateForm.control}
                  name="roles"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Vai trò</FormLabel>
                      <div className="space-y-2 border rounded-md p-4 max-h-60 overflow-y-auto">
                        {rolesLoading ? (
                          <div className="text-sm text-muted-foreground">
                            Đang tải vai trò...
                          </div>
                        ) : roles.length === 0 ? (
                          <div className="text-sm text-muted-foreground">
                            Không có vai trò nào
                          </div>
                        ) : (
                          roles.map((role: RoleResponse) => (
                            <div
                              key={role.name}
                              className="flex items-start space-x-3 p-2 hover:bg-accent rounded-md"
                            >
                              <Checkbox
                                checked={field.value?.includes(role.name) || false}
                                onCheckedChange={(checked) => {
                                  const currentRoles = field.value || [];
                                  if (checked) {
                                    field.onChange([...currentRoles, role.name]);
                                  } else {
                                    field.onChange(
                                      currentRoles.filter((r) => r !== role.name)
                                    );
                                  }
                                }}
                                id={`role-${role.name}`}
                              />
                              <label
                                htmlFor={`role-${role.name}`}
                                className="flex-1 cursor-pointer"
                              >
                                <div className="font-medium text-sm">{role.name}</div>
                                {role.description && (
                                  <div className="text-xs text-muted-foreground">
                                    {role.description}
                                  </div>
                                )}
                              </label>
                            </div>
                          ))
                        )}
                      </div>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <div className="flex justify-end gap-2 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setIsEditDialogOpen(false);
                    setEditingUser(null);
                    updateForm.reset({
                      username: "",
                      email: "",
                      fullName: "",
                      phoneNumber: "",
                      password: "",
                      roles: [],
                    });
                  }}
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

      {/* User Detail Dialog */}
      <Dialog
        open={isDetailDialogOpen}
        onOpenChange={(open) => {
          setIsDetailDialogOpen(open);
          if (!open) setViewingUser(null);
        }}
      >
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Chi tiết người dùng</DialogTitle>
            <DialogDescription>
              Thông tin chi tiết về tài khoản người dùng
            </DialogDescription>
          </DialogHeader>

          {viewingUser && (
            <div className="space-y-6">
              {/* User Avatar & Basic Info */}
              <div className="flex items-center space-x-4 p-4 bg-gray-50 rounded-lg">
                <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center">
                  <User className="w-8 h-8 text-primary" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold">
                    {getUserDisplayName(viewingUser)}
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    @{viewingUser.username}
                  </p>
                  <div className="mt-1">{getRoleBadge(viewingUser)}</div>
                </div>
              </div>

              {/* User Information */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-4">
                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      ID người dùng
                    </label>
                    <p className="text-base text-gray-600 font-mono">
                      #{viewingUser.id}
                    </p>
                  </div>

                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Tên đăng nhập
                    </label>
                    <p className="text-base">{viewingUser.username}</p>
                  </div>

                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Họ và tên
                    </label>
                    <p className="text-base">
                      {viewingUser.fullName || "Chưa có"}
                    </p>
                  </div>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Email
                    </label>
                    <p className="text-base">
                      {viewingUser.email || "Chưa có"}
                    </p>
                  </div>

                  <div>
                    <label className="text-sm font-medium text-gray-500">
                      Số điện thoại
                    </label>
                    <p className="text-base">
                      {viewingUser.phoneNumber || "Chưa có"}
                    </p>
                  </div>
                </div>
              </div>

              {/* Role & Permissions */}
              {viewingUser.roles && viewingUser.roles.length > 0 && (
                <div>
                  <label className="text-sm font-medium text-gray-500">
                    Vai trò và quyền hạn
                  </label>
                  <div className="mt-2 space-y-2">
                    {viewingUser.roles.map((role, idx) => (
                      <div key={idx} className="p-3 bg-gray-50 rounded-lg">
                        <div className="flex items-center gap-2 mb-2">
                          <Shield className="w-4 h-4" />
                          <span className="font-medium">
                            {role.name || "Unknown"}
                          </span>
                        </div>
                        {role.description && (
                          <p className="text-sm text-muted-foreground">
                            {role.description}
                          </p>
                        )}
                        {role.permissions &&
                          role.permissions.length > 0 && (
                            <div className="mt-2">
                              <p className="text-xs text-muted-foreground mb-1">
                                Quyền hạn:
                              </p>
                              <div className="flex flex-wrap gap-1">
                                {role.permissions.map(
                                  (permission, permIdx) => (
                                    <Badge
                                      key={permIdx}
                                      variant="outline"
                                      className="text-xs"
                                    >
                                      {permission.name}
                                    </Badge>
                                  )
                                )}
                              </div>
                            </div>
                          )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Action buttons */}
              <div className="flex justify-end gap-2 pt-4 border-t">
                <Button
                  variant="outline"
                  onClick={() => {
                    setIsDetailDialogOpen(false);
                    setViewingUser(null);
                  }}
                >
                  Đóng
                </Button>
                <Button
                  onClick={() => {
                    setIsDetailDialogOpen(false);
                    handleEdit(viewingUser);
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
