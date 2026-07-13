import { Header } from "@/components/Header";
import { HeroSlider } from "@/components/HeroSlider";
import { Services } from "@/components/Services";
import { Contact } from "@/components/Contact";
import { Footer } from "@/components/Footer";
import { Products } from "@/features/products/components/Products";
import { Blog } from "@/components/Blog";
import { ChatWidget } from "@/components/ChatWidget";

const Index = () => {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      <HeroSlider />
      <Products />
      <Services />
      <Blog />
      <Contact />
      <Footer />
      <ChatWidget />
    </div>
  );
};

export default Index;
