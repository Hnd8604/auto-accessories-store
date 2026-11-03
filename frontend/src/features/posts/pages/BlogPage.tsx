import { StoreLayout } from "@/app/components/StoreLayout";
import { Blog } from "@/features/posts/components/Blog";

const BlogPage = () => {
  return (
    <StoreLayout>
      <div className="pt-20">
        <Blog />
      </div>
    </StoreLayout>
  );
};

export default BlogPage;
