import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { StoreLayout } from "@/app/components/StoreLayout";
import { HeroSlider } from "@/features/banners/components/HeroSlider";
import { Services } from "@/features/services/components/Services";
import { Contact } from "@/app/components/Contact";
import { Products } from "@/features/products/components/Products";
import { Blog } from "@/features/posts/components/Blog";

const HomePage = () => {
  const { hash } = useLocation();

  useEffect(() => {
    if (!hash) return;
    const frame = requestAnimationFrame(() => {
      document.getElementById(hash.slice(1))?.scrollIntoView({ behavior: "smooth" });
    });
    return () => cancelAnimationFrame(frame);
  }, [hash]);

  return (
    <StoreLayout showChat>
      <HeroSlider />
      <Products />
      <Services />
      <Blog />
      <Contact />
    </StoreLayout>
  );
};

export default HomePage;
