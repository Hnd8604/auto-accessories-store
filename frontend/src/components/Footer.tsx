import { Car, Phone, Mail, MapPin, Facebook, Instagram, Youtube } from "lucide-react";
import { Button } from "@/components/ui/button";

const COMPANY_INFO = {
  hotline: import.meta.env.VITE_COMPANY_HOTLINE,
  email: import.meta.env.VITE_COMPANY_EMAIL,
  address: import.meta.env.VITE_COMPANY_ADDRESS,
};

export const Footer = () => {
  return (
    <footer className="bg-deep-black text-primary-foreground border-t border-border/30">
      <div className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {/* Company Info */}
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Car className="h-8 w-8 text-primary" />
              <span className="text-2xl font-bold bg-gradient-accent bg-clip-text text-transparent">
                AutoLux Interior
              </span>
            </div>
            <p className="text-primary-foreground/80">
              Chuyên gia hàng đầu về nội thất ô tô cao cấp tại Việt Nam. 
              Mang đến trải nghiệm lái xe đẳng cấp và sang trọng.
            </p>
            <div className="flex gap-4">
              <Button variant="ghost" size="icon" className="text-primary-foreground hover:bg-primary/20 hover:text-primary">
                <Facebook className="h-5 w-5" />
              </Button>
              <Button variant="ghost" size="icon" className="text-primary-foreground hover:bg-primary/20 hover:text-primary">
                <Instagram className="h-5 w-5" />
              </Button>
              <Button variant="ghost" size="icon" className="text-primary-foreground hover:bg-primary/20 hover:text-primary">
                <Youtube className="h-5 w-5" />
              </Button>
            </div>
          </div>

          {/* Quick Links */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-primary-foreground">Liên Kết Nhanh</h3>
            <ul className="space-y-2">
              <li><a href="#home" className="text-primary-foreground/80 hover:text-primary transition-colors">Trang Chủ</a></li>
              <li><a href="#services" className="text-primary-foreground/80 hover:text-primary transition-colors">Dịch Vụ</a></li>
              <li><a href="#contact" className="text-primary-foreground/80 hover:text-primary transition-colors">Liên Hệ</a></li>
              <li><a href="#" className="text-primary-foreground/80 hover:text-primary transition-colors">Về Chúng Tôi</a></li>
            </ul>
          </div>

          {/* Services */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-primary-foreground">Dịch Vụ</h3>
            <ul className="space-y-2">
              <li><a href="#" className="text-primary-foreground/80 hover:text-primary transition-colors">Bọc Ghế Da</a></li>
              <li><a href="#" className="text-primary-foreground/80 hover:text-primary transition-colors">Độ Taplo Carbon</a></li>
              <li><a href="#" className="text-primary-foreground/80 hover:text-primary transition-colors">Đèn LED Ambient</a></li>
              <li><a href="#" className="text-primary-foreground/80 hover:text-primary transition-colors">Độ Vô Lăng</a></li>
              <li><a href="#" className="text-primary-foreground/80 hover:text-primary transition-colors">Phủ Nano</a></li>
            </ul>
          </div>

          {/* Contact Info */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-primary-foreground">Liên Hệ</h3>
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <Phone className="h-4 w-4 text-primary" />
                <span className="text-primary-foreground/80">{COMPANY_INFO.hotline}</span>
              </div>
              <div className="flex items-center gap-3">
                <Mail className="h-4 w-4 text-primary" />
                <span className="text-primary-foreground/80">{COMPANY_INFO.email}</span>
              </div>
              <div className="flex items-start gap-3">
                <MapPin className="h-4 w-4 text-primary mt-1" />
                <span className="text-primary-foreground/80">{COMPANY_INFO.address}</span>
              </div>
            </div>
          </div>
        </div>

        <div className="border-t border-border/30 mt-12 pt-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4">
            <p className="text-primary-foreground/70 text-sm">
              © 2024 AutoLux Interior. Tất cả quyền được bảo lưu.
            </p>
            <div className="flex gap-6 text-sm">
              <a href="#" className="text-primary-foreground/80 hover:text-primary transition-colors">
                Chính Sách Bảo Mật
              </a>
              <a href="#" className="text-primary-foreground/80 hover:text-primary transition-colors">
                Điều Khoản Sử Dụng
              </a>
              <a href="#" className="text-primary-foreground/80 hover:text-primary transition-colors">
                Chính Sách Bảo Hành
              </a>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
};