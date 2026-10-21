import { useState, type DragEvent, type KeyboardEvent } from 'react';
import { CATALOG, TEMPLATES } from '@/lib/sistema/catalogo';
import { normalize } from '@/lib/sistema/core';
import type { ClavePlantilla, TipoBloque } from '@/lib/sistema/tipos';
import { agregarBloque, agregarConjunto } from '@/stores/accionesDelEditor';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { varsDeColor } from '../colores';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';
import { IconTile } from '../IconTile';
import { Sami } from '../Sami';
import type { CargaDeArrastre } from '@/hooks/sistemas/useArrastreDelEditor';

const GRUPOS: [string, TipoBloque[]][] = [
  ['DATOS Y TRABAJO', ['clients', 'orders', 'inventory', 'sales', 'entries', 'exits', 'expenses']],
  ['DARLE FORMA', ['summary', 'form', 'note']],
];

type Props = {
  abierta: boolean;
  fijada: boolean;
  onCerrar: () => void;
  onFijar: (fijada: boolean) => void;
  onArrastrar: (evento: DragEvent<HTMLElement>, carga: CargaDeArrastre) => void;
  onFinArrastre: () => void;
};

// El catálogo de piezas de la izquierda en Construir: bloques sueltos y conjuntos listos. Flota encima
// del lienzo y se abre con «Bloques»; fijado, ocupa su columna y achica el lienzo (en el celular, nunca).
export function BibliotecaDeBloques({
  abierta,
  fijada,
  onCerrar,
  onFijar,
  onArrastrar,
  onFinArrastre,
}: Props) {
  const [pestania, setPestania] = useState<'blocks' | 'sections'>('blocks');
  const [consulta, setConsulta] = useState('');
  const buscado = normalize(consulta);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);

  const teclear = (evento: KeyboardEvent<HTMLElement>, tipo: TipoBloque) => {
    if (evento.key === 'Enter' || evento.key === ' ') {
      evento.preventDefault();
      agregarBloque(tipo);
    }
  };

  return (
    <aside
      aria-label="Piezas disponibles"
      className={`block-library ${abierta ? 'mobile-open' : ''} ${fijada ? 'fijada' : ''}`
        .replace(/\s+/g, ' ')
        .trim()}
    >
      <div className="row between library-head">
        <h2>Tus piezas</h2>
        <div className="library-head-acciones">
          <button
            type="button"
            className={`fijar-piezas ${fijada ? 'activo' : ''}`.trim()}
            aria-pressed={fijada}
            title={fijada ? 'Soltar: el panel vuelve a flotar' : 'Fijar el panel al costado'}
            onClick={() => onFijar(!fijada)}
          >
            <Icon name="pin" /> {fijada ? 'Soltar' : 'Fijar'}
          </button>
          <IconBtn
            name="close"
            label="Cerrar catálogo"
            data-mobile-close="true"
            data-accion="close-library"
            onClick={onCerrar}
          />
        </div>
      </div>
      <p className="small-text muted" style={{ fontSize: 10, margin: '5px 0 12px' }}>
        Arrastrá una pieza o tocá +.
      </p>
      <div className="library-tabs">
        <button
          type="button"
          className={pestania === 'blocks' ? 'active' : ''}
          onClick={() => setPestania('blocks')}
        >
          Bloques
        </button>
        <button
          type="button"
          className={pestania === 'sections' ? 'active' : ''}
          onClick={() => setPestania('sections')}
        >
          Conjuntos listos
        </button>
      </div>
      {pestania === 'blocks' ? (
        <>
          <label className="searchbox">
            <Icon name="search" />
            <input
              className="input"
              id="block-search"
              placeholder="Buscar una pieza"
              aria-label="Buscar bloques"
              value={consulta}
              onChange={(evento) => setConsulta(evento.target.value)}
            />
          </label>
          {GRUPOS.map(([etiqueta, tipos]) => (
            <div key={etiqueta}>
              <div className="library-label">{etiqueta}</div>
              {tipos.map((tipo) => {
                const definicion = CATALOG[tipo];
                const texto = normalize(`${definicion.name} ${definicion.description}`);
                return (
                  <div
                    key={tipo}
                    className="library-item"
                    style={varsDeColor(definicion.color)}
                    data-color={definicion.color}
                    role="button"
                    tabIndex={0}
                    draggable
                    hidden={!texto.includes(buscado)}
                    data-accion="add-block"
                    data-tour={
                      definicion.fields.length > 0 && !['entries', 'exits'].includes(tipo)
                        ? 'agregar-pieza'
                        : undefined
                    }
                    data-drag-kind="catalog"
                    data-value={tipo}
                    aria-label={`Agregar ${definicion.name}`}
                    onClick={() => agregarBloque(tipo)}
                    onKeyDown={(evento) => teclear(evento, tipo)}
                    onDragStart={(evento) => onArrastrar(evento, { tipo: 'catalogo', valor: tipo })}
                    onDragEnd={onFinArrastre}
                  >
                    <IconTile tipo={tipo} />
                    <div>
                      <strong>{definicion.name}</strong>
                      <p>{definicion.description}</p>
                    </div>
                    <Icon name="plus" />
                  </div>
                );
              })}
            </div>
          ))}
        </>
      ) : (
        <>
          <p className="small-text muted" style={{ fontSize: 10, marginBottom: 15 }}>
            Agregá un conjunto en una sección nueva. Sus piezas ya vienen conectadas.
          </p>
          {(
            Object.entries(TEMPLATES) as [ClavePlantilla, (typeof TEMPLATES)[ClavePlantilla]][]
          ).map(([clave, definicion]) => (
            <button
              key={clave}
              type="button"
              className="section-preset"
              style={varsDeColor(definicion.color)}
              data-color={definicion.color}
              onClick={() => agregarConjunto(clave)}
            >
              <div className="row">
                {definicion.types.map((tipo) => (
                  <IconTile key={tipo} tipo={tipo} />
                ))}
              </div>
              <h3>{definicion.name}</h3>
              <p>{definicion.subtitle}</p>
            </button>
          ))}
          <Btn cls="secondary small full" onClick={() => abrirModal({ tipo: 'agregarSeccion' })}>
            <Icon name="plus" /> Sección vacía
          </Btn>
        </>
      )}
      <div className="library-tip">
        <Sami />
        <p>
          Para poner dos piezas juntas, soltá una al lado de la otra. Elegí ½ o 1 para cambiar el
          ancho.
        </p>
      </div>
    </aside>
  );
}
