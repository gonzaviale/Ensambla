import { useCallback, useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { SOLAPA_DE_VENTAS, type Solapas } from '@/hooks/metricas/useSolapas';
import { colorDeLaMetrica, idDeLaSolapa, idDelCuerpo } from '@/lib/metricas/solapas';
import type { Metrica } from '@/types/metricas';

type Props = { metricas: Metrica[]; solapas: Solapas; onAgregar: () => void };

// Las solapas de «Tu negocio»: primero las ventas, después cada métrica y, al final, el botón para agregar otra. Con
// las flechas del teclado se pasa de una a otra; en el celular la tira se desliza de costado.
export function BarraDeSolapas({ metricas, solapas, onAgregar }: Props) {
  const tira = useTiraDeslizable(metricas.length);
  return (
    <div className="panel-negocio-barra">
      <FlechaDeLaTira lado="izquierda" visible={tira.hayAntes} onCorrer={tira.correr} />
      <div
        ref={tira.ref}
        className="panel-negocio-solapas"
        role="tablist"
        aria-label="Lo que mirás de tu negocio"
        onKeyDown={moverConLasFlechas}
        onScroll={tira.medir}
      >
        <Solapa id={SOLAPA_DE_VENTAS} nombre="Ventas" color="petroleo" solapas={solapas} />
        {metricas.map((metrica) => (
          <Solapa
            key={metrica.id}
            id={metrica.id}
            nombre={metrica.definicion.nombre}
            color={colorDeLaMetrica(metrica.id, metricas)}
            solapas={solapas}
          />
        ))}
      </div>
      <FlechaDeLaTira lado="derecha" visible={tira.hayDespues} onCorrer={tira.correr} />
      <BotonParaAgregar onAgregar={onAgregar} />
    </div>
  );
}

type Lado = 'izquierda' | 'derecha';

const FLECHAS: Record<Lado, { etiqueta: string; icono: 'back' | 'arrow'; paso: number }> = {
  izquierda: { etiqueta: 'Ver las solapas anteriores', icono: 'back', paso: -1 },
  derecha: { etiqueta: 'Ver más solapas', icono: 'arrow', paso: 1 },
};

type PropsDeLaFlecha = { lado: Lado; visible: boolean; onCorrer: (paso: number) => void };

// Con muchas métricas la tira no entra: las flechas la corren de a un tramo, también con el mouse.
function FlechaDeLaTira({ lado, visible, onCorrer }: PropsDeLaFlecha) {
  const { etiqueta, icono, paso } = FLECHAS[lado];
  if (!visible) return null;
  return (
    <button
      type="button"
      className="panel-negocio-flecha"
      aria-label={etiqueta}
      onClick={() => onCorrer(paso)}
    >
      <Icon name={icono} />
    </button>
  );
}

/** La tira de solapas: si hay más de las que entran, sabe si quedan antes o después y la corre de a un tramo. */
function useTiraDeslizable(cantidad: number) {
  const ref = useRef<HTMLDivElement>(null);
  const [lados, setLados] = useState({ hayAntes: false, hayDespues: false });
  const medir = useCallback(() => setLados(ladosOcultos(ref.current)), []);
  useEffect(() => {
    medir();
    window.addEventListener('resize', medir);
    return () => window.removeEventListener('resize', medir);
  }, [medir, cantidad]);
  const correr = (paso: number) =>
    ref.current?.scrollBy?.({ left: paso * TRAMO, behavior: 'smooth' });
  return { ref, ...lados, medir, correr };
}

const TRAMO = 240;

function ladosOcultos(tira: HTMLDivElement | null) {
  if (!tira) return { hayAntes: false, hayDespues: false };
  return {
    hayAntes: tira.scrollLeft > 0,
    hayDespues: tira.scrollLeft + tira.clientWidth < tira.scrollWidth - 1,
  };
}

type PropsDeLaSolapa = { id: string; nombre: string; color: string; solapas: Solapas };

function Solapa({ id, nombre, color, solapas }: PropsDeLaSolapa) {
  const elegida = solapas.activa === id;
  const solapa = useALaVistaSiEstaElegida(elegida);
  return (
    <button
      ref={solapa}
      type="button"
      role="tab"
      id={idDeLaSolapa(id)}
      className="panel-negocio-solapa"
      data-color={color}
      aria-selected={elegida}
      aria-controls={idDelCuerpo(id)}
      tabIndex={elegida ? 0 : -1}
      title={nombre}
      onClick={() => solapas.elegir(id)}
    >
      <span className="panel-negocio-solapa-punto" aria-hidden="true" />
      <span className="panel-negocio-solapa-nombre">{nombre}</span>
    </button>
  );
}

// No es una solapa: abre el panel del costado, así lo que se está mirando sigue a la vista.
function BotonParaAgregar({ onAgregar }: { onAgregar: () => void }) {
  return (
    <button
      type="button"
      className="panel-negocio-agregar"
      aria-haspopup="dialog"
      onClick={onAgregar}
    >
      <Icon name="plus" />
      Agregar métrica
    </button>
  );
}

// En el celular la tira se desliza de costado: la solapa abierta siempre queda a la vista.
function useALaVistaSiEstaElegida(elegida: boolean) {
  const solapa = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (elegida) correrLaTiraHasta(solapa.current);
  }, [elegida]);
  return solapa;
}

// Sólo desliza la tira de costado; la página no se mueve.
function correrLaTiraHasta(solapa: HTMLButtonElement | null) {
  const tira = solapa?.parentElement;
  if (!solapa || !tira) return;
  const seVe =
    solapa.offsetLeft >= tira.scrollLeft &&
    solapa.offsetLeft + solapa.offsetWidth <= tira.scrollLeft + tira.clientWidth;
  if (!seVe) tira.scrollLeft = solapa.offsetLeft - 8;
}

const PASO_DE_LA_FLECHA: Record<string, number> = { ArrowRight: 1, ArrowLeft: -1 };

function moverConLasFlechas(evento: KeyboardEvent<HTMLDivElement>) {
  const paso = PASO_DE_LA_FLECHA[evento.key];
  if (paso === undefined) return;
  const todas = [...evento.currentTarget.querySelectorAll<HTMLButtonElement>('[role="tab"]')];
  const actual = todas.indexOf(document.activeElement as HTMLButtonElement);
  const siguiente = todas[(actual + paso + todas.length) % todas.length];
  siguiente?.focus();
  siguiente?.click();
}
