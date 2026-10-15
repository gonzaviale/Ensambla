import type {
  CambioAplicado,
  ItemLista,
  ListaProveedor,
  MedioPago,
  NegocioRed,
  OrigenLista,
  PeriodoPlan,
  PlanId,
  ProductoRed,
  RedState,
  ReglasPrecio,
  Umbrales,
} from '@/types/red';
import { costoDeItem, proponerCambios, sumarDias, variacionPct } from './calculos';
import { DIAS_DE_PRUEBA, PLANES, importeACobrar } from './planes';

// Transiciones puras del estado de la demostración: (estado, datos, ahora) → estado nuevo.
// El store (stores/red.ts) sólo las aplica; así se pueden probar sin React.

let secuencia = 0;
const nuevoId = (prefijo: string, ahora: Date) =>
  `${prefijo}-${ahora.getTime().toString(36)}${(secuencia++).toString(36)}`;

/* ---------- precios ---------- */

function registrarCambio(
  productosActuales: ProductoRed[],
  producto: ProductoRed,
  precioNuevo: number,
  costoNuevo: number,
  listaId: string | null,
  ahora: Date,
): { productos: ProductoRed[]; cambio: CambioAplicado | null } {
  const productos = productosActuales.map((p) =>
    p.id !== producto.id
      ? p
      : {
          ...p,
          costo: costoNuevo,
          precio: precioNuevo,
          costoDesde: costoNuevo !== p.costo ? ahora.toISOString() : p.costoDesde,
          costoListaId: costoNuevo !== p.costo ? listaId : p.costoListaId,
          historial:
            costoNuevo !== p.costo
              ? [
                  ...p.historial,
                  {
                    fecha: ahora.toISOString(),
                    costo: costoNuevo,
                    listaId,
                    proveedorId: p.proveedorId,
                  },
                ]
              : p.historial,
        },
  );
  const cambio: CambioAplicado | null =
    precioNuevo !== producto.precio || costoNuevo !== producto.costo
      ? {
          id: nuevoId('cambio', ahora),
          productoId: producto.id,
          nombre: producto.nombre,
          precioAnterior: producto.precio,
          precioNuevo,
          costoAnterior: producto.costo,
          costoNuevo,
          fecha: ahora.toISOString(),
          listaId,
          trasladado: false,
        }
      : null;
  return { productos, cambio };
}

/** Aplica un precio nuevo a un producto (un toque desde un aviso o desde «Mis precios»). */
export function aplicarPrecio(
  estado: RedState,
  productoId: string,
  precioNuevo: number,
  ahora: Date = new Date(),
): RedState {
  const producto = estado.productos.find((p) => p.id === productoId);
  if (!producto || precioNuevo <= 0) return estado;
  const { productos, cambio } = registrarCambio(
    estado.productos,
    producto,
    precioNuevo,
    producto.costo,
    null,
    ahora,
  );
  return { ...estado, productos, cambios: cambio ? [cambio, ...estado.cambios] : estado.cambios };
}

/* ---------- listas ---------- */

function mapearLista(
  estado: RedState,
  listaId: string,
  fn: (lista: ListaProveedor) => ListaProveedor,
): RedState {
  return { ...estado, listas: estado.listas.map((l) => (l.id === listaId ? fn(l) : l)) };
}

function mapearItem(
  estado: RedState,
  listaId: string,
  itemId: string,
  fn: (item: ItemLista) => ItemLista,
): RedState {
  return mapearLista(estado, listaId, (lista) => ({
    ...lista,
    items: lista.items.map((item) => (item.id === itemId ? fn(item) : item)),
  }));
}

export type DecisionItem = {
  /** null = «no es ninguno de mis productos». */
  productoId: string | null;
  presentacion?: ItemLista['presentacion'];
  unidadesPorBulto?: number | null;
};

