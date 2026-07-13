import { useState, useRef, useCallback, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { 
  Bold, 
  Italic, 
  List, 
  ListOrdered, 
  Image as ImageIcon, 
  Video, 
  Link as LinkIcon,
  Code,
  Heading1,
  Heading2,
  Quote,
  Upload,
  Loader2,
  X
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { CloudinaryApi } from "@/services/cloudinary";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

interface RichTextEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
}

export const RichTextEditor: React.FC<RichTextEditorProps> = ({
  value,
  onChange,
  placeholder = "Nhập nội dung...",
  className,
}) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const editorRef = useRef<HTMLDivElement>(null);
  const [isImageDialogOpen, setIsImageDialogOpen] = useState(false);
  const [isVideoDialogOpen, setIsVideoDialogOpen] = useState(false);
  const [isLinkDialogOpen, setIsLinkDialogOpen] = useState(false);
  const [imageUrl, setImageUrl] = useState("");
  const [imageAlt, setImageAlt] = useState("");
  const [imageCaption, setImageCaption] = useState("");
  const [videoUrl, setVideoUrl] = useState("");
  const [linkUrl, setLinkUrl] = useState("");
  const [linkText, setLinkText] = useState("");
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<string>("");
  const [imageUploadTab, setImageUploadTab] = useState<"upload" | "url">("upload");
  const [selectedImageFile, setSelectedImageFile] = useState<File | null>(null);
  const [previewImageUrl, setPreviewImageUrl] = useState<string>("");
  const { toast } = useToast();

  // Log component mount
  useEffect(() => {
    console.log('✅ RichTextEditor mounted');
    return () => {
      console.log('❌ RichTextEditor unmounted');
    };
  }, []);

  // Note: We don't add document/window level preventDefault because:
  // 1. RichTextEditor can be used inside Dialog/Modal
  // 2. React synthetic events will handle preventDefault on the editor element
  // 3. Window-level listeners can't reliably detect if drop is on editor when inside modals

  const insertText = useCallback((before: string, after: string = "") => {
    const textarea = textareaRef.current;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = value.substring(start, end);
    const newText = value.substring(0, start) + before + selectedText + after + value.substring(end);

    onChange(newText);

    // Set cursor position after insertion
    setTimeout(() => {
      textarea.focus();
      const newCursorPos = start + before.length + selectedText.length;
      textarea.setSelectionRange(newCursorPos, newCursorPos);
    }, 0);
  }, [value, onChange]);

  const handleBold = () => insertText("**", "**");
  const handleItalic = () => insertText("*", "*");
  const handleCode = () => insertText("`", "`");
  
  const handleHeading1 = () => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = value.substring(start, end);
    
    // Find start of current line
    const lineStart = value.lastIndexOf('\n', start - 1) + 1;
    const lineEnd = value.indexOf('\n', end);
    const actualEnd = lineEnd === -1 ? value.length : lineEnd;
    
    const lineText = value.substring(lineStart, actualEnd);
    const newLine = `# ${selectedText || lineText}`;
    const newText = value.substring(0, lineStart) + newLine + value.substring(actualEnd);
    
    onChange(newText);
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(lineStart + 2, lineStart + 2 + (selectedText || lineText).length);
    }, 0);
  };
  
  const handleHeading2 = () => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = value.substring(start, end);
    
    const lineStart = value.lastIndexOf('\n', start - 1) + 1;
    const lineEnd = value.indexOf('\n', end);
    const actualEnd = lineEnd === -1 ? value.length : lineEnd;
    
    const lineText = value.substring(lineStart, actualEnd);
    const newLine = `## ${selectedText || lineText}`;
    const newText = value.substring(0, lineStart) + newLine + value.substring(actualEnd);
    
    onChange(newText);
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(lineStart + 3, lineStart + 3 + (selectedText || lineText).length);
    }, 0);
  };
  
  const handleUnorderedList = () => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = value.substring(start, end);
    
    if (selectedText.includes('\n')) {
      // Multiple lines selected
      const lines = selectedText.split('\n');
      const listText = lines.map(line => line.trim() ? `- ${line}` : line).join('\n');
      const newText = value.substring(0, start) + listText + value.substring(end);
      onChange(newText);
    } else {
      // Single line
      const lineStart = value.lastIndexOf('\n', start - 1) + 1;
      const lineEnd = value.indexOf('\n', end);
      const actualEnd = lineEnd === -1 ? value.length : lineEnd;
      const lineText = value.substring(lineStart, actualEnd);
      const newLine = `- ${selectedText || lineText}`;
      const newText = value.substring(0, lineStart) + newLine + value.substring(actualEnd);
      onChange(newText);
    }
    setTimeout(() => textarea.focus(), 0);
  };
  
  const handleOrderedList = () => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = value.substring(start, end);
    
    if (selectedText.includes('\n')) {
      const lines = selectedText.split('\n');
      const listText = lines.map((line, i) => line.trim() ? `${i + 1}. ${line}` : line).join('\n');
      const newText = value.substring(0, start) + listText + value.substring(end);
      onChange(newText);
    } else {
      const lineStart = value.lastIndexOf('\n', start - 1) + 1;
      const lineEnd = value.indexOf('\n', end);
      const actualEnd = lineEnd === -1 ? value.length : lineEnd;
      const lineText = value.substring(lineStart, actualEnd);
      const newLine = `1. ${selectedText || lineText}`;
      const newText = value.substring(0, lineStart) + newLine + value.substring(actualEnd);
      onChange(newText);
    }
    setTimeout(() => textarea.focus(), 0);
  };
  
  const handleQuote = () => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = value.substring(start, end);
    
    if (selectedText.includes('\n')) {
      const lines = selectedText.split('\n');
      const quoteText = lines.map(line => line.trim() ? `> ${line}` : line).join('\n');
      const newText = value.substring(0, start) + quoteText + value.substring(end);
      onChange(newText);
    } else {
      const lineStart = value.lastIndexOf('\n', start - 1) + 1;
      const lineEnd = value.indexOf('\n', end);
      const actualEnd = lineEnd === -1 ? value.length : lineEnd;
      const lineText = value.substring(lineStart, actualEnd);
      const newLine = `> ${selectedText || lineText}`;
      const newText = value.substring(0, lineStart) + newLine + value.substring(actualEnd);
      onChange(newText);
    }
    setTimeout(() => textarea.focus(), 0);
  };

  const uploadImageToCloudinary = async (file: File): Promise<string> => {
    if (!CloudinaryApi.isImageFile(file)) {
      throw new Error("File không phải là ảnh");
    }

    if (!CloudinaryApi.isValidFileSize(file, 10)) {
      throw new Error("Kích thước file vượt quá 10MB");
    }

    setIsUploading(true);
    setUploadProgress("Đang tải lên Cloudinary...");

    try {
      const response = await CloudinaryApi.uploadImage(file, "store/posts/content");
      setUploadProgress("Hoàn tất!");
      return response.secure_url;
    } catch (error) {
      console.error("Cloudinary upload error:", error);
      throw new Error(
        error instanceof Error ? error.message : "Không thể tải ảnh lên Cloudinary"
      );
    } finally {
      setIsUploading(false);
      setTimeout(() => setUploadProgress(""), 2000);
    }
  };

  const handleFileSelect = (file: File) => {
    console.log('📂 handleFileSelect called:', file.name);
    
    if (!CloudinaryApi.isImageFile(file)) {
      console.log('❌ Not an image file');
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: "Chỉ chấp nhận file ảnh (PNG, JPG, WEBP, GIF)",
      });
      return;
    }

    if (!CloudinaryApi.isValidFileSize(file, 10)) {
      console.log('❌ File too large');
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: "Kích thước file vượt quá 10MB",
      });
      return;
    }

    console.log('✅ File validation passed, creating preview...');
    
    // Show preview
    setSelectedImageFile(file);
    const reader = new FileReader();
    reader.onloadend = () => {
      console.log('✅ Preview image ready');
      setPreviewImageUrl(reader.result as string);
    };
    reader.readAsDataURL(file);

    // Set default alt text from filename
    if (!imageAlt) {
      setImageAlt(file.name.split(".")[0] || "image");
    }
  };

  const handleUploadAndInsert = async () => {
    if (!selectedImageFile) return;

    try {
      const cloudinaryUrl = await uploadImageToCloudinary(selectedImageFile);
      
      const altText = imageAlt || selectedImageFile.name.split(".")[0] || "image";
      let imageMarkdown = `\n![${altText}](${cloudinaryUrl})`;
      
      if (imageCaption) {
        imageMarkdown += `\n*${imageCaption}*`;
      }
      imageMarkdown += `\n`;
      
      insertText(imageMarkdown);
      
      toast({
        title: "Thành công",
        description: "Đã tải ảnh lên và chèn vào nội dung",
      });
      
      // Reset states
      setIsImageDialogOpen(false);
      setSelectedImageFile(null);
      setPreviewImageUrl("");
      setImageUrl("");
      setImageAlt("");
      setImageCaption("");
    } catch (error) {
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: error instanceof Error ? error.message : "Không thể tải ảnh lên",
      });
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    console.log(`🎯 handleDrag called: ${e.type}`, {
      target: e.target,
      currentTarget: e.currentTarget,
      types: Array.from(e.dataTransfer?.types || []),
      items: e.dataTransfer?.items.length
    });
    
    e.preventDefault();
    e.stopPropagation();
    
    // Check if it's a file being dragged
    const hasFiles = e.dataTransfer?.types.includes('Files');
    
    console.log(`   ├─ hasFiles: ${hasFiles}`);
    console.log(`   └─ isDragging state: ${isDragging}`);
    
    if (!hasFiles) {
      console.log('   ⚠️  No files detected, ignoring');
      return;
    }
    
    if (e.type === "dragenter" || e.type === "dragover") {
      console.log('   ✅ Setting isDragging = true');
      setIsDragging(true);
    } else if (e.type === "dragleave") {
      // Only hide overlay if we're leaving the editor container
      const rect = editorRef.current?.getBoundingClientRect();
      if (rect && (
        e.clientX < rect.left ||
        e.clientX >= rect.right ||
        e.clientY < rect.top ||
        e.clientY >= rect.bottom
      )) {
        console.log('   ❌ Setting isDragging = false (leaving editor)');
        setIsDragging(false);
      } else {
        console.log('   ℹ️  Still inside editor, keeping overlay');
      }
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    console.log('🎯 handleDrop called!', {
      target: e.target,
      currentTarget: e.currentTarget,
      dataTransfer: e.dataTransfer,
      filesCount: e.dataTransfer?.files?.length
    });
    
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = e.dataTransfer?.files;
    if (!files || files.length === 0) {
      console.log("❌ No files in drop event");
      return;
    }

    const file = files[0];
    console.log("✅ File dropped:", {
      name: file.name,
      type: file.type,
      size: file.size,
      lastModified: file.lastModified
    });
    
    handleFileSelect(file);
    setIsImageDialogOpen(true);
    setImageUploadTab("upload");
    console.log('📝 Dialog should be opening now...');
  };

  const handleInsertImage = () => {
    if (!imageUrl) {
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: "Vui lòng nhập URL ảnh",
      });
      return;
    }

    const altText = imageAlt || "image";
    let imageMarkdown = `\n![${altText}](${imageUrl})`;
    
    // Add caption if provided
    if (imageCaption) {
      imageMarkdown += `\n*${imageCaption}*`;
    }
    imageMarkdown += `\n`;
    
    insertText(imageMarkdown);
    
    setIsImageDialogOpen(false);
    setImageUrl("");
    setImageAlt("");
    setImageCaption("");
  };

  const handleInsertVideo = () => {
    if (!videoUrl) {
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: "Vui lòng nhập URL video",
      });
      return;
    }

    let embedCode = "";

    // Check if it's a YouTube URL
    const youtubeRegex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/;
    const youtubeMatch = videoUrl.match(youtubeRegex);

    if (youtubeMatch) {
      const videoId = youtubeMatch[1];
      embedCode = `\n<iframe width="560" height="315" src="https://www.youtube.com/embed/${videoId}" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>\n`;
    } 
    // Check if it's a Vimeo URL
    else if (videoUrl.includes("vimeo.com")) {
      const vimeoRegex = /vimeo\.com\/(\d+)/;
      const vimeoMatch = videoUrl.match(vimeoRegex);
      if (vimeoMatch) {
        const videoId = vimeoMatch[1];
        embedCode = `\n<iframe src="https://player.vimeo.com/video/${videoId}" width="560" height="315" frameborder="0" allow="autoplay; fullscreen; picture-in-picture" allowfullscreen></iframe>\n`;
      }
    }
    // Direct video URL (mp4, webm, etc.)
    else if (videoUrl.match(/\.(mp4|webm|ogg)$/i)) {
      embedCode = `\n<video width="560" height="315" controls>\n  <source src="${videoUrl}" type="video/mp4">\n  Your browser does not support the video tag.\n</video>\n`;
    }
    else {
      embedCode = `\n[Video: ${videoUrl}](${videoUrl})\n`;
    }

    insertText(embedCode);
    setIsVideoDialogOpen(false);
    setVideoUrl("");
  };

  const handleInsertLink = () => {
    if (!linkUrl) {
      toast({
        variant: "destructive",
        title: "Lỗi",
        description: "Vui lòng nhập URL",
      });
      return;
    }

    const text = linkText || linkUrl;
    const linkMarkdown = `[${text}](${linkUrl})`;
    insertText(linkMarkdown);
    
    setIsLinkDialogOpen(false);
    setLinkUrl("");
    setLinkText("");
  };

  // Test function to verify handlers work
  const testDragDrop = () => {
    console.log('🧪 Testing drag/drop functionality...');
    console.log('   editorRef.current:', editorRef.current);
    console.log('   isDragging:', isDragging);
    console.log('   isImageDialogOpen:', isImageDialogOpen);
  };

  return (
    <div className={className}>
      {/* Debug Info */}
      {process.env.NODE_ENV === 'development' && (
        <div className="text-xs p-2 bg-yellow-100 dark:bg-yellow-900/20 border border-yellow-300 rounded mb-2">
          <strong>🐛 Debug:</strong> isDragging={String(isDragging)} | dialogOpen={String(isImageDialogOpen)}
          <button 
            onClick={testDragDrop}
            className="ml-2 px-2 py-1 bg-blue-500 text-white rounded text-xs"
          >
            Test Handler
          </button>
        </div>
      )}
      
      {/* Toolbar */}
      <div className="flex flex-wrap gap-1 p-2 border rounded-t-md bg-muted/50">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={handleHeading1}
          title="Heading 1"
        >
          <Heading1 className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={handleHeading2}
          title="Heading 2"
        >
          <Heading2 className="h-4 w-4" />
        </Button>
        <div className="w-px h-8 bg-border mx-1" />
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={handleBold}
          title="Bold"
        >
          <Bold className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={handleItalic}
          title="Italic"
        >
          <Italic className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={handleCode}
          title="Code"
        >
          <Code className="h-4 w-4" />
        </Button>
        <div className="w-px h-8 bg-border mx-1" />
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={handleUnorderedList}
          title="Bullet List"
        >
          <List className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={handleOrderedList}
          title="Numbered List"
        >
          <ListOrdered className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={handleQuote}
          title="Quote"
        >
          <Quote className="h-4 w-4" />
        </Button>
        <div className="w-px h-8 bg-border mx-1" />
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setIsImageDialogOpen(true)}
          title="Insert Image"
        >
          <ImageIcon className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setIsVideoDialogOpen(true)}
          title="Insert Video"
        >
          <Video className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setIsLinkDialogOpen(true)}
          title="Insert Link"
        >
          <LinkIcon className="h-4 w-4" />
        </Button>
      </div>

      {/* Editor with drag & drop support */}
      <div 
        ref={editorRef} 
        className="relative"
        onDragEnter={(e) => {
          if (e.dataTransfer?.types.includes('Files')) {
            console.log('🎯 Wrapper: dragenter with files');
            setIsDragging(true);
            e.preventDefault();
          }
        }}
        onDragOver={(e) => {
          if (e.dataTransfer?.types.includes('Files')) {
            e.preventDefault(); // Required to allow drop
          }
        }}
      >
        <Textarea
          ref={textareaRef}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className={`min-h-[400px] rounded-t-none font-mono text-sm transition-colors ${
            isDragging ? "border-red-500 border-2" : ""
          }`}
        />
        
        {/* Invisible drop zone that sits on top of textarea to catch drops */}
        <div 
          className="absolute inset-0 z-[5]"
          style={{ pointerEvents: isDragging ? 'auto' : 'none' }}
          onDragEnter={(e) => {
            console.log('🎯 Drop zone: dragenter');
            handleDrag(e);
          }}
          onDragLeave={(e) => {
            console.log('🎯 Drop zone: dragleave');
            handleDrag(e);
          }}
          onDragOver={(e) => {
            console.log('🎯 Drop zone: dragover');
            handleDrag(e);
          }}
          onDrop={(e) => {
            console.log('🎯 Drop zone: DROP!!!');
            handleDrop(e);
          }}
        />
        
        {/* Drag & Drop Overlay */}
        {isDragging && (
          <div className="absolute inset-0 bg-red-50/90 dark:bg-red-950/30 border-2 border-dashed border-red-500 rounded-b-md flex items-center justify-center z-10 pointer-events-none">
            <div className="bg-white dark:bg-gray-900 p-6 rounded-lg shadow-lg border-2 border-red-500">
              <Upload className="h-12 w-12 mx-auto mb-2 text-red-500" />
              <p className="text-lg font-semibold text-red-600 dark:text-red-400">
                Thả ảnh vào đây
              </p>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                Ảnh sẽ được xem trước trước khi tải lên
              </p>
            </div>
          </div>
        )}
        
        {/* Upload Progress Indicator */}
        {isUploading && (
          <div className="absolute inset-0 bg-background/80 flex items-center justify-center z-20">
            <div className="bg-card p-6 rounded-lg shadow-lg border flex flex-col items-center">
              <Loader2 className="h-8 w-8 animate-spin text-primary mb-2" />
              <p className="text-sm font-medium">{uploadProgress}</p>
            </div>
          </div>
        )}
      </div>

      {/* Image Dialog with Upload/URL tabs */}
      <Dialog 
        open={isImageDialogOpen} 
        onOpenChange={(open) => {
          setIsImageDialogOpen(open);
          if (!open) {
            setImageUploadTab("upload");
            setImageUrl("");
            setImageAlt("");
            setImageCaption("");
          }
        }}
      >
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>Chèn ảnh</DialogTitle>
            <DialogDescription>
              Tải ảnh lên Cloudinary hoặc nhập URL ảnh có sẵn
            </DialogDescription>
          </DialogHeader>
          
          <Tabs value={imageUploadTab} onValueChange={(v) => setImageUploadTab(v as "upload" | "url")}>
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="upload">
                <Upload className="h-4 w-4 mr-2" />
                Tải lên
              </TabsTrigger>
              <TabsTrigger value="url">
                <LinkIcon className="h-4 w-4 mr-2" />
                URL
              </TabsTrigger>
            </TabsList>
            
            <TabsContent value="upload" className="space-y-4">
              {previewImageUrl ? (
                <>
                  <div>
                    <Label>Xem trước</Label>
                    <div className="mt-2 relative">
                      <img
                        src={previewImageUrl}
                        alt="Preview"
                        className="max-h-64 w-full object-contain rounded-lg border"
                      />
                      <Button
                        type="button"
                        variant="destructive"
                        size="icon"
                        className="absolute top-2 right-2"
                        onClick={() => {
                          setSelectedImageFile(null);
                          setPreviewImageUrl("");
                          setImageAlt("");
                          setImageCaption("");
                        }}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                    {selectedImageFile && (
                      <p className="text-sm text-muted-foreground mt-2">
                        {selectedImageFile.name} ({(selectedImageFile.size / 1024 / 1024).toFixed(2)} MB)
                      </p>
                    )}
                  </div>
                  
                  <div>
                    <Label htmlFor="imageAltUpload">Mô tả ảnh (Alt text)</Label>
                    <Input
                      id="imageAltUpload"
                      placeholder="Mô tả ngắn gọn về ảnh"
                      value={imageAlt}
                      onChange={(e) => setImageAlt(e.target.value)}
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="imageCaptionUpload">Chú thích ảnh (Caption)</Label>
                    <Input
                      id="imageCaptionUpload"
                      placeholder="Chú thích hiển thị dưới ảnh"
                      value={imageCaption}
                      onChange={(e) => setImageCaption(e.target.value)}
                    />
                    <p className="text-xs text-muted-foreground mt-1">
                      Chú thích sẽ hiển thị ngay dưới ảnh
                    </p>
                  </div>
                </>
              ) : (
                <div>
                  <Label>Chọn ảnh từ máy tính</Label>
                  <div className="mt-2">
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/*"
                      onChange={handleFileInputChange}
                      className="hidden"
                      id="image-file-input"
                    />
                    <label
                      htmlFor="image-file-input"
                      className="flex flex-col items-center justify-center w-full h-32 border-2 border-dashed rounded-lg cursor-pointer transition-colors hover:bg-muted/50"
                    >
                      <div className="flex flex-col items-center justify-center pt-5 pb-6">
                        <Upload className="w-8 h-8 mb-2 text-muted-foreground" />
                        <p className="mb-2 text-sm text-muted-foreground">
                          <span className="font-semibold">Nhấp để chọn</span> hoặc kéo thả
                        </p>
                        <p className="text-xs text-muted-foreground">PNG, JPG, WEBP, GIF (tối đa 10MB)</p>
                      </div>
                    </label>
                  </div>
                  <p className="text-xs text-muted-foreground mt-2">
                    Ảnh sẽ được xem trước trước khi tải lên Cloudinary
                  </p>
                </div>
              )}
            </TabsContent>
            
            <TabsContent value="url" className="space-y-4">
              <div>
                <Label htmlFor="imageUrl">URL ảnh *</Label>
                <Input
                  id="imageUrl"
                  placeholder="https://example.com/image.jpg"
                  value={imageUrl}
                  onChange={(e) => setImageUrl(e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="imageAlt">Mô tả ảnh (Alt text)</Label>
                <Input
                  id="imageAlt"
                  placeholder="Mô tả ngắn gọn về ảnh"
                  value={imageAlt}
                  onChange={(e) => setImageAlt(e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="imageCaption">Chú thích ảnh (Caption)</Label>
                <Input
                  id="imageCaption"
                  placeholder="Chú thích hiển thị dưới ảnh"
                  value={imageCaption}
                  onChange={(e) => setImageCaption(e.target.value)}
                />
                <p className="text-xs text-muted-foreground mt-1">
                  Chú thích sẽ hiển thị ngay dưới ảnh
                </p>
              </div>
            </TabsContent>
          </Tabs>
          
          <DialogFooter>
            <Button 
              variant="outline" 
              onClick={() => {
                setIsImageDialogOpen(false);
                setSelectedImageFile(null);
                setPreviewImageUrl("");
                setImageUrl("");
                setImageAlt("");
                setImageCaption("");
              }}
            >
              Hủy
            </Button>
            {imageUploadTab === "upload" ? (
              <Button 
                onClick={handleUploadAndInsert} 
                disabled={!selectedImageFile || isUploading}
              >
                {isUploading ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Đang tải lên...
                  </>
                ) : (
                  "Chèn ảnh"
                )}
              </Button>
            ) : (
              <Button onClick={handleInsertImage} disabled={!imageUrl}>
                Chèn ảnh
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Video Dialog */}
      <Dialog open={isVideoDialogOpen} onOpenChange={setIsVideoDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Chèn video</DialogTitle>
            <DialogDescription>
              Nhập URL YouTube, Vimeo hoặc link video trực tiếp (.mp4, .webm)
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <Label htmlFor="videoUrl">URL video *</Label>
              <Input
                id="videoUrl"
                placeholder="https://youtube.com/watch?v=... hoặc https://example.com/video.mp4"
                value={videoUrl}
                onChange={(e) => setVideoUrl(e.target.value)}
              />
              <p className="text-xs text-muted-foreground mt-1">
                Hỗ trợ: YouTube, Vimeo, MP4, WebM, OGG
              </p>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsVideoDialogOpen(false)}>
              Hủy
            </Button>
            <Button onClick={handleInsertVideo}>Chèn video</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Link Dialog */}
      <Dialog open={isLinkDialogOpen} onOpenChange={setIsLinkDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Chèn liên kết</DialogTitle>
            <DialogDescription>
              Tạo một liên kết trong bài viết
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <Label htmlFor="linkUrl">URL *</Label>
              <Input
                id="linkUrl"
                placeholder="https://example.com"
                value={linkUrl}
                onChange={(e) => setLinkUrl(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="linkText">Văn bản hiển thị</Label>
              <Input
                id="linkText"
                placeholder="Nhấn vào đây"
                value={linkText}
                onChange={(e) => setLinkText(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsLinkDialogOpen(false)}>
              Hủy
            </Button>
            <Button onClick={handleInsertLink}>Chèn liên kết</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
