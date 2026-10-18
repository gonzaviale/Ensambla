import { cn } from '@/lib/cn';

// Isotipo: cinco bloques que sugieren una "E" sin dibujarla (kit de marca, colores actualizados).
export function Isotipo({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden
      viewBox="0 0 320 340"
      className={cn('size-7 shrink-0', className)}
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect y="240" width="150" height="100" rx="20" fill="#00506A" />
      <rect x="170" y="120" width="150" height="100" rx="20" fill="#4EB2D4" />
      <rect width="150" height="220" rx="20" fill="#EE8000" />
      <rect x="170" y="240" width="150" height="100" rx="20" fill="#F85F5A" />
      <rect x="170" width="150" height="100" rx="20" fill="#EBCE32" />
    </svg>
  );
}

export function Logo() {
  return (
    <span className="flex items-center gap-2.5">
      <Isotipo className="size-8" />
      <span className="text-xl font-extrabold tracking-tight text-petrol">Ensambla</span>
    </span>
  );
}
