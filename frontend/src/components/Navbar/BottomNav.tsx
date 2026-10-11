import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { CameraIcon, FolderIcon, UserCircleIcon } from '@phosphor-icons/react';
import { Button, Sheet } from '@/components/ui';
import { useCerrarSesion } from '@/hooks/auth/mutations';
import { useUsuario } from '@/hooks/auth/queries';
import { cn } from '@/lib/cn';
import { SECCION_CUADERNO, SECCION_CUENTA, SECCION_PROYECTOS } from './secciones';

// El borde superior siempre ocupa su lugar (transparente) para que el estado activo no mueva nada.
const itemClasses =
  'flex min-h-16 flex-1 cursor-pointer flex-col items-center justify-center gap-0.5 border-t-4 border-transparent bg-transparent px-2 text-hint font-semibold text-soft no-underline hover:text-fg';
const activeClasses = 'border-accent-700 text-accent-800';

// Barra inferior del celular (oculta desde md): tres destinos fijos, siempre los mismos.
export function BottomNav() {
  const [cuentaAbierta, setCuentaAbierta] = useState(false);

  return (
    <>
      <nav
        aria-label="Secciones"
        className="fixed inset-x-0 bottom-0 z-(--z-nav) flex border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] md:hidden"
      >
        <Link
          to="/proyectos"
          className={itemClasses}
          activeProps={{ className: cn(itemClasses, activeClasses) }}
        >
          <FolderIcon aria-hidden size={26} />
          {SECCION_PROYECTOS}
        </Link>
        <Link
          to="/cuaderno"
          className={itemClasses}
          activeProps={{ className: cn(itemClasses, activeClasses) }}
        >
          <CameraIcon aria-hidden size={26} />
          {SECCION_CUADERNO}
        </Link>
        <button type="button" className={itemClasses} onClick={() => setCuentaAbierta(true)}>
          <UserCircleIcon aria-hidden size={26} />
          {SECCION_CUENTA}
        </button>
      </nav>
      <CuentaSheet open={cuentaAbierta} onClose={() => setCuentaAbierta(false)} />
    </>
  );
}

function CuentaSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const usuario = useUsuario();
  const { cerrarSesion, cerrando, error } = useCerrarSesion();

  return (
    <Sheet open={open} onClose={onClose} title={SECCION_CUENTA}>
      <div className="flex flex-col gap-4">
        <p className="m-0 text-body">
          Ingresaste como <strong className="break-words">{usuario.nombre}</strong>.
        </p>
        {error && (
          <p role="alert" className="m-0 font-semibold text-bad">
            No pudimos cerrar sesión. Volvé a intentarlo.
          </p>
        )}
        <Button
          fullWidth
          loading={cerrando}
          onClick={() => void cerrarSesion().catch(() => undefined)}
        >
          {cerrando ? 'Cerrando…' : 'Cerrar sesión'}
        </Button>
      </div>
    </Sheet>
  );
}
