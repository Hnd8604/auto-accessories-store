import { useQuery } from "@tanstack/react-query";
import { useParams, useNavigate } from "react-router-dom";
import { PostsApi } from "@/features/posts/api";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Loader2, ArrowLeft, Calendar, Eye, User } from "lucide-react";
import { useMemo } from "react";
import { renderContent } from "@/utils/contentRenderer";

const PageLayout = ({ children }: { children: React.ReactNode }) => (
  <div className="min-h-screen bg-background">
    <Header />
    {children}
    <Footer />
  </div>
);

const LoadingState = () => (
  <PageLayout>
    <div className="pt-20 flex items-center justify-center min-h-[60vh]">
      <Loader2 className="h-12 w-12 animate-spin text-primary" />
    </div>
  </PageLayout>
);

const ErrorState = ({ onBack }: { onBack: () => void }) => (
  <PageLayout>
    <div className="pt-20 container mx-auto px-4 py-12">
      <div className="text-center">
        <h1 className="text-3xl font-bold mb-4">Không tìm thấy bài viết</h1>
        <Button onClick={onBack}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Quay lại blog
        </Button>
      </div>
    </div>
  </PageLayout>
);

const PostMeta = ({ 
  authorName, 
  createdAt, 
  viewCount 
}: { 
  authorName?: string; 
  createdAt?: string; 
  viewCount?: number;
}) => (
  <div className="flex items-center gap-4 text-sm text-muted-foreground">
    {authorName && (
      <div className="flex items-center gap-1">
        <User className="h-4 w-4" />
        <span>{authorName}</span>
      </div>
    )}
    {createdAt && (
      <div className="flex items-center gap-1">
        <Calendar className="h-4 w-4" />
        <span>{new Date(createdAt).toLocaleDateString("vi-VN")}</span>
      </div>
    )}
    <div className="flex items-center gap-1">
      <Eye className="h-4 w-4" />
      <span>{viewCount || 0} lượt xem</span>
    </div>
  </div>
);

export const PostDetailPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const { data, isLoading, error } = useQuery({
    queryKey: ["post", slug],
    queryFn: async () => {
      // Fetch post data by slug
      const response = await PostsApi.getBySlug(slug!);
      // Increment view count after getting post
      if (response?.result?.id) {
        try {
          await PostsApi.incrementViewCount(response.result.id);
        } catch (err) {
          console.error('Failed to increment view count:', err);
        }
      }
      return response;
    },
    enabled: !!slug,
  });

  const post = useMemo(() => data?.result, [data]);
  const handleBackToBlog = () => navigate("/blog");

  if (isLoading) return <LoadingState />;
  if (error || !post) return <ErrorState onBack={handleBackToBlog} />;

  return (
    <PageLayout>
      <div className="pt-20">
        <article className="container mx-auto px-4 py-12 max-w-4xl">
          <Button
            variant="ghost"
            className="mb-6"
            onClick={handleBackToBlog}
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Quay lại blog
          </Button>

          {post.thumbnailUrl && (
            <div className="mb-8 rounded-lg overflow-hidden">
              <img
                src={post.thumbnailUrl}
                alt={post.title}
                className="w-full h-[400px] object-cover"
                loading="lazy"
              />
            </div>
          )}

          <div className="mb-6">
            {post.categoryName && (
              <Badge className="mb-4">{post.categoryName}</Badge>
            )}
            <h1 className="text-4xl font-bold mb-4">{post.title}</h1>
            <PostMeta 
              authorName={post.authorName}
              createdAt={post.createdAt}
              viewCount={post.viewCount}
            />
          </div>

          {post.shortDescription && (
            <div className="mb-8 p-4 bg-muted rounded-lg">
              <p className="text-lg text-muted-foreground italic">
                {post.shortDescription}
              </p>
            </div>
          )}

          <div
            className="prose prose-lg max-w-none dark:prose-invert 
                       prose-img:rounded-lg prose-img:shadow-md prose-img:my-6 prose-img:mx-auto
                       prose-video:rounded-lg prose-video:mx-auto prose-video:my-6 prose-video:w-full
                       [&_iframe]:w-full [&_iframe]:my-6 [&_iframe]:rounded-lg [&_iframe]:aspect-video [&_iframe]:h-auto
                       [&_.image-wrapper]:text-center [&_.image-wrapper]:my-6
                       [&_.image-caption]:text-center [&_.image-caption]:text-sm [&_.image-caption]:text-gray-500 
                       [&_.image-caption]:mt-2 [&_.image-caption]:italic [&_.image-caption]:mb-6"
            dangerouslySetInnerHTML={{ __html: renderContent(post.content) }}
          />
        </article>
      </div>
    </PageLayout>
  );
};

export default PostDetailPage;
