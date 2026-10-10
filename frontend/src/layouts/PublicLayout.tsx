import { Outlet } from '@tanstack/react-router';
import { PublicHeader } from '@/components/Navbar/PublicHeader';

export function PublicLayout() {
  return (
    <>
      <PublicHeader />
      <main className="min-h-[calc(100vh-4rem)]">
        <Outlet />
      </main>
    </>
  );
}
