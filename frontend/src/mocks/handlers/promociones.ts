import { delay, http, HttpResponse } from 'msw';
import { sistemasDe } from '../db/estado';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';
import { formatoPesos } from '@/lib/sistema/formato';
import type { Fila, Sistema } from '@/lib/sistema/tipos';
import type { Canal, ProductoPromocionado, Promocion } from '@/types/promociones';

const CANALES: Canal[] = [
  { clave: 'instagram', nombre: 'Instagram' },
  { clave: 'estado', nombre: 'Estado de WhatsApp' },
  { clave: 'clientes', nombre: 'Mensaje a clientes' },
];

type ProductoConStock = ProductoPromocionado & { id: string; cantidad: number };

const numeroDe = (valor: unknown) => (typeof valor === 'number' ? valor : 0);

const filasDe = (sistema: Sistema, tipo: 'inventory' | 'sales'): Fila[] =>
  sistema.sections.flatMap((seccion) =>
    seccion.blocks.filter((bloque) => bloque.type === tipo).flatMap((bloque) => bloque.rows),
  );

const productosDe = (sistema: Sistema): ProductoConStock[] =>
  filasDe(sistema, 'inventory').flatMap((fila) =>
    !fila.__example && typeof fila.name === 'string' && numeroDe(fila.price) > 0
      ? [{ id: fila.id, nombre: fila.name, precio: numeroDe(fila.price), cantidad: numeroDe(fila.quantity) }]
      : [],
  );

const unidadesVendidas = (sistema: Sistema) => {
  const unidades = new Map<string, number>();
  filasDe(sistema, 'sales').forEach((venta) => {
    const id = String(venta.product ?? '');
    unidades.set(id, (unidades.get(id) ?? 0) + numeroDe(venta.quantity));
  });
  return unidades;
};

const estrellaDe = (productos: ProductoConStock[], unidades: Map<string, number>): Promocion[] => {
  const [masVendido] = [...productos]
    .filter((producto) => (unidades.get(producto.id) ?? 0) > 0)
    .sort((a, b) => (unidades.get(b.id) ?? 0) - (unidades.get(a.id) ?? 0));
  return masVendido
    ? [
        {
          clave: 'estrella',
          titulo: 'Tu producto estrella',
          gancho: 'Lo que más se vendió este mes',
          productos: [{ nombre: masVendido.nombre, precio: masVendido.precio }],
        },
      ]
    : [];
};

const liquidarDe = (productos: ProductoConStock[], unidades: Map<string, number>): Promocion[] => {
  const parados = productos
    .filter((producto) => producto.cantidad > 0 && (unidades.get(producto.id) ?? 0) === 0)
    .sort((a, b) => b.cantidad - a.cantidad)
    .slice(0, 3)
    .map(({ nombre, precio }) => ({ nombre, precio }));
  return unidades.size > 0 && parados.length > 0
    ? [
        {
          clave: 'liquidar',
          titulo: 'Liquidá lo que no se mueve',
          gancho: 'Productos que tenés en stock y no se vendieron este mes',
          productos: parados,
        },
      ]
    : [];
};

const textoDe = ({ titulo, gancho, productos }: Promocion & { canal: string }) =>
  `${titulo}\n${gancho}\n\n${productos.map((p) => `• ${p.nombre}: ${formatoPesos(p.precio)}`).join('\n')}`;

// Las ideas para promocionar de un sistema y el texto de una promoción en un canal (en el modo de demostración).
export const handlersPromociones = [
  http.get(api('/proyectos/:sistemaId/promociones'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const sistema = sistemasDe(sesion.usuario.email).find((otro) => otro.id === params.sistemaId);
    if (!sistema) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    const productos = productosDe(sistema);
    const unidades = unidadesVendidas(sistema);
    return HttpResponse.json({
      promociones: [...estrellaDe(productos, unidades), ...liquidarDe(productos, unidades)],
      canales: CANALES,
    });
  }),
  http.post(api('/promociones/texto'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request, true);
    if (sesion.error) return sesion.error;
    const cuerpo = (await request.json()) as Promocion & { canal: string };
    if (!CANALES.some((canal) => canal.clave === cuerpo.canal)) {
      return errorResponse(400, 'No conocemos ese canal para compartir', pathDe(request));
    }
    return HttpResponse.json({ texto: textoDe(cuerpo) });
  }),
];
