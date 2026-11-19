import { useRef, type CSSProperties, type DragEvent, type PointerEvent } from 'react';
import type { CargaDeArrastre, LadoDelCambio } from '@/hooks/sistemas/useArrastreDelEditor';
import { CATALOG } from '@/lib/sistema/catalogo';
import type { Ancho, Bloque, Sistema } from '@/lib/sistema/tipos';
import { cambiarAncho } from '@/stores/accionesDelEditor';
import type { ModoDelEditor } from '@/stores/sistemaStore';
import { varsDeColor } from '../colores';
import { Icon } from '../Icon';
import { IconTile } from '../IconTile';
import { WidthButtons } from '../WidthButtons';
import { ContenidoDelBloque } from './ContenidoDelBloque';
import { HerramientasDelBloque } from './HerramientasDelBloque';

type BloqueEnLienzoProps = {
  bloque: Bloque;
  sistema: Sistema;
  modo: ModoDelEditor;
  seleccionado: boolean;
  primero: boolean;
  ultimo: boolean;
  arrastrando: boolean;
  sobre: boolean;
  /** Si otro bloque se suelta encima, este se corre hacia ese lado: ahí se dibuja la barra. */
  ladoDelCambio?: LadoDelCambio;
  /** Ancho que se está probando con el tirador, antes de soltarlo. */
  anchoProvisorio?: number;
  onArrastrar: (evento: DragEvent<HTMLElement>, carga: CargaDeArrastre) => void;
  onFinArrastre: () => void;
  onTirador: (evento: PointerEvent<HTMLButtonElement>, bloque: Bloque, nodo: HTMLElement) => void;
  onEscanear?: () => void;
};

// Un bloque del lienzo, en modo Construir (con herramientas, ancho y tirador) o Usar.
export function BloqueEnLienzo({
  bloque,
  sistema,
  modo,
  seleccionado,
  primero,
  ultimo,
  arrastrando,
  sobre,
  ladoDelCambio,
  anchoProvisorio,
  onArrastrar,
  onFinArrastre,
  onTirador,
  onEscanear,
}: BloqueEnLienzoProps) {
  const definicion = CATALOG[bloque.type];
  const nodo = useRef<HTMLElement>(null);

  const estilo: CSSProperties = {
    ...varsDeColor(bloque.color),
    ...(anchoProvisorio ? { gridColumn: `span ${anchoProvisorio}` } : {}),
  };

  return (
    <section
      ref={nodo}
      className={`block ${seleccionado ? 'selected' : ''} ${arrastrando ? 'dragging' : ''} ${sobre && !ladoDelCambio ? 'drag-over' : ''} ${ladoDelCambio ? `cambio-${ladoDelCambio}` : ''}`
        .replace(/\s+/g, ' ')
        .trim()}
      style={estilo}
      data-block={bloque.id}
      data-tour="done"
      data-tour-id={bloque.id}
      data-width={bloque.width}
      data-color={bloque.color}
      aria-label={bloque.name}
    >
      <header className="block-head">
        <IconTile name={definicion.icon} color={bloque.color} />
        <div className="block-title">
          <h2>{bloque.name}</h2>
          <p>{bloque.description}</p>
        </div>
        {modo === 'build' && (
          <HerramientasDelBloque
            bloque={bloque}
            primero={primero}
            ultimo={ultimo}
            onArrastrar={onArrastrar}
            onFinArrastre={onFinArrastre}
          />
        )}
      </header>
      {modo === 'build' && (
        <div className="block-width-row">
          <span>
            <Icon name="grid" />
            Ancho de esta pieza
          </span>
          <WidthButtons
            ancho={bloque.width}
            onElegir={(ancho: Ancho) => cambiarAncho(bloque.id, ancho)}
          />
        </div>
      )}
      <ContenidoDelBloque bloque={bloque} sistema={sistema} onEscanear={onEscanear} />
      {modo === 'build' && (
        <>
          <button
            type="button"
            className="resize-grip"
            data-resize-block={bloque.id}
            aria-label={`Arrastrar para cambiar el ancho de ${bloque.name}`}
            title="Arrastrá para cambiar el ancho"
            onPointerDown={(evento) => nodo.current && onTirador(evento, bloque, nodo.current)}
          >
            ↔
          </button>
        </>
      )}
    </section>
  );
}
