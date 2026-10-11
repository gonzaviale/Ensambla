import { Link } from '@tanstack/react-router';
import { CameraIcon, ListChecksIcon, TableIcon } from '@phosphor-icons/react';
import { Isotipo, buttonClasses } from '@/components/ui';
import { useTituloPagina } from '@/hooks/useTituloPagina';

const PASOS = [
  {
    Icono: CameraIcon,
    titulo: 'Sacá una foto',
    texto: 'Fotografiá las hojas de tu cuaderno con el celular.',
  },
  {
    Icono: ListChecksIcon,
    titulo: 'Revisá lo que leímos',
    texto: 'Corregís lo que haga falta, con la foto al lado.',
  },
  {
    Icono: TableIcon,
    titulo: 'Usá tu sistema',
    texto: 'Cargás, buscás y mirás tus datos ordenados.',
  },
];

export function LandingPage() {
  useTituloPagina('Tu sistema, un bloque a la vez');

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-14 px-4 py-12 sm:py-20">
      <section className="flex flex-col items-start gap-6">
        <Isotipo className="size-16" />
        <h1 className="m-0 max-w-3xl text-h1 sm:text-display">Tu sistema, un bloque a la vez.</h1>
        <p className="m-0 max-w-2xl text-lead text-soft">
          Armá el sistema de tu negocio sin programar: pasá tu cuaderno a una foto, elegí una
          plantilla o empezá en blanco, y cargá tus datos hoy.
        </p>
        <div className="flex flex-wrap gap-3">
          <Link to="/registro" className={buttonClasses({ variant: 'solid', size: 'lg' })}>
            Crear mi cuenta
          </Link>
          <Link to="/login" className={buttonClasses({ size: 'lg' })}>
            Ya tengo cuenta
          </Link>
        </div>
      </section>

      <section aria-labelledby="como-funciona" className="flex flex-col gap-5">
        <h2 id="como-funciona" className="m-0 text-h2">
          Cómo funciona
        </h2>
        <ol className="m-0 grid list-none gap-4 p-0 sm:grid-cols-3">
          {PASOS.map(({ Icono, titulo, texto }, i) => (
            <li
              key={titulo}
              className="flex flex-col gap-3 rounded-lg border border-line/60 bg-surface p-5 shadow-sm"
            >
              <span className="flex size-12 items-center justify-center rounded-full bg-accent-100 text-accent-800">
                <Icono aria-hidden size={28} />
              </span>
              <h3 className="m-0 text-h3">
                {i + 1}. {titulo}
              </h3>
              <p className="m-0 text-body text-soft">{texto}</p>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}
