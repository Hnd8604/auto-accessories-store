import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { PostsApi } from "@/features/posts/api";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Loader2, Calendar, Eye, ArrowRight, Search } from "lucide-react";
import type { PaginationParams } from "@/types";

export const PostListPage = () => {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");
  const [pagination, setPagination] = useState<PaginationParams>({
    page: 0,
    size: 12,
  });

  const { data, isLoading } = useQuery({
    queryKey: ["posts", "published", pagination],
    queryFn: () => PostsApi.getPublished(pagination),
  });

  const posts = data?.result?.content || [];
  const totalPages = data?.result?.totalPages || 0;

  const filteredPosts = posts.filter((post) =>
    post.title.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="pt-20">
        {/* Hero Section */}
        <section className="bg-primary/5 py-16">
          <div className="container mx-auto px-4">
            <h1 className="text-4xl md:text-5xl font-bold mb-4 text-center">
              Blog
            </h1>
            <p className="text-xl text-muted-foreground text-center max-w-2xl mx-auto mb-8">
              Khám phá những bài viết hữu ích và cập nhật mới nhất
            </p>
            
            <div className="max-w-xl mx-auto relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-muted-foreground" />
              <Input
                placeholder="Tìm kiếm bài viết..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>
        </section>

        {/* Posts Grid */}
        <section className="container mx-auto px-4 py-12">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-12 w-12 animate-spin text-primary" />
            </div>
          ) : filteredPosts.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-lg text-muted-foreground">
                {searchQuery
                  ? "Không tìm thấy bài viết nào"
                  : "Chưa có bài viết nào"}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredPosts.map((post) => (
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
