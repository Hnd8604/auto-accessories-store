import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Search, Loader2 } from "lucide-react";
import { ProductsApi, ProductImagesApi } from "@/features/products/api";
import type { ProductResponse } from "@/features/products/types";
import { cn } from "@/utils/cn";

export const SearchBar = () => {
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [productsWithImages, setProductsWithImages] = useState<ProductResponse[]>([]);
  const searchRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  // Debounce search query
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery);
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Fetch search results
  const { data, isLoading } = useQuery({
    queryKey: ["search", debouncedQuery],
    queryFn: () =>
      ProductsApi.search({ keyword: debouncedQuery }, { page: 0, size: 5 }),
    enabled: debouncedQuery.length > 0,
    staleTime: 1 * 60 * 1000,
  });

  const products = data?.result?.content || [];

  // Fetch images for products
  useEffect(() => {
    if (products.length > 0) {
      Promise.all(
        products.map(async (product: ProductResponse) => {
          try {
            const response = await ProductImagesApi.getByProductId(product.id);
            const images = response?.result || [];
            const primaryImage = images.find(img => img.isPrimary);
            return {
              ...product,
              images,
              primaryImageUrl: primaryImage?.imageUrl,
            };
          } catch (error) {
            return product;
          }
        })
      ).then(setProductsWithImages);
    } else {
      setProductsWithImages([]);
    }
  }, [products.length, JSON.stringify(products.map(p => p.id))]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        searchRef.current &&
        !searchRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Show dropdown when there's a query
  useEffect(() => {
    if (searchQuery.length > 0) {
      setIsOpen(true);
    } else {
      setIsOpen(false);
    }
  }, [searchQuery, products.length]);

  const handleProductClick = (slug: string) => {
    setIsOpen(false);
    setSearchQuery("");
    navigate(`/products/${slug}`);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      setIsOpen(false);
      navigate(`/products?search=${encodeURIComponent(searchQuery)}`);
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  return (
    <div ref={searchRef} className="relative w-full max-w-xl">
      <form onSubmit={handleSearchSubmit}>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Tìm kiếm sản phẩm..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10 pr-4"
          />
          {isLoading && (
            <Loader2 className="absolute right-3 top-1/2 transform -translate-y-1/2 h-4 w-4 animate-spin text-muted-foreground" />
          )}
        </div>
      </form>

      {/* Dropdown Results */}
      {isOpen && (
        <Card className="absolute top-full mt-2 w-full max-h-[400px] overflow-y-auto z-50 shadow-lg">
          {isLoading ? (
            <div className="p-4 text-center text-muted-foreground">
              <Loader2 className="h-6 w-6 animate-spin mx-auto" />
            </div>
          ) : productsWithImages.length > 0 ? (
            <div className="py-2">
              <div className="px-3 py-2 text-xs font-semibold text-muted-foreground">
                Sản phẩm gợi ý
              </div>
              {productsWithImages.map((product: ProductResponse) => (
                <div
                  key={product.id}
                  onClick={() => handleProductClick(product.slug || "")}
                  className="flex items-center gap-3 px-3 py-3 hover:bg-muted cursor-pointer transition-colors"
                >
                  {/* Product Image */}
                  <div className="flex-shrink-0 w-16 h-16 bg-muted rounded overflow-hidden">
                    {product.primaryImageUrl ? (
                      <img
                        src={product.primaryImageUrl}
                        alt={product.name}
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.currentTarget.src = "";
                          e.currentTarget.style.display = "none";
                        }}
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        <Search className="h-6 w-6 text-muted-foreground" />
                      </div>
                    )}
                  </div>

                  {/* Product Info */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground line-clamp-2">
                      {product.name}
                    </p>
                    <p className="text-sm font-semibold text-primary mt-1">
                      {formatPrice(product.unitPrice)}
                    </p>
                  </div>
                </div>
              ))}

              {/* View All Results */}
              <div
                onClick={() => {
                  setIsOpen(false);
                  navigate(`/products?search=${encodeURIComponent(searchQuery)}`);
                }}
                className="px-3 py-3 text-sm text-primary hover:bg-muted cursor-pointer text-center font-medium border-t"
              >
                Xem tất cả kết quả
              </div>
            </div>
          ) : (
            <div className="p-4 text-center text-muted-foreground">
              Không tìm thấy sản phẩm nào
            </div>
          )}
        </Card>
      )}
    </div>
  );
};
