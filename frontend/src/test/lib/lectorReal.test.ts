import { BrowserMultiFormatReader } from '@zxing/browser';
import { BarcodeFormat, BinaryBitmap, DecodeHintType, HybridBinarizer, RGBLuminanceSource } from '@zxing/library';
import { describe, expect, it } from 'vitest';

const CODIGO = '7790895000997';
const BARRAS = [
  '000000000000', '101',
  '0111011', '0010111', '0001101', '0001001', '0001011', '0111001',
  '01010',
  '1110010', '1110010', '1110010', '1110100', '1110100', '1000100',
  '101', '000000000000',
].join('');

function imagenDelCodigo() {
  const ancho = BARRAS.length * 3;
  const alto = 64;
  const pixeles = Uint8ClampedArray.from({ length: ancho * alto }, (_, indice) =>
    BARRAS[Math.floor((indice % ancho) / 3)] === '1' ? 0 : 255,
  );
  return new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(pixeles, ancho, alto)));
}

describe('decodificador real del lector', () => {
  it('lee un EAN-13 con las dependencias instaladas, sin simular ZXing', () => {
    const lector = new BrowserMultiFormatReader(
      new Map([[DecodeHintType.POSSIBLE_FORMATS, [BarcodeFormat.EAN_13]]]),
      { delayBetweenScanAttempts: 180, tryPlayVideoTimeout: 8_000 },
    );

    const resultado = lector.decodeBitmap(imagenDelCodigo());

    expect(resultado.getText()).toBe(CODIGO);
    expect(resultado.getBarcodeFormat()).toBe(BarcodeFormat.EAN_13);
  });
});
