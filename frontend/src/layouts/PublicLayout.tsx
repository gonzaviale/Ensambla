import { Outlet } from '@tanstack/react-router';
import { PublicHeader } from '@/components/Navbar/PublicHeader';
import { SaltarAlContenido } from '@/components/Navbar/SaltarAlContenido';

export function PublicLayout() {
  return (
    <>
      <SaltarAlContenido />
      <PublicHeader />
      <main id="contenido" tabIndex={-1} className="min-h-[calc(100dvh-4rem)] focus:outline-none">
        <Outlet />
      </main>
    </>
  );
}
