import { CaretDownIcon } from '@phosphor-icons/react';
import {
  DISTANCIA_MINIMA_KM,
  UN_RUBRO,
  formatoPuntaje,
  type Aporte,
  type Filtros,
  type ZonaPuntuada,
} from '@/lib/dondeAbrir';
import { factoresSinMirar, formatoDato } from '@/lib/dondeAbrirDetalle';

const DESCUENTO_POR_CERCANIA = '−1,20';

function FilaDelAporte({ aporte }: { aporte: Aporte }) {
  return (
    <tr>
      <th scope="row">{aporte.etiqueta}</th>
      <td>{formatoDato(aporte.valor, aporte.unidad)}</td>
      <td>{Math.round(aporte.peso * 100)} %</td>
      <td>{formatoPuntaje(aporte.puntaje)} de 10</td>
    </tr>
  );
}

function DescuentoPorCercania({ item }: { item: ZonaPuntuada }) {
  if (item.distanciaKm >= DISTANCIA_MINIMA_KM) return null;
  return (
    <tr>
      <th scope="row" colSpan={3}>
        Descuento por quedar muy cerca de tu local
      </th>
      <td>{DESCUENTO_POR_CERCANIA}</td>
    </tr>
  );
}

function TablaDelPuntaje({ item }: { item: ZonaPuntuada }) {
  const conPeso = item.aportes.filter((aporte) => aporte.peso > 0);
  return (
    <div className="donde-tabla">
      <table aria-label={`Detalle del puntaje de ${item.zona.nombre}`}>
        <caption>Cómo se arma el puntaje de {item.zona.nombre}</caption>
        <thead>
          <tr>
            <th scope="col">Qué miro</th>
            <th scope="col">Dato observado</th>
            <th scope="col">Importancia en el resultado</th>
            <th scope="col">Evaluación de la zona</th>
          </tr>
        </thead>
        <tbody>
          {conPeso.map((aporte) => (
            <FilaDelAporte key={aporte.clave} aporte={aporte} />
          ))}
        </tbody>
        <tfoot>
          <DescuentoPorCercania item={item} />
          <tr>
            <th scope="row" colSpan={3}>
              Puntaje final
            </th>
            <td>{formatoPuntaje(item.puntaje)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}

function ExplicacionDelCalculo({ filtros }: { filtros: Filtros }) {
  const sinMirar = factoresSinMirar(filtros);
  const noMire = sinMirar.length > 0 ? ` No miré: ${sinMirar.join(', ')}.` : '';
  return (
    <p>
      Comparo cada factor con las otras zonas: la mejor saca 10 y la peor, 0. Después lo combino
      según cuánto importa ese factor para {UN_RUBRO[filtros.rubro]} y para lo que me pediste. La
      importancia indica cuánto influye en el resultado; la evaluación compara esa zona con las
      demás, de 0 a 10.
      {noMire}
    </p>
  );
}

type Props = {
  item: ZonaPuntuada;
  filtros: Filtros;
  abierto: boolean;
  onAlternar: () => void;
};

/** El botón «Cómo lo calculé» y, abierto, el paso a paso del puntaje de la zona. */
export function CalculoDelPuntaje({ item, filtros, abierto, onAlternar }: Props) {
  return (
    <>
      <button
        type="button"
        className="donde-calculo-boton"
        aria-expanded={abierto}
        aria-controls="donde-calculo"
        onClick={onAlternar}
      >
        Cómo lo calculé <CaretDownIcon aria-hidden size={16} />
      </button>
      {abierto && (
        <div id="donde-calculo" className="donde-calculo">
          <ExplicacionDelCalculo filtros={filtros} />
          <TablaDelPuntaje item={item} />
        </div>
      )}
    </>
  );
}
