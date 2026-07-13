import { useState, useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ShoppingCart, Eye, Loader2 } from "lucide-react";
import {
  type CarouselApi,
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import { ProductsApi, ProductImagesApi } from "@/features/products/api";
import { CategoriesApi } from "@/features/categories/api/categories";
import type {
  ProductResponse,
  ProductSearchRequest,
  BrandResponse,
  CategoryResponse,
} from "@/features/products/types";
import { useNavigate, useLocation } from "react-router-dom";
import { useCart } from "@/context/cart-context";

interface ProductsProps {
  limit?: number;
  searchParams?: ProductSearchRequest;
  sortBy?: string;
  showHeader?: boolean;
  layout?: "grid" | "category-rows";
  perCategoryLimit?: number;
}

export const Products = ({
  limit,
  searchParams = {},
  sortBy = "featured",
  showHeader = true,
  layout = "category-rows",
  perCategoryLimit = 20,
}: ProductsProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { addToCart } = useCart();
  const [productsWithImages, setProductsWithImages] = useState<ProductResponse[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [selectedBrandId, setSelectedBrandId] = useState<number | null>(null);

  // Fetch categories for tabs
  const { data: categoriesData } = useQuery({
    queryKey: ["categories"],
    queryFn: CategoriesApi.getAll,
  });

  const categoriesList: CategoryResponse[] = categoriesData?.result || [];
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

  const categoryBrands: BrandResponse[] = categoryBrandsData?.result || [];
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
  const { data: productsData, isLoading: productsLoading, isFetching } = useQuery({
    queryKey: ["products", mergedSearchParams, selectedCategory, { page: 0, size: limit || 100 }],
    queryFn: () => {
      if (hasEffectiveSearchParams) {
        return ProductsApi.search(mergedSearchParams, { page: 0, size: limit || 100 });
      }
      return ProductsApi.getAll({ page: 0, size: limit || 100 });
    },
    enabled: layout === "grid",
    staleTime: 5 * 60 * 1000, // Data stays fresh for 5 minutes
    gcTime: 10 * 60 * 1000, // Garbage collection time for cache
    placeholderData: (previousData) => previousData, // Keep previous data while fetching new data
  });

  let products = productsData?.result?.content || [];

  // Apply client-side sorting
  if (sortBy && products.length > 0) {
    products = [...products].sort((a, b) => {
      switch (sortBy) {
        case "price-low":
          return a.unitPrice - b.unitPrice;
        case "price-high":
          return b.unitPrice - a.unitPrice;
        case "newest":
          return b.id - a.id;
        case "rating":
          // If you have rating field, sort by it
          return 0;
        default:
          return 0;
      }
    });
  }

  // Apply limit if specified
  const filteredProducts = limit ? products.slice(0, limit) : products;

  // Fetch images ONLY for filtered/displayed products
  useEffect(() => {
    let isCancelled = false;

    if (layout !== "grid") {
      setProductsWithImages([]);
      return undefined;
    }

    if (filteredProducts.length > 0) {
      Promise.all(
        filteredProducts.map(async (product) => {
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
            console.error(`Failed to fetch images for product ${product.id}:`, error);
            return product;
          }
        })
      )
        .then(productsWithImgs => {
          if (!isCancelled) {
            setProductsWithImages(productsWithImgs);
          }
        })
        .catch(error => {
          if (!isCancelled) {
            console.error('Failed to fetch product images:', error);
            setProductsWithImages(filteredProducts);
          }
        });

      return () => {
        isCancelled = true;
      };
    } else {
      setProductsWithImages([]);
    }
  }, [layout, filteredProducts.length, filteredProducts.map(p => p.id).join(',')]);

  const displayProducts = productsWithImages.length > 0 ? productsWithImages : filteredProducts;

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  const handleAddToCart = async (product: ProductResponse, e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation();
    }
    try {
      await addToCart(product.id, 1);
    } catch (error) {
      console.error("Failed to add to cart:", error);
    }
  };

  const handleViewProduct = (product: ProductResponse) => {
    navigate(`/products/${product.slug}`, {
      state: {
        returnTo: {
          productId: product.id,
          categoryName: product.categoryName,
        },
      },
    });
  };

  const ProductCard = ({ product }: { product: ProductResponse }) => (
    <Card
      className="group bg-card border-border/50 hover:border-primary/50 transition-all duration-300 hover:shadow-lg hover:-translate-y-1 overflow-hidden cursor-pointer"
      onClick={() => handleViewProduct(product)}
    >
      <div className="relative">
        <div className="aspect-square w-full overflow-hidden bg-muted">
          <img
            src={
              product.primaryImageUrl ||
              product.images?.find((img) => img.isPrimary)?.imageUrl ||
              "https://placehold.co/600x600?text=No+Image"
            }
            alt={product.name}
            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
            onError={(e) => {
              const target = e.target as HTMLImageElement;
              target.src = "https://placehold.co/600x600?text=No+Image";
            }}
          />
        </div>

        <div className="absolute top-1.5 right-1.5 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col gap-0.5">
          <Button
            variant="secondary"
            size="icon"
            className="bg-background/90 hover:bg-background h-7 w-7"
            onClick={(e) => {
              e.stopPropagation();
              handleViewProduct(product);
            }}
          >
            <Eye className="h-3 w-3" />
          </Button>
        </div>
      </div>

      <CardHeader className="p-3 pb-2">
        <CardTitle className="text-sm font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-2 leading-tight">
          {product.name}
        </CardTitle>
      </CardHeader>

      <CardContent className="p-3 pt-0 space-y-2">
        <div className="flex items-baseline justify-between">
          <span className="text-lg font-bold text-primary">{formatPrice(product.unitPrice)}</span>
          <span className="text-[10px] text-muted-foreground">Còn: {product.stockQuantity}</span>
        </div>
        <Button
          onClick={(e) => handleAddToCart(product, e)}
          className="w-full h-8 text-xs"
          disabled={product.stockQuantity === 0}
        >
          <ShoppingCart className="h-3 w-3 mr-0.5" />
          Thêm Vào Giỏ
        </Button>
      </CardContent>
    </Card>
  );

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
                  disabled={isFetching && !isLoading}
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

        <div className="text-center mt-8">
          <Button 
            variant="luxury" 
            size="lg"
            onClick={() => navigate("/products")}
          >
            Xem Tất Cả Sản Phẩm
          </Button>
        </div>
      </div>
    </section>
  );
};

