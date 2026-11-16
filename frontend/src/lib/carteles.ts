import type {
  ContenidoDeCartel,
  FormatoDeCartel,
  PedidoDeTextos,
  PromocionSugerida,
  TextosDeCartel,
} from '@/types/carteles';

export const NOMBRES_DE_FORMATO: Record<FormatoDeCartel, string> = {
  A4: 'Hoja A4 (imprimir)',
  A5: 'Hoja A5 (imprimir)',
  CUADRADO: 'Cuadrado (redes)',
  VERTICAL: 'Vertical (redes)',
};

export const pesos = (valor: number) => `$${valor.toLocaleString('es-AR')}`;

export const pedidoDeTextos = (promocion: PromocionSugerida): PedidoDeTextos => ({
  tiposPosibles: [promocion.tipo],
  productos: promocion.productos,
  numeros: [
    promocion.precioAnterior,
    promocion.precioNuevo,
    promocion.ahorro,
    promocion.descuentoPorcentaje,
  ],
});

export const contenidoDe = (
  promocion: PromocionSugerida,
  textos: TextosDeCartel,
  formato: FormatoDeCartel,
): ContenidoDeCartel => ({
  tipo: textos.plantilla,
  formato,
  titular: textos.titular,
  bajada: textos.bajada,
  protagonista: pesos(promocion.precioNuevo),
  detalle: `Antes ${pesos(promocion.precioAnterior)} · ${promocion.productos.join(' + ')}`,
  lineas: [],
});

/** El dibujo del servidor como imagen: así el navegador no ejecuta nada de lo que haya adentro. */
export const dibujoComoImagen = (dibujo: string) =>
  `data:image/svg+xml;charset=utf-8,${encodeURIComponent(dibujo)}`;

export const enlaceDeWhatsapp = (texto: string) =>
  `https://wa.me/?text=${encodeURIComponent(texto)}`;
