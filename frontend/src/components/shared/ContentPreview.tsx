import { useMemo } from "react";
import { renderContent } from "@/utils/contentRenderer";

interface ContentPreviewProps {
  content: string;
  className?: string;
}

export const ContentPreview: React.FC<ContentPreviewProps> = ({
  content,
  className = "",
}) => {
  const htmlContent = useMemo(() => {
    try {
      return renderContent(content);
    } catch (error) {
      console.error("Error rendering content:", error);
      return "<p>Lỗi khi hiển thị nội dung</p>";
    }
  }, [content]);

  return (
    <div
      className={`prose prose-lg max-w-none dark:prose-invert 
                  prose-img:rounded-lg prose-img:shadow-md prose-img:my-6 prose-img:mx-auto
                  prose-video:rounded-lg prose-video:mx-auto prose-video:my-6 prose-video:w-full
                  [&_iframe]:w-full [&_iframe]:my-6 [&_iframe]:rounded-lg [&_iframe]:aspect-video [&_iframe]:h-auto
                  [&_.image-wrapper]:text-center [&_.image-wrapper]:my-6
                  [&_.image-caption]:text-center [&_.image-caption]:text-sm [&_.image-caption]:text-gray-500 
                  [&_.image-caption]:mt-2 [&_.image-caption]:italic [&_.image-caption]:mb-6 ${className}`}
      dangerouslySetInnerHTML={{ __html: htmlContent }}
    />
  );
};
