import { useCallback, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { StoreLayout } from "@/app/components/StoreLayout";
import { ProductFilters } from "../components/ProductFilters";
import { Products } from "../components/Products";
import type { ProductSearchRequest } from "../types";

export default function ProductCatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get("search")?.trim() ?? "";
  const [filters, setFilters] = useState<ProductSearchRequest>(() =>
    keyword ? { keyword } : {}
  );
  const [sortBy, setSortBy] = useState("featured");

  const handleSearch = useCallback((next: ProductSearchRequest) => {
    setFilters(next);
    const nextKeyword = next.keyword ?? "";
    const currentKeyword = searchParams.get("search") ?? "";
    if (nextKeyword !== currentKeyword) {
      setSearchParams(nextKeyword ? { search: nextKeyword } : {}, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  return (
    <StoreLayout>
      <main className="pt-20">
        <ProductFilters
          initialKeyword={keyword}
          onSearch={handleSearch}
          onSort={setSortBy}
        />
        <Products
          layout="grid"
          showHeader={false}
          showViewAll={false}
          enablePagination
          pageSize={24}
          searchParams={filters}
          sortBy={sortBy}
        />
      </main>
    </StoreLayout>
  );
}
