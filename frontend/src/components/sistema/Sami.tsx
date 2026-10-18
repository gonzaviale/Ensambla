export type PoseDeSami = 'hello' | 'think' | 'build';

// Poses recortadas con transparencia real; el SVG solo elige el encuadre (igual que el mockup).
const ENCUADRES = {
  hello: { viewBox: '40 85 655 550', archivo: 'sami-poses-sin-fondo.png', ancho: 2172, alto: 724 },
  think: {
    viewBox: '800 135 590 490',
    archivo: 'sami-poses-sin-fondo.png',
    ancho: 2172,
    alto: 724,
  },
  build: {
    viewBox: '65 240 1165 855',
    archivo: 'sami-armando-sin-fondo.png',
    ancho: 1254,
    alto: 1254,
  },
} as const;

export function Sami({ pose = 'hello' }: { pose?: PoseDeSami }) {
  const { viewBox, archivo, ancho, alto } = ENCUADRES[pose];
  return (
    <svg className="sami" viewBox={viewBox} role="img" aria-label="Sami, el pulpito de Ensambla">
      <image href={`/mockup/${archivo}`} width={ancho} height={alto} />
    </svg>
  );
}
