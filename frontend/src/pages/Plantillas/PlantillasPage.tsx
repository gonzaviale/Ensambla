import { useState } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { TemplateCard } from '@/components/sistema/TemplateCard';
import { varsDeColor } from '@/components/sistema/colores';
import { TEMPLATES } from '@/lib/sistema/catalogo';
import { normalize } from '@/lib/sistema/core';
import type { ClavePlantilla, Color } from '@/lib/sistema/tipos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

const CLAVES = Object.keys(TEMPLATES) as ClavePlantilla[];

const FILTROS: { valor: 'all' | ClavePlantilla; etiqueta: string; color: Color }[] = [
  { valor: 'all', etiqueta: 'Todas', color: 'petrol' },
  { valor: 'shop', etiqueta: 'Almacén o kiosco', color: 'yellow' },
  { valor: 'warehouse', etiqueta: 'Stock y movimientos', color: 'orange' },
  { valor: 'business', etiqueta: 'Pedidos y clientes', color: 'coral' },
];

// El texto de cada tarjeta, para que el buscador encuentre por nombre, descripción o piezas.
const textoDe = (clave: ClavePlantilla) => {
  const plantilla = TEMPLATES[clave];
  return normalize(
    [plantilla.name, plantilla.subtitle, plantilla.description, ...plantilla.tags].join(' '),
  );
};

export function PlantillasPage() {
  const navegacion = useNavegacion();
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const [filtro, setFiltro] = useState<'all' | ClavePlantilla>('all');
  const [busqueda, setBusqueda] = useState('');

  const buscado = normalize(busqueda);
  const visibles = CLAVES.filter(
    (clave) => (filtro === 'all' || filtro === clave) && textoDe(clave).includes(buscado),
  );

  return (
    <>
      <CabeceraComun active="templates" />
      <main id="main" tabIndex={-1} className="page-wrap template-page reveal">
        <Btn cls="ghost small" onClick={() => void navegacion.irAInicio()}>
          <Icon name="back" /> Volver al inicio
        </Btn>
        <div className="page-title">
          <div className="eyebrow">EMPEZAR CON PIEZAS QUE YA ENCAJAN</div>
          <h1>
            Elegí una base.
            <br />
            Hacela de tu negocio.
          </h1>
          <p>
            Cada plantilla reúne varios bloques conectados. Todo se puede ajustar, quitar o ampliar.
          </p>
        </div>
        <div className="gallery-toolbar">
          <div className="filter-pills">
            {FILTROS.map(({ valor, etiqueta, color }) => (
              <button
                key={valor}
                type="button"
                className={filtro === valor ? 'active' : ''}
                style={varsDeColor(color)}
                data-color={color}
                aria-pressed={filtro === valor}
                data-accion="filter-template"
                onClick={() => setFiltro(valor)}
              >
                <span className="filtro-punto" aria-hidden="true" />
                {etiqueta}
              </button>
            ))}
          </div>
          <label className="searchbox">
            <Icon name="search" />
            <input
              className="input"
              id="template-search"
              placeholder="Buscar una plantilla"
              aria-label="Buscar una plantilla"
              value={busqueda}
              onChange={(evento) => setBusqueda(evento.target.value)}
            />
          </label>
        </div>
        <div className="template-grid" data-tour="pickTemplate">
          {CLAVES.map((clave) => (
            <TemplateCard
              key={clave}
              clave={clave}
              oculta={!visibles.includes(clave)}
              onVer={(elegida) => abrirModal({ tipo: 'plantilla', clave: elegida })}
            />
          ))}
        </div>
        <div id="template-empty" className="empty" hidden={visibles.length > 0}>
          <p>Probá con Almacén, Depósito o Pedidos.</p>
        </div>
        <p className="template-help">
          ¿Querés una mezcla distinta?{' '}
          <button type="button" onClick={() => void navegacion.irATaller()}>
            Contáselo a Sami.
          </button>
        </p>
        <PageFooter />
      </main>
    </>
  );
}
