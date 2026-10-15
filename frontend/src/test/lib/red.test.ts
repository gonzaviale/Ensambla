import { describe, expect, it } from 'vitest';
import * as acciones from '@/lib/red/acciones';
import { alertasVisibles, derivarAlertas } from '@/lib/red/alertas';
import {
  costoUnitario,
  margenSobreCosto,
  precioSegunRegla,
  proponerCambios,
  redondearHaciaArriba,
  resumenDeLista,
} from '@/lib/red/calculos';
import { importeACobrar, planEfectivo, planIncluye, precioPorMes } from '@/lib/red/planes';
import { crearEstadoInicial } from '@/lib/red/semilla';
import { listasDelMes, resumenValor } from '@/lib/red/valor';

const AHORA = new Date('2026-10-15T12:00:00Z');
const estado = () => crearEstadoInicial(AHORA);

function requerirLista(base: ReturnType<typeof estado>, id: string) {
  const lista = base.listas.find((l) => l.id === id);
  if (!lista) throw new Error(`falta la lista ${id}`);
  return lista;
}

describe('cálculos de precio', () => {
  it('redondea hacia arriba al múltiplo de la regla', () => {
    expect(redondearHaciaArriba(2859.5, 10)).toBe(2860);
    expect(redondearHaciaArriba(2860, 10)).toBe(2860);
    expect(redondearHaciaArriba(862.5, 50)).toBe(900);
  });

  it('calcula el margen sobre el costo y el precio según la regla', () => {
    expect(margenSobreCosto(2150, 2350)).toBeCloseTo(9.3, 1);
    expect(precioSegunRegla(2210, { margen: 33, redondeo: 10 })).toBe(2940);
  });

  it('divide el precio de un bulto por sus unidades sólo si se lo interpreta por bulto', () => {
    expect(costoUnitario(9480, 'BULTO', 6)).toBe(1580);
    expect(costoUnitario(9480, 'UNIDAD', 6)).toBe(9480);
  });
});

describe('avisos derivados de los datos', () => {
  it('detecta los siete tipos de aviso con los datos de la demostración', () => {
    const tipos = new Set(derivarAlertas(estado(), AHORA).map((a) => a.tipo));
    expect([...tipos].sort()).toEqual(
      [
        'cambio-raro',
        'compras-caro',
        'lista-nueva',
        'mejor-proveedor',
        'oferta-que-sirve',
        'precio-en-riesgo',
        'stock-por-agotarse',
      ].sort(),
    );
  });

  it('el aviso de precio en riesgo desaparece al aplicar el precio sugerido', () => {
    const inicial = estado();
    const aviso = derivarAlertas(inicial, AHORA).find((a) => a.id === 'riesgo:p-aceite');
    if (!aviso) throw new Error('falta el aviso');
    expect(aviso.accion).toMatchObject({ tipo: 'aplicar-precio', productoId: 'p-aceite' });

    const accion = aviso.accion;
    if (accion.tipo !== 'aplicar-precio') throw new Error('acción inesperada');
    const despues = acciones.aplicarPrecio(inicial, accion.productoId, accion.precioNuevo, AHORA);

    expect(derivarAlertas(despues, AHORA).some((a) => a.id === 'riesgo:p-aceite')).toBe(false);
    expect(despues.cambios[0]).toMatchObject({ productoId: 'p-aceite', trasladado: false });
  });

  it('no compara con la zona si hay pocos comercios (privacidad)', () => {
    const avisos = derivarAlertas(estado(), AHORA).filter((a) => a.tipo === 'compras-caro');
    expect(avisos.map((a) => a.productoId)).toEqual(['p-harina']);
  });

  it('con el plan gratis, los avisos de la red quedan bloqueados', () => {
    const gratis = { plan: 'gratis' as const };
    const { activas, bloqueadas } = alertasVisibles(estado(), gratis, AHORA);
    expect(
      activas.every((a) => ['precio-en-riesgo', 'lista-nueva', 'cambio-raro'].includes(a.tipo)),
    ).toBe(true);
    expect(bloqueadas.some((a) => a.tipo === 'mejor-proveedor')).toBe(true);
  });

  it('respeta los avisos que el comercio apagó', () => {
    const base = estado();
    const apagado = {
      ...base,
      umbrales: {
        ...base.umbrales,
        activas: { ...base.umbrales.activas, 'oferta-que-sirve': false },
      },
    };
    const { activas } = alertasVisibles(apagado, { plan: 'red' }, AHORA);
    expect(activas.some((a) => a.tipo === 'oferta-que-sirve')).toBe(false);
  });
});

