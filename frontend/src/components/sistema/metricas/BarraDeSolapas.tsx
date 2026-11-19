import { useEffect, useRef, type KeyboardEvent } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { SOLAPA_DE_VENTAS, SOLAPA_PARA_AGREGAR, type Solapas } from '@/hooks/metricas/useSolapas';
import { colorDeLaMetrica, idDeLaSolapa, idDelCuerpo } from '@/lib/metricas/solapas';
import type { Metrica } from '@/types/metricas';

type Props = { metricas: Metrica[]; solapas: Solapas };

// Las solapas de «Tu negocio»: primero las ventas, después cada métrica agregada y, al final, para agregar otra. Con
// las flechas del teclado se pasa de una a otra; en el celular la tira se desliza de costado.
export function BarraDeSolapas({ metricas, solapas }: Props) {
  return (
    <div
      className="panel-negocio-solapas"
      role="tablist"
      aria-label="Lo que mirás de tu negocio"
      onKeyDown={moverConLasFlechas}
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
      <SolapaParaAgregar solapas={solapas} />
    </div>
  );
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

function SolapaParaAgregar({ solapas }: { solapas: Solapas }) {
  const elegida = solapas.activa === SOLAPA_PARA_AGREGAR;
  return (
    <button
      type="button"
      role="tab"
      id={idDeLaSolapa(SOLAPA_PARA_AGREGAR)}
      className="panel-negocio-agregar"
      aria-selected={elegida}
      aria-controls={idDelCuerpo(SOLAPA_PARA_AGREGAR)}
      tabIndex={elegida ? 0 : -1}
      onClick={() => solapas.elegir(SOLAPA_PARA_AGREGAR)}
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
