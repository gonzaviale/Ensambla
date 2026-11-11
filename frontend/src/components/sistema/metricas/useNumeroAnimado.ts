import { useEffect, useRef, useState } from 'react';

const sinAnimacion = () =>
  import.meta.env.MODE === 'test' ||
  typeof window.requestAnimationFrame !== 'function' ||
  window.matchMedia?.('(prefers-reduced-motion: reduce)').matches === true;

/** El número sube (o baja) hasta su valor en un momento, como en las apps de cuentas. Sin animaciones, va directo. */
export function useNumeroAnimado(valor: number, duracion = 700) {
  const [mostrado, setMostrado] = useState(valor);
  const desde = useRef(valor);
  useEffect(() => {
    if (sinAnimacion()) {
      setMostrado(valor);
      return;
    }
    const inicio = performance.now();
    const origen = desde.current;
    let cuadro = 0;
    const paso = (ahora: number) => {
      const avance = Math.min(1, (ahora - inicio) / duracion);
      const suave = 1 - (1 - avance) ** 3;
      const actual = origen + (valor - origen) * suave;
      desde.current = actual;
      setMostrado(actual);
      if (avance < 1) cuadro = window.requestAnimationFrame(paso);
    };
    cuadro = window.requestAnimationFrame(paso);
    return () => window.cancelAnimationFrame(cuadro);
  }, [valor, duracion]);
  return Math.round(mostrado);
}
