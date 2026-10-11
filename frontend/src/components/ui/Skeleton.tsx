import { cn } from '@/lib/cn';

// Marcador de carga de altura fija: evita saltos de layout mientras llegan los datos.
export function Skeleton({ className }: { className?: string }) {
  return <div aria-hidden className={cn('animate-pulse-soft rounded-md bg-fg/10', className)} />;
}

export function SkeletonCards({
  cantidad = 3,
  className,
}: {
  cantidad?: number;
  className?: string;
}) {
  return (
    <div role="status" aria-label="Cargando…" className={cn('grid gap-4', className)}>
      {Array.from({ length: cantidad }, (_, i) => (
        <div key={i} className="rounded-lg border border-line/60 bg-surface p-5 shadow-sm">
          <Skeleton className="h-6 w-2/3" />
          <Skeleton className="mt-3 h-4 w-1/2" />
          <Skeleton className="mt-6 h-12 w-full" />
        </div>
      ))}
    </div>
  );
}
