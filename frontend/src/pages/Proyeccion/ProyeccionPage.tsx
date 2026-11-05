import { useMemo, useState, type FormEvent } from 'react';
import { GraficoDeProyeccion } from '@/components/gestion/GraficoDeProyeccion';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import { DiceSami, Seccion } from '@/components/gestion/piezas';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { pesos } from '@/lib/gestion/formato';
import {
  interpretarPregunta,
  proyectar,
  type Escenario,
  type Veredicto,
} from '@/lib/gestion/proyeccion';
import type { Cuenta, Producto } from '@/lib/gestion/resultado';
import type { Negocio } from '@/hooks/gestion/useNegocio';
import { useSistemaStore } from '@/stores/sistemaStore';

const EJEMPLOS = [
  '¿Qué pasa si subo la yerba un 10 %?',
  '¿Me conviene empezar a vender pan casero?',
  '¿Puedo tomar un ayudante por 60 mil?',
  '¿Y si bajo la Coca un 5 %?',
];

const TIPOS: { tipo: Escenario['tipo']; etiqueta: string }[] = [
  { tipo: 'precio', etiqueta: 'Cambiar un precio' },
  { tipo: 'producto', etiqueta: 'Sumar un producto' },
  { tipo: 'gastoFijo', etiqueta: 'Sumar un gasto fijo' },
];

const VEREDICTOS: Record<Veredicto, string> = {
  conviene: 'Te conviene',
  'no-conviene': 'Así como está, no te conviene',
  parecido: 'Da casi lo mismo',
};

// «¿Me conviene?»: la persona plantea una decisión y Sami la proyecta con los números de su
// negocio, con la misma cuenta de la ganancia real que el cierre. Se ve antes y después y cómo sigue en 90 días.
export function ProyeccionPage() {
  return (
    <MarcoDeNegocio
      pantalla="proyeccion"
      titulo={
        <>
          Antes de decidir, mirá cómo te va.
          <br />
          Sami lo proyecta con tus números.
        </>
      }
      bajada="Subir un precio, sumar un producto o tomar a alguien: cuánto cambia tu ganancia de verdad, tu punto de equilibrio y los próximos 90 días."
    >
      {(negocio) => <Proyeccion negocio={negocio} />}
    </MarcoDeNegocio>
  );
}

const escenarioInicial = (productos: Producto[]): Escenario => {
  const yerba = productos.find((producto) => producto.nombre.startsWith('Yerba')) ?? productos[0];
  return { tipo: 'precio', productoId: yerba?.id ?? '', cambio: 0.1 };
};

/** Un escenario nuevo del tipo pedido, con valores razonables para empezar. */
const escenarioDeTipo = (tipo: Escenario['tipo'], productos: Producto[]): Escenario =>
  tipo === 'precio'
    ? escenarioInicial(productos)
    : tipo === 'producto'
      ? { tipo, nombre: 'Pan casero', costo: 1000, precio: 1500, unidadesPorDia: 3 }
      : { tipo, concepto: 'Un ayudante', mensual: 60000, ventasExtraPorDia: 0 };

