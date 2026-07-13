import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { OrderForm } from "@/features/orders/components/OrderForm";

const OrderPage = () => {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="pt-20">
        <OrderForm />
      </div>
      <Footer />
    </div>
  );
};

export default OrderPage;