const CategoryProductsRow = ({
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
  const [productsWithImages, setProductsWithImages] = useState<ProductResponse[]>([]);
  const [loadingImages, setLoadingImages] = useState(false);
  const [carouselApi, setCarouselApi] = useState<CarouselApi | null>(null);
  const [selectedBrandId, setSelectedBrandId] = useState<number | null>(null);
  const rowRef = useRef<HTMLDivElement | null>(null);
  const hasAutoScrolled = useRef(false);

  const { data: categoryBrandsData } = useQuery({
    queryKey: ["category-brands", categoryId],
    queryFn: () => CategoriesApi.getBrandsByCategory(categoryId),
  });

  const brands: BrandResponse[] = categoryBrandsData?.result || [];
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

  const products = productsData?.result?.content || [];

  useEffect(() => {
    let isCancelled = false;

    if (products.length > 0) {
      const loadingTimer = setTimeout(() => {
        if (!isCancelled) setLoadingImages(true);
      }, 100);

      Promise.all(
        products.map(async (product) => {
          try {
            const response = await ProductImagesApi.getByProductId(product.id);
            const images = response?.result || [];
            const primaryImage = images.find((img) => img.isPrimary);
            return {
              ...product,
              images,
              primaryImageUrl: primaryImage?.imageUrl,
            };
          } catch (error) {
            console.error(`Failed to fetch images for product ${product.id}:`, error);
            return product;
          }
        })
      )
        .then((productsWithImgs) => {
          if (!isCancelled) {
            clearTimeout(loadingTimer);
            setProductsWithImages(productsWithImgs);
            setLoadingImages(false);
          }
        })
        .catch((error) => {
          if (!isCancelled) {
            clearTimeout(loadingTimer);
            console.error("Failed to fetch product images:", error);
            setProductsWithImages(products);
            setLoadingImages(false);
          }
        });

      return () => {
        isCancelled = true;
        clearTimeout(loadingTimer);
      };
    }

    setProductsWithImages([]);
    setLoadingImages(false);
  }, [products.length, products.map((p) => p.id).join(",")]);

  const displayProducts = productsWithImages.length > 0 ? productsWithImages : products;
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