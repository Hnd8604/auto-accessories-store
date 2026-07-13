import axios from "axios";
import { CLOUDINARY_CONFIG } from "@/constants/config";

export interface CloudinaryUploadResponse {
  secure_url: string;
  public_id: string;
  format: string;
  width: number;
  height: number;
  url: string;
}

// Helper function to generate SHA-1 signature
async function generateSignature(data: string): Promise<string> {
  const encoder = new TextEncoder();
  const dataBuffer = encoder.encode(data);
  const hashBuffer = await crypto.subtle.digest("SHA-1", dataBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");
  return hashHex;
}

export const CloudinaryApi = {
  /**
   * Upload an image to Cloudinary
   * @param file - The image file to upload
   * @param folder - Optional folder path in Cloudinary (e.g., "store/posts/content")
   * @returns Promise with the uploaded image URL
   */
  uploadImage: async (
    file: File,
    folder: string = "store/posts/content"
  ): Promise<CloudinaryUploadResponse> => {
    // Generate timestamp and signature
    const timestamp = Math.round(Date.now() / 1000);

    // Create signature string (folder + timestamp)
    const signatureString = `folder=${folder}&timestamp=${timestamp}${CLOUDINARY_CONFIG.apiSecret}`;

    // Simple hash function (in production, should use crypto library)
    const signature = await generateSignature(signatureString);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("folder", folder);
    formData.append("timestamp", timestamp.toString());
    formData.append("signature", signature);
    formData.append("api_key", CLOUDINARY_CONFIG.apiKey);

    const response = await axios.post<CloudinaryUploadResponse>(
      `https://api.cloudinary.com/v1_1/${CLOUDINARY_CONFIG.cloudName}/image/upload`,
      formData
    );

    return response.data;
  },

  /**
   * Validate if file is an image
   */
  isImageFile: (file: File): boolean => {
    return file.type.startsWith("image/");
  },

  /**
   * Validate file size (default max 10MB)
   */
  isValidFileSize: (file: File, maxSizeMB: number = 10): boolean => {
    const maxSizeBytes = maxSizeMB * 1024 * 1024;
    return file.size <= maxSizeBytes;
  },
};
