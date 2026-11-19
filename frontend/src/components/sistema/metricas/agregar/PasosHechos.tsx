import {
  CANTIDAD_DE_PASOS,
  PASO_DEL_NOMBRE,
  PASOS_CON_TARJETAS,
  etiquetaElegida,
} from '@/lib/metricas/pasosDeLaReceta';
import type { DefinicionDeMetrica, FuenteDeMetrica } from '@/types/metricas';

type Props = {
  definicion: DefinicionDeMetrica;
  fuentes: FuenteDeMetrica[];
  paso: number;
  onIrAlPaso: (paso: number) => void;
};

/** Arriba de todo, los cinco pasos con lo elegido en cada uno: tocando uno se vuelve a cambiarlo. */
export function PasosHechos({ definicion, fuentes, paso, onIrAlPaso }: Props) {
  const resumen = [
    ...PASOS_CON_TARJETAS.map((unPaso) => ({
      corto: unPaso.corto,
      elegido: etiquetaElegida(unPaso, definicion, fuentes),
    })),
    { corto: 'Nombre', elegido: definicion.nombre.trim() },
  ];
  return (
    <ol className="armala-vos-pasos" aria-label={`Los ${CANTIDAD_DE_PASOS} pasos`}>
      {resumen.map(({ corto, elegido }, lugar) => (
        <li key={corto}>
          <button
            type="button"
            aria-current={lugar === paso ? 'step' : undefined}
            aria-label={`Paso ${lugar + 1}: ${corto}`}
            onClick={() => onIrAlPaso(lugar)}
          >
            <span className="armala-vos-numero">{lugar + 1}</span>
            <span className="armala-vos-corto">{corto}</span>
            <span className="armala-vos-elegido">{elegido || textoSinElegir(lugar)}</span>
          </button>
        </li>
      ))}
    </ol>
  );
}

const textoSinElegir = (lugar: number) => (lugar === PASO_DEL_NOMBRE ? 'Falta' : '');
