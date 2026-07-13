import { useParams, useNavigate, useLocation } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useState, useEffect } from "react";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { 
  ShoppingCart,
  Package,
  Truck,
  Shield,
  ChevronLeft,
  ChevronRight,
  Loader2
} from "lucide-react";
import { ProductsApi, ProductImagesApi } from "@/features/products/api";
import { useCart } from "@/context/cart-context";
import { useToast } from "@/hooks/use-toast";

export const ProductDetailPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { addToCart } = useCart();
  const { toast } = useToast();
  const [quantity, setQuantity] = useState(1);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, [slug]);

  // Fetch product by slug
  const { data: productData, isLoading: productLoading } = useQuery({
    queryKey: ["product", slug],
    queryFn: async () => {
      const response = await ProductsApi.getAll({ page: 0, size: 100 });
      return response.result?.content?.find(p => p.slug === slug);
    },
    enabled: !!slug,
  });

  const product = productData;

  // Fetch product images
  const { data: imagesData, isLoading: imagesLoading } = useQuery({
    queryKey: ["productImages", product?.id],
    queryFn: () => ProductImagesApi.getByProductId(product!.id),
    enabled: !!product?.id,
  });

  const images = imagesData?.result || [];
  const sortedImages = images.length > 0
    ? [...images].sort((a, b) => Number(b.isPrimary) - Number(a.isPrimary))
    : [];
  const displayImages = sortedImages.length > 0
    ? sortedImages
    : [{ imageUrl: "https://placehold.co/800x800?text=No+Image", isPrimary: true }];

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(price);
  };

  const handleAddToCart = async () => {
    if (!product) return;
    try {
      await addToCart(product.id, quantity);
      toast({
        title: "Thành công",
        description: `Đã thêm ${quantity} sản phẩm vào giỏ hàng`,
      });
    } catch (error) {
      toast({
        title: "Lỗi",
        description: "Không thể thêm sản phẩm vào giỏ hàng",
        variant: "destructive",
      });
    }
  };

  const handleBuyNow = async () => {
    await handleAddToCart();
    navigate("/cart");
  };

  const nextImage = () => {
    setSelectedImageIndex((prev) => (prev + 1) % displayImages.length);
  };

  const prevImage = () => {
    setSelectedImageIndex((prev) => (prev - 1 + displayImages.length) % displayImages.length);
  };

  const handleBack = () => {
    const state = location.state as
      | { returnTo?: { productId?: number; categoryName?: string } }
      | undefined;
    const returnTo = state?.returnTo;

    if (returnTo?.productId) {
      navigate("/", {
        state: {
          scrollToProductId: returnTo.productId,
          scrollToCategory: returnTo.categoryName,
        },
      });
      return;
    }

    navigate("/");
  };

  if (productLoading || imagesLoading) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="pt-20 flex items-center justify-center min-h-[60vh]">
          <Loader2 className="h-12 w-12 animate-spin text-primary" />
        </div>
        <Footer />
      </div>
    );
  }

  if (!product) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="pt-20 container mx-auto px-4 py-12">
          <div className="text-center">
            <h1 className="text-2xl font-bold mb-4">Không tìm thấy sản phẩm</h1>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="pt-20">
        <div className="container mx-auto px-4 py-8">
          <div className="mb-4">
            <Button variant="ghost" className="px-2" onClick={handleBack}>
              <ChevronLeft className="mr-1 h-4 w-4" />
              Quay lại
            </Button>
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-12">
            {/* Product Images */}
            <div className="space-y-4">
              {/* Main Image */}
              <div className="relative aspect-square w-full overflow-hidden rounded-lg bg-muted">
                <img
                  src={displayImages[selectedImageIndex]?.imageUrl}
                  alt={product.name}
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    const target = e.target as HTMLImageElement;
                    target.src = "https://placehold.co/800x800?text=No+Image";
                  }}
                />
                
                {displayImages.length > 1 && (
                  <>
                    <Button
                      variant="secondary"
                      size="icon"
                      className="absolute left-2 top-1/2 -translate-y-1/2 h-10 w-10 rounded-full bg-background/80 hover:bg-background"
                      onClick={prevImage}
                    >
                      <ChevronLeft className="h-5 w-5" />
                    </Button>
                    <Button
                      variant="secondary"
                      size="icon"
                      className="absolute right-2 top-1/2 -translate-y-1/2 h-10 w-10 rounded-full bg-background/80 hover:bg-background"
                      onClick={nextImage}
                    >
                      <ChevronRight className="h-5 w-5" />
                    </Button>
                  </>
                )}

                <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
                  {displayImages.map((_, idx) => (
                    <button
                      key={idx}
                      className={`h-2 rounded-full transition-all ${
                        idx === selectedImageIndex 
                          ? "w-8 bg-primary" 
                          : "w-2 bg-primary/30 hover:bg-primary/50"
                      }`}
                      onClick={() => setSelectedImageIndex(idx)}
                    />
                  ))}
                </div>
              </div>

              {/* Thumbnail Images */}
              {displayImages.length > 1 && (
                <div className="grid grid-cols-5 gap-2">
                  {displayImages.map((image, idx) => (
                    <button
                      key={idx}
                      className={`relative aspect-square overflow-hidden rounded-lg border-2 transition-all ${
                        idx === selectedImageIndex
                          ? "border-primary"
                          : "border-border hover:border-primary/50"
                      }`}
                      onClick={() => setSelectedImageIndex(idx)}
                    >
                      <img
                        src={image.imageUrl}
                        alt={`${product.name} ${idx + 1}`}
                        className="w-full h-full object-cover"
                      />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Product Info */}
            <div className="space-y-6">
              <div>
                <div className="flex items-center gap-2 mb-3">
                  <Badge variant="outline">{product.categoryName}</Badge>
                  <Badge variant="outline">{product.brandName}</Badge>
                  {product.stockQuantity > 0 ? (
                    <Badge className="bg-green-500">Còn hàng</Badge>
                  ) : (
                    <Badge variant="destructive">Hết hàng</Badge>
                  )}
                </div>
                
                <h1 className="text-3xl lg:text-4xl font-bold text-foreground mb-4">
                  {product.name}
                </h1>

                <div className="flex items-baseline gap-4">
                  <span className="text-3xl lg:text-4xl font-bold text-primary">
                    {formatPrice(product.unitPrice)}
                  </span>
                </div>
              </div>

              <Separator />

              {/* Description */}
              {product.description && (
                <div>
                  <h3 className="text-lg font-semibold mb-2">Mô tả sản phẩm</h3>
                  <p className="text-muted-foreground leading-relaxed">
                    {product.description}
                  </p>
                </div>
              )}

              <Separator />

              {/* Quantity */}
              <div className="space-y-4">
                <div>
                  <label className="text-sm font-medium mb-2 block">Số lượng</label>
                  <div className="flex items-center gap-3">
                    <Button
                      variant="outline"
                      size="icon"
                      onClick={() => setQuantity(Math.max(1, quantity - 1))}
                      disabled={quantity <= 1}
                    >
                      -
                    </Button>
                    <span className="text-lg font-semibold w-12 text-center">
                      {quantity}
                    </span>
                    <Button
                      variant="outline"
                      size="icon"
                      onClick={() => setQuantity(Math.min(product.stockQuantity, quantity + 1))}
                      disabled={quantity >= product.stockQuantity}
                    >
                      +
                    </Button>
                    <span className="text-sm text-muted-foreground ml-2">
                      {product.stockQuantity} sản phẩm có sẵn
                    </span>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="space-y-3">
                  <Button
                    size="lg"
                    variant="luxury"
                    className="w-full"
                    onClick={handleBuyNow}
                    disabled={product.stockQuantity === 0}
                  >
                    Mua ngay
                  </Button>
                  <Button
                    size="lg"
                    variant="outline"
                    className="w-full border-red-500 text-red-600 hover:bg-red-50"
                    onClick={handleAddToCart}
                    disabled={product.stockQuantity === 0}
                  >
                    <ShoppingCart className="mr-2 h-5 w-5" />
                    Thêm vào giỏ
                  </Button>
                </div>
              </div>

              <Separator />

              {/* Features */}
              <div className="grid grid-cols-1 gap-4">
                <Card>
                  <CardContent className="flex items-center gap-3 p-4">
                    <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                      <Package className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium">Hàng chính hãng</p>
                      <p className="text-sm text-muted-foreground">100% chính hãng từ thương hiệu</p>
                    </div>
                  </CardContent>
                </Card>
                
                <Card>
                  <CardContent className="flex items-center gap-3 p-4">
                    <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                      <Truck className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium">Miễn phí vận chuyển</p>
                      <p className="text-sm text-muted-foreground">Cho đơn hàng trên 500.000đ</p>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="flex items-center gap-3 p-4">
                    <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                      <Shield className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium">Bảo hành 12 tháng</p>
                      <p className="text-sm text-muted-foreground">Bảo hành chính hãng toàn quốc</p>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          </div>
        </div>
      </div>
      <Footer />
    </div>
  );
};

export default ProductDetailPage;
