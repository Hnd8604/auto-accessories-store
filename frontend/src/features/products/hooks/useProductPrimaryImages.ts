import { useQueries } from "@tanstack/react-query";
import { ProductImagesApi } from "../api/productImages";

export function useProductPrimaryImages(productIds: number[]) {
  const queries = useQueries({
    queries: productIds.map((productId) => ({
      queryKey: ["productImages", productId],
      queryFn: () => ProductImagesApi.getByProductId(productId),
      staleTime: 5 * 60 * 1000,
    })),
  });

  const imageUrls: Record<number, string> = {};
  queries.forEach((query, index) => {
    const images = query.data?.result;
    const imageUrl = images?.find((image) => image.isPrimary)?.imageUrl ?? images?.[0]?.imageUrl;
    if (imageUrl) imageUrls[productIds[index]] = imageUrl;
  });

  return {
    imageUrls,
    isFetching: queries.some((query) => query.isFetching),
  };
}
