import { diaCorto, semanaDel } from '@/lib/objetivosAutogestion';
import type { Hizo, SemanaObjetivo } from '@/types/objetivos';

type ManualDeLaSemana = NonNullable<SemanaObjetivo['manuales']>[number];
type SemanaConManuales = SemanaObjetivo & { manuales: ManualDeLaSemana[] };

const COMO_LE_FUE: Record<Hizo, string> = { SI: 'Lo hice', EN_PARTE: 'En parte', NO: 'No lo hice' };
const ORDEN_DE_LAS_ACCIONES: Record<Hizo, number> = { SI: 0, EN_PARTE: 1, NO: 2 };
const FORMAS_DEL_EJEMPLO = [3, 2, 3];

const tieneAccionesContadas = (semana: SemanaObjetivo): semana is SemanaConManuales =>
  (semana.manuales ?? []).length > 0;

const delMasViejoAlMasNuevo = (a: SemanaObjetivo, b: SemanaObjetivo) =>
  a.semana.localeCompare(b.semana);

const cuantasSeHicieron = (manuales: ManualDeLaSemana[]) =>
  manuales.filter((manual) => manual.hizo === 'SI').length;

const sumar = (cantidades: number[]) => cantidades.reduce((suma, cantidad) => suma + cantidad, 0);

const hechasPrimero = (manuales: ManualDeLaSemana[]) =>
  [...manuales].sort((a, b) => ORDEN_DE_LAS_ACCIONES[a.hizo] - ORDEN_DE_LAS_ACCIONES[b.hizo]);

const detalleParaLectores = (semana: SemanaConManuales) => {
  const acciones = hechasPrimero(semana.manuales)
    .map((manual) => `${manual.titulo}: ${COMO_LE_FUE[manual.hizo]}`)
    .join('; ');
  return `${semanaDel(semana.semana)}: ${cuantasSeHicieron(semana.manuales)} de ${semana.manuales.length} hechas. ${acciones}`;
};

const plural = (cantidad: number, singular: string, enPlural: string) =>
  cantidad === 1 ? singular : enPlural;

function Cuadrado({ hizo, titulo }: { hizo?: Hizo; titulo?: string }) {
  return <span className="avance-semanas-cuadrado" data-hizo={hizo} title={titulo} />;
}

function ColumnaDeLaSemana({ semana }: { semana: SemanaConManuales }) {
  return (
    <li className="avance-semanas-columna">
      <div className="avance-semanas-pila" role="img" aria-label={detalleParaLectores(semana)}>
        {hechasPrimero(semana.manuales).map((manual) => (
          <Cuadrado
            key={manual.miniObjetivoId}
            hizo={manual.hizo}
            titulo={`${manual.titulo}: ${COMO_LE_FUE[manual.hizo]}`}
          />
        ))}
      </div>
      <span className="avance-semanas-dia">{diaCorto(semana.semana)}</span>
    </li>
  );
}

function LeyendaDelAvance() {
  return (
    <ul className="avance-semanas-leyenda">
      {(Object.keys(COMO_LE_FUE) as Hizo[]).map((hizo) => (
        <li key={hizo}>
          <span aria-hidden>
            <Cuadrado hizo={hizo} />
          </span>{' '}
          {COMO_LE_FUE[hizo]}
        </li>
      ))}
    </ul>
  );
}

function ColumnaDeEjemplo({ cuadrados }: { cuadrados: number }) {
  return (
    <div className="avance-semanas-columna">
      <div className="avance-semanas-pila">
        {Array.from({ length: cuadrados }, (_, orden) => (
          <Cuadrado key={orden} />
        ))}
      </div>
    </div>
  );
}

function AvanceSinSemanasTodavia() {
  return (
    <figure className="avance-semanas vacio">
      <p className="avance-semanas-vacio">
        Este gráfico se arma solo. Cuando le cuentes a Sami cómo te fue y cierre el domingo, aquí
        aparece tu primera semana.
      </p>
      <div className="avance-semanas-fantasma" aria-hidden>
        {FORMAS_DEL_EJEMPLO.map((cuadrados, orden) => (
          <ColumnaDeEjemplo key={orden} cuadrados={cuadrados} />
        ))}
      </div>
    </figure>
  );
}

function ResumenDelAvance({ semanas }: { semanas: SemanaConManuales[] }) {
  const total = sumar(semanas.map((semana) => semana.manuales.length));
  const hechas = sumar(semanas.map((semana) => cuantasSeHicieron(semana.manuales)));
  return (
    <figcaption>
      <strong>
        Hiciste {hechas} de {total} {plural(total, 'acción', 'acciones')}
      </strong>{' '}
      en {semanas.length} {plural(semanas.length, 'semana', 'semanas')}.
    </figcaption>
  );
}

// El avance de un objetivo sin meta en pesos: no hay una línea para dibujar, así que el gráfico se arma solo, una
// columna por semana cerrada y un cuadrado por cada acción que la persona contó.
export function AvanceDeLasSemanas({ semanas }: { semanas: SemanaObjetivo[] }) {
  const cerradasConAcciones = semanas.filter(tieneAccionesContadas).sort(delMasViejoAlMasNuevo);
  if (cerradasConAcciones.length === 0) return <AvanceSinSemanasTodavia />;

  return (
    <figure className="avance-semanas">
      <ResumenDelAvance semanas={cerradasConAcciones} />
      <ol className="avance-semanas-columnas" aria-label="Cómo te fue en cada semana">
        {cerradasConAcciones.map((semana) => (
          <ColumnaDeLaSemana key={semana.semana} semana={semana} />
        ))}
      </ol>
      <LeyendaDelAvance />
    </figure>
  );
}
