import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Phone, Mail, MapPin, Clock, MessageCircle, Star } from "lucide-react";

const COMPANY_INFO = {
  hotline: import.meta.env.VITE_COMPANY_HOTLINE,
  email: import.meta.env.VITE_COMPANY_EMAIL,
  address: import.meta.env.VITE_COMPANY_ADDRESS,
  hours: import.meta.env.VITE_COMPANY_HOURS,
};

export const Contact = () => {
  return (
    <section id="contact" className="py-20 bg-background">
      <div className="container mx-auto px-4">
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold text-foreground mb-6">
            Liên Hệ 
            <span className="bg-gradient-accent bg-clip-text text-transparent"> Tư Vấn</span>
          </h2>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto">
            Hãy để chúng tôi tư vấn và báo giá chi tiết cho dự án nội thất ô tô của bạn. 
            Đội ngũ chuyên gia sẵn sàng hỗ trợ 24/7.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
          {/* Contact Info */}
          <div className="space-y-8">
            <Card className="bg-gradient-card border-border/50 shadow-card">
              <CardHeader>
                <CardTitle className="text-2xl text-foreground">Thông Tin Liên Hệ</CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-primary/10 rounded-lg">
                    <Phone className="h-6 w-6 text-primary" />
                  </div>
                  <div>
                    <div className="font-semibold text-foreground">Hotline</div>
                    <div className="text-muted-foreground">{COMPANY_INFO.hotline}</div>
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  <div className="p-3 bg-primary/10 rounded-lg">
                    <Mail className="h-6 w-6 text-primary" />
                  </div>
                  <div>
                    <div className="font-semibold text-foreground">Email</div>
                    <div className="text-muted-foreground">{COMPANY_INFO.email}</div>
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  <div className="p-3 bg-primary/10 rounded-lg">
                    <MapPin className="h-6 w-6 text-primary" />
                  </div>
                  <div>
                    <div className="font-semibold text-foreground">Địa Chỉ</div>
                    <div className="text-muted-foreground">{COMPANY_INFO.address}</div>
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  <div className="p-3 bg-primary/10 rounded-lg">
                    <Clock className="h-6 w-6 text-primary" />
                  </div>
                  <div>
                    <div className="font-semibold text-foreground">Giờ Làm Việc</div>
                    <div className="text-muted-foreground">{COMPANY_INFO.hours}</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Quick Stats */}
            <div className="grid grid-cols-2 gap-4">
              <Card className="bg-gradient-card border-border/50 text-center p-6">
                <Star className="h-8 w-8 text-primary mx-auto mb-2" />
                <div className="text-2xl font-bold text-foreground">4.9/5</div>
                <div className="text-sm text-muted-foreground">Đánh Giá</div>
              </Card>
              <Card className="bg-gradient-card border-border/50 text-center p-6">
                <MessageCircle className="h-8 w-8 text-primary mx-auto mb-2" />
                <div className="text-2xl font-bold text-foreground">24/7</div>
                <div className="text-sm text-muted-foreground">Hỗ Trợ</div>
              </Card>
            </div>
          </div>

          {/* Contact Form */}
          <Card className="bg-gradient-card border-border/50 shadow-card">
            <CardHeader>
              <CardTitle className="text-2xl text-foreground">Gửi Yêu Cầu Tư Vấn</CardTitle>
            </CardHeader>
            <CardContent>
              <form className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-foreground mb-2 block">
                      Họ và Tên *
                    </label>
                    <Input 
                      placeholder="Nhập họ và tên" 
                      className="bg-background/50 border-border/50 focus:border-primary"
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium text-foreground mb-2 block">
                      Số Điện Thoại *
                    </label>
                    <Input 
                      placeholder="Nhập số điện thoại" 
                      className="bg-background/50 border-border/50 focus:border-primary"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium text-foreground mb-2 block">
                    Email
                  </label>
                  <Input 
                    type="email" 
                    placeholder="Nhập email" 
                    className="bg-background/50 border-border/50 focus:border-primary"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium text-foreground mb-2 block">
                    Dòng Xe
                  </label>
                  <Input 
                    placeholder="VD: Mercedes C200, BMW X5..." 
                    className="bg-background/50 border-border/50 focus:border-primary"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium text-foreground mb-2 block">
                    Mô Tả Yêu Cầu
                  </label>
                  <Textarea 
                    placeholder="Mô tả chi tiết về dịch vụ bạn quan tâm..."
                    rows={4}
                    className="bg-background/50 border-border/50 focus:border-primary"
                  />
                </div>

                <Button variant="luxury" size="lg" className="w-full">
                  Gửi Yêu Cầu Tư Vấn
                </Button>

                <p className="text-xs text-muted-foreground text-center">
                  * Chúng tôi cam kết không spam và bảo mật thông tin khách hàng
                </p>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
    </section>
  );
};