import { Link } from '@tanstack/react-router';
import { Probador } from '@/components/Probar/Probador';
import { PageHeading, buttonClasses } from '@/components/ui';

// Valor antes del registro: se prueba el ensamblado sin cuenta y sin tocar la API.
export function ProbarPage() {
  return (
    <div className="mx-auto flex w-full max-w-3xl animate-fade-in flex-col gap-8 px-4 py-8">
      <PageHeading
        kicker="Probá ahora"
        title="Contá tu negocio y mirá cómo se arma"
        lead="Sin registrarte y sin cargar nada: escribí una frase y Ensambla arma tu sistema a modo de ejemplo."
      />
      <Probador />
      <div className="flex flex-col gap-3 sm:flex-row">
        <Link to="/registro" className={buttonClasses({ variant: 'solid', size: 'lg' })}>
          Guardar y empezar
        </Link>
        <Link to="/login" className={buttonClasses({ variant: 'secondary', size: 'lg' })}>
          Ya tengo cuenta
        </Link>
      </div>
    </div>
  );
}
