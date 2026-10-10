import { Link } from '@tanstack/react-router';
import { Logo, buttonClasses } from '@/components/ui';

export function PublicHeader() {
  return (
    <header className="sticky top-0 z-40 flex h-16 items-center gap-3 border-b border-line bg-surface px-4 sm:px-6">
      <Link to="/" aria-label="Ensambla, ir al inicio" className="text-fg no-underline">
        <Logo />
      </Link>
      <nav aria-label="Cuenta" className="ml-auto flex items-center gap-2">
        <Link to="/login" className={buttonClasses({ variant: 'ghost' })}>
          Ingresar
        </Link>
        <Link to="/registro" className={buttonClasses({ variant: 'solid', size: 'sm' })}>
          Crear cuenta
        </Link>
      </nav>
    </header>
  );
}
