import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ImageIcon, ArrowRight, CheckCircle2 } from "lucide-react";
import { ServicesApi } from "@/features/services/api/services";

export const Services = () => {
  const navigate = useNavigate();

  const { data, isLoading } = useQuery({
    queryKey: ["services"],
    queryFn: () => ServicesApi.getAll(),
    staleTime: 1000 * 60 * 5,
  });

  const services = data?.result || [];

  return (
    <section id="services" className="py-20 bg-gradient-to-b from-background to-muted/20">
      <div className="container mx-auto px-4">
        {/* Header */}
        <div className="text-center mb-12">
          <h2 className="text-4xl md:text-5xl font-bold text-foreground mb-4">
            Dịch Vụ
            <span className="bg-gradient-accent bg-clip-text text-transparent"> Chuyên Nghiệp</span>
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Chúng tôi cung cấp đầy đủ các dịch vụ nội thất ô tô với chất lượng tốt nhất,
            đội ngũ kỹ thuật viên dày dặn kinh nghiệm.
          </p>
        </div>

        {/* Grid */}
        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="space-y-4 p-5 border border-border/50 rounded-xl">
                <Skeleton className="h-6 w-3/4" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-5/6" />
                <Skeleton className="h-4 w-4/6" />
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-10 w-full mt-2" />
              </div>
            ))}
          </div>
        ) : services.length === 0 ? (
          <div className="text-center py-16 text-muted-foreground">
            <ImageIcon className="h-16 w-16 mx-auto mb-4 opacity-30" />
            <p className="text-lg">Chưa có dịch vụ nào được cập nhật.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {services.map((service) => (
              <div
                key={service.id}
                className="group bg-gradient-card border border-border/50 hover:border-primary/50 rounded-xl overflow-hidden transition-all duration-300 hover:shadow-card hover:-translate-y-1 flex flex-col"
              >
                {/* Content */}
                <div className="p-5 flex flex-col flex-1">
                  <div className="mb-3">
                    <h3 className="text-lg font-bold text-foreground leading-snug">{service.name}</h3>
                  </div>

                  {service.shortDescription && (
                    <p className="text-sm text-muted-foreground leading-relaxed mb-3 line-clamp-2">
                      {service.shortDescription}
                    </p>
                  )}

                  {/* Highlights */}
                  {service.features && service.features.length > 0 && (
                    <ul className="space-y-1.5 mb-4">
                      {service.features.slice(0, 4).map((feat, i) => (
                        <li key={i} className="flex items-start gap-2 text-sm text-muted-foreground">
                          <CheckCircle2 className="h-4 w-4 text-primary flex-shrink-0 mt-0.5" />
                          <span>{feat}</span>
                        </li>
                      ))}
                    </ul>
                  )}

                  <div className="mt-auto">
                    <Button
                      className="w-full group-hover:bg-primary group-hover:text-primary-foreground transition-colors"
                      variant="outline"
                      onClick={() => navigate(`/services/${service.slug}`)}
                    >
                      Xem Chi Tiết
                      <ArrowRight className="h-4 w-4 ml-2 transition-transform group-hover:translate-x-1" />
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
};
