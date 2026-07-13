import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { useForm } from "react-hook-form";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { useToast } from "@/hooks/use-toast";
import { CalendarDays, Clock, Car, Phone, Mail, MapPin } from "lucide-react";

interface OrderFormData {
  customerName: string;
  phone: string;
  email: string;
  address: string;
  carModel: string;
  carYear: string;
  service: string;
  appointmentDate: string;
  appointmentTime: string;
  notes: string;
}

const services = [
  { id: "boc-ghe-da", name: "Bọc Ghế Da Cao Cấp", price: 15000000 },
  { id: "op-taplo", name: "Ốp Táp-lô Carbon", price: 8000000 },
  { id: "tham-san", name: "Thảm Sàn 5D/6D", price: 3500000 },
  { id: "bao-ve-noi-that", name: "Bảo Vệ Nội Thất", price: 12000000 },
  { id: "am-thanh", name: "Âm Thanh Cao Cấp", price: 25000000 },
  { id: "goi-vip", name: "Gói VIP Toàn Diện", price: 50000000 },
];

const timeSlots = [
  "08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00", "17:00"
];

export const OrderForm = () => {
  const [selectedService, setSelectedService] = useState<string>("");
  const { toast } = useToast();
  
  const form = useForm<OrderFormData>({
    defaultValues: {
      customerName: "",
      phone: "",
      email: "",
      address: "",
      carModel: "",
      carYear: "",
      service: "",
      appointmentDate: "",
      appointmentTime: "",
      notes: "",
    },
  });

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(price);
  };

  const selectedServiceData = services.find(s => s.id === selectedService);

  const onSubmit = (data: OrderFormData) => {
    console.log("Order data:", data);
    toast({
      title: "Đặt hàng thành công!",
      description: "Chúng tôi sẽ liên hệ với bạn trong thời gian sớm nhất.",
    });
    form.reset();
    setSelectedService("");
  };

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-8">
      <div className="text-center space-y-4">
        <h1 className="text-4xl font-bold bg-gradient-to-r from-automotive-red to-automotive-gold bg-clip-text text-transparent">
          Đặt Hàng Dịch Vụ
        </h1>
        <p className="text-muted-foreground text-lg">
          Điền thông tin để đặt lịch tư vấn và báo giá chi tiết
        </p>
      </div>

      <div className="grid lg:grid-cols-2 gap-8">
        {/* Form Section */}
        <Card className="border-automotive-silver/20 bg-gradient-to-br from-background to-muted/30">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Car className="h-5 w-5 text-automotive-red" />
              Thông Tin Đặt Hàng
            </CardTitle>
            <CardDescription>
              Vui lòng điền đầy đủ thông tin để chúng tôi hỗ trợ bạn tốt nhất
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                {/* Customer Information */}
                <div className="space-y-4">
                  <h3 className="font-semibold text-foreground flex items-center gap-2">
                    <Phone className="h-4 w-4" />
                    Thông Tin Khách Hàng
                  </h3>
                  
                  <div className="grid sm:grid-cols-2 gap-4">
                    <FormField
                      control={form.control}
                      name="customerName"
                      rules={{ required: "Vui lòng nhập họ tên" }}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Họ và Tên</FormLabel>
                          <FormControl>
                            <Input placeholder="Nguyễn Văn A" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    
                    <FormField
                      control={form.control}
                      name="phone"
                      rules={{ 
                        required: "Vui lòng nhập số điện thoại",
                        pattern: {
                          value: /^[0-9+\-\s()]+$/,
                          message: "Số điện thoại không hợp lệ"
                        }
                      }}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Số Điện Thoại</FormLabel>
                          <FormControl>
                            <Input placeholder="0901234567" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <FormField
                    control={form.control}
                    name="email"
                    rules={{ 
                      required: "Vui lòng nhập email",
                      pattern: {
                        value: /\S+@\S+\.\S+/,
                        message: "Email không hợp lệ"
                      }
                    }}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Email</FormLabel>
                        <FormControl>
                          <Input placeholder="example@email.com" type="email" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="address"
                    rules={{ required: "Vui lòng nhập địa chỉ" }}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel className="flex items-center gap-2">
                          <MapPin className="h-4 w-4" />
                          Địa Chỉ
                        </FormLabel>
                        <FormControl>
                          <Input placeholder="Số nhà, đường, quận, thành phố" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                <Separator />

                {/* Car Information */}
                <div className="space-y-4">
                  <h3 className="font-semibold text-foreground flex items-center gap-2">
                    <Car className="h-4 w-4" />
                    Thông Tin Xe
                  </h3>
                  
                  <div className="grid sm:grid-cols-2 gap-4">
                    <FormField
                      control={form.control}
                      name="carModel"
                      rules={{ required: "Vui lòng nhập dòng xe" }}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Dòng Xe</FormLabel>
                          <FormControl>
                            <Input placeholder="Toyota Camry, BMW 320i..." {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    
                    <FormField
                      control={form.control}
                      name="carYear"
                      rules={{ required: "Vui lòng chọn năm sản xuất" }}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Năm Sản Xuất</FormLabel>
                          <Select onValueChange={field.onChange} defaultValue={field.value}>
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue placeholder="Chọn năm" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              {Array.from({ length: 15 }, (_, i) => 2024 - i).map((year) => (
                                <SelectItem key={year} value={year.toString()}>
                                  {year}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                </div>

                <Separator />

                {/* Service Selection */}
                <div className="space-y-4">
                  <h3 className="font-semibold text-foreground">Dịch Vụ</h3>
                  
                  <FormField
                    control={form.control}
                    name="service"
                    rules={{ required: "Vui lòng chọn dịch vụ" }}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Chọn Dịch Vụ</FormLabel>
                        <Select 
                          onValueChange={(value) => {
                            field.onChange(value);
                            setSelectedService(value);
                          }} 
                          defaultValue={field.value}
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Chọn dịch vụ cần thực hiện" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {services.map((service) => (
                              <SelectItem key={service.id} value={service.id}>
                                {service.name} - {formatPrice(service.price)}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                <Separator />

                {/* Appointment */}
                <div className="space-y-4">
                  <h3 className="font-semibold text-foreground flex items-center gap-2">
                    <CalendarDays className="h-4 w-4" />
                    Đặt Lịch Hẹn
                  </h3>
                  
                  <div className="grid sm:grid-cols-2 gap-4">
                    <FormField
                      control={form.control}
                      name="appointmentDate"
                      rules={{ required: "Vui lòng chọn ngày hẹn" }}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Ngày Hẹn</FormLabel>
                          <FormControl>
                            <Input type="date" {...field} min={new Date().toISOString().split('T')[0]} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    
                    <FormField
                      control={form.control}
                      name="appointmentTime"
                      rules={{ required: "Vui lòng chọn giờ hẹn" }}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="flex items-center gap-2">
                            <Clock className="h-4 w-4" />
                            Giờ Hẹn
                          </FormLabel>
                          <Select onValueChange={field.onChange} defaultValue={field.value}>
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue placeholder="Chọn giờ" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              {timeSlots.map((time) => (
                                <SelectItem key={time} value={time}>
                                  {time}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                </div>

                {/* Notes */}
                <FormField
                  control={form.control}
                  name="notes"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Ghi Chú (Tùy chọn)</FormLabel>
                      <FormControl>
                        <Textarea 
                          placeholder="Mô tả chi tiết yêu cầu của bạn..."
                          className="resize-none"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <Button type="submit" variant="luxury" className="w-full" size="lg">
                  Đặt Hàng Ngay
                </Button>
              </form>
            </Form>
          </CardContent>
        </Card>

        {/* Order Summary */}
        <div className="space-y-6">
          <Card className="border-automotive-red/20 bg-gradient-to-br from-automotive-red/5 to-transparent">
            <CardHeader>
              <CardTitle className="text-automotive-red">Tóm Tắt Đơn Hàng</CardTitle>
              <CardDescription>
                Chi tiết dịch vụ bạn đã chọn
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {selectedServiceData ? (
                <>
                  <div className="flex justify-between items-start">
                    <div>
                      <h4 className="font-medium">{selectedServiceData.name}</h4>
                      <Badge variant="outline" className="mt-1">
                        Dịch vụ cao cấp
                      </Badge>
                    </div>
                    <div className="text-right">
                      <div className="font-bold text-lg text-automotive-red">
                        {formatPrice(selectedServiceData.price)}
                      </div>
                    </div>
                  </div>
                  
                  <Separator />
                  
                  <div className="space-y-2 text-sm text-muted-foreground">
                    <div className="flex items-center gap-2">
                      ✓ Tư vấn miễn phí tại showroom
                    </div>
                    <div className="flex items-center gap-2">
                      ✓ Bảo hành 2 năm
                    </div>
                    <div className="flex items-center gap-2">
                      ✓ Lắp đặt chuyên nghiệp
                    </div>
                    <div className="flex items-center gap-2">
                      ✓ Hỗ trợ 24/7
                    </div>
                  </div>
                </>
              ) : (
                <p className="text-muted-foreground text-center py-8">
                  Vui lòng chọn dịch vụ để xem chi tiết đơn hàng
                </p>
              )}
            </CardContent>
          </Card>

          {/* Contact Info */}
          <Card className="border-automotive-gold/20">
            <CardHeader>
              <CardTitle className="text-automotive-gold">Liên Hệ Hỗ Trợ</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-center gap-3">
                <Phone className="h-4 w-4 text-automotive-red" />
                <span>Hotline: 1900 1234</span>
              </div>
              <div className="flex items-center gap-3">
                <Mail className="h-4 w-4 text-automotive-red" />
                <span>Email: support@autointerior.vn</span>
              </div>
              <div className="flex items-center gap-3">
                <MapPin className="h-4 w-4 text-automotive-red" />
                <span>123 Đường ABC, Quận 1, TP.HCM</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};