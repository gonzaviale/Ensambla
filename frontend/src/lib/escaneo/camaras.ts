import type { CamaraDelLector } from '@/types/escaneo';

/** Los nombres son pistas del navegador, no una medición de calidad óptica. */
export function elegirCamaraAutomatica(
  camaras: CamaraDelLector[],
  comprobada = '',
  elegida?: CamaraDelLector,
) {
  const manual =
    camaras.find((camara) => camara.id === elegida?.id) ??
    camaras.find((camara) => elegida?.nombre && camara.nombre === elegida.nombre);
  if (manual) return manual;
  const ordenadas = camaras
    .map((camara) => {
      const nombre = camara.nombre
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase();
      const trasera = /back|rear|environment|trasera|posterior/.test(nombre);
      const frontal = /front|user|frontal|facetime/.test(nombre);
      const especial = /ultra|telephoto|teleobjetivo|macro|depth|profundidad|dual|triple/.test(
        nombre,
      );
      const principal = /main|principal|standard|wide angle|gran angular/.test(nombre);
      const primeraAndroid = /camera\s*0\b|camara\s*0\b/.test(nombre);
      return {
        camara,
        puntos:
          trasera && !frontal
            ? 100 + (principal ? 20 : 0) + (primeraAndroid ? 40 : 0) - (especial ? 100 : 0)
            : 0,
      };
    })
    .filter(({ puntos }) => puntos > 0)
    .sort((a, b) => b.puntos - a.puntos);
  // Haber leído una vez con una lente automática no la convierte en la mejor para próximos códigos.
  const principal = ordenadas.find(({ puntos }) => puntos >= 100)?.camara;
  return principal ?? camaras.find((camara) => camara.id === comprobada) ?? ordenadas[0]?.camara;
}