function Proyeccion({ negocio }: { negocio: Negocio }) {
  const { datos, hoy } = negocio;
  const [escenario, setEscenario] = useState<Escenario>(() => escenarioInicial(datos.productos));
  const [pregunta, setPregunta] = useState('');
  const [preguntado, setPreguntado] = useState<string | null>(null);
  const [noEntendi, setNoEntendi] = useState(false);
  const avisar = useSistemaStore((estado) => estado.avisar);
  const proyeccion = useMemo(() => proyectar(datos, hoy, escenario), [datos, hoy, escenario]);

  const preguntar = (texto: string) => {
    const entendido = interpretarPregunta(texto, datos.productos);
    setPreguntado(texto);
    setNoEntendi(!entendido);
    if (entendido) setEscenario(entendido);
    setPregunta('');
  };

  const alEnviar = (evento: FormEvent) => {
    evento.preventDefault();
    if (pregunta.trim()) preguntar(pregunta.trim());
  };

  const aplicar = () =>
    avisar(
      escenario.tipo === 'precio'
        ? 'Listo, cambié el precio. En dos semanas te cuento si se cumplió la proyección.'
        : escenario.tipo === 'producto'
          ? `Sumé ${escenario.nombre} al inventario. En dos semanas te cuento si se cumplió la proyección.`
          : `Sumé «${escenario.concepto}» a tus gastos fijos. En dos semanas te cuento cómo vas.`,
    );

  return (
    <>
      <Seccion
        titulo="Preguntale a Sami"
        bajada="Escribí la decisión como se la contarías a alguien."
      >
        <form className="gn-pregunta" onSubmit={alEnviar}>
          <input
            type="text"
            value={pregunta}
            onChange={(evento) => setPregunta(evento.target.value)}
            placeholder="¿Qué pasa si subo la yerba un 10 %?"
            aria-label="Tu pregunta para Sami"
          />
          <Btn cls="primary" type="submit" disabled={!pregunta.trim()}>
            Proyectar <Icon name="arrow" />
          </Btn>
        </form>
        <div className="gn-ejemplos">
          {EJEMPLOS.map((ejemplo) => (
            <button
              key={ejemplo}
              type="button"
              className="gn-chip"
              onClick={() => preguntar(ejemplo)}
            >
              {ejemplo}
            </button>
          ))}
        </div>
        {preguntado && (
          <p className="gn-preguntado">
            <Icon name="note" /> «{preguntado}»
          </p>
        )}
        {noEntendi && (
          <DiceSami pose="think">
            <p>
              No terminé de entender la decisión. Probá con un precio («subo la yerba 10 %»), un
              producto nuevo («vender pan casero») o un gasto fijo («un ayudante por 60 mil»), o
              armala abajo.
            </p>
          </DiceSami>
        )}
      </Seccion>

      <Seccion titulo="La decisión" bajada="Ajustá los números: la proyección cambia sola.">
        <div className="gn-tipos" role="tablist" aria-label="Qué querés decidir">
          {TIPOS.map(({ tipo, etiqueta }) => (
            <button
              key={tipo}
              type="button"
              role="tab"
              aria-selected={escenario.tipo === tipo}
              className={escenario.tipo === tipo ? 'active' : ''}
              onClick={() =>
                escenario.tipo !== tipo && setEscenario(escenarioDeTipo(tipo, datos.productos))
              }
            >
              {etiqueta}
            </button>
          ))}
        </div>
        <EditorDeEscenario
          escenario={escenario}
          productos={datos.productos}
          onCambiar={setEscenario}
        />
      </Seccion>

      <section
        className="gn-seccion gn-resultado"
        data-veredicto={proyeccion.veredicto}
        aria-live="polite"
      >
        <header>
          <div>
            <span className="eyebrow">LA PROYECCIÓN</span>
            <h2>{VEREDICTOS[proyeccion.veredicto]}</h2>
            <p>
              Ganarías{' '}
              <strong>
                {pesos(Math.abs(proyeccion.diferencia))}{' '}
                {proyeccion.diferencia >= 0 ? 'más' : 'menos'}
              </strong>{' '}
              por mes.
            </p>
          </div>
          <Btn cls="primary" onClick={aplicar}>
            <Icon name="check" /> Aplicar la decisión
          </Btn>
        </header>
        <div className="gn-dos">
          <AntesYDespues antes={proyeccion.antes} despues={proyeccion.despues} />
          <DiceSami pose={proyeccion.veredicto === 'conviene' ? 'wink' : 'think'}>
            <ul className="gn-por-que">
              {proyeccion.porQue.map((frase) => (
                <li key={frase}>{frase}</li>
              ))}
            </ul>
          </DiceSami>
        </div>
        <GraficoDeProyeccion serie={proyeccion.serie} />
      </section>
    </>
  );
}