describe('lista de proveedor', () => {
  it('no calcula los ítems dudosos hasta que el comercio los confirma', () => {
    const base = estado();
    const lista = requerirLista(base, 'lista-sur-oct');
    const antes = proponerCambios(lista, base.productos, base.reglas, base.umbrales);
    expect(antes.some((c) => c.productoId === 'p-sprite')).toBe(false);
    expect(resumenDeLista(lista, base.productos, base.reglas, base.umbrales).pendientes).toBe(3);

    const confirmado = acciones.confirmarItem(base, lista.id, 'lista-sur-oct-6', {
      productoId: 'p-sprite',
      presentacion: 'BULTO',
      unidadesPorBulto: 6,
    });
    const nueva = requerirLista(confirmado, lista.id);
    const despues = proponerCambios(
      nueva,
      confirmado.productos,
      confirmado.reglas,
      confirmado.umbrales,
    );
    expect(despues.find((c) => c.productoId === 'p-sprite')?.costoNuevo).toBe(1580);
  });

  it('aplicar la lista actualiza costos y precios y deja los cambios para el mostrador', () => {
    const base = estado();
    const despues = acciones.aplicarLista(base, 'lista-sur-oct', {}, AHORA);

    const aceite = despues.productos.find((p) => p.id === 'p-aceite');
    if (!aceite) throw new Error('falta el aceite');
    expect(aceite.costo).toBe(2210);
    expect(aceite.precio).toBe(2940);
    expect(aceite.costoListaId).toBe('lista-sur-oct');
    expect(despues.listas.find((l) => l.id === 'lista-sur-oct')?.estado).toBe('aplicada');
    expect(
      despues.cambios.filter((c) => c.listaId === 'lista-sur-oct' && !c.trasladado).length,
    ).toBeGreaterThan(5);
    // Los dudosos sin confirmar no se tocan.
    expect(despues.productos.find((p) => p.id === 'p-sprite')?.costo).toBe(1500);
  });

  it('subir una lista de un proveedor crea una lista por revisar', () => {
    const { estado: despues, listaId } = acciones.subirLista(estado(), 'prov-valle', 'foto', AHORA);
    const lista = despues.listas.find((l) => l.id === listaId);
    expect(lista).toMatchObject({ estado: 'por-revisar', proveedorId: 'prov-valle' });
    expect(lista?.items).toHaveLength(2);
  });
});

describe('valor aportado', () => {
  it('resume precios corregidos, plata protegida y tiempo ahorrado', () => {
    const valor = resumenValor(estado(), AHORA);
    expect(valor.preciosCorregidos).toBeGreaterThan(5);
    expect(valor.bajoCosto).toBeGreaterThanOrEqual(1);
    expect(valor.plataProtegidaMes).toBeGreaterThan(0);
    expect(valor.pendientesDeMostrador).toBe(2);
  });

  it('cuenta las listas del mes para el tope del plan gratis', () => {
    expect(listasDelMes(estado(), AHORA)).toBe(3);
  });
});

describe('planes y suscripción', () => {
  it('la prueba da acceso al plan completo y al vencer vuelve al gratis', () => {
    const base = estado();
    expect(planEfectivo(base.suscripcion, AHORA)).toMatchObject({
      plan: 'red',
      enPrueba: true,
      diasDePrueba: 5,
    });
    const vencida = acciones.simularFinDePrueba(base, AHORA);
    expect(planEfectivo(vencida.suscripcion, AHORA)).toMatchObject({
      plan: 'gratis',
      estado: 'vencida',
    });
  });

  it('suscribirse activa el plan, registra el pago y cancelar conserva lo ya pagado', () => {
    const base = estado();
    const activa = acciones.suscribirse(
      base,
      {
        plan: 'comercio',
        periodo: 'anual',
        medioPago: { tipo: 'mercado-pago', detalle: 'Mercado Pago' },
      },
      AHORA,
    );
    expect(planEfectivo(activa.suscripcion, AHORA)).toMatchObject({
      plan: 'comercio',
      estado: 'activa',
    });
    expect(activa.suscripcion.pagos[0]).toMatchObject({ importe: 99000, estado: 'aprobado' });

    const cancelada = acciones.cancelarSuscripcion(activa, AHORA);
    expect(planEfectivo(cancelada.suscripcion, AHORA)).toMatchObject({
      plan: 'comercio',
      estado: 'cancelada',
    });
    const alFinal = new Date(AHORA.getTime() + 400 * 24 * 60 * 60 * 1000);
    expect(planEfectivo(cancelada.suscripcion, alFinal).plan).toBe('gratis');
  });

  it('la prueba sólo se puede usar una vez', () => {
    const base = estado();
    expect(acciones.iniciarPrueba(base, AHORA)).toBe(base);
  });

  it('el plan anual cobra diez meses de doce', () => {
    expect(importeACobrar('red', 'anual')).toBe(179000);
    expect(precioPorMes('red', 'anual')).toBe(14917);
    expect(planIncluye('comercio', 'ofertas')).toBe(false);
    expect(planIncluye('red', 'ofertas')).toBe(true);
  });
});
