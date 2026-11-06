import { useEffect, useMemo, useState } from 'react';
import { CATALOG } from '@/lib/sistema/catalogo';
import { crearDemoProveedores, crearRevisionSimulada, hoyLocal } from '@/lib/proveedores/demo';
import {
  compararCostos,
  confirmarCompra,
  lineasDelPedido,
  mensajeDelPedido,
  redondear,
  resumenDelMes,
  sugerirPedido,
  totalCompra,
  validarRevision,
} from '@/lib/proveedores/calculos';
import { demoProveedoresSchema } from '@/types/proveedores';
import type {
  GastoProveedor,
  LineaCompra,
  ProductoProveedor,
  RevisionCompra,
  VistaProveedor,
} from '@/types/proveedores';

export const claveDemoProveedores = (usuarioId: string) =>
  `ensambla:proveedores-demo:v1:${usuarioId}`;

function cargarDemo(clave: string) {
  try {
    const texto = localStorage.getItem(clave);
    if (texto) {
      const datos = demoProveedoresSchema.safeParse(JSON.parse(texto));
      if (datos.success) return { datos: datos.data, aviso: '' };
      return {
        datos: crearDemoProveedores(),
        aviso: 'Los datos locales no eran compatibles. Se cargó un nuevo ejemplo.',
      };
    }
    return { datos: crearDemoProveedores(), aviso: '' };
  } catch {
    return {
      datos: crearDemoProveedores(),
      aviso: 'No se pudo recuperar la demo local. Podés probar con datos de ejemplo.',
    };
  }
}

const PRODUCTO_VACIO = { nombre: '', presentacion: '', stock: 0, minimo: 1, costo: 0, precio: 0 };
const GASTO_VACIO = { name: '', total: 0, category: 'Servicios' as GastoProveedor['category'] };

