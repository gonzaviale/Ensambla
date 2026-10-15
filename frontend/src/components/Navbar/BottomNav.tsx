import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import {
  BellRingingIcon,
  CurrencyDollarIcon,
  DotsThreeIcon,
  ListChecksIcon,
  UsersThreeIcon,
} from '@phosphor-icons/react';
import { BotonRecorrido } from '@/components/Recorrido/BotonRecorrido';
import { Button, Sheet, buttonClasses } from '@/components/ui';
import { useCerrarSesion } from '@/hooks/auth/mutations';
import { useUsuario } from '@/hooks/auth/queries';
import { cn } from '@/lib/cn';
import {
  SECCION_CUADERNO,
  SECCION_CUENTA,
  SECCION_HOY,
  SECCION_LISTAS,
  SECCION_MAS,
  SECCION_MOSTRADOR,
  SECCION_PLAN,
  SECCION_PROYECTOS,
} from './secciones';

// El borde superior siempre ocupa su lugar (transparente) para que el estado activo no mueva nada.
const itemClasses =
  'flex min-h-16 flex-1 cursor-pointer flex-col items-center justify-center gap-0.5 border-t-4 border-transparent bg-transparent px-1 text-caption font-semibold text-soft no-underline hover:text-fg';
const activeClasses = 'border-accent-700 text-accent-800';

// Barra inferior del celular (oculta desde md): cuatro destinos fijos y «Más», siempre los mismos.
export function BottomNav() {
  const [masAbierto, setMasAbierto] = useState(false);

  return (
    <>
      <nav
        aria-label="Secciones"
        className="fixed inset-x-0 bottom-0 z-(--z-nav) flex border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] md:hidden"
      >
        <Link
          to="/hoy"
          className={itemClasses}
          activeProps={{ className: cn(itemClasses, activeClasses) }}
        >
          <BellRingingIcon aria-hidden size={26} />
          {SECCION_HOY}
        </Link>
        <Link
          to="/listas"
          className={itemClasses}
          activeProps={{ className: cn(itemClasses, activeClasses) }}
        >
          <ListChecksIcon aria-hidden size={26} />
          {SECCION_LISTAS}
        </Link>
        <Link
          to="/precios"
          className={itemClasses}
          activeProps={{ className: cn(itemClasses, activeClasses) }}
        >
          <CurrencyDollarIcon aria-hidden size={26} />
          Precios
        </Link>
        <Link
          to="/red"
          className={itemClasses}
          activeProps={{ className: cn(itemClasses, activeClasses) }}
        >
          <UsersThreeIcon aria-hidden size={26} />
          Red
        </Link>
        <button type="button" className={itemClasses} onClick={() => setMasAbierto(true)}>
          <DotsThreeIcon aria-hidden size={26} />
          {SECCION_MAS}
        </button>
      </nav>
      <MasSheet open={masAbierto} onClose={() => setMasAbierto(false)} />
    </>
  );
}

function MasSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const usuario = useUsuario();
  const { cerrarSesion, cerrando, error } = useCerrarSesion();
  const enlace = buttonClasses({ variant: 'ghost', fullWidth: true, className: 'justify-start' });

  return (
    <Sheet open={open} onClose={onClose} title={SECCION_MAS}>
      <div className="flex flex-col gap-4">
        <p className="m-0 text-body">
          Ingresaste como <strong className="break-words">{usuario.nombre}</strong>.
        </p>
        <nav aria-label="Más secciones" className="flex flex-col gap-1">
          <Link to="/mostrador" className={enlace} onClick={onClose}>
            {SECCION_MOSTRADOR}
          </Link>
          <Link to="/proyectos" className={enlace} onClick={onClose}>
            {SECCION_PROYECTOS}
          </Link>
          <Link to="/cuaderno" className={enlace} onClick={onClose}>
            {SECCION_CUADERNO}
          </Link>
          <Link to="/plan" className={enlace} onClick={onClose}>
            {SECCION_PLAN}
          </Link>
          <Link to="/cuenta" className={enlace} onClick={onClose}>
            {SECCION_CUENTA}
          </Link>
          <BotonRecorrido fullWidth alIniciar={onClose} />
        </nav>
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
