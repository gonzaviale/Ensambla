import { useMemo } from 'react';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { pesos } from '@/lib/sistema/alertas';
import {
  destinoParaSumar,
  nombresDeInventario,
  precioParaSumar,
  productosPropios,
  type DestinoDeProducto,
  type ProductoDeZona,
} from '@/lib/sistema/zona';
import { seleccionarSistemasDelNegocio, useSistemaStore } from '@/stores/sistemaStore';

/** Lo que la persona ya tiene en sus inventarios y adónde se suman los productos nuevos. */
export function useInventarioPropio() {
  const sistemas = useSistemaStore(seleccionarSistemasDelNegocio);
  const propios = useMemo(() => productosPropios(sistemas), [sistemas]);
  const destino = useMemo(() => destinoParaSumar(sistemas), [sistemas]);
  const nombres = useMemo(() => nombresDeInventario(sistemas), [sistemas]);
  return { propios, destino, nombres };
}

/** Suma un producto de la zona al inventario de la persona. */
export function useSumarProducto() {
  const acciones = useAccionesDeAvisos();
  return (destino: DestinoDeProducto, producto: ProductoDeZona) => {
    const precio = precioParaSumar(producto);
    acciones.ejecutar({
      tipo: 'crear',
      texto: 'Sumar a mis productos',
      sistemaId: destino.sistema.id,
      bloqueId: destino.inventario.id,
      valores: { name: producto.nombre, quantity: 0, price: precio, cost: '', minimum: 5 },
      confirmacion: `Sumamos ${producto.nombre} a tu stock, a ${pesos(precio)}.`,
    });
  };
}
