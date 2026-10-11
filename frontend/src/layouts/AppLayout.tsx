import { Outlet } from '@tanstack/react-router';
import { BottomNav } from '@/components/Navbar/BottomNav';
import { Breadcrumbs } from '@/components/Navbar/Breadcrumbs';
import { Navbar } from '@/components/Navbar/Navbar';
import { ModoProyecto } from '@/components/Navbar/ModoProyecto';
import { SaltarAlContenido } from '@/components/Navbar/SaltarAlContenido';
import { useUsuarioOpcional } from '@/hooks/auth/queries';

export function AppLayout() {
  const usuario = useUsuarioOpcional();
  // Sesión cerrada: el guard de la ruta destino redirige en un instante; mientras tanto no se
  // renderiza nada privado.
  if (!usuario) return null;

  return (
    <>
      <SaltarAlContenido />
      <Navbar />
      <main
        id="contenido"
        tabIndex={-1}
        className="mx-auto min-h-[calc(100dvh-4rem)] w-full max-w-300 px-4 pt-6 pb-[calc(6rem+env(safe-area-inset-bottom))] focus:outline-none md:px-8 md:pt-8 md:pb-16"
      >
        <Breadcrumbs />
        <ModoProyecto />
        <Outlet />
      </main>
      <BottomNav />
    </>
  );
}