/** El comercio confirma o corrige cómo se emparejó una línea (y si es por unidad o por bulto). */
export function confirmarItem(
  estado: RedState,
  listaId: string,
  itemId: string,
  decision: DecisionItem,
): RedState {
  return mapearItem(estado, listaId, itemId, (item) => ({
    ...item,
    productoId: decision.productoId,
    presentacion: decision.presentacion ?? item.presentacion,
    unidadesPorBulto:
      decision.unidadesPorBulto === undefined ? item.unidadesPorBulto : decision.unidadesPorBulto,
    estado: decision.productoId ? 'seguro' : 'sin-coincidencia',
    confirmado: decision.productoId !== null,
    ignorado: decision.productoId === null ? item.ignorado : false,
  }));
}

export const ignorarItem = (
  estado: RedState,
  listaId: string,
  itemId: string,
  ignorado = true,
): RedState => mapearItem(estado, listaId, itemId, (item) => ({ ...item, ignorado }));

/**
 * Aplica la lista: actualiza costos y precios de los ítems listos. `precios` trae, por itemId, el
 * precio final elegido por el comercio (si falta, se usa el sugerido por sus reglas).
 * Nada se aplica sin que el comercio lo pida: esta función es la confirmación.
 */
export function aplicarLista(
  estado: RedState,
  listaId: string,
  precios: Record<string, number> = {},
  ahora: Date = new Date(),
): RedState {
  const lista = estado.listas.find((l) => l.id === listaId);
  if (!lista || lista.estado !== 'por-revisar') return estado;
  const propuestos = proponerCambios(lista, estado.productos, estado.reglas, estado.umbrales);
  let siguiente = estado;
  const cambios: CambioAplicado[] = [];
  for (const propuesto of propuestos) {
    const producto = siguiente.productos.find((p) => p.id === propuesto.productoId);
    if (!producto) continue;
    const precioFinal = precios[propuesto.itemId] ?? propuesto.precioSugerido;
    const { productos, cambio } = registrarCambio(
      siguiente.productos,
      producto,
      precioFinal,
      propuesto.costoNuevo,
      listaId,
      ahora,
    );
    siguiente = { ...siguiente, productos };
    if (cambio) cambios.push(cambio);
  }
  const aplicados = propuestos.length;
  const aumentos = propuestos.filter((p) => p.variacion > 0.05);
  const promedio = aumentos.length
    ? aumentos.reduce((suma, p) => suma + p.variacion, 0) / aumentos.length
    : 0;
  return {
    ...siguiente,
    cambios: [...cambios, ...siguiente.cambios],
    listas: siguiente.listas.map((l) =>
      l.id === listaId
        ? {
            ...l,
            estado: 'aplicada',
            aplicadaEl: ahora.toISOString(),
            resumen: { productos: aplicados, aumentoPromedio: promedio },
          }
        : l,
    ),
  };
}

export const descartarLista = (estado: RedState, listaId: string): RedState =>
  mapearLista(estado, listaId, (lista) => ({ ...lista, estado: 'descartada' }));

const NOMBRE_ORIGEN: Record<OrigenLista, string> = {
  whatsapp: 'Lista reenviada por WhatsApp',
  pdf: 'Lista en PDF',
  excel: 'Planilla de precios',
  foto: 'Foto de la lista',
};

/**
 * Simula que el comercio sube la lista de un proveedor: genera una lista nueva «por revisar» con
 * los productos de ese proveedor y variaciones de costo repetibles. En el producto real la lectura
 * la hace la IA (docs/lectura-cuaderno-ia.md); acá es una demostración.
 */
