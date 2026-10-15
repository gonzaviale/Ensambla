import { useMemo } from 'react';
import { useShallow } from 'zustand/react/shallow';
import { alertasVisibles } from '@/lib/red/alertas';
import { resumenDeLista } from '@/lib/red/calculos';
import { PLANES, planEfectivo, planIncluye } from '@/lib/red/planes';
import { listasDelMes, resumenValor } from '@/lib/red/valor';
import { useRedStore } from '@/stores/red';
import type { FuncionPlan } from '@/types/red';

// Hooks de la red proactiva: lo único que las pantallas importan para leer y cambiar la demostración.
// Los datos viven en stores/red.ts (en el navegador); no hay llamadas a la API.

export const useNegocio = () => useRedStore((s) => s.negocio);
export const useProductos = () => useRedStore((s) => s.productos);
export const useProveedores = () => useRedStore((s) => s.proveedores);
export const useListas = () => useRedStore((s) => s.listas);
export const useCambios = () => useRedStore((s) => s.cambios);
export const useReglas = () => useRedStore((s) => s.reglas);
export const useUmbrales = () => useRedStore((s) => s.umbrales);
export const useOfertas = () => useRedStore((s) => s.ofertas);
export const useZona = () => useRedStore((s) => s.zona);
export const useBarrio = () => useRedStore((s) => s.barrio);
export const usePedido = () => useRedStore((s) => s.pedido);
export const useSuscripcion = () => useRedStore((s) => s.suscripcion);

export function useProducto(id: string | undefined) {
  const productos = useProductos();
  return useMemo(() => productos.find((p) => p.id === id), [productos, id]);
}

export function useProveedor(id: string | undefined) {
  const proveedores = useProveedores();
  return useMemo(() => proveedores.find((p) => p.id === id), [proveedores, id]);
}

export function useLista(id: string | undefined) {
  const listas = useListas();
  return useMemo(() => listas.find((l) => l.id === id), [listas, id]);
}

/** Resumen de una lista con los cambios que proponen las reglas del comercio. */
export function useResumenLista(id: string | undefined) {
  const lista = useLista(id);
  const productos = useProductos();
  const reglas = useReglas();
  const umbrales = useUmbrales();
  return useMemo(
    () => (lista ? resumenDeLista(lista, productos, reglas, umbrales) : undefined),
    [lista, productos, reglas, umbrales],
  );
}

/** Plan que rige hoy, estado de la suscripción y qué incluye (con candados para el resto). */
export function usePlanActual() {
  const suscripcion = useSuscripcion();
  const listas = useListas();
  return useMemo(() => {
    const ahora = new Date();
    const efectivo = planEfectivo(suscripcion, ahora);
    const definicion = PLANES[efectivo.plan];
    const usadas = listasDelMes({ listas }, ahora);
    return {
      ...efectivo,
      definicion,
      suscripcion,
      listasUsadasMes: usadas,
      limiteListas: definicion.listasPorMes,
      /** Llegó al tope de listas del mes del plan gratis. */
      sinListasEsteMes: definicion.listasPorMes !== null && usadas >= definicion.listasPorMes,
      incluye: (funcion: FuncionPlan) => planIncluye(efectivo.plan, funcion),
    };
  }, [suscripcion, listas]);
}

/** Avisos derivados de los datos: activos, bloqueados por el plan e ignorados. */
export function useAlertas() {
  const productos = useProductos();
  const proveedores = useProveedores();
  const listas = useListas();
  const reglas = useReglas();
  const umbrales = useUmbrales();
  const ofertas = useOfertas();
  const zona = useZona();
  const alertas = useRedStore((s) => s.alertas);
  const plan = usePlanActual();
  return useMemo(
    () =>
      alertasVisibles(
        { productos, proveedores, listas, reglas, umbrales, ofertas, zona, alertas },
        plan,
      ),
    [productos, proveedores, listas, reglas, umbrales, ofertas, zona, alertas, plan],
  );
}

/** «El valor que aporta»: precios corregidos, plata protegida y tiempo ahorrado. */
export function useValor() {
  const productos = useProductos();
  const listas = useListas();
  const cambios = useCambios();
  return useMemo(() => resumenValor({ productos, listas, cambios }), [productos, listas, cambios]);
}

/** Todas las acciones que cambian la demostración (referencias estables). */
export function useAccionesRed() {
  return useRedStore(
    useShallow((s) => ({
      reiniciarDemo: s.reiniciarDemo,
      aplicarPrecio: s.aplicarPrecio,
      confirmarItem: s.confirmarItem,
      ignorarItem: s.ignorarItem,
      aplicarLista: s.aplicarLista,
      descartarLista: s.descartarLista,
      subirLista: s.subirLista,
      publicarOferta: s.publicarOferta,
      marcarTrasladado: s.marcarTrasladado,
      marcarTodosTrasladados: s.marcarTodosTrasladados,
      resolverAlerta: s.resolverAlerta,
      ignorarAlerta: s.ignorarAlerta,
      restaurarAlertas: s.restaurarAlertas,
      guardarReglas: s.guardarReglas,
      guardarUmbrales: s.guardarUmbrales,
      agregarAlPedido: s.agregarAlPedido,
      quitarDelPedido: s.quitarDelPedido,
      vaciarPedido: s.vaciarPedido,
      guardarNegocio: s.guardarNegocio,
      iniciarPrueba: s.iniciarPrueba,
      suscribirse: s.suscribirse,
      cancelarSuscripcion: s.cancelarSuscripcion,
      reactivarSuscripcion: s.reactivarSuscripcion,
      simularFinDePrueba: s.simularFinDePrueba,
    })),
  );
}
