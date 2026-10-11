import { Link } from '@tanstack/react-router';
import { buttonClasses } from '@/components/ui';

export function LandingPage() {
  return (
    <section className="mx-auto flex max-w-3xl flex-col items-start gap-5 px-4 py-16 sm:py-24">
      <p className="text-caption font-bold tracking-widest text-accent-800 uppercase">Ensambla</p>
      <h1 className="m-0 text-h1 sm:text-display">Tu sistema, un bloque a la vez.</h1>
      <p className="max-w-xl text-lead leading-relaxed text-soft">
        Armá el sistema de tu negocio sin programar: elegí una plantilla o empezá en blanco, sumá
        bloques y campos, y empezá a cargar tus datos hoy.
      </p>
      <div className="flex flex-wrap gap-3">
        <Link to="/registro" className={buttonClasses({ variant: 'solid' })}>
          Crear mi cuenta
        </Link>
        <Link to="/login" className={buttonClasses()}>
          Ya tengo cuenta
        </Link>
      </div>
    </section>
  );
}
