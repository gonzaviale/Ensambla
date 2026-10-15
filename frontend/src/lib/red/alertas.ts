import type { Alerta, RedState } from '@/types/red';
import {
  cantidadSugeridaDePedido,
  diasDeStock,
  diasEntre,
  margenSobreCosto,
  precioSegunRegla,
  reglaDe,
  resumenDeLista,
  variacionPct,
} from './calculos';
import { nombreDelDia, plural, pesos, porcentaje } from './formato';
import { FUNCION_DE_ALERTA, planIncluye, type PlanEfectivo } from './planes';

// Los avisos no se guardan: se derivan de los datos. Así, al aplicar un precio, el aviso desaparece
// solo, y un mismo problema siempre produce el mismo id (estable para «ignorar»).

export type DatosAlertas = Pick<
  RedState,
  'productos' | 'proveedores' | 'listas' | 'reglas' | 'umbrales' | 'ofertas' | 'zona'
>;

export function derivarAlertas(estado: DatosAlertas, ahora: Date = new Date()): Alerta[] {
  const { productos, proveedores, listas, reglas, umbrales, ofertas, zona } = estado;
  const proveedor = (id: string) => proveedores.find((p) => p.id === id)?.nombre ?? 'Tu proveedor';
  const alertas: Alerta[] = [];

  // Precio en riesgo: el costo subió y el margen quedó por debajo del mínimo del comercio.
  for (const producto of productos) {
    const margen = margenSobreCosto(producto.costo, producto.precio);
    if (margen >= reglas.margenMinimo) continue;
    const sugerido = precioSegunRegla(producto.costo, reglaDe(reglas, producto.categoria));
    alertas.push({
      id: `riesgo:${producto.id}`,
      tipo: 'precio-en-riesgo',
      titulo: `${producto.nombre} quedó con ${porcentaje(margen)} de ganancia. ¿Lo paso a ${pesos(sugerido)}?`,
      porque: `El costo es ${pesos(producto.costo)} y lo vendés a ${pesos(producto.precio)}; tu mínimo es ${porcentaje(reglas.margenMinimo)}.`,
      impacto: Math.max(0, sugerido - producto.precio) * producto.ventasSemana * 4,
      productoId: producto.id,
      proveedorId: producto.proveedorId,
      accion: { tipo: 'aplicar-precio', productoId: producto.id, precioNuevo: sugerido },
      etiquetaAccion: 'Aplicar',
    });
  }

  // Lista nueva y cambios raros: una lista por revisar con productos que vendés.
  for (const lista of listas.filter((l) => l.estado === 'por-revisar')) {
    const resumen = resumenDeLista(lista, productos, reglas, umbrales);
    const total = lista.items.filter((i) => !i.ignorado && i.productoId).length;
    if (total > 0) {
      const cambiosQueSuben = resumen.cambios.filter((c) => c.variacion > 0);
      const impacto = cambiosQueSuben.reduce((suma, c) => {
        const producto = productos.find((p) => p.id === c.productoId);
        return (
          suma + Math.max(0, c.precioSugerido - c.precioActual) * (producto?.ventasSemana ?? 0) * 4
        );
      }, 0);
      alertas.push({
        id: `lista:${lista.id}`,
        tipo: 'lista-nueva',
        titulo: `Llegó la lista de ${proveedor(lista.proveedorId)}: ${plural(resumen.suben, 'producto subió', 'productos subieron')}${resumen.suben ? `, ${porcentaje(resumen.aumentoPromedio, { decimales: 1 })} en promedio` : ''}.`,
        porque: `Tus reglas ya calcularon los precios nuevos. ${resumen.pendientes ? `Hay ${plural(resumen.pendientes, 'línea', 'líneas')} para revisar.` : 'No hay dudas para revisar.'}`,
        impacto,
        productoId: null,
        proveedorId: lista.proveedorId,
        accion: { tipo: 'revisar-lista', listaId: lista.id },
        etiquetaAccion: 'Revisar y aplicar',
      });
    }
    for (const item of lista.items) {
      if (item.ignorado || !item.productoId) continue;
      const producto = productos.find((p) => p.id === item.productoId);
      if (!producto) continue;
      const cambio = resumen.cambios.find((c) => c.itemId === item.id);
      const variacion = cambio
        ? cambio.variacion
        : variacionPct(producto.costo, item.precioLista / (item.unidadesPorBulto ?? 1));
      if (Math.abs(variacion) < umbrales.aumentoRaro || item.confirmado) continue;
      alertas.push({
        id: `raro:${lista.id}:${item.id}`,
        tipo: 'cambio-raro',
        titulo: `${producto.nombre} ${variacion >= 0 ? 'subió' : 'bajó'} ${porcentaje(Math.abs(variacion))} de golpe. ¿Error de la lista?`,
        porque: `Lo normal en ${proveedor(lista.proveedorId)} es menos de ${porcentaje(umbrales.aumentoRaro)} por lista.`,
        impacto: Math.abs(producto.costo * (variacion / 100)) * producto.ventasSemana * 4,
        productoId: producto.id,
        proveedorId: lista.proveedorId,
        accion: { tipo: 'confirmar-cambio', listaId: lista.id, itemId: item.id },
        etiquetaAccion: 'Confirmar o ignorar',
      });
    }
  }

  // Stock por agotarse: el ritmo de ventas indica que se termina antes de la próxima compra.
  for (const producto of productos) {
    const dias = diasDeStock(producto);
    if (dias >= umbrales.diasDeStock) continue;
    const cuando =
      dias < 1
        ? 'hoy'
        : dias < 2
          ? 'mañana'
          : `el ${nombreDelDia(new Date(ahora.getTime() + dias * 24 * 60 * 60 * 1000))}`;
    alertas.push({
      id: `stock:${producto.id}`,
      tipo: 'stock-por-agotarse',
      titulo: `Al ritmo de esta semana, ${producto.nombre} se termina ${cuando}.`,
      porque: `Quedan ${producto.stock} y vendés ${producto.ventasSemana} por semana.`,
      impacto: (producto.precio - producto.costo) * producto.ventasSemana * 2,
      productoId: producto.id,
      proveedorId: producto.proveedorId,
      accion: {
        tipo: 'agregar-al-pedido',
        productoId: producto.id,
        cantidad: cantidadSugeridaDePedido(producto),
      },
      etiquetaAccion: 'Agregar al pedido',
    });
  }

  // Oferta que sirve: un proveedor publicó algo que el comercio realmente vende.
  for (const oferta of ofertas) {
    if (!oferta.productoId || new Date(oferta.vence).getTime() < ahora.getTime()) continue;
    const producto = productos.find((p) => p.id === oferta.productoId);
    if (!producto) continue;
    const descuento = -variacionPct(oferta.costoHabitual, oferta.costoOferta);
    alertas.push({
      id: `oferta:${oferta.id}`,
      tipo: 'oferta-que-sirve',
      titulo: `${proveedor(oferta.proveedorId)} tiene ${producto.nombre} ${porcentaje(descuento)} más barato hasta el ${nombreDelDia(oferta.vence)}.`,
      porque: `Costo de oferta ${pesos(oferta.costoOferta)} contra ${pesos(oferta.costoHabitual)} habitual. Vendés ${producto.ventasSemana} por semana.`,
      impacto: (oferta.costoHabitual - oferta.costoOferta) * producto.ventasSemana * 2,
      productoId: producto.id,
      proveedorId: oferta.proveedorId,
      accion: { tipo: 'ver-oferta', ofertaId: oferta.id },
      etiquetaAccion: 'Ver oferta',
    });
  }

  // Comprás caro: el comercio paga bastante más que el promedio de su zona (sólo con muestra suficiente).
  for (const referencia of zona) {
    if (referencia.comercios < umbrales.minimoComerciosZona) continue;
    const producto = productos.find((p) => p.id === referencia.productoId);
    if (!producto) continue;
    const diferencia = variacionPct(referencia.costoPromedio, producto.costo);
    if (diferencia < umbrales.diferenciaCompra) continue;
    alertas.push({
      id: `caro:${producto.id}`,
      tipo: 'compras-caro',
      titulo: `${producto.nombre} te sale ${porcentaje(diferencia)} más que a otros comercios de tu zona.`,
      porque: `Pagás ${pesos(producto.costo)}; el promedio de ${referencia.comercios} comercios de la zona es ${pesos(referencia.costoPromedio)}.`,
      impacto: (producto.costo - referencia.costoPromedio) * producto.ventasSemana * 4,
      productoId: producto.id,
      proveedorId: producto.proveedorId,
      accion: { tipo: 'ver-proveedores', productoId: producto.id },
      etiquetaAccion: 'Ver proveedores',
    });
  }

  // Mejor proveedor: otro proveedor de la red tiene el mismo producto a menor costo.
  for (const producto of productos) {
    const mejor = [...producto.alternativas].sort((a, b) => a.costo - b.costo)[0];
    if (!mejor) continue;
    const ahorro = -variacionPct(producto.costo, mejor.costo);
    if (ahorro < umbrales.diferenciaCompra) continue;
    alertas.push({
      id: `mejor:${producto.id}:${mejor.proveedorId}`,
      tipo: 'mejor-proveedor',
      titulo: `${producto.nombre} está ${pesos(producto.costo - mejor.costo)} más barato en ${proveedor(mejor.proveedorId)}.`,
      porque: `Hoy le comprás a ${proveedor(producto.proveedorId)} a ${pesos(producto.costo)}; ${proveedor(mejor.proveedorId)} lo tiene a ${pesos(mejor.costo)}.`,
      impacto: (producto.costo - mejor.costo) * producto.ventasSemana * 4,
      productoId: producto.id,
      proveedorId: mejor.proveedorId,
      accion: { tipo: 'comparar', productoId: producto.id },
      etiquetaAccion: 'Comparar',
    });
  }

  return alertas.sort((a, b) => b.impacto - a.impacto);
}

