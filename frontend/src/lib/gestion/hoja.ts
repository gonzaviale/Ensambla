import { CATALOG } from '@/lib/sistema/catalogo';
import type { Sistema, TipoBloque } from '@/lib/sistema/tipos';
import type { Renglon, TipoDeRenglon } from './datosDeEjemplo';

// La hoja del día trae de todo mezclado. Cada renglón va al bloque del cuaderno que le corresponde: así la foto no
// es una segunda carga, sino una manera de llenar el cuaderno de una sola vez. Si el cuaderno no tiene el bloque,
// Sami propone crearlo.

type Regla = {
  /** Los bloques que sirven, en orden de preferencia. */
  bloques: TipoBloque[];
  /** Cómo queda anotado en ese bloque. */
  como: string;
};

const REGLAS: Record<TipoDeRenglon, Regla> = {
  venta: { bloques: ['sales'], como: 'cobrada' },
  fiado: { bloques: ['sales'], como: 'a fiado' },
  cobro: { bloques: ['sales'], como: 'cobro de fiado' },
  pago: { bloques: ['expenses'], como: 'pago a proveedor' },
  gasto: { bloques: ['expenses'], como: 'gasto' },
  merma: { bloques: ['exits', 'inventory'], como: 'pérdida' },
};

export const TIPOS_DE_RENGLON = Object.keys(REGLAS) as TipoDeRenglon[];

export type Destino = {
  tipo: TipoDeRenglon;
  /** El nombre del bloque en el cuaderno de la persona, o el de la plantilla si no lo tiene. */
  bloque: string;
  tipoDeBloque: TipoBloque;
  como: string;
  /** El cuaderno no tiene ese bloque. */
  falta: boolean;
};

/** A qué bloque del cuaderno va cada tipo de renglón. `creados` son los bloques que la persona ya aceptó sumar. */
export function destinosDelCuaderno(
  sistema: Sistema,
  creados: TipoBloque[] = [],
): Record<TipoDeRenglon, Destino> {
  const bloques = sistema.sections.flatMap((seccion) => seccion.blocks);
  const destino = (tipo: TipoDeRenglon): Destino => {
    const regla = REGLAS[tipo];
    const propio = regla.bloques
      .map((tipoDeBloque) => bloques.find((bloque) => bloque.type === tipoDeBloque))
      .find(Boolean);
    const tipoDeBloque = propio?.type ?? (regla.bloques[0] as TipoBloque);
    return {
      tipo,
      bloque: propio?.name ?? CATALOG[tipoDeBloque].name,
      tipoDeBloque,
      como: regla.como,
      falta: !propio && !creados.includes(tipoDeBloque),
    };
  };
  return Object.fromEntries(TIPOS_DE_RENGLON.map((tipo) => [tipo, destino(tipo)])) as Record<
    TipoDeRenglon,
    Destino
  >;
}

/** Los tipos que mueven plata por el cajón o la cuenta (el fiado y las pérdidas no). */
export const llevaMedio = (tipo: TipoDeRenglon) => tipo !== 'fiado' && tipo !== 'merma';

/** El fiado y su cobro tienen que decir de quién son. */
export const llevaCliente = (tipo: TipoDeRenglon) => tipo === 'fiado' || tipo === 'cobro';

/** Lo que falta resolver antes de cerrar el día: bloques que no existen y fiados sin cliente. */
export function pendientesDeLaHoja(renglones: Renglon[], destinos: Record<TipoDeRenglon, Destino>) {
  const sinBloque = [
    ...new Set(
      renglones
        .filter((renglon) => destinos[renglon.tipo].falta)
        .map((r) => destinos[r.tipo].bloque),
    ),
  ];
  const sinCliente = renglones.filter(
    (renglon) => llevaCliente(renglon.tipo) && !renglon.cliente?.trim(),
  ).length;
  return { sinBloque, sinCliente, listo: sinBloque.length === 0 && sinCliente === 0 };
}

/** Cuántos renglones van a cada bloque, para contar dónde quedó anotado cada cosa. */
export function repartoPorBloque(
  renglones: Renglon[],
  destinos: Record<TipoDeRenglon, Destino>,
): { bloque: string; cantidad: number }[] {
  const cuenta = new Map<string, number>();
  for (const renglon of renglones) {
    const { bloque } = destinos[renglon.tipo];
    cuenta.set(bloque, (cuenta.get(bloque) ?? 0) + 1);
  }
  return [...cuenta].map(([bloque, cantidad]) => ({ bloque, cantidad }));
}
