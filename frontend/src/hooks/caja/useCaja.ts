import { useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { cobrarVenta } from '@/api/caja';
import { consultarCatalogos } from '@/api/catalogosProductos';
import { listarSistemas } from '@/api/sistemas';
import { ApiError } from '@/api/apiError';
import { sistemasKeys } from '@/hooks/sistemas/queries';
import { useSistemaStore } from '@/stores/sistemaStore';
import { allBlocks } from '@/lib/sistema/core';
import { codigoGtin, mismoGtin } from '@/lib/escaneo/codigo';
import { sugerirProducto } from '@/lib/escaneo/catalogos';
import { agregarLinea, cambiarCantidad, crearLinea, guardarArticuloCaja, idCaja, modoVenta } from '@/lib/caja';
import type { DatosArticuloCaja, LineaCaja, SolicitudCobro, VentaCaja } from '@/types/caja';
import type { Fila } from '@/lib/sistema/tipos';

export type SeleccionCaja = { producto?: Fila; codigo: string; nombre: string; favorito: boolean;
  soloGuardar: boolean; mensaje: string; imagen?: string };

export function useCaja(sistemaId: string, inventarioId: string, ventasId: string) {
  const usuarioId = useSistemaStore(s => s.usuarioId);
  const sistema = useSistemaStore(s => s.sistemas.find(item => item.id === sistemaId));
  const pendiente = useSistemaStore(s => s.pendientes.includes(sistemaId));
  const inventario = allBlocks(sistema).find(b => b.id === inventarioId);
  const clave = `ensambla:caja:${usuarioId}:${sistemaId}:${inventarioId}`;
  const [recuperado] = useState(() => {
    try { return JSON.parse(sessionStorage.getItem(clave) ?? 'null') as
      { lineas: LineaCaja[]; solicitud: SolicitudCobro | null } | null; } catch { return null; }
  });
  const [lineas, setLineas] = useState<LineaCaja[]>(recuperado?.lineas ?? []);
  const lineasActuales = useRef(lineas);
  const [solicitud, setSolicitud] = useState<SolicitudCobro | null>(recuperado?.solicitud ?? null);
  const [seleccion, setSeleccion] = useState<SeleccionCaja | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [ocupada, setOcupada] = useState(false);
  const [ticket, setTicket] = useState<VentaCaja | null>(null);
  const [cantidad, ponerCantidad] = useState('1');
  const cantidadActual = useRef('1');
  const setCantidad = (valor: string) => { cantidadActual.current = valor; ponerCantidad(valor); };
  const [codigo, setCodigo] = useState('');
  const [anterior, setAnterior] = useState<LineaCaja[] | null>(null);
  const bloqueada = useRef(false);
  const consulta = useRef<AbortController | null>(null);
  const cliente = useQueryClient();
  const productos = inventario?.rows.filter(p => !p.__example) ?? [];
  useEffect(() => {
    sessionStorage.setItem(clave, JSON.stringify({ lineas, solicitud }));
  }, [clave, lineas, solicitud]);
  useEffect(() => () => consulta.current?.abort(), []);

  const modificar = (nuevas: LineaCaja[]) => {
    if (solicitud || ocupada) return;
    setAnterior(lineasActuales.current); lineasActuales.current = nuevas; setLineas(nuevas); setError('');
  };
  const agregar = (producto: Fila, entrada: string) => {
    try {
      const linea = crearLinea(producto, entrada);
      modificar(agregarLinea(lineasActuales.current, linea));
      setCantidad('1'); setCodigo('');
      setMensaje(`${linea.nombre} agregado a la compra.`);
    } catch (e) { setError(e instanceof Error ? e.message : 'Revisá el artículo.'); }
  };
  const elegir = (producto: Fila) => {
    if (solicitud || ocupada) return;
    if (modoVenta(producto) === 'peso' || !Number(producto.price)) {
      setSeleccion({ producto, codigo: String(producto.barcode ?? ''), nombre: String(producto.name ?? ''),
        favorito: producto.favorite === true, soloGuardar: false, mensaje: '' });
    } else agregar(producto, cantidadActual.current);
  };
  const buscar = async (entrada: string) => {
    if (bloqueada.current || seleccion || solicitud || ocupada) return;
    try {
      const valido = codigoGtin(entrada);
      const existentes = productos.filter(p => mismoGtin(p.barcode, valido));
      if (existentes.length > 1) throw new Error('Ese código está repetido. Revisá el inventario.');
      if (existentes[0]) { elegir(existentes[0]); return; }
      bloqueada.current = true;
      const controller = new AbortController(); consulta.current?.abort(); consulta.current = controller;
      setError(''); setBuscando(true);
      setSeleccion({ codigo: valido, nombre: '', favorito: false, soloGuardar: false,
        mensaje: 'Artículo no reconocido. ¿Querés agregarlo?' });
      const timeout = setTimeout(() => controller.abort(), 12_000);
      try {
        const respuesta = await consultarCatalogos(valido, controller.signal);
        const sugerido = sugerirProducto(respuesta);
        if (controller.signal.aborted) return;
        setSeleccion(actual => actual?.codigo === valido ? { ...actual,
          nombre: sugerido.nombre ?? '', imagen: sugerido.imagen,
          mensaje: sugerido.nombre ? 'Encontramos una referencia. Revisá el nombre y definí tu precio.'
            : respuesta.fuentes.some(f => f.estado === 'no_disponible')
              ? 'El catálogo no está disponible. Podés completar los datos a mano.' : actual.mensaje } : actual);
      } finally { clearTimeout(timeout); }
    } catch (e) {
      if (e instanceof Error && e.name !== 'AbortError') setError(e.message);
    } finally { bloqueada.current = false; setBuscando(false); }
  };
  const cerrar = () => { consulta.current?.abort(); setSeleccion(null); setError(''); setBuscando(false); };
  const registrar = (datos: DatosArticuloCaja) => {
    if (!sistema || !inventario || !seleccion || solicitud) return;
    try {
      if (!seleccion.soloGuardar) crearLinea({ id: seleccion.producto?.id ?? 'nuevo', name: datos.nombre,
        price: datos.precio.replace(',', '.'), saleMode: datos.modo }, datos.cantidad);
      if (seleccion.producto && modoVenta(seleccion.producto) === 'peso' && !seleccion.soloGuardar &&
        datos.nombre === seleccion.producto.name && datos.precio === String(seleccion.producto.price) &&
        datos.modo === 'peso' && datos.favorito === (seleccion.producto.favorite === true) &&
        datos.controlarStock === (seleccion.producto.stockControl === true)) {
        const linea = crearLinea(seleccion.producto, datos.cantidad);
        modificar(agregarLinea(lineasActuales.current, linea)); cerrar(); return;
      }
      let guardado: Fila | undefined;
      useSistemaStore.getState().actualizarSistema(sistemaId, copia => {
        const tabla = allBlocks(copia).find(b => b.id === inventarioId);
        if (!tabla) throw new Error('El inventario ya no existe.');
        guardado = guardarArticuloCaja(copia, tabla, datos, seleccion.codigo, seleccion.producto?.id);
      });
      if (guardado && !seleccion.soloGuardar) agregar(guardado, datos.cantidad);
      setMensaje(seleccion.soloGuardar ? 'Artículo guardado. Ya está disponible en Caja.' : 'Artículo agregado a la compra.');
      cerrar();
    } catch (e) { setError(e instanceof Error ? e.message : 'No pudimos agregar el artículo.'); }
  };
  const confirmar = async (medio: SolicitudCobro['medio'], recibido: number | null) => {
    if (bloqueada.current || pendiente || !lineas.length) return;
    const request = solicitud ?? { id: crypto.randomUUID(), proyectoId: idCaja(sistemaId),
      inventarioId: idCaja(inventarioId), ventasId: idCaja(ventasId), medio, recibido,
      lineas: lineas.map(l => ({ productoId: idCaja(l.productoId), cantidad: l.cantidad,
        precio: l.precio, modo: l.modo === 'peso' ? 'PESO' as const : 'UNIDAD' as const })) };
    bloqueada.current = true; setOcupada(true); setSolicitud(request); setError('');
    sessionStorage.setItem(clave, JSON.stringify({ lineas, solicitud: request }));
    try {
      const venta = await cobrarVenta(request);
      setTicket(venta); lineasActuales.current = []; setLineas([]); setSolicitud(null); setAnterior(null);
      const sistemas = await listarSistemas();
      useSistemaStore.getState().actualizarDesdeServidor(sistemas);
      cliente.setQueryData(sistemasKeys.all, sistemas);
      void cliente.invalidateQueries({ predicate: q => q.queryKey[0] !== 'sistemas' });
    } catch (e) {
      if (e instanceof ApiError && e.status >= 400 && e.status < 500) setSolicitud(null);
      setError(e instanceof Error ? e.message : 'No pudimos verificar la venta. Volvé a consultar la misma operación.');
    } finally { bloqueada.current = false; setOcupada(false); }
  };
  return { sistema, inventario, productos, pendiente, lineas, seleccion, buscando, error, mensaje, ocupada,
    ticket, solicitud, codigo, cantidad, anterior,
    total: lineas.reduce((sum, l) => sum + l.importeCentavos, 0),
    setCodigo, setCantidad, elegir, buscar, registrar, cerrar, confirmar, setTicket,
    nuevo: (favorito = false) => setSeleccion({ codigo: '', nombre: '', favorito, soloGuardar: favorito, mensaje: '' }),
    editar: (producto: Fila) => setSeleccion({ producto, codigo: String(producto.barcode ?? ''),
      nombre: String(producto.name ?? ''), favorito: producto.favorite === true, soloGuardar: true, mensaje: '' }),
    favorito: (producto: Fila) => useSistemaStore.getState().actualizarSistema(sistemaId, copia => {
      const actual = allBlocks(copia).find(b => b.id === inventarioId)?.rows.find(p => p.id === producto.id);
      if (actual) actual.favorite = actual.favorite !== true;
    }),
    quitar: (id: string) => modificar(lineasActuales.current.filter(l => l.id !== id)),
    ajustar: (id: string, cantidad: number) => {
      try { modificar(lineasActuales.current.flatMap(l => l.id !== id ? [l] : cantidad <= 0 ? [] : [cambiarCantidad(l, cantidad)])); }
      catch (e) { setError(e instanceof Error ? e.message : 'Revisá la cantidad.'); }
    },
    deshacer: () => { if (anterior && !solicitud && !ocupada) { lineasActuales.current = anterior; setLineas(anterior); setAnterior(null); } },
  };
}
