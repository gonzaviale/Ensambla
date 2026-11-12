import { formatoKm, formatoPuntaje, type Filtros, type ZonaPuntuada } from '@/lib/dondeAbrir';

type Props = {
  zonas: ZonaPuntuada[];
  filtros: Filtros;
};

const nivel = (valor: number, invertir = false) => {
  const ajustado = invertir ? 100 - valor : valor;
  if (ajustado >= 67) return 'Alto';
  if (ajustado >= 34) return 'Medio';
  return 'Bajo';
};

const alquiler = (item: ZonaPuntuada) => {
  const dato = item.aportes.find((aporte) => aporte.clave === 'alquiler');
  if (dato?.unidad === 'ARS_mensuales' && dato.valor) {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 0,
    }).format(dato.valor);
  }
  return `${nivel(item.zona.alquiler, true)} · nivel ${Math.round(item.zona.alquiler)} de 100`;
};

// Una comparación corta para decidir. El detalle matemático queda separado en «Cómo lo calculé».
export function ComparacionDeZonas({ zonas, filtros }: Props) {
  return (
    <section className="donde-comparacion" aria-labelledby="donde-comparacion-titulo">
      <div className="donde-seccion-cabeza">
        <div>
          <p className="objetivos-etiqueta">Comparación rápida</p>
          <h2 id="donde-comparacion-titulo">Qué cambia entre las mejores zonas</h2>
        </div>
        <p>Los valores permiten comparar estas zonas entre sí; no son una predicción de ventas.</p>
      </div>
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
            <Fila
              titulo="Puntaje general"
              zonas={zonas}
              valor={(item) => `${formatoPuntaje(item.puntaje)} de 10`}
              destacada
            />
            <Fila titulo="Alquiler estimado" zonas={zonas} valor={alquiler} />
            <Fila
              titulo="Movimiento"
              zonas={zonas}
              valor={(item) =>
                `${nivel(item.zona.movimiento)} · ${Math.round(item.zona.movimiento)} de 100`
              }
            />
            <Fila
              titulo="Competencia cercana"
              zonas={zonas}
              valor={(item) => `${item.zona.competencia[filtros.rubro]} locales a 500 m`}
            />
            <Fila
              titulo="Distancia"
              zonas={zonas}
              valor={(item) => `${formatoKm(item.distanciaKm)} de tu local`}
            />
          </tbody>
        </table>
      </div>
    </section>
  );
}

function Fila({
  titulo,
  zonas,
  valor,
  destacada = false,
}: {
  titulo: string;
  zonas: ZonaPuntuada[];
  valor: (item: ZonaPuntuada) => string;
  destacada?: boolean;
}) {
  return (
    <tr className={destacada ? 'destacada' : undefined}>
      <th scope="row">{titulo}</th>
      {zonas.map((item) => (
        <td key={item.zona.id}>{valor(item)}</td>
      ))}
    </tr>
  );
}
