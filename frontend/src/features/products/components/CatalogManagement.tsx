import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { CategoryManagement } from "@/features/categories/components/CategoryManagement";
import { BrandManagement } from "@/features/brands/components/BrandManagement";

interface CatalogManagementProps {
  className?: string;
}

export const CatalogManagement: React.FC<CatalogManagementProps> = ({
  className,
}) => {
  return (
    <div className={className}>
      <Tabs defaultValue="categories" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="categories">Danh mục</TabsTrigger>
          <TabsTrigger value="brands">Thương hiệu</TabsTrigger>
        </TabsList>

        <TabsContent value="categories" className="mt-6">
          <CategoryManagement />
        </TabsContent>

        <TabsContent value="brands" className="mt-6">
          <BrandManagement />
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default CatalogManagement;
