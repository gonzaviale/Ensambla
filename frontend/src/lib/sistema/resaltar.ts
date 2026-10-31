const DURACION_POR_DEFECTO = 5600;
const DURACION_DE_SALIDA = 400;
const RADIO_POR_DEFECTO = 10;
const SEPARACION = 6;

const activos = new WeakMap<Element, () => void>();

/**
 * Marca un elemento con un marco de borde que da vueltas con los colores de la paleta (como una barra de carga).
 * El marco es una capa aparte pegada al elemento, así sirve igual para un bloque, una fila de tabla o un
 * contenedor con scroll. Devuelve una función que lo quita antes de tiempo.
 */
export function resaltar(
  nodo: Element | null | undefined,
  { duracion = DURACION_POR_DEFECTO }: { duracion?: number } = {},
) {
  if (!nodo) return () => undefined;
  activos.get(nodo)?.();

  const marco = document.createElement('div');
  marco.className = 'resaltado-marco';
  marco.setAttribute('aria-hidden', 'true');
  const anillo = document.createElement('i');
  anillo.className = 'anillo';
  marco.appendChild(anillo);
  (nodo.closest('.mk-root') ?? document.body).appendChild(marco);

  const radioPropio = Number.parseFloat(getComputedStyle(nodo).borderTopLeftRadius);
  const radio = (radioPropio > 0 ? radioPropio : RADIO_POR_DEFECTO) + SEPARACION;

  let cuadro = 0;
  const seguir = () => {
    if (!nodo.isConnected) {
      quitar();
      return;
    }
    const caja = nodo.getBoundingClientRect();
    const visible = caja.width > 0 && caja.height > 0;
    marco.style.display = visible ? '' : 'none';
    marco.style.left = `${caja.left - SEPARACION}px`;
    marco.style.top = `${caja.top - SEPARACION}px`;
    marco.style.width = `${caja.width + SEPARACION * 2}px`;
    marco.style.height = `${caja.height + SEPARACION * 2}px`;
    marco.style.borderRadius = `${radio}px`;
    cuadro = requestAnimationFrame(seguir);
  };
  seguir();

  let salida = 0;
  const final = setTimeout(() => {
    marco.classList.add('saliendo');
    salida = window.setTimeout(quitar, DURACION_DE_SALIDA);
  }, duracion);

  function quitar() {
    cancelAnimationFrame(cuadro);
    clearTimeout(final);
    clearTimeout(salida);
    marco.remove();
    if (activos.get(nodo as Element) === quitar) activos.delete(nodo as Element);
  }

  activos.set(nodo, quitar);
  return quitar;
}