function AntesYDespues({ antes, despues }: { antes: Cuenta; despues: Cuenta }) {
  const filas: { etiqueta: string; antes: string; despues: string; mejor: boolean | null }[] = [
    {
      etiqueta: 'Ganancia de verdad por mes',
      antes: pesos(antes.ganancia),
      despues: pesos(despues.ganancia),
      mejor: despues.ganancia === antes.ganancia ? null : despues.ganancia > antes.ganancia,
    },
    {
      etiqueta: 'Ventas por mes',
      antes: pesos(antes.ventas),
      despues: pesos(despues.ventas),
      mejor: null,
    },
    {
      etiqueta: 'Punto de equilibrio por día',
      antes: antes.equilibrioMensual === null ? 'No se llega' : pesos(antes.equilibrioMensual / 30),
      despues:
        despues.equilibrioMensual === null ? 'No se llega' : pesos(despues.equilibrioMensual / 30),
      mejor:
        antes.equilibrioMensual === null || despues.equilibrioMensual === null
          ? null
          : despues.equilibrioMensual === antes.equilibrioMensual
            ? null
            : despues.equilibrioMensual < antes.equilibrioMensual,
    },
    {
      etiqueta: 'Cubrís los gastos fijos el',
      antes: antes.diaDeEquilibrio === null ? 'Nunca' : `Día ${antes.diaDeEquilibrio}`,
      despues:
        despues.diaDeEquilibrio === null
          ? 'Nunca'
          : despues.diaDeEquilibrio > 30
            ? 'No llegás en el mes'
            : `Día ${despues.diaDeEquilibrio}`,
      mejor:
        antes.diaDeEquilibrio === null || despues.diaDeEquilibrio === null
          ? null
          : despues.diaDeEquilibrio === antes.diaDeEquilibrio
            ? null
            : despues.diaDeEquilibrio < antes.diaDeEquilibrio,
    },
  ];
  return (
    <table className="gn-antes-despues">
      <thead>
        <tr>
          <th scope="col">
            <span className="sr-only">Dato</span>
          </th>
          <th scope="col">Hoy</th>
          <th scope="col">Con la decisión</th>
        </tr>
      </thead>
      <tbody>
        {filas.map((fila) => (
          <tr key={fila.etiqueta}>
            <th scope="row">{fila.etiqueta}</th>
            <td>{fila.antes}</td>
            <td data-mejor={fila.mejor === null ? undefined : String(fila.mejor)}>
              {fila.despues}
              {fila.mejor !== null && (
                <span className="sr-only">{fila.mejor ? ' (mejora)' : ' (empeora)'}</span>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function EditorDeEscenario({
  escenario,
  productos,
  onCambiar,
}: {
  escenario: Escenario;
  productos: Producto[];
  onCambiar: (escenario: Escenario) => void;
}) {
  const numero = (valor: string) => Math.max(0, Number(valor) || 0);

  if (escenario.tipo === 'precio') {
    const producto = productos.find((item) => item.id === escenario.productoId);
    return (
      <div className="gn-editor">
        <label>
          <span>Producto</span>
          <select
            value={escenario.productoId}
            onChange={(evento) => onCambiar({ ...escenario, productoId: evento.target.value })}
          >
            {productos.map((item) => (
              <option key={item.id} value={item.id}>
                {item.nombre} · {pesos(item.precio)}
              </option>
            ))}
          </select>
        </label>
        <label className="gn-editor-ancho">
          <span>
            Cambio de precio:{' '}
            <strong>
              {escenario.cambio > 0 ? '+' : ''}
              {Math.round(escenario.cambio * 100)} %
            </strong>
            {producto && <> → {pesos(producto.precio * (1 + escenario.cambio))}</>}
          </span>
          <input
            type="range"
            min={-20}
            max={30}
            step={1}
            value={Math.round(escenario.cambio * 100)}
            onChange={(evento) =>
              onCambiar({ ...escenario, cambio: Number(evento.target.value) / 100 })
            }
          />
        </label>
      </div>
    );
  }

  if (escenario.tipo === 'producto')
    return (
      <div className="gn-editor">
        <label>
          <span>Qué vas a vender</span>
          <input
            type="text"
            value={escenario.nombre}
            onChange={(evento) => onCambiar({ ...escenario, nombre: evento.target.value })}
          />
        </label>
        <label>
          <span>Te cuesta</span>
          <input
            type="number"
            min={0}
            value={escenario.costo}
            onChange={(evento) => onCambiar({ ...escenario, costo: numero(evento.target.value) })}
          />
        </label>
        <label>
          <span>Lo vendés a</span>
          <input
            type="number"
            min={0}
            value={escenario.precio}
            onChange={(evento) => onCambiar({ ...escenario, precio: numero(evento.target.value) })}
          />
        </label>
        <label>
          <span>Cuántos por día calculás</span>
          <input
            type="number"
            min={0}
            value={escenario.unidadesPorDia}
            onChange={(evento) =>
              onCambiar({ ...escenario, unidadesPorDia: numero(evento.target.value) })
            }
          />
        </label>
      </div>
    );

  return (
    <div className="gn-editor">
      <label>
        <span>Qué es</span>
        <input
          type="text"
          value={escenario.concepto}
          onChange={(evento) => onCambiar({ ...escenario, concepto: evento.target.value })}
        />
      </label>
      <label>
        <span>Cuánto por mes</span>
        <input
          type="number"
          min={0}
          step={5000}
          value={escenario.mensual}
          onChange={(evento) => onCambiar({ ...escenario, mensual: numero(evento.target.value) })}
        />
      </label>
      <label>
        <span>¿Cuánto más venderías por día?</span>
        <input
          type="number"
          min={0}
          step={1000}
          value={escenario.ventasExtraPorDia}
          onChange={(evento) =>
            onCambiar({ ...escenario, ventasExtraPorDia: numero(evento.target.value) })
          }
        />
        <small className="gn-gris">Por ejemplo, si con un ayudante abrís los domingos.</small>
      </label>
    </div>
  );
}
