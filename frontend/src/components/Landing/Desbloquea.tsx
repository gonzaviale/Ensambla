import { nombreDePieza } from '@/lib/catalogo/piezas';
import { pesos } from '@/lib/formato';
import { definicion } from '@/lib/respuestas/definiciones';
import type { RespuestaId } from '@/types/negocio';
import { variables } from './estilos';

// Números de muestra, rotulados como ejemplo: no salen de ningún negocio real.
const EJEMPLOS: { id: RespuestaId; numero: string; detalle: string }[] = [
  {
    id: 'te-quedo',
    numero: pesos(182000),
    detalle: `Vendiste ${pesos(640000)} y gastaste ${pesos(458000)}.`,
  },
  {
    id: 'margen-producto',
    numero: `Empanadas: ${pesos(900)}`,
    detalle: `Es lo que te deja cada una. Y el aceite lo vendés con pérdida: ${pesos(150)} por botella.`,
  },
  {
    id: 'quien-te-debe',
    numero: pesos(47500),
    detalle: 'Entre 6 clientes. Ves quién es y hace cuánto.',
  },
  {
    id: 'costo-plato',
    numero: pesos(2350),
    detalle: 'Es lo que te cuesta cada vianda, sumando sus insumos.',
  },
];

export function Desbloquea() {
  return (
    <section className="seccion" id="desbloquea" aria-labelledby="desbloquea-titulo">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Cada bloque suma</p>
          <h2 id="desbloquea-titulo">Cada bloque desbloquea una respuesta.</h2>
          <p>
            No hace falta cargar todo de entrada. Sumás un bloque y Ensambla ya te contesta algo que
            antes no sabías.
          </p>
        </header>

        <ul className="respuestas">
          {EJEMPLOS.map(({ id, numero, detalle }, i) => {
            const { pregunta, requiere } = definicion(id);
            return (
              <li key={id} className="respuesta revelar" style={variables({ '--d': `${i / 10}s` })}>
                <ul className="respuesta__con" aria-label="Con estos bloques">
                  {requiere.map((pieza) => (
                    <li key={pieza}>{nombreDePieza(pieza)}</li>
                  ))}
                </ul>
                <h3>{pregunta}</h3>
                <p className="respuesta__numero">{numero}</p>
                <p className="respuesta__detalle">{detalle}</p>
                <p className="respuesta__ejemplo">Número de ejemplo</p>
              </li>
            );
          })}
        </ul>
        <p className="respuestas__nota">
          Los números son de muestra, para que veas cómo se ve. Los tuyos aparecen cuando cargás los
          tuyos.
        </p>
      </div>
    </section>
  );
}
