import { CheckIcon, WarningIcon } from '@phosphor-icons/react';
import { queVerificar, type Filtros, type ZonaPuntuada } from '@/lib/dondeAbrir';
import { fuentesDeLosDatos } from '@/lib/dondeAbrirDetalle';
import { CalculoDelPuntaje } from './CalculoDelPuntaje';

type FuenteDelDato = ReturnType<typeof fuentesDeLosDatos>[number];

function RazonesYRiesgos({ item }: { item: ZonaPuntuada }) {
  return (
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
  );
}

function QueVerificarEnPersona({ filtros }: { filtros: Filtros }) {
  return (
    <details className="donde-plegable">
      <summary>Qué verificar en persona</summary>
      <ul>
        {queVerificar(filtros.rubro).map((texto) => (
          <li key={texto}>{texto}</li>
        ))}
      </ul>
    </details>
  );
}

function NombreDeLaFuente({ fuente }: { fuente: FuenteDelDato }) {
  if (!fuente.url) return fuente.nombre;
  return (
    <a href={fuente.url} target="_blank" rel="noreferrer">
      {fuente.nombre}
    </a>
  );
}

function FuentesDeLosDatos({ item }: { item: ZonaPuntuada }) {
  const fuentes = fuentesDeLosDatos(item.aportes);
  if (fuentes.length === 0) return null;
  return (
    <ul>
      {fuentes.map((fuente) => (
        <li key={fuente.codigo}>
          <NombreDeLaFuente fuente={fuente} />
          {fuente.licencia ? ` · ${fuente.licencia}` : ''}
        </li>
      ))}
    </ul>
  );
}

function ComoUsamosLosDatos({ item }: { item: ZonaPuntuada }) {
  return (
    <details className="donde-plegable donde-datos">
      <summary>Cómo usamos los datos</summary>
      <p>
        Contamos lugares registrados dentro de 500 metros de cada zona. Sirven para comparar
        cercanía de comercios, escuelas, plazas, oficinas, transporte y salud; no reemplazan una
        recorrida porque puede haber lugares todavía no registrados. Abajo podés revisar la fuente
        de los datos.
      </p>
      <FuentesDeLosDatos item={item} />
    </details>
  );
}

type Props = {
  item: ZonaPuntuada;
  filtros: Filtros;
  calculoAbierto: boolean;
  onAlternarCalculo: () => void;
};

export function PorQueEstaZona({ item, filtros, calculoAbierto, onAlternarCalculo }: Props) {
  return (
    <>
      <RazonesYRiesgos item={item} />
      <QueVerificarEnPersona filtros={filtros} />
      <ComoUsamosLosDatos item={item} />
      <CalculoDelPuntaje
        item={item}
        filtros={filtros}
        abierto={calculoAbierto}
        onAlternar={onAlternarCalculo}
      />
    </>
  );
}