export type AlertasVisibles = {
  /** Lo que el comercio ve y puede accionar. */
  activas: Alerta[];
  /** Avisos de funciones que el plan actual no incluye (se muestran con candado). */
  bloqueadas: Alerta[];
  ignoradas: Alerta[];
};

/** Aplica lo que decidió el comercio (umbrales, ignorados) y lo que incluye su plan. */
export function alertasVisibles(
  estado: DatosAlertas & Pick<RedState, 'alertas'>,
  plan: Pick<PlanEfectivo, 'plan'>,
  ahora: Date = new Date(),
): AlertasVisibles {
  const activas: Alerta[] = [];
  const bloqueadas: Alerta[] = [];
  const ignoradas: Alerta[] = [];
  for (const alerta of derivarAlertas(estado, ahora)) {
    if (!estado.umbrales.activas[alerta.tipo]) continue;
    const decision = estado.alertas[alerta.id];
    if (decision === 'resuelta') continue;
    if (decision === 'ignorada') {
      ignoradas.push(alerta);
      continue;
    }
    if (planIncluye(plan.plan, FUNCION_DE_ALERTA[alerta.tipo])) activas.push(alerta);
    else bloqueadas.push(alerta);
  }
  return { activas, bloqueadas, ignoradas };
}

/** Días enteros desde una fecha (para «hace 3 días»). */
export const diasDesde = (iso: string, ahora: Date = new Date()) =>
  Math.max(0, Math.floor(diasEntre(iso, ahora)));
