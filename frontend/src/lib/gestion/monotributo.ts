import {
  CATEGORIA_ACTUAL,
  CATEGORIAS_DE_EJEMPLO,
  FACTURADO_ANTES_DEL_CUADERNO,
  VENCIMIENTO_CUOTA,
  type CategoriaMonotributo,
} from './datosDeEjemplo';
import { diasEntre, type DatosDelNegocio } from './resultado';

// Monotributo: lo facturado en los últimos 12 meses contra el tope de la categoría, cuándo se pasa a este ritmo, qué
// categoría corresponde en la próxima recategorización (enero y julio) y cuándo vence la cuota. Las categorías del
// mockup son de ejemplo; las vigentes las publica ARCA.

export type MesFacturado = {
  /** «aaaa-mm». */
  mes: string;
  importe: number;
  /** Sale del cuaderno; si no, de las facturas viejas. */
  delCuaderno: boolean;
};

export type MesProyectado = { mes: string; acumulado12: number; superaElTope: boolean };

export type EstadoMonotributo = {
  categoria: CategoriaMonotributo;
  meses: MesFacturado[];
  total12: number;
  /** Qué parte del tope ya se usó. */
  usado: number;
  /** Lo que se factura por mes, en promedio, en los últimos 90 días. */
  ritmoMensual: number;
  proyeccion: MesProyectado[];
  /** El primer mes en que lo facturado en 12 meses pasa el tope; `null` si no pasa en un año. */
  superaEn: string | null;
  /** Las dos próximas recategorizaciones (enero y julio). */
  recategorizaciones: Recategorizacion[];
  vencimiento: { fecha: string; dias: number };
};

export type Recategorizacion = {
  mes: string;
  /** Lo facturado en los 12 meses anteriores a esa ventana. */
  acumulado: number;
  /** `null` si se pasa del tope de la categoría más alta. */
  corresponde: CategoriaMonotributo | null;
};

const mesDe = (iso: string) => iso.slice(0, 7);

function sumarMeses(mes: string, cantidad: number) {
  const [anio = 0, numero = 1] = mes.split('-').map(Number);
  const total = anio * 12 + (numero - 1) + cantidad;
  return `${Math.floor(total / 12)}-${String((total % 12) + 1).padStart(2, '0')}`;
}

/** «2026-07» como «julio de 2026». */
export function nombreDelMes(mes: string) {
  const [anio = 0, numero = 1] = mes.split('-').map(Number);
  return new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric' }).format(
    new Date(anio, numero - 1, 15),
  );
}

/** La primera categoría cuyo tope alcanza para lo facturado; `null` si se pasa de todas. */
export const categoriaPara = (facturado: number, categorias = CATEGORIAS_DE_EJEMPLO) =>
  categorias.find((categoria) => categoria.tope >= facturado) ?? null;

export function estadoMonotributo(
  datos: DatosDelNegocio,
  hoy: string,
  letra = CATEGORIA_ACTUAL,
  categorias = CATEGORIAS_DE_EJEMPLO,
): EstadoMonotributo {
  const categoria =
    categorias.find((item) => item.letra === letra) ?? (categorias[0] as CategoriaMonotributo);
  const mesActual = mesDe(hoy);

  // Los tres últimos «meses» son tramos de 30 días del cuaderno; los nueve anteriores, las facturas viejas.
  const tramo = (desde: number) =>
    datos.ventas
      .filter((venta) => {
        const atras = diasEntre(venta.fecha, hoy);
        return atras >= desde && atras < desde + 30;
      })
      .reduce((total, venta) => total + venta.precio * venta.cantidad, 0);
  const delCuaderno = [tramo(60), tramo(30), tramo(0)];
  const importes = [...FACTURADO_ANTES_DEL_CUADERNO.slice(-9), ...delCuaderno];
  const meses = importes.map((importe, indice) => ({
    mes: sumarMeses(mesActual, indice - (importes.length - 1)),
    importe,
    delCuaderno: indice >= importes.length - 3,
  }));
  const total12 = importes.reduce((total, importe) => total + importe, 0);
  const ritmoMensual = delCuaderno.reduce((total, importe) => total + importe, 0) / 3;

  // Hacia adelante, cada mes suma el ritmo actual y sale el mes más viejo.
  const ventana = [...importes];
  const proyeccion: MesProyectado[] = [];
  for (let paso = 1; paso <= 12; paso++) {
    ventana.shift();
    ventana.push(ritmoMensual);
    const acumulado12 = ventana.reduce((total, importe) => total + importe, 0);
    proyeccion.push({
      mes: sumarMeses(mesActual, paso),
      acumulado12,
      superaElTope: acumulado12 > categoria.tope,
    });
  }

  // La recategorización es en enero y en julio, con lo facturado en los 12 meses anteriores. Si la ventana es el
  // mes que viene, cuenta lo facturado hasta hoy.
  const recategorizaciones = proyeccion
    .filter((item) => item.mes.endsWith('-01') || item.mes.endsWith('-07'))
    .map((item): Recategorizacion => {
      const anterior = sumarMeses(item.mes, -1);
      const acumulado = proyeccion.find((otro) => otro.mes === anterior)?.acumulado12 ?? total12;
      return { mes: item.mes, acumulado, corresponde: categoriaPara(acumulado, categorias) };
    });

  const diaDeHoy = Number(hoy.slice(8, 10));
  const mesDelVencimiento = diaDeHoy <= VENCIMIENTO_CUOTA ? mesActual : sumarMeses(mesActual, 1);
  const fecha = `${mesDelVencimiento}-${String(VENCIMIENTO_CUOTA).padStart(2, '0')}`;

  return {
    categoria,
    meses,
    total12,
    usado: total12 / categoria.tope,
    ritmoMensual,
    proyeccion,
    superaEn: proyeccion.find((item) => item.superaElTope)?.mes ?? null,
    recategorizaciones,
    vencimiento: { fecha, dias: diasEntre(hoy, fecha) },
  };
}

export type FacturaC = {
  numero: string;
  cae: string;
  venceCae: string;
  cliente: string;
  total: number;
};

/**
 * Una factura C simulada. En el producto se pide el CAE al servicio web de facturación de ARCA con el certificado
 * del comercio; acá sólo se arma un número con el formato real para mostrar cómo quedaría.
 */
export function facturaSimulada(
  cliente: string,
  total: number,
  hoy: string,
  numero: number,
): FacturaC {
  const vence = new Date(`${hoy}T12:00:00`);
  vence.setDate(vence.getDate() + 10);
  const iso = `${vence.getFullYear()}-${String(vence.getMonth() + 1).padStart(2, '0')}-${String(vence.getDate()).padStart(2, '0')}`;
  return {
    numero: `00003-${String(numero).padStart(8, '0')}`,
    cae: `7${String(4512036 + numero).padStart(7, '0')}${hoy.replace(/-/g, '').slice(2)}`,
    venceCae: iso,
    cliente,
    total,
  };
}
