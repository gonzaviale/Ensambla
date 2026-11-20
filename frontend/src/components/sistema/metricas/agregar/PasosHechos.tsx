import { Icon } from '@/components/sistema/Icon';
import {
  CANTIDAD_DE_PASOS,
  PASO_DEL_NOMBRE,
  PASOS_CON_TARJETAS,
  SOLAPAS_DE_LOS_PASOS,
  etiquetaElegida,
  sePuedeIrAlPaso,
} from '@/lib/metricas/pasosDeLaReceta';
import type { DefinicionDeMetrica, FuenteDeMetrica } from '@/types/metricas';

type Props = {
  definicion: DefinicionDeMetrica;
  fuentes: FuenteDeMetrica[];
  hechos: number[];
  paso: number;
  onIrAlPaso: (paso: number) => void;
};

/** Una solapa por pregunta, cada una con su nombre y lo que se eligió. Las que faltan contestar se ven sin tilde. */
export function PasosHechos({ definicion, fuentes, hechos, paso, onIrAlPaso }: Props) {
  const elegidoEn = (lugar: number) => {
    if (lugar === PASO_DEL_NOMBRE) return definicion.nombre.trim();
    const pasoConTarjetas = PASOS_CON_TARJETAS[lugar];
    return pasoConTarjetas && hechos.includes(lugar)
      ? etiquetaElegida(pasoConTarjetas, definicion, fuentes)
      : '';
  };
  const contestado = (lugar: number) =>
    lugar === PASO_DEL_NOMBRE ? definicion.nombre.trim() !== '' : hechos.includes(lugar);
  return (
    <ol className="armala-vos-pasos" role="tablist" aria-label={`Los ${CANTIDAD_DE_PASOS} pasos`}>
      {SOLAPAS_DE_LOS_PASOS.map(({ corto, color }, lugar) => (
        <li key={corto}>
          <button
            type="button"
            role="tab"
            aria-selected={lugar === paso}
            aria-label={`Paso ${lugar + 1}: ${corto}`}
            data-color={color}
            disabled={!sePuedeIrAlPaso(hechos, lugar)}
            onClick={() => onIrAlPaso(lugar)}
          >
            <NumeroDelPaso lugar={lugar} contestado={contestado(lugar)} />
            <span className="armala-vos-corto">{corto}</span>
            <span className="armala-vos-elegido">{elegidoEn(lugar)}</span>
          </button>
        </li>
      ))}
    </ol>
  );
}

function NumeroDelPaso({ lugar, contestado }: { lugar: number; contestado: boolean }) {
  return (
    <span className="armala-vos-numero" aria-hidden="true">
      {contestado ? <Icon name="check" /> : lugar + 1}
    </span>
  );
}
