import { useState } from 'react';
import { CaretDownIcon, CheckIcon, WarningIcon } from '@phosphor-icons/react';
import {
  DISTANCIA_MINIMA_KM,
  ETIQUETA_DE_COINCIDENCIA,
  FACTORES,
  NOMBRE_DE_JURISDICCION,
  UN_RUBRO,
  formatoKm,
  formatoPuntaje,
  queVerificar,
  type Filtros,
  type ZonaPuntuada,
} from '@/lib/dondeAbrir';
import { GuiaDeTramites } from './GuiaDeTramites';

type Pestania = 'porque' | 'tramites';

type Props = {
  item: ZonaPuntuada;
  filtros: Filtros;
  superficie: number;
  onSuperficie: (metros: number) => void;
  calculoAbierto: boolean;
  onAlternarCalculo: () => void;
};

const dosDecimales = new Intl.NumberFormat('es-AR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const UNIDAD: Record<string, string> = {
  indice_0_100: 'de 100',
  comercios_500m: 'comercios a 500 m',
  lugares_500m: 'lugares a 500 m',
  habitantes_km2: 'personas por km²',
};

const formatoDato = (valor: number | undefined, unidad: string | undefined) => {
  if (valor === undefined) return 'Sin dato suficiente';
  if (unidad === 'ARS_mensuales') {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(valor);
  }
  const numero = valor.toLocaleString('es-AR');
  return unidad && UNIDAD[unidad] ? `${numero} ${UNIDAD[unidad]}` : numero;
};

// La zona elegida, en dos partes: por qué se la recomienda y qué hay que tramitar para abrir ahí.
export function DetalleDeZona({
  item,
  filtros,
  superficie,
  onSuperficie,
  calculoAbierto,
  onAlternarCalculo,
}: Props) {
  const [pestania, setPestania] = useState<Pestania>('porque');
  return (
    <article className="donde-detalle" aria-labelledby="donde-detalle-titulo">
      <header className="donde-detalle-cabeza">
        <div>
          <h3 id="donde-detalle-titulo">{item.zona.nombre}</h3>
          <p>
            {NOMBRE_DE_JURISDICCION[item.zona.jurisdiccion]} · {formatoKm(item.distanciaKm)} de tu
            local
          </p>
        </div>
        <span className={`donde-sello ${item.coincidencia}`}>
          <i aria-hidden /> {ETIQUETA_DE_COINCIDENCIA[item.coincidencia]}
        </span>
      </header>

      <div className="donde-pestanias" role="tablist" aria-label={`Sobre ${item.zona.nombre}`}>
        <button
          type="button"
          role="tab"
          id="pestania-porque"
          aria-selected={pestania === 'porque'}
          aria-controls="panel-porque"
          onClick={() => setPestania('porque')}
        >
          Por qué esta zona
        </button>
        <button
          type="button"
          role="tab"
          id="pestania-tramites"
          aria-selected={pestania === 'tramites'}
          aria-controls="panel-tramites"
          onClick={() => setPestania('tramites')}
        >
          Qué tenés que tramitar
        </button>
      </div>

      {pestania === 'porque' ? (
        <div role="tabpanel" id="panel-porque" aria-labelledby="pestania-porque">
          <PorQueEstaZona
            item={item}
            filtros={filtros}
            calculoAbierto={calculoAbierto}
            onAlternarCalculo={onAlternarCalculo}
          />
        </div>
      ) : (
        <div role="tabpanel" id="panel-tramites" aria-labelledby="pestania-tramites">
          <GuiaDeTramites
            zona={item.zona}
            rubro={filtros.rubro}
            superficie={superficie}
            onSuperficie={onSuperficie}
          />
        </div>
      )}
    </article>
  );
}

