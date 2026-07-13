import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams, useNavigate } from "react-router-dom";
import { PostsApi } from "@/features/posts/api";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Loader2, Calendar, Eye, ArrowRight, ArrowLeft } from "lucide-react";
import type { PaginationParams } from "@/types";

export const PostsByCategoryPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const [pagination, setPagination] = useState<PaginationParams>({
    page: 0,
    size: 12,
  });

  const { data, isLoading } = useQuery({
    queryKey: ["posts", "category", slug, pagination],
    queryFn: () => PostsApi.getByCategorySlug(slug!, pagination),
    enabled: !!slug,
  });

  const posts = data?.result?.content || [];
  const totalPages = data?.result?.totalPages || 0;

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="pt-20">
        {/* Hero Section */}
        <section className="bg-primary/5 py-16">
          <div className="container mx-auto px-4">
            <Button
              variant="ghost"
              className="mb-4"
              onClick={() => navigate("/blog")}
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Quay lại blog
            </Button>
            <h1 className="text-4xl md:text-5xl font-bold mb-4 capitalize">
              {slug?.replace(/-/g, " ")}
            </h1>
            <p className="text-xl text-muted-foreground">
              Các bài viết trong danh mục này
            </p>
          </div>
        </section>

        {/* Posts Grid */}
        <section className="container mx-auto px-4 py-12">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-12 w-12 animate-spin text-primary" />
            </div>
          ) : posts.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-lg text-muted-foreground">
                Chưa có bài viết nào trong danh mục này
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {posts.map((post) => (
                <Card
                  key={post.id}
                  className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group"
                  onClick={() => navigate(`/blog/${post.slug}`)}
                >
                  {post.thumbnailUrl && (
                    <div className="aspect-video overflow-hidden">
                      <img
                        src={post.thumbnailUrl}
                        alt={post.title}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                    </div>
                  )}
                  <CardHeader>
                    {post.categoryName && (
                      <Badge className="w-fit mb-2">{post.categoryName}</Badge>
                    )}
                    <h3 className="text-xl font-bold line-clamp-2 group-hover:text-primary transition-colors">
                      {post.title}
                    </h3>
                  </CardHeader>
                  <CardContent>
                    {post.shortDescription && (
                      <p className="text-muted-foreground line-clamp-3 mb-4">
                        {post.shortDescription}
                      </p>
                    )}
                    
                    <div className="flex items-center justify-between text-sm text-muted-foreground">
                      <div className="flex items-center gap-4">
                        {post.createdAt && (
                          <div className="flex items-center gap-1">
                            <Calendar className="h-4 w-4" />
                            <span>
                              {new Date(post.createdAt).toLocaleDateString("vi-VN")}
                            </span>
                          </div>
                        )}
                        <div className="flex items-center gap-1">
                          <Eye className="h-4 w-4" />
                          <span>{post.viewCount || 0}</span>
                        </div>
                      </div>
                      <ArrowRight className="h-5 w-5 group-hover:translate-x-1 transition-transform" />
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-8">
              <Button
                variant="outline"
                disabled={pagination.page === 0}
                onClick={() =>
                  setPagination((prev) => ({ ...prev, page: prev.page! - 1 }))
                }
              >
                Trang trước
              </Button>
              <div className="flex items-center px-4">
                Trang {(pagination.page || 0) + 1} / {totalPages}
              </div>
              <Button
                variant="outline"
                disabled={pagination.page! >= totalPages - 1}
                onClick={() =>
                  setPagination((prev) => ({ ...prev, page: prev.page! + 1 }))
                }
              >
                Trang sau
              </Button>
            </div>
          )}
        </section>
      </div>
      <Footer />
    </div>
  );
};
