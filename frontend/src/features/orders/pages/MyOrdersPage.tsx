import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { MyOrders } from "@/features/orders/components/MyOrders";

const MyOrdersPage = () => {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="pt-20">
        <MyOrders />
      </div>
      <Footer />
    </div>
  );
};

export default MyOrdersPage;
