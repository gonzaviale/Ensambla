import { EJEMPLO, calcularDolor } from '@/hooks/landing/ejemploLista';
import { pesos } from '@/lib/red/formato';

export function Dolor() {
  const d = calcularDolor();

  return (
    <section className="seccion" id="dolor" aria-labelledby="dolor-titulo">
      <div className="contenedor dolor">
        <div className="dolor__texto revelar">
          <p className="sobretitulo">El dolor más caro</p>
          <h2 id="dolor-titulo">Vendés por debajo del costo y no te enterás.</h2>
          <p className="dolor__bajada">
            Pasa en todos los comercios: el proveedor sube un precio, el cartel queda igual y la
            diferencia sale de tu bolsillo. Nadie te avisa, y el cuaderno no lo muestra.
          </p>
          <p className="dolor__cuenta">
            En un mes son <strong>{pesos(d.perdidaPorMes)}</strong> que no ves, sólo con el aceite.
            Y con una suba más de {pesos(d.faltaParaPerder)}, ya vendés por debajo del costo.
          </p>
        </div>

        <div className="dolor__ejemplo revelar">
          <table className="dolor__tabla">
            <caption>{EJEMPLO.producto}, por botella. Ejemplo con números sencillos.</caption>
            <thead>
              <tr>
                <th scope="col">
                  <span className="visualmente-oculto">Concepto</span>
                </th>
                <th scope="col">Hace un mes</th>
                <th scope="col">Hoy</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">Te cuesta</th>
                <td>{pesos(EJEMPLO.costoAnterior)}</td>
                <td>
                  {pesos(d.costoNuevo)}
                  <small>subió {pesos(d.costoNuevo - EJEMPLO.costoAnterior)}</small>
                </td>
              </tr>
              <tr>
                <th scope="row">Lo vendés a</th>
                <td>{pesos(EJEMPLO.precioActual)}</td>
                <td>
                  {pesos(EJEMPLO.precioActual)}
                  <small>el cartel quedó igual</small>
                </td>
              </tr>
              <tr className="dolor__resultado">
                <th scope="row">Te queda</th>
                <td>{pesos(d.gananciaAntes)}</td>
                <td>
                  {pesos(d.gananciaHoy)}
                  <small>{pesos(d.perdidaPorBotella)} menos por botella</small>
                </td>
              </tr>
            </tbody>
          </table>
          <p className="dolor__nota">
            A {EJEMPLO.ventasPorSemana} botellas por semana. Y eso es un solo producto de los
            cientos que vendés.
          </p>
        </div>
      </div>
    </section>
  );
}
