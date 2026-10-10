import { Outlet } from '@tanstack/react-router';
import { Navbar } from '@/components/Navbar/Navbar';
import { useUsuarioOpcional } from '@/hooks/auth/queries';

export function AppLayout() {
  const usuario = useUsuarioOpcional();
  // Sesión cerrada: el guard de la ruta destino redirige en un instante; mientras tanto no se
  // renderiza nada privado.
  if (!usuario) return null;

  return (
    <>
      <Navbar />
      <main className="mx-auto min-h-[calc(100vh-4rem)] w-full max-w-300 px-4 pt-6 pb-16 md:px-8 md:pt-8">
        <Outlet />
      </main>
    </>
  );
}
