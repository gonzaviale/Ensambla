// Los negocios de una cuenta. Cada negocio es un cuaderno (un `Sistema`, el proyecto del backend): la lista sale de los
// cuadernos que ya trae `GET /api/sistemas` y el plan fija cuántos se pueden tener. Uno es el activo, que es el que
// usan todas las demás pantallas; cuál es se recuerda en el navegador (ver `lib/negocios/lista`).

export const NOMBRE_NEGOCIO_MAX = 120;

export type NegocioDeLaCuenta = {
  id: string;
  nombre: string;
  activo: boolean;
  creadoEn: string;
};

export type ListaDeNegocios = {
  /** `null` mientras la cuenta no tiene ningún negocio. */
  activoId: string | null;
  /** Cuántos negocios permite el plan de la cuenta; `null` es sin límite. */
  limite: number | null;
  puedeCrear: boolean;
  negocios: NegocioDeLaCuenta[];
};

export type ResumenDeNegocio = {
  id: string;
  nombre: string;
  activo: boolean;
  hayDatos: boolean;
  vendiste: number | null;
  gastos: number | null;
  queda: number | null;
  teDeben: number | null;
  /** `null` cuando no se sabe: los avisos del servidor son de toda la cuenta y no dicen de qué negocio son. */
  avisosPendientes: number | null;
};

export type ResumenConjunto = {
  dias: number;
  /** La suma de los negocios con datos; los que no tienen ventas en el período no aportan. */
  total: {
    negocios: number;
    vendiste: number;
    gastos: number;
    queda: number;
    teDeben: number;
  };
  negocios: ResumenDeNegocio[];
};
