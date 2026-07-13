import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useState, useEffect, useCallback } from "react";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
} from "@/components/ui/dialog";
import {
  ChevronLeft,
  ChevronRight,
  X,
  Phone,
  ArrowLeft,
  Loader2,
} from "lucide-react";
import useEmblaCarousel from "embla-carousel-react";
import { ServicesApi } from "@/features/services/api/services";
import { ServiceImagesApi } from "@/features/services/api/serviceImages";
import type { ServiceImageResponse } from "../types";

const ServiceDetailPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [lightboxIndex, setLightboxIndex] = useState(0);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, [slug]);

  const { data: serviceData, isLoading: serviceLoading } = useQuery({
    queryKey: ["service", slug],
    queryFn: () => ServicesApi.getBySlug(slug!),
    enabled: !!slug,
  });

  const service = serviceData?.result;

  const { data: imagesData } = useQuery({
    queryKey: ["serviceImages", service?.id],
    queryFn: () => ServiceImagesApi.getByServiceId(service!.id),
    enabled: !!service?.id,
  });

  const rawImages: ServiceImageResponse[] = imagesData?.result || [];
  const displayImages =
    rawImages.length > 0
      ? [...rawImages].sort((a, b) => Number(b.isPrimary) - Number(a.isPrimary))
      : [];

  const [emblaRef, emblaApi] = useEmblaCarousel({ loop: true, align: "center" });
  const [selectedIndex, setSelectedIndex] = useState(0);

  const onSelect = useCallback(() => {
    if (!emblaApi) return;
    setSelectedIndex(emblaApi.selectedScrollSnap());
  }, [emblaApi]);

  useEffect(() => {
    if (!emblaApi) return;
    emblaApi.on("select", onSelect);
    onSelect();
    return () => { emblaApi.off("select", onSelect); };
  }, [emblaApi, onSelect]);

  const scrollPrev = useCallback(() => emblaApi?.scrollPrev(), [emblaApi]);
  const scrollNext = useCallback(() => emblaApi?.scrollNext(), [emblaApi]);

  const openLightbox = (index: number) => {
    setLightboxIndex(index);
    setLightboxOpen(true);
  };

  const lightboxPrev = () => setLightboxIndex((i) => (i - 1 + displayImages.length) % displayImages.length);
  const lightboxNext = () => setLightboxIndex((i) => (i + 1) % displayImages.length);

  const handleContactClick = () => {
    navigate("/");
    setTimeout(() => {
      document.getElementById("contact")?.scrollIntoView({ behavior: "smooth" });
    }, 100);
  };

  if (serviceLoading) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex items-center justify-center min-h-[60vh]">
          <Loader2 className="h-10 w-10 animate-spin text-primary" />
        </div>
        <Footer />
      </div>
    );
  }

  if (!service) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
          <p className="text-xl text-muted-foreground">Không tìm thấy dịch vụ.</p>
          <Button onClick={() => navigate(-1)}>
            <ArrowLeft className="h-4 w-4 mr-2" />Quay lại
          </Button>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <main className="container mx-auto px-4 py-10">
        {/* Back button */}
        <Button variant="ghost" onClick={() => navigate(-1)} className="mb-6 -ml-2">
          <ArrowLeft className="h-4 w-4 mr-2" />Quay lại
        </Button>

        {/* Service Info — full width */}
        <div className="space-y-6 mb-12">
          <div>
            <h1 className="text-3xl font-bold text-foreground mb-3">{service.name}</h1>
            {service.fullDescription ? (
              <p className="text-muted-foreground leading-relaxed whitespace-pre-wrap">
                {service.fullDescription}
              </p>
            ) : service.shortDescription ? (
              <p className="text-muted-foreground leading-relaxed">{service.shortDescription}</p>
            ) : null}
          </div>

          {/* CTA */}
          <div className="flex gap-3 max-w-md">
            <Button className="flex-1" size="lg" onClick={handleContactClick}>
              <Phone className="h-4 w-4 mr-2" />Liên Hệ Tư Vấn
            </Button>
            <Button variant="outline" className="flex-1" size="lg" onClick={() => navigate(-1)}>
              Xem Dịch Vụ Khác
            </Button>
          </div>
        </div>

        {/* Image Carousel — bottom, half width */}
        {displayImages.length > 0 && (
          <div className="space-y-4 max-w-[62.5%] mx-auto">
            <h2 className="text-xl font-semibold text-foreground">Hình ảnh dịch vụ</h2>

            {/* Embla Carousel */}
            <div className="relative overflow-hidden rounded-xl" ref={emblaRef}>
              <div className="flex">
                {displayImages.map((img, idx) => (
                  <div
                    key={img.id}
                    className="relative flex-[0_0_100%] aspect-video cursor-pointer"
                    onClick={() => openLightbox(idx)}
                  >
                    <img
                      src={img.imageUrl}
                      alt={img.altText || service.name}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        (e.target as HTMLImageElement).src =
                          "https://placehold.co/800x450?text=Service+Image";
                      }}
                    />
                    <div className="absolute inset-0 bg-black/10 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center">
                      <span className="text-white text-sm font-medium bg-black/50 px-3 py-1.5 rounded-full">
                        Xem ảnh lớn
                      </span>
                    </div>
                  </div>
                ))}
              </div>

              {/* Prev/Next buttons */}
              {displayImages.length > 1 && (
                <>
                  <button
                    onClick={scrollPrev}
                    className="absolute left-3 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white rounded-full p-2 transition-colors"
                  >
                    <ChevronLeft className="h-5 w-5" />
                  </button>
                  <button
                    onClick={scrollNext}
                    className="absolute right-3 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white rounded-full p-2 transition-colors"
                  >
                    <ChevronRight className="h-5 w-5" />
                  </button>
                </>
              )}
            </div>

            {/* Caption */}
            {displayImages[selectedIndex]?.altText && (
              <p className="text-center text-sm text-muted-foreground italic">
                {displayImages[selectedIndex].altText}
              </p>
            )}

            {/* Dot indicators */}
            {displayImages.length > 1 && (
              <div className="flex justify-center gap-2">
                {displayImages.map((_, idx) => (
                  <button
                    key={idx}
                    onClick={() => emblaApi?.scrollTo(idx)}
                    className={`w-2 h-2 rounded-full transition-all ${
                      idx === selectedIndex ? "bg-primary w-4" : "bg-muted-foreground/40"
                    }`}
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </main>

      <Footer />

      {/* Lightbox */}
      {displayImages.length > 0 && (
        <Dialog open={lightboxOpen} onOpenChange={setLightboxOpen}>
          <DialogContent className="max-w-[95vw] max-h-[95vh] p-0 bg-black/95 border-0">
            <div className="relative flex items-center justify-center w-full h-full min-h-[60vh]">
              <button
                onClick={() => setLightboxOpen(false)}
                className="absolute top-4 right-4 z-50 text-white hover:text-gray-300 bg-black/50 rounded-full p-1.5"
              >
                <X className="h-5 w-5" />
              </button>

              {displayImages.length > 1 && (
                <>
                  <button
                    onClick={lightboxPrev}
                    className="absolute left-4 top-1/2 -translate-y-1/2 z-50 text-white hover:text-gray-300 bg-black/50 rounded-full p-2"
                  >
                    <ChevronLeft className="h-6 w-6" />
                  </button>
                  <button
                    onClick={lightboxNext}
                    className="absolute right-4 top-1/2 -translate-y-1/2 z-50 text-white hover:text-gray-300 bg-black/50 rounded-full p-2"
                  >
                    <ChevronRight className="h-6 w-6" />
                  </button>
                </>
              )}

              <div className="p-8 text-center">
                <img
                  src={displayImages[lightboxIndex]?.imageUrl}
                  alt={displayImages[lightboxIndex]?.altText || service.name}
                  className="max-w-full max-h-[75vh] object-contain rounded-lg mx-auto"
                />
                {displayImages[lightboxIndex]?.altText && (
                  <p className="text-white/80 text-sm mt-3">
                    {displayImages[lightboxIndex].altText}
                  </p>
                )}
                <p className="text-white/50 text-xs mt-1">
                  {lightboxIndex + 1} / {displayImages.length}
                </p>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
};

export default ServiceDetailPage;