export function useProveedoresDemo(usuarioId: string) {
  const clave = claveDemoProveedores(usuarioId);
  const [inicial] = useState(() => cargarDemo(clave));
  const [datos, setDatos] = useState(inicial.datos);
  const [aviso, setAviso] = useState(inicial.aviso);
  const [errorGuardado, setErrorGuardado] = useState('');
  const [vista, setVista] = useState<VistaProveedor>('pedido');
  const [proveedorId, setProveedorId] = useState(inicial.datos.proveedores[0]?.id ?? '');
  const [nuevoProveedor, setNuevoProveedor] = useState({ nombre: '', telefono: '' });
  const [mostrarProveedor, setMostrarProveedor] = useState(false);
  const [productoAAgregar, setProductoAAgregar] = useState('');
  const [revision, setRevision] = useState<RevisionCompra | null>(null);
  const [revisado, setRevisado] = useState(false);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [vistaArchivo, setVistaArchivo] = useState('');
  const [productoNuevo, setProductoNuevo] = useState(PRODUCTO_VACIO);
  const [gastoNuevo, setGastoNuevo] = useState({ ...GASTO_VACIO, date: hoyLocal() });
  const [mes, setMes] = useState(hoyLocal().slice(0, 7));
  const [error, setError] = useState('');
  const proveedor = datos.proveedores.find((p) => p.id === proveedorId) ?? datos.proveedores[0];
  if (!proveedor) throw new Error('La demo necesita un proveedor.');

  useEffect(() => {
    try {
      localStorage.setItem(clave, JSON.stringify(datos));
      setErrorGuardado('');
    } catch {
      setErrorGuardado(
        'El navegador no pudo guardar los cambios. La demo funciona en esta sesión, pero los perderás al recargar.',
      );
    }
  }, [datos, clave]);

  useEffect(() => {
    if (!archivo) {
      setVistaArchivo('');
      return;
    }
    const url = URL.createObjectURL(archivo);
    setVistaArchivo(url);
    return () => URL.revokeObjectURL(url);
  }, [archivo]);

  const lineasPedido = useMemo(() => lineasDelPedido(datos, proveedorId), [datos, proveedorId]);
  const disponibles = datos.productos.filter(
    (p) => p.proveedorId === proveedorId && !lineasPedido.some((l) => l.productoId === p.id),
  );
  const mensaje = mensajeDelPedido(datos, proveedor);
  const totalPedido = redondear(
    lineasPedido.reduce((s, l) => s + l.cantidad * l.producto.costo, 0),
  );
  const pedidoValido = lineasPedido.some((l) => l.cantidad > 0);
  const compras = datos.compras
    .filter((c) => c.proveedorId === proveedorId)
    .sort((a, b) => b.fecha.localeCompare(a.fecha));
  const comparaciones = compararCostos(datos, proveedorId);
  const resumen = resumenDelMes(datos, mes);
  const gastos = datos.gastos
    .filter((g) => g.date.slice(0, 7) === mes)
    .sort((a, b) => b.date.localeCompare(a.date));
  const errorRevision = revision ? validarRevision(datos, revision) : null;

  const cambiarProveedor = (id: string) => {
    setProveedorId(id);
    setProductoAAgregar('');
    setError('');
    // Un comprobante abierto conserva su proveedor en la revisión, independientemente de este filtro.
  };
  const cambiarCantidad = (productoId: string, cantidad: number) => {
    if (!Number.isInteger(cantidad) || cantidad < 0 || cantidad > 1_000_000) return;
    setDatos((d) => ({
      ...d,
      pedido: [...d.pedido.filter((l) => l.productoId !== productoId), { productoId, cantidad }],
    }));
  };
  const quitarLinea = (id: string) =>
    setDatos((d) => ({ ...d, pedido: d.pedido.filter((l) => l.productoId !== id) }));
  const agregarLinea = () => {
    if (!disponibles.some((p) => p.id === productoAAgregar)) return;
    cambiarCantidad(productoAAgregar, 1);
    setProductoAAgregar('');
  };
  const regenerar = () => {
    setDatos((d) => ({
      ...d,
      pedido: [
        ...d.pedido.filter(
          (l) => d.productos.find((p) => p.id === l.productoId)?.proveedorId !== proveedorId,
        ),
        ...sugerirPedido(d.productos.filter((p) => p.proveedorId === proveedorId)),
      ],
    }));
    setAviso('Se reemplazó el pedido de este proveedor por una nueva sugerencia. Podés editarla.');
  };
  const guardarProveedor = () => {
    if (!nuevoProveedor.nombre.trim()) {
      setError('Ingresá el nombre del proveedor.');
      return;
    }
    const nuevo = {
      ...nuevoProveedor,
      nombre: nuevoProveedor.nombre.trim(),
      id: crypto.randomUUID(),
    };
    setDatos((d) => ({ ...d, proveedores: [...d.proveedores, nuevo] }));
    cambiarProveedor(nuevo.id);
    setNuevoProveedor({ nombre: '', telefono: '' });
    setMostrarProveedor(false);
    setAviso('Proveedor agregado a la demo. Sumale productos desde Stock de prueba.');
  };
  const seleccionarArchivo = (seleccion: File | undefined) => {
    setError('');
    setArchivo(null);
    setRevision(null);
    setRevisado(false);
    if (!seleccion) return;
    if (!['application/pdf', 'image/jpeg', 'image/png', 'image/webp'].includes(seleccion.type)) {
      setError('Elegí un PDF o una imagen JPG, PNG o WebP.');
      return;
    }
    if (seleccion.size > 10 * 1024 * 1024 || seleccion.size === 0) {
      setError('El archivo debe tener contenido y pesar hasta 10 MB.');
      return;
    }
    setArchivo(seleccion);
  };
  const simularLectura = (deEjemplo = false) => {
    if (!deEjemplo && !archivo) return;
    setRevision(
      crearRevisionSimulada(
        datos,
        proveedorId,
        deEjemplo ? 'Comprobante de ejemplo (sin archivo)' : (archivo?.name ?? ''),
      ),
    );
    if (deEjemplo) setArchivo(null);
    setRevisado(false);
    setError('');
  };
  const actualizarRevision = (cambio: Partial<RevisionCompra>) => {
    setRevision((r) => (r ? { ...r, ...cambio } : r));
    setRevisado(false);
  };
  const actualizarLinea = (indice: number, cambio: Partial<LineaCompra>) => {
    if (revision)
      actualizarRevision({
        lineas: revision.lineas.map((l, i) => (i === indice ? { ...l, ...cambio } : l)),
      });
  };
  const agregarLineaCompra = () => {
    if (!revision) return;
    const producto = datos.productos.find(
      (p) => !revision.lineas.some((l) => l.productoId === p.id),
    );
    actualizarRevision({
      lineas: [
        ...revision.lineas,
        { productoId: producto?.id ?? '', cantidad: 1, costo: producto?.costo || 1 },
      ],
    });
  };
  const guardarCompra = () => {
    if (!revision || !revisado) return;
    const problema = validarRevision(datos, revision);
    if (problema) {
      setError(problema);
      return;
    }
    setDatos((d) => confirmarCompra(d, revision));
    setProveedorId(revision.proveedorId);
    setRevision(null);
    setArchivo(null);
    setRevisado(false);
    setError('');
    setAviso(
      'Compra confirmada: stock, historial y gasto de mercadería actualizados en la demo. Los precios de venta se conservaron.',
    );
  };
  const guardarProducto = () => {
    const nuevo: ProductoProveedor = {
      ...productoNuevo,
      nombre: productoNuevo.nombre.trim(),
      presentacion: productoNuevo.presentacion.trim(),
      id: crypto.randomUUID(),
      proveedorId,
    };
    const candidato = demoProveedoresSchema.safeParse({
      ...datos,
      productos: [...datos.productos, nuevo],
    });
    if (!candidato.success) {
      setError(
        'Completá producto y presentación. Usá cantidades enteras e importes válidos, mayores o iguales a cero.',
      );
      return;
    }
    setDatos(candidato.data);
    setProductoNuevo(PRODUCTO_VACIO);
    setError('');
    setAviso('Producto agregado al stock de prueba. Ya podés incluirlo en el pedido.');
  };
  const guardarGasto = () => {
    const candidato = demoProveedoresSchema.safeParse({
      ...datos,
      gastos: [
        ...datos.gastos,
        { ...gastoNuevo, id: crypto.randomUUID(), name: gastoNuevo.name.trim() },
      ],
    });
    if (!candidato.success) {
      setError('Completá concepto, fecha e importe positivo del gasto.');
      return;
    }
    setDatos(candidato.data);
    setMes(gastoNuevo.date.slice(0, 7));
    setGastoNuevo({ ...GASTO_VACIO, date: hoyLocal() });
    setError('');
    setAviso('Gasto general agregado al resultado estimado del mes.');
  };
  const copiarMensaje = async () => {
    try {
      await navigator.clipboard.writeText(mensaje);
      setAviso('Pedido copiado. Pegalo en el chat de tu proveedor cuando quieras enviarlo.');
    } catch {
      setAviso('No se pudo copiar automáticamente. Seleccioná y copiá el mensaje del recuadro.');
    }
  };

  return {
    datos,
    vista,
    setVista,
    proveedor,
    proveedorId,
    cambiarProveedor,
    mostrarProveedor,
    setMostrarProveedor,
    nuevoProveedor,
    setNuevoProveedor,
    guardarProveedor,
    aviso,
    error: error || errorGuardado,
    lineasPedido,
    lineasParaImprimir: lineasPedido.filter((l) => l.cantidad > 0),
    pedidoValido,
    disponibles,
    productoAAgregar,
    setProductoAAgregar,
    totalPedido,
    mensaje,
    cambiarCantidad,
    quitarLinea,
    agregarLinea,
    regenerar,
    copiarMensaje,
    imprimir: () => window.print(),
    compras,
    comparaciones,
    costosComparables: comparaciones.filter((c) => c.anterior !== null),
    costosSinReferencia: comparaciones.filter((c) => c.anterior === null),
    archivo,
    vistaArchivo,
    seleccionarArchivo,
    simularLectura,
    revision,
    actualizarRevision,
    actualizarLinea,
    agregarLineaCompra,
    revisado,
    setRevisado,
    guardarCompra,
    errorRevision,
    cancelarRevision: () => {
      setRevision(null);
      setArchivo(null);
      setRevisado(false);
      setError('');
    },
    totalRevision: revision ? totalCompra(revision) : 0,
    mes,
    setMes,
    resumen,
    gastos,
    gastoNuevo,
    setGastoNuevo,
    guardarGasto,
    categoriasGasto: (
      CATALOG.expenses.fields.find((f) => f.key === 'category')?.options ?? []
    ).filter((c) => c !== 'Mercadería') as GastoProveedor['category'][],
    productoNuevo,
    setProductoNuevo,
    guardarProducto,
  };
}

export type ProveedoresDemo = ReturnType<typeof useProveedoresDemo>;
