import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  type CarouselApi,
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import { CategoriesApi } from "@/features/categories/api/categories";
import { ProductsApi } from "@/features/products/api";
import type { BrandResponse, ProductResponse } from "@/features/products/types";
import { useProductPrimaryImages } from "../hooks/useProductPrimaryImages";

const EMPTY_BRANDS: BrandResponse[] = [];

export const CategoryProductsRow = ({
  categoryId,
  categoryName,
  perCategoryLimit,
  scrollToProductId,
  scrollToCategory,
  renderProduct,
}: {
  categoryId: number;
  categoryName: string;
  perCategoryLimit: number;
  scrollToProductId?: number;
  scrollToCategory?: string;
  renderProduct: (product: ProductResponse) => React.ReactNode;
}) => {
  const [carouselApi, setCarouselApi] = useState<CarouselApi | null>(null);
  const [selectedBrandId, setSelectedBrandId] = useState<number | null>(null);
  const rowRef = useRef<HTMLDivElement | null>(null);
  const hasAutoScrolled = useRef(false);

  const { data: categoryBrandsData } = useQuery({
    queryKey: ["category-brands", categoryId],
    queryFn: () => CategoriesApi.getBrandsByCategory(categoryId),
  });

  const brands: BrandResponse[] = categoryBrandsData?.result ?? EMPTY_BRANDS;
  const selectedBrand = brands.find((brand) => brand.id === selectedBrandId);

  useEffect(() => {
    if (!selectedBrandId) {
      return;
    }
    if (!brands.some((brand) => brand.id === selectedBrandId)) {
      setSelectedBrandId(null);
    }
  }, [brands, selectedBrandId]);

  const { data: productsData, isLoading, isFetching } = useQuery({
    queryKey: [
      "products",
      "category",
      categoryName,
      selectedBrand?.name,
      { page: 0, size: perCategoryLimit },
    ],
    queryFn: () =>
      ProductsApi.search(
        {
          category: categoryName,
          ...(selectedBrand?.name && { brand: selectedBrand.name }),
        },
        { page: 0, size: perCategoryLimit }
      ),
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    placeholderData: (previousData) => previousData,
  });

  const products = productsData?.result?.content ?? [];
  const { imageUrls, isFetching: loadingImages } = useProductPrimaryImages(
    products.map((product) => product.id)
  );
  const displayProducts = products.map((product) => ({
    ...product,
    primaryImageUrl: imageUrls[product.id] ?? product.primaryImageUrl,
  }));
  const shouldScroll =
    !!scrollToProductId && (!scrollToCategory || scrollToCategory === categoryName);
  const targetIndex = shouldScroll
    ? displayProducts.findIndex((product) => product.id === scrollToProductId)
    : -1;

  useEffect(() => {
    if (!shouldScroll || targetIndex < 0 || hasAutoScrolled.current) {
      return;
    }

    rowRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
    carouselApi?.scrollTo(targetIndex);
    hasAutoScrolled.current = true;
  }, [shouldScroll, targetIndex, carouselApi]);

  if (isLoading && products.length === 0) {
    return (
      <div className="space-y-4">
        <div className="h-6 w-48 bg-muted rounded" />
        <div className="h-48 bg-muted/40 rounded-lg" />
      </div>
    );
  }

  if (!isFetching && displayProducts.length === 0) {
    return null;
  }

  return (
    <div ref={rowRef} className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex flex-wrap items-center gap-3">
          <h3 className="text-xl md:text-2xl font-semibold text-foreground">{categoryName}</h3>
          {brands.length > 0 && (
            <div className="flex flex-wrap items-center gap-2">
              <Button
                variant={selectedBrandId === null ? "default" : "outline"}
                onClick={() => setSelectedBrandId(null)}
                size="sm"
                className="h-7 rounded-md px-3 text-xs"
              >
                Tất Cả
              </Button>
              {brands.map((brand) => (
                <Button
                  key={brand.id}
                  variant={selectedBrandId === brand.id ? "default" : "outline"}
                  onClick={() => setSelectedBrandId(brand.id)}
                  size="sm"
                  className="h-7 rounded-md px-3 text-xs"
                >
                  {brand.name}
                </Button>
              ))}
            </div>
          )}
        </div>
        {isFetching && loadingImages && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Đang tải
          </div>
        )}
      </div>

      <Carousel
        opts={{ align: "start", slidesToScroll: 1 }}
        className="px-10 sm:px-12"
        setApi={setCarouselApi}
      >
        <CarouselContent>
          {displayProducts.map((product) => (
            <CarouselItem key={product.id} className="basis-1/2 sm:basis-1/3 lg:basis-1/5">
              {renderProduct(product)}
            </CarouselItem>
          ))}
        </CarouselContent>
        <CarouselPrevious className="-left-2 sm:-left-6" />
        <CarouselNext className="-right-2 sm:-right-6" />
      </Carousel>
    </div>
  );
};
