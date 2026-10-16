import { useState, useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search, Filter, SlidersHorizontal, X } from "lucide-react";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { CategoriesApi } from "@/features/categories/api/categories";
import type { ProductSearchRequest } from "@/features/products/types";
import { Badge } from "@/components/ui/badge";
import { FilterContent } from "./FilterContent";

interface ProductFiltersProps {
  onSearch: (params: ProductSearchRequest) => void;
  onSort: (sort: string) => void;
  initialKeyword?: string;
}

export const ProductFilters = ({ onSearch, onSort, initialKeyword = "" }: ProductFiltersProps) => {
  const [searchQuery, setSearchQuery] = useState(initialKeyword);
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState(initialKeyword);
  const [priceRange, setPriceRange] = useState([0, 50000000]);
  const [selectedCategories, setSelectedCategories] = useState<number[]>([]);
  const [selectedBrands, setSelectedBrands] = useState<number[]>([]);
  const [inStockOnly, setInStockOnly] = useState(false);
  const [showFilters, setShowFilters] = useState(false);

  useEffect(() => {
    setSearchQuery(initialKeyword);
    setDebouncedSearchQuery(initialKeyword);
  }, [initialKeyword]);

  // Debounce search query
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchQuery(searchQuery);
    }, 500); // Wait 500ms after user stops typing

    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Fetch categories and brands from API
  const { data: categoriesData } = useQuery({
    queryKey: ["categories"],
    queryFn: CategoriesApi.getAll,
  });

  const activeCategoryId = selectedCategories[0];

  const { data: categoryBrandsData } = useQuery({
    queryKey: ["category-brands", activeCategoryId],
    queryFn: () => CategoriesApi.getBrandsByCategory(activeCategoryId),
    enabled: Boolean(activeCategoryId),
  });

  const categories = useMemo(() => categoriesData?.result ?? [], [categoriesData]);
  const brands = useMemo(
    () => activeCategoryId ? (categoryBrandsData?.result ?? []) : [],
    [activeCategoryId, categoryBrandsData]
  );

  // Apply filters whenever search params change (using debounced search query)
  useEffect(() => {
    const searchParams: ProductSearchRequest = {};
    
    if (debouncedSearchQuery.trim()) {
      searchParams.keyword = debouncedSearchQuery.trim();
    }
    
    if (selectedCategories.length > 0) {
      // Backend expects single category, use first selected
      const selectedCategory = categories.find(cat => Number(cat.id) === selectedCategories[0]);
      if (selectedCategory?.name) {
        searchParams.category = selectedCategory.name;
      }
    }
    
    if (selectedBrands.length > 0) {
      // Backend expects single brand, use first selected
      const selectedBrand = brands.find(brand => brand.id === selectedBrands[0]);
      if (selectedBrand?.name) {
        searchParams.brand = selectedBrand.name;
      }
    }
    
    if (priceRange[0] > 0) {
      searchParams.minPrice = priceRange[0];
    }
    
    if (priceRange[1] < 50000000) {
      searchParams.maxPrice = priceRange[1];
    }
    
    if (inStockOnly) {
      searchParams.inStock = true;
    }
    
    onSearch(searchParams);
  }, [debouncedSearchQuery, selectedCategories, selectedBrands, priceRange, inStockOnly, onSearch, categories, brands]);

  useEffect(() => {
    if (!activeCategoryId) {
      setSelectedBrands([]);
      return;
    }
    setSelectedBrands((prev) => prev.filter((id) => brands.some((brand) => brand.id === id)));
  }, [activeCategoryId, brands]);

  const handleCategoryToggle = (categoryId: number) => {
    setSelectedCategories(prev =>
      prev.includes(categoryId) ? [] : [categoryId]
    );
  };

  const handleBrandToggle = (brandId: number) => {
    setSelectedBrands(prev =>
      prev.includes(brandId) ? [] : [brandId]
    );
  };

  const handleClearFilters = () => {
    setSearchQuery("");
    setDebouncedSearchQuery("");
    setPriceRange([0, 50000000]);
    setSelectedCategories([]);
    setSelectedBrands([]);
    setInStockOnly(false);
  };

  const hasActiveFilters = debouncedSearchQuery || selectedCategories.length > 0 || 
    selectedBrands.length > 0 || priceRange[0] > 0 || priceRange[1] < 50000000 || inStockOnly;

  return (
    <div className="bg-card border-b border-border/50 py-6 mt-4">
      <div className="container mx-auto px-4">
        <div className="flex flex-col lg:flex-row gap-4 items-center justify-between">
          {/* Search */}
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Tìm kiếm sản phẩm..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
            {searchQuery && (
              <Button
                variant="ghost"
                size="icon"
                className="absolute right-1 top-1/2 transform -translate-y-1/2 h-8 w-8"
                onClick={() => setSearchQuery("")}
              >
                <X className="h-4 w-4" />
              </Button>
            )}
          </div>

          {/* Sort & Filter Controls */}
          <div className="flex items-center gap-4">
            {/* Sort */}
            <Select defaultValue="featured" onValueChange={onSort}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Sắp xếp theo" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="featured">Nổi bật</SelectItem>
                <SelectItem value="price-low">Giá thấp đến cao</SelectItem>
                <SelectItem value="price-high">Giá cao đến thấp</SelectItem>
                <SelectItem value="newest">Mới nhất</SelectItem>
              </SelectContent>
            </Select>

            {/* Mobile Filter Toggle */}
            <Sheet>
              <SheetTrigger asChild>
                <Button variant="outline" size="sm" className="lg:hidden">
                  <SlidersHorizontal className="h-4 w-4 mr-2" />
                  Bộ Lọc
                </Button>
              </SheetTrigger>
              <SheetContent side="left" className="w-80">
                <SheetHeader>
                  <SheetTitle>Bộ Lọc Sản Phẩm</SheetTitle>
                </SheetHeader>
                <FilterContent 
                  categories={categories}
                  brands={brands}
                  selectedCategories={selectedCategories}
                  selectedBrands={selectedBrands}
                  onCategoryToggle={handleCategoryToggle}
                  onBrandToggle={handleBrandToggle}
                  priceRange={priceRange}
                  setPriceRange={setPriceRange}
                  inStockOnly={inStockOnly}
                  setInStockOnly={setInStockOnly}
                  idPrefix="mobile"
                />
              </SheetContent>
            </Sheet>

            {/* Clear Filters */}
            {hasActiveFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={handleClearFilters}
              >
                <X className="h-4 w-4 mr-2" />
                Xóa bộ lọc
              </Button>
            )}

            {/* Desktop Filter Toggle */}
            <Button 
              variant="outline" 
              size="sm" 
              className="hidden lg:flex"
              onClick={() => setShowFilters(!showFilters)}
            >
              <Filter className="h-4 w-4 mr-2" />
              Bộ Lọc
              {hasActiveFilters && (
                <Badge variant="destructive" className="ml-2 h-5 w-5 rounded-full p-0 flex items-center justify-center">
                  {(selectedCategories.length + selectedBrands.length + (inStockOnly ? 1 : 0))}
                </Badge>
              )}
            </Button>
          </div>
        </div>

        {/* Active Filters Display */}
        {hasActiveFilters && (
          <div className="flex flex-wrap gap-2 mt-4">
            {selectedCategories.map(catId => {
              const category = categories.find(c => Number(c.id) === catId);
              return (
                <Badge key={catId} variant="secondary" className="gap-1">
                  {category?.name || ""}
                  <X 
                    className="h-3 w-3 cursor-pointer" 
                    onClick={() => handleCategoryToggle(catId)}
                  />
                </Badge>
              );
            })}
            {selectedBrands.map(brandId => {
              const brand = brands.find(b => b.id === brandId);
              return (
                <Badge key={brandId} variant="secondary" className="gap-1">
                  {brand?.name || ""}
                  <X 
                    className="h-3 w-3 cursor-pointer" 
                    onClick={() => handleBrandToggle(brandId)}
                  />
                </Badge>
              );
            })}
            {inStockOnly && (
              <Badge variant="secondary" className="gap-1">
                Còn hàng
                <X 
                  className="h-3 w-3 cursor-pointer" 
                  onClick={() => setInStockOnly(false)}
                />
              </Badge>
            )}
          </div>
        )}

        {/* Desktop Filters */}
        {showFilters && (
          <div className="hidden lg:block mt-6 pt-6 border-t border-border/50">
            <FilterContent 
              categories={categories}
              brands={brands}
              selectedCategories={selectedCategories}
              selectedBrands={selectedBrands}
              onCategoryToggle={handleCategoryToggle}
              onBrandToggle={handleBrandToggle}
              priceRange={priceRange}
              setPriceRange={setPriceRange}
              inStockOnly={inStockOnly}
              setInStockOnly={setInStockOnly}
              idPrefix="desktop"
              isHorizontal
            />
          </div>
        )}
      </div>
    </div>
  );
};
