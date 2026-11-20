import { cloneElement, useEffect, useRef, useState, type ReactElement } from 'react';

type Medidas = { width: number; height: number };
type Props = { alto: number; children: ReactElement<Partial<Medidas>> };

const ANCHO_INICIAL = 360;

// El dibujo toma el ancho de su lugar y lo recibe como número. Antes de medirlo (y en las pruebas, que no tienen
// pantalla) parte de un ancho razonable en lugar de no dibujar nada.
export function LienzoDelGrafico({ alto, children }: Props) {
  const lugar = useRef<HTMLDivElement>(null);
  const ancho = useAnchoDelLugar(lugar);
  return (
    <div ref={lugar} className="lienzo-del-grafico" style={{ width: '100%', height: alto }}>
      {cloneElement(children, { width: ancho, height: alto })}
    </div>
  );
}

function useAnchoDelLugar(lugar: React.RefObject<HTMLDivElement | null>): number {
  const [ancho, setAncho] = useState(ANCHO_INICIAL);
  useEffect(() => {
    const elemento = lugar.current;
    if (!elemento) return;
    const medir = () => {
      const medido = Math.floor(elemento.getBoundingClientRect().width);
      if (medido > 0) setAncho(medido);
    };
    medir();
    if (typeof ResizeObserver === 'undefined') return;
    const observador = new ResizeObserver(medir);
    observador.observe(elemento);
    return () => observador.disconnect();
  }, [lugar]);
  return ancho;
}
