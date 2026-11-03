import { useCallback, useEffect, useRef, useState } from "react";

export interface UploadImageItem {
  id: string;
  file: File;
  previewUrl: string;
  isPrimary: boolean;
}

export function useProductImageUploads() {
  const [uploadImages, setUploadImages] = useState<UploadImageItem[]>([]);
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const [isUploadingImages, setIsUploadingImages] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const previewUrls = useRef(new Set<string>());

  useEffect(() => () => {
    previewUrls.current.forEach((url) => URL.revokeObjectURL(url));
    previewUrls.current.clear();
  }, []);

  const addFiles = useCallback((files: FileList | File[]) => {
    const newItems = Array.from(files)
      .filter((file) => file.type.startsWith("image/"))
      .map((file): UploadImageItem => {
        const previewUrl = URL.createObjectURL(file);
        previewUrls.current.add(previewUrl);
        return {
          id: `${Date.now()}-${Math.random()}`,
          file,
          previewUrl,
          isPrimary: false,
        };
      });

    setUploadImages((previous) => {
      const merged = [...previous, ...newItems];
      return merged.some((image) => image.isPrimary)
        ? merged
        : merged.map((image, index) => ({ ...image, isPrimary: index === 0 }));
    });
  }, []);

  const removeImage = (id: string) => {
    const removed = uploadImages.find((image) => image.id === id);
    if (removed) {
      URL.revokeObjectURL(removed.previewUrl);
      previewUrls.current.delete(removed.previewUrl);
    }
    setUploadImages((previous) => {
      const remaining = previous.filter((image) => image.id !== id);
      return remaining.some((image) => image.isPrimary)
        ? remaining
        : remaining.map((image, index) => ({ ...image, isPrimary: index === 0 }));
    });
  };

  const setPrimaryImage = (id: string) => {
    setUploadImages((previous) =>
      previous.map((image) => ({ ...image, isPrimary: image.id === id }))
    );
  };

  const handleFileInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files) addFiles(event.target.files);
    event.target.value = "";
  };

  const handleDragOver = (event: React.DragEvent) => {
    event.preventDefault();
    setIsDraggingOver(true);
  };

  const handleDragLeave = () => setIsDraggingOver(false);

  const handleDrop = (event: React.DragEvent) => {
    event.preventDefault();
    setIsDraggingOver(false);
    if (event.dataTransfer.files) addFiles(event.dataTransfer.files);
  };

  const resetUploadImages = () => {
    previewUrls.current.forEach((url) => URL.revokeObjectURL(url));
    previewUrls.current.clear();
    setUploadImages([]);
  };

  return {
    uploadImages,
    isDraggingOver,
    isUploadingImages,
    setIsUploadingImages,
    fileInputRef,
    removeImage,
    setPrimaryImage,
    handleFileInputChange,
    handleDragOver,
    handleDragLeave,
    handleDrop,
    resetUploadImages,
  };
}
