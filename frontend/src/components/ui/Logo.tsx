import { cn } from '@/lib/cn';

// Isotipo: cinco bloques que sugieren una "E" sin dibujarla (manual de marca, p. 5).
// Aproximación vectorial: reemplazar por el SVG oficial del kit de marca cuando esté disponible.
export function Isotipo({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden
      viewBox="0 0 24 24"
      className={cn('size-7 shrink-0', className)}
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="1" y="1" width="9" height="14" rx="2" fill="#F97316" />
      <rect x="12" y="1" width="11" height="6" rx="2" fill="#FB5852" />
      <rect x="12" y="9" width="11" height="6" rx="2" fill="#E6C9A8" />
      <rect x="1" y="17" width="11" height="6" rx="2" fill="#263443" />
      <rect x="14" y="17" width="9" height="6" rx="2" fill="#F59E0B" />
    </svg>
  );
}

export function Logo() {
  return (
    <span className="flex items-center gap-2">
      <Isotipo />
      <span className="text-lg font-extrabold tracking-tight">Ensambla</span>
    </span>
  );
}
