import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import * as acciones from '@/lib/red/acciones';
import { crearEstadoInicial } from '@/lib/red/semilla';
import type {
  ItemLista,
  NegocioRed,
  OrigenLista,
  RedState,
  ReglasPrecio,
  Umbrales,
} from '@/types/red';

// Estado de la demostración de la red proactiva. Vive en el navegador (localStorage): no hay API.
// Cada acción delega en una transición pura de lib/red/acciones.ts.

type Acciones = {
  reiniciarDemo: () => void;
  aplicarPrecio: (productoId: string, precioNuevo: number) => void;
  confirmarItem: (
    listaId: string,
    itemId: string,
    decision: {
      productoId: string | null;
      presentacion?: ItemLista['presentacion'];
      unidadesPorBulto?: number | null;
    },
  ) => void;
  ignorarItem: (listaId: string, itemId: string, ignorado?: boolean) => void;
  /** `precios`: precio final elegido por itemId; si falta, se usa el que calculan las reglas. */
  aplicarLista: (listaId: string, precios?: Record<string, number>) => void;
  descartarLista: (listaId: string) => void;
  /** Devuelve el id de la lista creada, o null si el proveedor no tiene productos. */
  subirLista: (proveedorId: string, origen: OrigenLista) => string | null;
  /** Un mayorista publica una oferta de un producto (vista del mayorista, demostración). */
  publicarOferta: (datos: acciones.DatosOferta) => void;
  marcarTrasladado: (cambioId: string, trasladado?: boolean) => void;
  marcarTodosTrasladados: () => void;
  resolverAlerta: (id: string) => void;
  ignorarAlerta: (id: string) => void;
  restaurarAlertas: () => void;
  guardarReglas: (reglas: ReglasPrecio) => void;
  guardarUmbrales: (umbrales: Umbrales) => void;
  agregarAlPedido: (productoId: string, cantidad: number) => void;
  quitarDelPedido: (productoId: string) => void;
  vaciarPedido: () => void;
  guardarNegocio: (datos: Partial<NegocioRed>) => void;
  iniciarPrueba: () => void;
  suscribirse: (datos: acciones.DatosSuscripcion) => void;
  cancelarSuscripcion: () => void;
  reactivarSuscripcion: () => void;
  simularFinDePrueba: () => void;
};

export type RedStore = RedState & Acciones;

const CLAVE = 'ensambla-red-demo-v1';

export const useRedStore = create<RedStore>()(
  persist(
    (set, get) => ({
      ...crearEstadoInicial(),
      reiniciarDemo: () => set(crearEstadoInicial()),
      aplicarPrecio: (productoId, precioNuevo) =>
        set((s) => acciones.aplicarPrecio(s, productoId, precioNuevo)),
      confirmarItem: (listaId, itemId, decision) =>
        set((s) => acciones.confirmarItem(s, listaId, itemId, decision)),
      ignorarItem: (listaId, itemId, ignorado) =>
        set((s) => acciones.ignorarItem(s, listaId, itemId, ignorado)),
      aplicarLista: (listaId, precios) => set((s) => acciones.aplicarLista(s, listaId, precios)),
      descartarLista: (listaId) => set((s) => acciones.descartarLista(s, listaId)),
      subirLista: (proveedorId, origen) => {
        const { estado, listaId } = acciones.subirLista(get(), proveedorId, origen);
        if (listaId) set(estado);
        return listaId;
      },
      publicarOferta: (datos) => set((s) => acciones.publicarOferta(s, datos)),
      marcarTrasladado: (cambioId, trasladado) =>
        set((s) => acciones.marcarTrasladado(s, cambioId, trasladado)),
      marcarTodosTrasladados: () => set((s) => acciones.marcarTodosTrasladados(s)),
      resolverAlerta: (id) => set((s) => acciones.resolverAlerta(s, id)),
      ignorarAlerta: (id) => set((s) => acciones.ignorarAlerta(s, id)),
      restaurarAlertas: () => set((s) => acciones.restaurarAlertas(s)),
      guardarReglas: (reglas) => set((s) => acciones.guardarReglas(s, reglas)),
      guardarUmbrales: (umbrales) => set((s) => acciones.guardarUmbrales(s, umbrales)),
      agregarAlPedido: (productoId, cantidad) =>
        set((s) => acciones.agregarAlPedido(s, productoId, cantidad)),
      quitarDelPedido: (productoId) => set((s) => acciones.quitarDelPedido(s, productoId)),
      vaciarPedido: () => set((s) => acciones.vaciarPedido(s)),
      guardarNegocio: (datos) => set((s) => acciones.guardarNegocio(s, datos)),
      iniciarPrueba: () => set((s) => acciones.iniciarPrueba(s)),
      suscribirse: (datos) => set((s) => acciones.suscribirse(s, datos)),
      cancelarSuscripcion: () => set((s) => acciones.cancelarSuscripcion(s)),
      reactivarSuscripcion: () => set((s) => acciones.reactivarSuscripcion(s)),
      simularFinDePrueba: () => set((s) => acciones.simularFinDePrueba(s)),
    }),
    {
      name: CLAVE,
      version: 1,
      storage: createJSONStorage(() => localStorage),
      // Sólo se guardan los datos, no las funciones.
      partialize: (s): RedState => ({
        negocio: s.negocio,
        proveedores: s.proveedores,
        productos: s.productos,
        listas: s.listas,
        cambios: s.cambios,
        reglas: s.reglas,
        umbrales: s.umbrales,
        ofertas: s.ofertas,
        zona: s.zona,
        barrio: s.barrio,
        pedido: s.pedido,
        alertas: s.alertas,
        suscripcion: s.suscripcion,
      }),
    },
  ),
);

/** Para las pruebas: vuelve a la demostración inicial, con la fecha que se indique. */
export function reiniciarRed(ahora: Date = new Date()) {
  useRedStore.setState(crearEstadoInicial(ahora));
}
