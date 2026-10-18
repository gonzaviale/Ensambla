import {
  useEffect,
  useState,
  type DragEvent,
  type PointerEvent as PointerEventDeReact,
} from 'react';
import type { Ancho, Bloque, TipoBloque } from '@/lib/sistema/tipos';
import {
  agregarBloque,
  cambiarAncho,
  juntarBloques,
  reubicarBloque,
} from '@/stores/accionesDelEditor';

export type CargaDeArrastre = { tipo: 'catalogo' | 'bloque'; valor: string };

/** Lo que está bajo el cursor mientras se arrastra: una pieza, una pestaña de sección o el lienzo. */
export type SobreElDestino = { bloque?: string; destino?: string } | null;

export type AjusteDeAncho = { id: string; span: number };

const LIENZO = '#canvas-drop';
const DESTINOS = `${LIENZO},[data-drop-section]`;

// Las marcas que el CSS del mockup espera en el `body` viven en el contenedor `.mk-root`.
function marcarRaiz(accion: (raiz: HTMLElement) => void) {
  const raiz = document.querySelector<HTMLElement>('.mk-root');
  if (raiz) accion(raiz);
}

// Un tamaño por cada ancho posible: qué fracción de la fila ocupa y cuántas de las 12 columnas.
const TAMANIOS: [Ancho, number, number][] = [
  ['third', 1 / 3, 4],
  ['half', 0.5, 6],
  ['wide', 2 / 3, 8],
  ['full', 1, 12],
];

// Arrastrar y soltar del lienzo (piezas de la biblioteca y bloques) y el tirador de ancho de cada bloque.
export function useArrastreDelEditor(seccionId: string | undefined) {
  const [carga, setCarga] = useState<CargaDeArrastre | null>(null);
  const [sobre, setSobre] = useState<SobreElDestino>(null);
  const [ajuste, setAjuste] = useState<AjusteDeAncho | null>(null);

  useEffect(() => {
    marcarRaiz((raiz) => {
      if (carga) raiz.dataset.blockDrag = 'true';
      else delete raiz.dataset.blockDrag;
    });
    return () => marcarRaiz((raiz) => delete raiz.dataset.blockDrag);
  }, [carga]);

  const limpiar = () => {
    setCarga(null);
    setSobre(null);
  };

  const alArrastrar = (evento: DragEvent<HTMLElement>, nueva: CargaDeArrastre) => {
    const texto = JSON.stringify(nueva);
    evento.dataTransfer.setData('application/x-ensambla', texto);
    evento.dataTransfer.setData('text/plain', texto);
    evento.dataTransfer.effectAllowed = nueva.tipo === 'bloque' ? 'move' : 'copy';
    setCarga(nueva);
  };

  const alPasarPor = (evento: DragEvent<HTMLElement>) => {
    if (!carga) return;
    const objetivo = evento.target as HTMLElement;
    const destino = objetivo.closest<HTMLElement>(DESTINOS);
    if (!destino) return;
    evento.preventDefault();
    const bloque = objetivo.closest<HTMLElement>('[data-block]')?.dataset.block;
    setSobre(
      bloque && bloque !== carga.valor
        ? { bloque }
        : { destino: destino.dataset.dropSection ?? LIENZO },
    );
  };

  const alSoltar = (evento: DragEvent<HTMLElement>) => {
    if (!carga) return;
    const objetivo = evento.target as HTMLElement;
    const destino = objetivo.closest<HTMLElement>(DESTINOS);
    if (!destino) return;
    evento.preventDefault();
    const origen = carga;
    limpiar();
    const seccionDestino = destino.dataset.dropSection ?? seccionId;
    if (!seccionDestino) return;
    const antes = objetivo.closest<HTMLElement>('[data-block]')?.dataset.block ?? null;
    const lado = objetivo.closest<HTMLElement>('[data-drop-side]')?.dataset.dropSide;
    if ((lado === 'left' || lado === 'right') && antes) {
      juntarBloques(
        origen.tipo === 'catalogo'
          ? { tipo: 'catalogo', valor: origen.valor as TipoBloque }
          : { tipo: 'bloque', valor: origen.valor },
        antes,
        lado,
        seccionDestino,
      );
    } else if (origen.tipo === 'catalogo') {
      agregarBloque(origen.valor as TipoBloque, antes, seccionDestino);
    } else if (origen.valor !== antes) {
      reubicarBloque(origen.valor, seccionDestino, antes);
    }
  };

  // El tirador de la esquina: se arrastra y el bloque salta al ancho más cercano.
  const alTirador = (
    evento: PointerEventDeReact<HTMLButtonElement>,
    bloque: Bloque,
    nodo: HTMLElement,
  ) => {
    const grilla = nodo.parentElement;
    const anchoDeLaGrilla = grilla?.getBoundingClientRect().width;
    if (!anchoDeLaGrilla) return;
    const inicioX = evento.clientX;
    const anchoInicial = nodo.getBoundingClientRect().width;
    let elegido: Ancho = bloque.width;
    evento.currentTarget.setPointerCapture?.(evento.pointerId);
    evento.preventDefault();
    marcarRaiz((raiz) => raiz.classList.add('resizing'));

    const mover = (movimiento: PointerEvent) => {
      const fraccion = Math.max(
        0.25,
        Math.min(1, (anchoInicial + movimiento.clientX - inicioX) / anchoDeLaGrilla),
      );
      const cercano = [...TAMANIOS].sort(
        (a, b) => Math.abs(a[1] - fraccion) - Math.abs(b[1] - fraccion),
      )[0];
      if (!cercano) return;
      elegido = cercano[0];
      setAjuste({ id: bloque.id, span: cercano[2] });
    };
    const terminar = () => {
      window.removeEventListener('pointermove', mover);
      window.removeEventListener('pointerup', soltar);
      window.removeEventListener('pointercancel', cancelar);
      marcarRaiz((raiz) => raiz.classList.remove('resizing'));
      setAjuste(null);
    };
    const soltar = () => {
      terminar();
      if (elegido !== bloque.width) cambiarAncho(bloque.id, elegido);
    };
    const cancelar = () => terminar();
    window.addEventListener('pointermove', mover);
    window.addEventListener('pointerup', soltar);
    window.addEventListener('pointercancel', cancelar);
  };

  return { carga, sobre, ajuste, alArrastrar, alPasarPor, alSoltar, alTirador, limpiar };
}