function PorQueEstaZona({
  item,
  filtros,
  calculoAbierto,
  onAlternarCalculo,
}: {
  item: ZonaPuntuada;
  filtros: Filtros;
  calculoAbierto: boolean;
  onAlternarCalculo: () => void;
}) {
  const penalizada = item.distanciaKm < DISTANCIA_MINIMA_KM;
  const conPeso = item.aportes.filter((aporte) => aporte.peso > 0);
  const fuentes = Array.from(
    new Map(
      item.aportes
        .filter((aporte) => aporte.fuente)
        .map((aporte) => [aporte.fuente?.codigo, aporte.fuente] as const),
    ).values(),
  );
  const sinMirar = FACTORES.filter((factor) => filtros.importancia[factor.clave] === 0).map(
    (factor) => factor.etiqueta.toLowerCase(),
  );
  return (
    <>
      <div className="donde-columnas">
        <section>
          <h4>Por qué la recomiendo</h4>
          <ul className="positivo">
            {item.porQue.map((texto) => (
              <li key={texto}>
                <CheckIcon aria-hidden size={16} weight="bold" /> {texto}
              </li>
            ))}
          </ul>
        </section>
        <section>
          <h4>Riesgos</h4>
          <ul className="riesgo">
            {item.riesgos.map((texto) => (
              <li key={texto}>
                <WarningIcon aria-hidden size={16} weight="bold" /> {texto}
              </li>
            ))}
          </ul>
        </section>
      </div>

      <details className="donde-plegable">
        <summary>Qué verificar en persona</summary>
        <ul>
          {queVerificar(filtros.rubro).map((texto) => (
            <li key={texto}>{texto}</li>
          ))}
        </ul>
      </details>

      <details className="donde-plegable donde-datos">
        <summary>Cómo usamos los datos</summary>
        <p>
          Comparamos el snapshot guardado de OpenStreetMap con las lecturas agregadas y anónimas
          disponibles en Ensambla. La fecha y la fuente se muestran para que puedas revisar de dónde
          sale cada dato.
        </p>
        {fuentes.length > 0 && (
          <ul>
            {fuentes.map((fuente) => (
              <li key={fuente?.codigo}>
                {fuente?.url ? (
                  <a href={fuente.url} target="_blank" rel="noreferrer">
                    {fuente.nombre}
                  </a>
                ) : (
                  fuente?.nombre
                )}
                {fuente?.licencia ? ` · ${fuente.licencia}` : ''}
              </li>
            ))}
          </ul>
        )}
      </details>

      <button
        type="button"
        className="donde-calculo-boton"
        aria-expanded={calculoAbierto}
        aria-controls="donde-calculo"
        onClick={onAlternarCalculo}
      >
        Cómo lo calculé <CaretDownIcon aria-hidden size={16} />
      </button>
      {calculoAbierto && (
        <div id="donde-calculo" className="donde-calculo">
          <p>
            Comparo cada factor con las otras zonas: la mejor saca 10 y la peor, 0. Después lo
            combino según cuánto importa ese factor para {UN_RUBRO[filtros.rubro]} y para lo que me
            pediste. La importancia indica cuánto influye en el resultado; la evaluación compara esa
            zona con las demás, de 0 a 10.
            {sinMirar.length > 0 && ` No miré: ${sinMirar.join(', ')}.`}
          </p>
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
                  <tr key={aporte.clave}>
                    <th scope="row">{aporte.etiqueta}</th>
                    <td>
                      {formatoDato(aporte.valor, aporte.unidad)}
                      {aporte.fuente && (
                        <small className="donde-fuente">{aporte.fuente.nombre}</small>
                      )}
                    </td>
                    <td>{Math.round(aporte.peso * 100)} %</td>
                    <td>{formatoPuntaje(aporte.puntaje)} de 10</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                {penalizada && (
                  <tr>
                    <th scope="row" colSpan={3}>
                      Descuento por quedar muy cerca de tu local
                    </th>
                    <td>−{dosDecimales.format(1.2)}</td>
                  </tr>
                )}
                <tr>
                  <th scope="row" colSpan={3}>
                    Puntaje final
                  </th>
                  <td>{formatoPuntaje(item.puntaje)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}
    </>
  );
}
