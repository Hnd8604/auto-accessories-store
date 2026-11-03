import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export default function NotFoundPage() {
  return (
    <main className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="text-center space-y-4">
        <h1 className="text-5xl font-bold">404</h1>
        <p className="text-muted-foreground">Không tìm thấy trang bạn yêu cầu.</p>
        <Button asChild><Link to="/">Về trang chủ</Link></Button>
      </div>
    </main>
  );
}
