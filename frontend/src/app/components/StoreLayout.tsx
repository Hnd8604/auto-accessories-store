import type { ReactNode } from "react";
import { Header } from "./Header";
import { Footer } from "./Footer";
import { ChatWidget } from "@/features/inbox/components/ChatWidget";

interface StoreLayoutProps {
  children: ReactNode;
  showChat?: boolean;
}

export function StoreLayout({ children, showChat = false }: StoreLayoutProps) {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      {children}
      <Footer />
      {showChat && <ChatWidget />}
    </div>
  );
}
