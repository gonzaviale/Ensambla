import { Outlet } from '@tanstack/react-router';
import { PublicHeader } from '@/components/Navbar/PublicHeader';
import { IconSprite } from '@/components/sistema/IconSprite';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import { SistemaLayout } from './SistemaLayout';
import '@/styles/sistema/index.css';

export function PlanesLayout() {
  const usuario = useUsuarioOpcional();
  if (usuario) return <SistemaLayout />;
  return (
    <>
      <a
        className="sr-only rounded-md bg-fg px-4 py-3 font-semibold text-white focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-(--z-toast)"
        href="#main"
      >
        Ir al contenido
      </a>
      <PublicHeader />
      <div className="mk-root">
        <IconSprite />
        <Outlet />
      </div>
    </>
  );
}
