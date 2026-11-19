import { useState, type ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import { ContenidoPremium } from '@/components/planes/ContenidoPremium';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { useInventarioPropio } from '@/hooks/zona/useInventarioPropio';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { useZona } from '@/hooks/zona/queries';
import type { Zona } from '@/lib/sistema/zona';
import { VistaDeLoQueSeVende } from './VistaDeLoQueSeVende';
import { VistaDePrecios } from './VistaDePrecios';

type Vista = 'precios' | 'cerca';

const VISTAS: { clave: Vista; etiqueta: string; icono: 'chart' | 'orders' }[] = [
  { clave: 'precios', etiqueta: 'Precios', icono: 'chart' },
  { clave: 'cerca', etiqueta: 'Qué se vende cerca', icono: 'orders' },
];

const TITULOS: Record<Vista, { titulo: ReactNode; texto: string }> = {
  precios: {
    titulo: (
      <>
        Mirá cuánto se cobra en tu zona.
        <br />
        Sin saber quién cobra qué.
      </>
    ),
    texto: 'Promedios de al menos 3 comercios. Nadie ve qué cobra cada uno.',
  },
  cerca: {
    titulo: (
      <>
        Qué se vende cerca.
        <br />Y qué todavía no tenés.
      </>
    ),
    texto: 'Lo que más sale cerca. Sumá lo que te falte.',
  },
};

type PropsDeVista = { zona: Zona };

function VistaDeLosPrecios({ zona }: PropsDeVista) {
  const { propios } = useInventarioPropio();
  return (
    <>
      <VistaDePrecios zona={zona} propios={propios} titulo={`Precios en ${zona.nombre}`} />
      <ContenidoPremium acceso={zona.acceso} recurso="recomendaciones" compacto />
    </>
  );
}

const CONTENIDO_DE_LA_VISTA: Record<Vista, (props: PropsDeVista) => ReactNode> = {
  precios: VistaDeLosPrecios,
  cerca: VistaDeLoQueSeVende,
};

function SelectorDeVista({ vista, onElegir }: { vista: Vista; onElegir: (vista: Vista) => void }) {
  return (
    <div className="zona-vistas" role="tablist" aria-label="Qué querés ver">
      {VISTAS.map(({ clave, etiqueta, icono }) => (
        <button
          key={clave}
          type="button"
          role="tab"
          aria-selected={vista === clave}
          className={vista === clave ? 'active' : ''}
          onClick={() => onElegir(clave)}
        >
          <Icon name={icono} /> {etiqueta}
        </button>
      ))}
    </div>
  );
}

// «Mi zona»: precios y ventas de los comercios cercanos, siempre agregados y anónimos. Sin mapa y sin
// comercios individuales; con menos de tres comercios no se muestra ningún dato.
export function ZonaPage() {
  useTituloPagina('Mi zona');
  const zona = useZona();
  const [vista, setVista] = useState<Vista>('precios');
  const { titulo, texto } = TITULOS[vista];
  const ContenidoDeLaVista = CONTENIDO_DE_LA_VISTA[vista];

  return (
    <main id="main" tabIndex={-1} className="page-wrap zona-page reveal">
      <Link className="text-link" to="/ubicacion">
        Probar ubicación y rubro de mi comercio
      </Link>
      <div className="zona-cabeza">
        <div className="page-title">
          <div className="eyebrow">Datos de {zona.nombre || 'tu zona'}</div>
          <h1>{titulo}</h1>
          <p>{texto}</p>
        </div>
        <SelectorDeVista vista={vista} onElegir={setVista} />
      </div>
      <ContenidoDeLaVista zona={zona} />
      <PageFooter />
    </main>
  );
}
