import type { ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import { FileXlsIcon, NotebookIcon, TagIcon } from '@phosphor-icons/react';
import { Badge, Panel, buttonClasses } from '@/components/ui';
import { cn } from '@/lib/cn';

type TarjetaProps = {
  icono: ReactNode;
  titulo: string;
  texto: string;
  cta?: { etiqueta: string };
  pronto?: boolean;
};

function Tarjeta({ icono, titulo, texto, cta, pronto }: TarjetaProps) {
  return (
    <li className="list-none">
      <Panel
        flat={pronto}
        tight
        className={cn('flex h-full flex-col items-start gap-3', pronto && 'opacity-80')}
      >
        <span className="flex size-12 items-center justify-center rounded-full bg-sand-100 text-fg">
          {icono}
        </span>
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="m-0 text-body font-bold">{titulo}</h3>
          {pronto && <Badge>Pronto</Badge>}
        </div>
        <p className="m-0 text-body text-soft">{texto}</p>
        {cta && (
          <Link to="/cuaderno" className={buttonClasses({ variant: 'primary' })}>
            {cta.etiqueta}
          </Link>
        )}
      </Panel>
    </li>
  );
}

// Lo opcional: traer lo que ya anotaste en papel. La foto es un atajo, no hace falta para usar Ensambla.
export function TraeTuHistoria() {
  return (
    <section
      data-recorrido="subir-historia"
      aria-labelledby="subir-historia-titulo"
      className="flex flex-col gap-4"
    >
      <div className="flex flex-col gap-1">
        <h2 id="subir-historia-titulo" className="m-0 text-h3">
          Traé tu historia
        </h2>
        <p className="m-0 text-body text-soft">
          Es opcional: podés usar Ensambla sin ninguna foto, sólo contando qué pasó. Si ya tenés
          todo anotado en papel, esto te ahorra tipear.
        </p>
      </div>
      <ul className="m-0 grid list-none gap-4 p-0 sm:grid-cols-3">
        <Tarjeta
          icono={<NotebookIcon aria-hidden size={26} />}
          titulo="Foto de tu cuaderno"
          texto="Sacale una foto y Ensambla lee lo que anotaste. Vos revisás antes de guardar."
          cta={{ etiqueta: 'Subir foto del cuaderno' }}
        />
        <Tarjeta
          icono={<TagIcon aria-hidden size={26} />}
          titulo="Foto de una lista de precios"
          texto="Se lee igual que un cuaderno: subís la foto y revisás antes de guardar."
          cta={{ etiqueta: 'Subir lista de precios' }}
        />
        <Tarjeta
          icono={<FileXlsIcon aria-hidden size={26} />}
          titulo="Planilla"
          texto="Traer una planilla de Excel que ya tengas. Todavía no está lista, pero la estamos preparando."
          pronto
        />
      </ul>
    </section>
  );
}
