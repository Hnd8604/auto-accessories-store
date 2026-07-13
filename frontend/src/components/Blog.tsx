import { useState, useMemo, useCallback, memo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Calendar, User, ArrowRight, Tag, Eye, Loader2 } from "lucide-react";
import { PostsApi } from "@/features/posts/api";
import { PostCategoriesApi } from "@/features/posts/api";
import type { PostResponse } from "@/features/posts/types";

export const Blog = memo(() => {
  const [selectedCategory, setSelectedCategory] = useState("Tất Cả");
  const navigate = useNavigate();

  // Fetch published posts
  const { data: postsData, isLoading: postsLoading } = useQuery({
    queryKey: ["posts", "published"],
    queryFn: () => PostsApi.getPublished({ page: 0, size: 20 }),
  });

  // Fetch categories
  const { data: categoriesData, isLoading: categoriesLoading } = useQuery({
    queryKey: ["post-categories"],
    queryFn: () => PostCategoriesApi.getAll(),
  });

  const posts = useMemo(() => postsData?.result?.content || [], [postsData]);
  const categoryList = useMemo(() => categoriesData?.result || [], [categoriesData]);
  const categories = useMemo(() => 
    ["Tất Cả", ...categoryList.map(c => c.name)], 
    [categoryList]
  );

  const filteredPosts = useMemo(() => 
    selectedCategory === "Tất Cả" 
      ? posts 
      : posts.filter(post => post.categoryName === selectedCategory),
    [selectedCategory, posts]
  );

  const formatDate = useCallback((dateString?: string) => {
    if (!dateString) return "";
    return new Date(dateString).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }, []);

  const handlePostClick = useCallback((slug: string) => {
    navigate(`/blog/${slug}`);
  }, [navigate]);

  if (postsLoading || categoriesLoading) {
    return (
      <section className="py-20 bg-muted/20">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-center min-h-[400px]">
            <Loader2 className="h-12 w-12 animate-spin text-primary" />
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="py-20 bg-muted/20">
      <div className="container mx-auto px-4">
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold text-foreground mb-6">
            Blog & 
            <span className="bg-gradient-accent bg-clip-text text-transparent"> Tin Tức</span>
          </h2>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto mb-8">
            Cập nhật những thông tin mới nhất về xu hướng nội thất ô tô, 
            hướng dẫn bảo dưỡng và kiến thức chuyên môn từ đội ngũ chuyên gia.
          </p>

          {/* Category Filter */}
          <div className="flex flex-wrap justify-center gap-4">
            {categories.map((category) => (
              <Button
                key={category}
                variant={selectedCategory === category ? "default" : "outline"}
                onClick={() => setSelectedCategory(category)}
                className="transition-all duration-300"
              >
                {category}
              </Button>
            ))}
          </div>
        </div>

        {/* Blog Posts Grid */}
        {filteredPosts.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-muted-foreground text-lg">Chưa có bài viết nào được xuất bản</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {filteredPosts.map((post) => (
              <Card 
                key={post.id} 
                className="group bg-card border-border/50 hover:border-primary/50 transition-all duration-300 hover:shadow-card hover:-translate-y-2 cursor-pointer overflow-hidden"
                onClick={() => handlePostClick(post.slug)}
              >
                <div className="relative overflow-hidden">
                  {post.thumbnailUrl ? (
                    <img
                      src={post.thumbnailUrl}
                      alt={post.title}
                      className="w-full h-48 object-cover transition-transform duration-500 group-hover:scale-110"
                    />
                  ) : (
                    <div className="w-full h-48 bg-muted flex items-center justify-center">
                      <Tag className="h-12 w-12 text-muted-foreground" />
                    </div>
                  )}
                  {post.categoryName && (
                    <Badge 
                      variant="secondary" 
                      className="absolute top-4 left-4 bg-primary/20 text-primary border-primary/30 backdrop-blur-sm"
                    >
                      {post.categoryName}
                    </Badge>
                  )}
                </div>

                <CardHeader className="pb-4">
                  <div className="flex items-center gap-4 text-sm text-muted-foreground mb-2 flex-wrap">
                    {post.createdAt && (
                      <div className="flex items-center gap-1">
                        <Calendar className="h-4 w-4" />
                        <span>{formatDate(post.createdAt)}</span>
                      </div>
                    )}
                    {post.authorName && (
                      <div className="flex items-center gap-1">
                        <User className="h-4 w-4" />
                        <span>{post.authorName}</span>
                      </div>
                    )}
                    {post.viewCount !== undefined && (
                      <div className="flex items-center gap-1">
                        <Eye className="h-4 w-4" />
                        <span>{post.viewCount}</span>
                      </div>
                    )}
                  </div>
                  <CardTitle className="text-lg text-foreground group-hover:text-primary transition-colors line-clamp-2">
                    {post.title}
                  </CardTitle>
                  {post.shortDescription && (
                    <CardDescription className="text-muted-foreground line-clamp-3">
                      {post.shortDescription}
                    </CardDescription>
                  )}
                </CardHeader>

                <CardContent>
                  <div className="flex items-center justify-end">
                    <div className="flex items-center gap-2 text-sm text-primary">
                      <span>Đọc thêm</span>
                      <ArrowRight className="h-4 w-4 group-hover:translate-x-1 transition-transform" />
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        <div className="text-center mt-12">
          <Button variant="luxury" size="xl" onClick={() => navigate("/blog")}>
            Xem Tất Cả Bài Viết
          </Button>
        </div>
      </div>
    </section>
  );
});

Blog.displayName = "Blog";
