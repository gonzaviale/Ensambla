import {
  BarcodeFormat,
  EncodeHintType,
  QRCodeDecoderErrorCorrectionLevel,
  QRCodeWriter,
  type BitMatrix,
} from '@zxing/library';

export type MatrizDelQr = boolean[][];

const SIN_MARGEN = new Map<EncodeHintType, unknown>([
  [EncodeHintType.MARGIN, 0],
  [EncodeHintType.ERROR_CORRECTION, QRCodeDecoderErrorCorrectionLevel.M],
]);

const filasDe = (bits: BitMatrix): MatrizDelQr =>
  Array.from({ length: bits.getHeight() }, (_, y) =>
    Array.from({ length: bits.getWidth() }, (_, x) => bits.get(x, y)),
  );

/** Los módulos del QR del texto, uno por casillero: `true` es oscuro. Sin borde blanco. */
export function matrizDelQr(texto: string): MatrizDelQr {
  const bits = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0, SIN_MARGEN);
  return filasDe(bits);
}

const cuadradoEn = (x: number, y: number) => `M${x} ${y}h1v1h-1z`;

/** Un solo trazo SVG con todos los módulos oscuros, cada uno de 1×1. */
export const trazoDelQr = (matriz: MatrizDelQr): string =>
  matriz
    .flatMap((fila, y) => fila.flatMap((oscuro, x) => (oscuro ? [cuadradoEn(x, y)] : [])))
    .join('');