export function subirLista(
  estado: RedState,
  proveedorId: string,
  origen: OrigenLista,
  ahora: Date = new Date(),
): { estado: RedState; listaId: string | null } {
  const proveedor = estado.proveedores.find((p) => p.id === proveedorId);
  if (!proveedor) return { estado, listaId: null };
  const productos = estado.productos.filter((p) => p.proveedorId === proveedorId);
  if (productos.length === 0) return { estado, listaId: null };
  const listaId = nuevoId('lista', ahora);
  const variaciones = [3.2, 5.8, 1.5, 4.4, 9, 2.1, 6.3, 0, -1.2, 3.8];
  const items: ItemLista[] = productos.map((producto, indice) => {
    const factor = 1 + (variaciones[indice % variaciones.length] ?? 0) / 100;
    const costoUnidad = Math.round(producto.costo * factor);
    const porBulto = producto.unidadesPorBulto;
    return {
      id: `${listaId}-${indice + 1}`,
      textoOriginal: `${producto.nombre.toUpperCase()}${porBulto ? ` X${porBulto}` : ''}`,
      precioLista: porBulto ? costoUnidad * porBulto : costoUnidad,
      presentacion: porBulto ? 'BULTO' : 'UNIDAD',
      unidadesPorBulto: porBulto,
      productoId: producto.id,
      confianza: 0.95,
      estado: 'seguro',
      motivo: null,
      sugeridos: [],
      confirmado: false,
      ignorado: false,
    };
  });
  const lista: ListaProveedor = {
    id: listaId,
    proveedorId,
    nombre: `${NOMBRE_ORIGEN[origen]} · ${proveedor.nombre}`,
    recibidaEl: ahora.toISOString(),
    origen,
    estado: 'por-revisar',
    aplicadaEl: null,
    items,
    resumen: null,
  };
  return { estado: { ...estado, listas: [lista, ...estado.listas] }, listaId };
}

/* ---------- ofertas del lado del mayorista ---------- */

export type DatosOferta = {
  proveedorId: string;
  productoId: string;
  costoOferta: number;
  /** Días de vigencia desde hoy. */
  dias: number;
  minimoBultos: number | null;
};

/**
 * Un mayorista publica una oferta: llega sólo a los comercios que venden ese producto (aparece como
 * aviso «oferta que sirve»). No puede esconder una mejor oferta de otro proveedor (neutralidad).
 */
export function publicarOferta(
  estado: RedState,
  datos: DatosOferta,
  ahora: Date = new Date(),
): RedState {
  const producto = estado.productos.find((p) => p.id === datos.productoId);
  if (!producto || datos.costoOferta <= 0 || datos.costoOferta >= producto.costo) return estado;
  const oferta = {
    id: nuevoId('oferta', ahora),
    proveedorId: datos.proveedorId,
    productoId: producto.id,
    nombreProducto: producto.nombre,
    costoHabitual: producto.costo,
    costoOferta: datos.costoOferta,
    vence: sumarDias(ahora, datos.dias),
    minimoBultos: datos.minimoBultos,
  };
  return { ...estado, ofertas: [oferta, ...estado.ofertas] };
}

/* ---------- cambios en el mostrador ---------- */

export const marcarTrasladado = (
  estado: RedState,
  cambioId: string,
  trasladado = true,
): RedState => ({
  ...estado,
  cambios: estado.cambios.map((c) => (c.id === cambioId ? { ...c, trasladado } : c)),
});

export const marcarTodosTrasladados = (estado: RedState): RedState => ({
  ...estado,
  cambios: estado.cambios.map((c) => ({ ...c, trasladado: true })),
});

/* ---------- avisos, reglas, pedido, negocio ---------- */

export const resolverAlerta = (estado: RedState, id: string): RedState => ({
  ...estado,
  alertas: { ...estado.alertas, [id]: 'resuelta' },
});

export const ignorarAlerta = (estado: RedState, id: string): RedState => ({
  ...estado,
  alertas: { ...estado.alertas, [id]: 'ignorada' },
});

export const restaurarAlertas = (estado: RedState): RedState => ({ ...estado, alertas: {} });

export const guardarReglas = (estado: RedState, reglas: ReglasPrecio): RedState => ({
  ...estado,
  reglas,
});

export const guardarUmbrales = (estado: RedState, umbrales: Umbrales): RedState => ({
  ...estado,
  umbrales,
});

export function agregarAlPedido(estado: RedState, productoId: string, cantidad: number): RedState {
  if (cantidad <= 0) return estado;
  const existe = estado.pedido.some((i) => i.productoId === productoId);
  return {
    ...estado,
    pedido: existe
      ? estado.pedido.map((i) => (i.productoId === productoId ? { ...i, cantidad } : i))
      : [...estado.pedido, { productoId, cantidad }],
  };
}

export const quitarDelPedido = (estado: RedState, productoId: string): RedState => ({
  ...estado,
  pedido: estado.pedido.filter((i) => i.productoId !== productoId),
});

