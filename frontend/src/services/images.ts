import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";

export interface ImageUploadResponse {
  imageUrl: string;
}

/**
 * Upload ảnh qua backend.
 *
 * Trước đây file này ký request bằng CLOUDINARY_API_SECRET ngay trên trình
 * duyệt. Mọi biến VITE_* đều bị Vite nhúng cứng vào bundle, nên secret đó nằm
 * công khai trong file .js mà ai cũng tải về được. Giờ frontend chỉ gửi file
 * lên backend; backend giữ secret và tự gọi Cloudinary.
 */
export const ImagesApi = {
  /**
   * @param file - Ảnh cần tải lên
   * @param folder - Thư mục trên Cloudinary, backend chỉ chấp nhận đường dẫn
   *                 nằm trong "store/"
   */
  upload: async (
    file: File,
    folder: string = "store/posts/content"
  ): Promise<ImageUploadResponse> => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await http.request<ApiResponse<ImageUploadResponse>>(
      `/images/upload?folder=${encodeURIComponent(folder)}`,
      {
        method: "POST",
        body: formData,
        headers: {
          // Để trình duyệt tự set Content-Type kèm boundary cho multipart
        },
      }
    );

    if (!response.result?.imageUrl) {
      throw new Error("Máy chủ không trả về đường dẫn ảnh");
    }
    return response.result;
  },

  /** Kiểm tra file có phải ảnh không */
  isImageFile: (file: File): boolean => file.type.startsWith("image/"),

  /** Kiểm tra dung lượng file (mặc định tối đa 10MB, khớp giới hạn của backend) */
  isValidFileSize: (file: File, maxSizeMB: number = 10): boolean =>
    file.size <= maxSizeMB * 1024 * 1024,
};
