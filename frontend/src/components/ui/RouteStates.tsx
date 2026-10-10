import { Link, useRouter, type ErrorComponentProps } from '@tanstack/react-router';
import { ApiError } from '@/api';
import { EmptyState, ErrorState, buttonClasses } from '@/components/ui';
import { MapTrifoldIcon } from '@phosphor-icons/react';

const MENSAJE_GENERICO = 'Ocurrió un error inesperado. Probá de nuevo en unos minutos.';

export function RouteError({ error, reset }: ErrorComponentProps) {
  const router = useRouter();
  const mensaje = error instanceof ApiError ? error.message : MENSAJE_GENERICO;

  return (
    <ErrorState
      message={mensaje}
      onRetry={() => {
        reset();
        void router.invalidate();
      }}
    />
  );
}

export function RouteNotFound() {
  return (
    <div className="mx-auto mt-10 max-w-xl">
      <EmptyState
        icon={<MapTrifoldIcon size={32} aria-hidden />}
        title="No encontramos esta página"
      >
        <Link to="/" className={buttonClasses({ className: 'mt-3' })}>
          Volver al inicio
        </Link>
      </EmptyState>
    </div>
  );
}