export const vaciarPedido = (estado: RedState): RedState => ({ ...estado, pedido: [] });

export const guardarNegocio = (estado: RedState, datos: Partial<NegocioRed>): RedState => ({
  ...estado,
  negocio: { ...estado.negocio, ...datos },
});

/* ---------- suscripción (T-066 a T-070) ---------- */

/** Prueba de 7 días del plan más completo. Se puede usar una sola vez. */
export function iniciarPrueba(estado: RedState, ahora: Date = new Date()): RedState {
  const { suscripcion } = estado;
  if (suscripcion.pruebaUsada || suscripcion.estado === 'activa') return estado;
  return {
    ...estado,
    suscripcion: {
      ...suscripcion,
      plan: 'red',
      estado: 'prueba',
      pruebaUsada: true,
      pruebaDesde: ahora.toISOString(),
      pruebaHasta: sumarDias(ahora, DIAS_DE_PRUEBA),
    },
  };
}

export type DatosSuscripcion = {
  plan: PlanId;
  periodo: PeriodoPlan;
  medioPago: MedioPago;
};

/** Pago aprobado: activa el plan. Los datos de pago son simulados, nunca reales. */
export function suscribirse(
  estado: RedState,
  datos: DatosSuscripcion,
  ahora: Date = new Date(),
): RedState {
  if (datos.plan === 'gratis') return estado;
  const importe = importeACobrar(datos.plan, datos.periodo);
  const dias = datos.periodo === 'anual' ? 365 : 30;
  return {
    ...estado,
    suscripcion: {
      ...estado.suscripcion,
      plan: datos.plan,
      estado: 'activa',
      periodo: datos.periodo,
      medioPago: datos.medioPago,
      proximoCobro: sumarDias(ahora, dias),
      activoHasta: null,
      pagos: [
        {
          id: nuevoId('pago', ahora),
          fecha: ahora.toISOString(),
          concepto: `Plan ${PLANES[datos.plan].nombre} · ${datos.periodo === 'anual' ? 'anual' : 'mensual'}`,
          importe,
          estado: 'aprobado',
        },
        ...estado.suscripcion.pagos,
      ],
    },
  };
}

/** Cancelar no corta de golpe: lo ya pagado sigue activo hasta el próximo cobro. */
export function cancelarSuscripcion(estado: RedState, ahora: Date = new Date()): RedState {
  const { suscripcion } = estado;
  if (suscripcion.estado === 'prueba') {
    return {
      ...estado,
      suscripcion: {
        ...suscripcion,
        plan: 'gratis',
        estado: 'gratis',
        pruebaHasta: ahora.toISOString(),
      },
    };
  }
  if (suscripcion.estado !== 'activa') return estado;
  return {
    ...estado,
    suscripcion: {
      ...suscripcion,
      estado: 'cancelada',
      activoHasta: suscripcion.proximoCobro ?? ahora.toISOString(),
      proximoCobro: null,
    },
  };
}

export function reactivarSuscripcion(estado: RedState): RedState {
  const { suscripcion } = estado;
  if (suscripcion.estado !== 'cancelada' || !suscripcion.activoHasta) return estado;
  return {
    ...estado,
    suscripcion: {
      ...suscripcion,
      estado: 'activa',
      proximoCobro: suscripcion.activoHasta,
      activoHasta: null,
    },
  };
}

/** Sólo para la demostración: adelanta el fin de la prueba para mostrar cómo queda el plan gratis. */
export function simularFinDePrueba(estado: RedState, ahora: Date = new Date()): RedState {
  if (estado.suscripcion.estado !== 'prueba') return estado;
  return {
    ...estado,
    suscripcion: { ...estado.suscripcion, pruebaHasta: sumarDias(ahora, -1) },
  };
}

/** Variación de costo de una línea de lista contra el costo vigente (para ordenar y mostrar). */
export function variacionDeItem(item: ItemLista, productos: ProductoRed[]): number | null {
  const producto = productos.find((p) => p.id === item.productoId);
  return producto ? variacionPct(producto.costo, costoDeItem(item)) : null;
}
