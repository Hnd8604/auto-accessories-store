import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { Blog } from "@/components/Blog";

const BlogPage = () => {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="pt-20">
        <Blog />
      </div>
      <Footer />
    </div>
  );
};

export default BlogPage;