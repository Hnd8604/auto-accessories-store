import { Link } from "react-router-dom";
import { Eye, ShoppingCart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useCart } from "@/features/cart/hooks/useCart";
import { formatPrice } from "@/utils/formatPrice";
import type { ProductResponse } from "../types";

const FALLBACK_IMAGE = "https://placehold.co/600x600?text=No+Image";

export function ProductCard({ product }: { product: ProductResponse }) {
  const { addToCart } = useCart();
  const detailPath = `/products/${product.slug}`;
  const returnTo = {
    productId: product.id,
    categoryName: product.categoryName,
  };

  return (
    <Card className="group bg-card border-border/50 hover:border-primary/50 transition-all duration-300 hover:shadow-lg hover:-translate-y-1 overflow-hidden">
      <div className="relative">
        <Link to={detailPath} state={{ returnTo }} aria-label={`Xem ${product.name}`}>
          <div className="aspect-square w-full overflow-hidden bg-muted">
            <img
              src={product.primaryImageUrl || product.images?.find((image) => image.isPrimary)?.imageUrl || FALLBACK_IMAGE}
              alt={product.name}
              className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
              onError={(event) => {
                if (event.currentTarget.src !== FALLBACK_IMAGE) {
                  event.currentTarget.src = FALLBACK_IMAGE;
                }
              }}
            />
          </div>
        </Link>
        <Button asChild variant="secondary" size="icon"
          className="absolute top-1.5 right-1.5 bg-background/90 hover:bg-background h-7 w-7">
          <Link to={detailPath} state={{ returnTo }} aria-label={`Xem chi tiết ${product.name}`}>
            <Eye className="h-3 w-3" />
          </Link>
        </Button>
      </div>

      <CardHeader className="p-3 pb-2">
        <CardTitle className="text-sm font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-2 leading-tight">
          <Link to={detailPath} state={{ returnTo }}>{product.name}</Link>
        </CardTitle>
      </CardHeader>

      <CardContent className="p-3 pt-0 space-y-2">
        <div className="flex items-baseline justify-between">
          <span className="text-lg font-bold text-primary">{formatPrice(product.unitPrice)}</span>
          <span className="text-[10px] text-muted-foreground">Còn: {product.stockQuantity}</span>
        </div>
        <Button onClick={() => { void addToCart(product.id, 1); }}
          className="w-full h-8 text-xs" disabled={product.stockQuantity === 0}>
          <ShoppingCart className="h-3 w-3 mr-0.5" />
          Thêm Vào Giỏ
        </Button>
      </CardContent>
    </Card>
  );
}
