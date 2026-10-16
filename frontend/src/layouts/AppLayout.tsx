import { Outlet, useRouterState } from '@tanstack/react-router';
import { BottomNav } from '@/components/Navbar/BottomNav';
import { Breadcrumbs } from '@/components/Navbar/Breadcrumbs';
import { Navbar } from '@/components/Navbar/Navbar';
import { BannerPlan } from '@/components/Plan/BannerPlan';
import { OfertaRecorrido } from '@/components/Recorrido/OfertaRecorrido';
import { RecorridoGuiado } from '@/components/Recorrido/RecorridoGuiado';
import { ModoProyecto } from '@/components/Navbar/ModoProyecto';
import { SaltarAlContenido } from '@/components/Navbar/SaltarAlContenido';
import { useUsuarioOpcional } from '@/hooks/auth/queries';

export function AppLayout() {
  const usuario = useUsuarioOpcional();
  const enInicio = useRouterState({
    select: (estado) => estado.location.pathname.replace(/\/+$/, '') === '/inicio',
  });
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
        <BannerPlan />
        <Breadcrumbs />
        <ModoProyecto />
        {enInicio && <OfertaRecorrido />}
        <Outlet />
      </main>
      <BottomNav />
      <RecorridoGuiado />
    </>
  );
}
