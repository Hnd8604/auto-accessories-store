import { StoreLayout } from "@/app/components/StoreLayout";
import { MyOrders } from "@/features/orders/components/MyOrders";

const MyOrdersPage = () => {
  return (
    <StoreLayout>
      <div className="pt-20">
        <MyOrders />
      </div>
    </StoreLayout>
  );
};

export default MyOrdersPage;
