import { Sheet } from '@/components/ui';
import type { RespuestaVista } from '@/types/negocio';
import { numeroDe } from './negocioUtil';
import { TendenciaRespuesta } from './TendenciaRespuesta';

type Props = { vista: RespuestaVista | null; onClose: () => void };

// Detalle de una respuesta lista: titular, explicación, tendencia (si el plan la incluye) y los
// renglones con barras hechas con div: el valor siempre está escrito, la barra sólo ayuda a comparar.
export function DetalleRespuesta({ vista, onClose }: Props) {
  const lista = vista?.resultado.estado === 'lista' ? vista.resultado : null;
  return (
    <Sheet
      open={Boolean(vista && lista)}
      onClose={onClose}
      title={vista?.definicion.pregunta ?? ''}
    >
      {lista && (
        <div className="flex flex-col gap-4">
          <p className="m-0 text-h1 leading-tight font-bold break-words">{lista.titular}</p>
          <p className="m-0 text-body text-soft">{lista.detalle}</p>
          <TendenciaRespuesta tendencia={lista.tendencia} />
          {lista.items && lista.items.length > 0 && <Renglones items={lista.items} />}
        </div>
      )}
    </Sheet>
  );
}

function Renglones({
  items,
}: {
  items: NonNullable<Extract<RespuestaVista['resultado'], { estado: 'lista' }>['items']>;
}) {
  const numeros = items.map((item) => numeroDe(item.valor));
  const maximo = Math.max(...numeros.map((n) => n ?? 0), 0);

  return (
    <ul className="m-0 flex list-none flex-col gap-4 p-0">
      {items.map((item, indice) => {
        const numero = numeros[indice];
        const ancho =
          maximo > 0 && numero !== null && numero !== undefined ? (numero / maximo) * 100 : 0;
        return (
          <li key={`${item.nombre}-${indice}`} className="flex flex-col gap-1.5">
            <div className="flex items-baseline justify-between gap-3">
              <span className="min-w-0 text-body font-semibold break-words">{item.nombre}</span>
              <span className="shrink-0 text-body font-bold">{item.valor}</span>
            </div>
            {ancho > 0 && (
              <div aria-hidden className="h-3 w-full overflow-hidden rounded-full bg-sand-100">
                <div className="h-full rounded-full bg-accent" style={{ width: `${ancho}%` }} />
              </div>
            )}
            {item.nota && <span className="text-hint text-soft">{item.nota}</span>}
          </li>
        );
      })}
    </ul>
  );
}
