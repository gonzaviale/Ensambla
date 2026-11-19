import { formatoKm, formatoPuntaje, type Filtros, type ZonaPuntuada } from '@/lib/dondeAbrir';

type Props = {
  zonas: ZonaPuntuada[];
  filtros: Filtros;
};

type FilaDeLaComparacion = {
  titulo: string;
  valor: (item: ZonaPuntuada, filtros: Filtros) => string;
  destacada?: boolean;
};

const FILAS: FilaDeLaComparacion[] = [
  {
    titulo: 'Puntaje general',
    valor: (item) => `${formatoPuntaje(item.puntaje)} de 10`,
    destacada: true,
  },
  { titulo: 'Escuelas cerca', valor: (item) => `${item.zona.escuelas} a menos de 500 m` },
  { titulo: 'Parques y plazas', valor: (item) => `${item.zona.parques} a menos de 500 m` },
  {
    titulo: 'Competencia cercana',
    valor: (item, filtros) => `${item.zona.competencia[filtros.rubro]} locales a 500 m`,
  },
  { titulo: 'Distancia', valor: (item) => `${formatoKm(item.distanciaKm)} de tu local` },
];

function CabezaDeLaComparacion() {
  return (
    <div className="donde-seccion-cabeza">
      <div>
        <p className="objetivos-etiqueta">Comparación rápida</p>
        <h2 id="donde-comparacion-titulo">Qué cambia entre las mejores zonas</h2>
      </div>
      <p>Sirven para comparar estas zonas entre sí; no predicen ventas.</p>
    </div>
  );
}

function Fila({ fila, zonas, filtros }: { fila: FilaDeLaComparacion } & Props) {
  return (
    <tr className={fila.destacada ? 'destacada' : undefined}>
      <th scope="row">{fila.titulo}</th>
      {zonas.map((item) => (
        <td key={item.zona.id}>{fila.valor(item, filtros)}</td>
      ))}
    </tr>
  );
}

// Una comparación corta para decidir. El detalle matemático queda separado en «Cómo lo calculé».
export function ComparacionDeZonas({ zonas, filtros }: Props) {
  return (
    <section className="donde-comparacion" aria-labelledby="donde-comparacion-titulo">
      <CabezaDeLaComparacion />
      <div className="donde-comparacion-tabla">
        <table aria-label="Comparación de las mejores zonas">
          <thead>
            <tr>
              <th scope="col">Qué comparás</th>
              {zonas.map((item, indice) => (
                <th scope="col" key={item.zona.id}>
                  <span>{indice + 1}</span> {item.zona.nombre}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {FILAS.map((fila) => (
              <Fila key={fila.titulo} fila={fila} zonas={zonas} filtros={filtros} />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
