import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Loader2 } from "lucide-react";
import { ProductsApi } from "@/features/products/api";
import { CategoriesApi } from "@/features/categories/api/categories";
import type {
  ProductSearchRequest,
  BrandResponse,
  CategoryResponse,
} from "@/features/products/types";
import { useNavigate, useLocation } from "react-router-dom";
import { CategoryProductsRow } from "./CategoryProductsRow";
import { ProductCard } from "./ProductCard";
import { useProductPrimaryImages } from "../hooks/useProductPrimaryImages";

interface ProductsProps {
  limit?: number;
  searchParams?: ProductSearchRequest;
  sortBy?: string;
  showHeader?: boolean;
  layout?: "grid" | "category-rows";
  perCategoryLimit?: number;
  pageSize?: number;
  enablePagination?: boolean;
  showViewAll?: boolean;
}

const EMPTY_SEARCH: ProductSearchRequest = {};
const EMPTY_BRANDS: BrandResponse[] = [];
const PRODUCT_SORT: Record<string, string> = {
  featured: "name,asc",
  "price-low": "unitPrice,asc",
  "price-high": "unitPrice,desc",
  newest: "id,desc",
};

export const Products = ({
  limit,
  searchParams = EMPTY_SEARCH,
  sortBy = "featured",
  showHeader = true,
  layout = "category-rows",
  perCategoryLimit = 20,
  pageSize = 24,
  enablePagination = false,
  showViewAll = true,
}: ProductsProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [selectedBrandId, setSelectedBrandId] = useState<number | null>(null);
  const [currentPage, setCurrentPage] = useState(0);

  useEffect(() => {
    setCurrentPage(0);
  }, [searchParams, sortBy, selectedCategoryId, selectedBrandId]);

  // Fetch categories for tabs
  const { data: categoriesData } = useQuery({
    queryKey: ["categories"],
    queryFn: CategoriesApi.getAll,
  });

  const categoriesList: CategoryResponse[] = categoriesData?.result ?? [];
  const categoryTabs: Array<{ id: number | null; name: string }> = [
    { id: null, name: "Tất Cả" },
    ...categoriesList.map((category) => ({ id: Number(category.id), name: category.name })),
  ];
  const selectedCategory = categoriesList.find(
    (category) => Number(category.id) === selectedCategoryId
  );

  const { data: categoryBrandsData } = useQuery({
    queryKey: ["category-brands", selectedCategoryId],
    queryFn: () => CategoriesApi.getBrandsByCategory(selectedCategoryId as number),
    enabled: Boolean(selectedCategoryId),
  });

  const categoryBrands: BrandResponse[] = categoryBrandsData?.result ?? EMPTY_BRANDS;
  const selectedBrand = categoryBrands.find((brand) => brand.id === selectedBrandId);

  useEffect(() => {
    setSelectedBrandId(null);
  }, [selectedCategoryId]);

  useEffect(() => {
    if (!selectedBrandId) {
      return;
    }
    if (!categoryBrands.some((brand) => brand.id === selectedBrandId)) {
      setSelectedBrandId(null);
    }
  }, [categoryBrands, selectedBrandId]);

  // Determine if we need to use search API or regular getAll
  // Merge category filter with searchParams
  const mergedSearchParams = {
    ...searchParams,
    ...(selectedCategory?.name && { category: selectedCategory.name }),
    ...(selectedBrand?.name && { brand: selectedBrand.name }),
  };

  const hasEffectiveSearchParams = mergedSearchParams && (
    mergedSearchParams.keyword || 
    mergedSearchParams.category || 
    mergedSearchParams.brand || 
    mergedSearchParams.minPrice !== undefined || 
    mergedSearchParams.maxPrice !== undefined || 
    mergedSearchParams.inStock
  );

  // Fetch products - use search if params exist
  const { data: productsData, isLoading: productsLoading, isFetching, isError: productsError, refetch: refetchProducts } = useQuery({
    queryKey: ["products", mergedSearchParams, { page: currentPage, size: enablePagination ? pageSize : limit || 100, sort: PRODUCT_SORT[sortBy] ?? PRODUCT_SORT.featured }],
    queryFn: () => {
      const pagination = { page: currentPage, size: enablePagination ? pageSize : limit || 100, sort: PRODUCT_SORT[sortBy] ?? PRODUCT_SORT.featured };
      if (hasEffectiveSearchParams) {
        return ProductsApi.search(mergedSearchParams, pagination);
      }
      return ProductsApi.getAll(pagination);
    },
    enabled: layout === "grid",
    staleTime: 5 * 60 * 1000, // Data stays fresh for 5 minutes
    gcTime: 10 * 60 * 1000, // Garbage collection time for cache
    placeholderData: (previousData) => previousData, // Keep previous data while fetching new data
  });

  const products = productsData?.result?.content || [];

  // Apply limit if specified
  const filteredProducts = limit ? products.slice(0, limit) : products;

  const { imageUrls } = useProductPrimaryImages(
    layout === "grid" ? filteredProducts.map((product) => product.id) : []
  );
  const displayProducts = filteredProducts.map((product) => ({
    ...product,
    primaryImageUrl: imageUrls[product.id] ?? product.primaryImageUrl,
  }));

  // Skeleton loader component
  const ProductSkeleton = () => (
    <Card className="overflow-hidden animate-pulse">
      <div className="aspect-square bg-muted" />
      <CardContent className="p-4 space-y-2">
        <div className="h-4 bg-muted rounded w-3/4" />
        <div className="h-4 bg-muted rounded w-1/2" />
        <div className="h-8 bg-muted rounded w-full mt-4" />
      </CardContent>
    </Card>
  );

  // Only show skeleton on initial load when there's no data at all
  const isInitialLoading = productsLoading && !productsData;

  if (isInitialLoading && layout === "grid") {
    return (
      <section className="py-12 bg-muted/20">
        <div className="container mx-auto px-4">
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
            {Array.from({ length: limit || 8 }).map((_, i) => (
              <ProductSkeleton key={i} />
            ))}
          </div>
        </div>
      </section>
    );
  }

  if (productsError && layout === "grid") {
    return (
      <section className="container mx-auto px-4 py-12 text-center" role="alert">
        <p className="mb-4">Không thể tải sản phẩm. Vui lòng thử lại.</p>
        <Button variant="outline" onClick={() => { void refetchProducts(); }}>Thử lại</Button>
      </section>
    );
  }

  if (layout === "category-rows") {
    const categoriesForRows = selectedCategoryId
      ? categoriesList.filter((category) => Number(category.id) === selectedCategoryId)
      : categoriesList;
    const scrollState = location.state as
      | { scrollToProductId?: number; scrollToCategory?: string }
      | undefined;
    const scrollToProductId = scrollState?.scrollToProductId;
    const scrollToCategory = scrollState?.scrollToCategory;

    return (
      <section className="py-12 bg-muted/20">
        <div className="container mx-auto px-4 space-y-12">
          {showHeader && (
            <div className="text-center max-w-3xl mx-auto space-y-4">
              <h2 className="text-4xl md:text-5xl font-bold text-foreground">
                Sản Phẩm Theo Danh Mục
              </h2>
            </div>
          )}

          {categoriesForRows.length === 0 && (
            <div className="text-center py-12">
              <p className="text-lg text-muted-foreground">Không có danh mục nào</p>
            </div>
          )}

          {showHeader && categoriesList.length > 0 && (
            <div className="flex flex-wrap justify-center gap-3">
              {categoryTabs.map((category) => (
                <Button
                  key={category.id ?? "all"}
                  variant={selectedCategoryId === category.id ? "default" : "outline"}
                  onClick={() => setSelectedCategoryId(category.id)}
                  className="transition-all duration-300"
                  disabled={isFetching && !productsLoading}
                >
                  {category.name}
                </Button>
              ))}
            </div>
          )}

          {categoriesForRows.map((category) => (
            <CategoryProductsRow
              key={category.id}
              categoryId={Number(category.id)}
              categoryName={category.name}
              perCategoryLimit={perCategoryLimit}
              scrollToProductId={scrollToProductId}
              scrollToCategory={scrollToCategory}
              renderProduct={(product) => <ProductCard product={product} />}
            />
          ))}
        </div>
      </section>
    );
  }

  return (
    <section className="py-12 bg-muted/20">
      <div className="container mx-auto px-4">
        {showHeader && (
          <div className="text-center max-w-3xl mx-auto mb-12 space-y-6">
            <h2 className="text-4xl md:text-5xl font-bold text-foreground">
              Sản Phẩm Nội Thất Ô Tô
            </h2>
            {/* <p className="text-lg text-muted-foreground">
              Khám phá bộ sưu tập phụ kiện và nội thất ô tô cao cấp từ các thương hiệu hàng đầu
            </p> */}

            {/* Category Filter */}
            <div className="flex flex-wrap justify-center gap-3">
              {categoryTabs.map((category) => (
                <Button
                  key={category.id ?? "all"}
                  variant={selectedCategoryId === category.id ? "default" : "outline"}
                  onClick={() => setSelectedCategoryId(category.id)}
                  className="transition-all duration-300"
                  disabled={isFetching && !productsLoading}
                >
                  {category.name}
                </Button>
              ))}

              {selectedCategoryId && categoryBrands.length > 0 && (
                <div className="flex flex-wrap items-center gap-2">
                  <Button
                    variant={selectedBrandId === null ? "default" : "outline"}
                    onClick={() => setSelectedBrandId(null)}
                    size="sm"
                    className="h-7 rounded-md px-3 text-xs"
                  >
                    Tất Cả
                  </Button>
                  {categoryBrands.map((brand) => (
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
            
            {/* Loading indicator when refetching */}
            {isFetching && !productsLoading && (
              <div className="flex justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-primary" />
              </div>
            )}
          </div>
        )}

        {/* Products Grid */}
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6 relative">
          {/* Overlay loading indicator when switching categories */}
          {isFetching && displayProducts.length > 0 && (
            <div className="absolute inset-0 bg-background/50 backdrop-blur-sm z-10 flex items-center justify-center rounded-lg">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          )}
          {displayProducts.length === 0 && !isFetching ? (
            <div className="col-span-full text-center py-12">
              <p className="text-lg text-muted-foreground">Không có sản phẩm nào</p>
            </div>
          ) : (
            displayProducts.map((product) => <ProductCard key={product.id} product={product} />)
          )}
        </div>

        {enablePagination && (productsData?.result?.totalPages ?? 0) > 1 && (
          <nav aria-label="Phân trang sản phẩm" className="mt-8 flex items-center justify-center gap-3">
            <Button variant="outline" disabled={currentPage === 0 || isFetching}
              onClick={() => setCurrentPage((page) => page - 1)}>
              Trang trước
            </Button>
            <span className="text-sm text-muted-foreground">
              Trang {currentPage + 1} / {productsData?.result?.totalPages}
            </span>
            <Button variant="outline"
              disabled={isFetching || currentPage + 1 >= (productsData?.result?.totalPages ?? 0)}
              onClick={() => setCurrentPage((page) => page + 1)}>
              Trang sau
            </Button>
          </nav>
        )}

        {showViewAll && (
          <div className="text-center mt-8">
            <Button variant="luxury" size="lg" onClick={() => navigate("/products")}>
              Xem Tất Cả Sản Phẩm
            </Button>
          </div>
        )}
      </div>
    </section>
  );
};
