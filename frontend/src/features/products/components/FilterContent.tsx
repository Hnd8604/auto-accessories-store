import { Checkbox } from "@/components/ui/checkbox";

interface FilterContentProps {
  categories: Array<{ id: string; name: string }>;
  brands: Array<{ id: number; name: string }>;
  selectedCategories: number[];
  selectedBrands: number[];
  onCategoryToggle: (categoryId: number) => void;
  onBrandToggle: (brandId: number) => void;
  priceRange: number[];
  setPriceRange: (range: number[]) => void;
  inStockOnly: boolean;
  setInStockOnly: (value: boolean) => void;
  idPrefix: string;
  isHorizontal?: boolean;
}

export const FilterContent = ({ 
  categories, 
  brands, 
  selectedCategories,
  selectedBrands,
  onCategoryToggle,
  onBrandToggle,
  priceRange, 
  setPriceRange, 
  inStockOnly,
  setInStockOnly,
  idPrefix,
  isHorizontal = false 
}: FilterContentProps) => {
  const containerClass = isHorizontal 
    ? "grid grid-cols-1 md:grid-cols-3 gap-8" 
    : "space-y-8";

  return (
    <div className={containerClass}>
      {/* Categories */}
      <div>
        <h3 className="font-semibold text-foreground mb-4">Danh Mục</h3>
        <div className="space-y-3 max-h-64 overflow-y-auto">
          {categories.map((category) => (
            <div key={category.id} className="flex items-center space-x-2">
              <Checkbox 
                id={`${idPrefix}-category-${category.id}`}
                checked={selectedCategories.includes(Number(category.id))}
                onCheckedChange={() => onCategoryToggle(Number(category.id))}
              />
              <label 
                htmlFor={`${idPrefix}-category-${category.id}`} 
                className="text-sm text-muted-foreground hover:text-foreground cursor-pointer"
              >
                {category.name}
              </label>
            </div>
          ))}
        </div>
      </div>

      {/* Price Range */}
      <div>
        <h3 className="font-semibold text-foreground mb-4">Khoảng Giá</h3>
        <div className="space-y-2">
          {[
            { label: "Dưới 5 triệu", min: 0, max: 5000000 },
            { label: "5 - 10 triệu", min: 5000000, max: 10000000 },
            { label: "10 - 20 triệu", min: 10000000, max: 20000000 },
            { label: "20 - 30 triệu", min: 20000000, max: 30000000 },
            { label: "30 - 50 triệu", min: 30000000, max: 50000000 },
            { label: "Trên 50 triệu", min: 50000000, max: 100000000 }
          ].map((range) => (
            <div key={range.label} className="flex items-center space-x-2">
              <Checkbox 
                id={`${idPrefix}-${range.min}-${range.max}`}
                checked={priceRange[0] === range.min && priceRange[1] === range.max}
                onCheckedChange={(checked) => {
                  setPriceRange(checked ? [range.min, range.max] : [0, 50000000]);
                }}
              />
              <label 
                htmlFor={`${idPrefix}-${range.min}-${range.max}`}
                className="text-sm text-muted-foreground hover:text-foreground cursor-pointer"
              >
                {range.label}
              </label>
            </div>
          ))}
        </div>
      </div>

      {/* Brands */}
      <div>
        <h3 className="font-semibold text-foreground mb-4">Thương Hiệu Xe</h3>
        <div className="space-y-3 max-h-64 overflow-y-auto">
          {brands.map((brand) => (
            <div key={brand.id} className="flex items-center space-x-2">
              <Checkbox 
                id={`${idPrefix}-brand-${brand.id}`}
                checked={selectedBrands.includes(brand.id)}
                onCheckedChange={() => onBrandToggle(brand.id)}
              />
              <label 
                htmlFor={`${idPrefix}-brand-${brand.id}`} 
                className="text-sm text-muted-foreground hover:text-foreground cursor-pointer"
              >
                {brand.name}
              </label>
            </div>
          ))}
        </div>
        
        {/* In Stock Filter */}
        <div className="mt-4 pt-4 border-t border-border/50">
          <div className="flex items-center space-x-2">
            <Checkbox 
              id={`${idPrefix}-in-stock`}
              checked={inStockOnly}
              onCheckedChange={setInStockOnly}
            />
            <label 
              htmlFor={`${idPrefix}-in-stock`} 
              className="text-sm text-muted-foreground hover:text-foreground cursor-pointer"
            >
              Chỉ hiển thị sản phẩm còn hàng
            </label>
          </div>
        </div>
      </div>
    </div>
  );
};
