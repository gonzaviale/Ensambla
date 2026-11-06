import { useMemo, useState, type FormEvent, type ReactNode } from 'react';
import { GraficoDeProyeccion } from '@/components/gestion/GraficoDeProyeccion';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import { CampoDePlata, DiceSami, Seccion } from '@/components/gestion/piezas';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { pesos } from '@/lib/gestion/formato';
import {
  interpretarPregunta,
  proyectar,
  supuestoNuevo,
  type Origen,
  type Supuesto,
  type TipoDeSupuesto,
  type Umbral,
  type Veredicto,
} from '@/lib/gestion/proyeccion';
import type { Cuenta, Producto } from '@/lib/gestion/resultado';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Negocio } from '@/hooks/gestion/useNegocio';
import { useSistemaStore } from '@/stores/sistemaStore';

const EJEMPLOS = [
  '¿Qué pasa si subo la yerba un 10 %?',
  '¿Me conviene abrir los domingos?',
  '¿Puedo tomar un ayudante por 60 mil?',
  '¿Me conviene comprar una heladera de 450 mil?',
  '¿Me conviene empezar a vender pan casero?',
];

const ORIGENES: Record<Origen, string> = {
  cuaderno: 'De tu cuaderno',
  zona: 'De comercios parecidos',
  supuesto: 'Lo supuse yo',
  vos: 'Lo pusiste vos',
};

const PARA_SUMAR: { tipo: TipoDeSupuesto; etiqueta: string; icono: NombreDeIcono }[] = [
  { tipo: 'ventas', etiqueta: 'Ventas de más', icono: 'chart' },
  { tipo: 'gastoFijo', etiqueta: 'Un gasto por mes', icono: 'wallet' },
  { tipo: 'inversion', etiqueta: 'Una compra grande', icono: 'box' },
  { tipo: 'precio', etiqueta: 'Un precio', icono: 'edit' },
  { tipo: 'producto', etiqueta: 'Un producto nuevo', icono: 'plus' },
  { tipo: 'aparte', etiqueta: 'Algo que no es plata', icono: 'note' },
];

const VEREDICTOS: Record<Veredicto, string> = {
  conviene: 'Te conviene',
  'no-conviene': 'Así como está, no te conviene',
  parecido: 'Da casi lo mismo',
};

// «¿Me conviene?»: la persona pregunta lo que quiera y Sami lo traduce en supuestos sobre la
// cuenta, cada uno con de dónde sale su número. Lo que no se puede saber se convierte en un umbral: «te conviene si
// vendés más de…». Se ve antes y después con la misma cuenta de la ganancia real y cómo sigue en 90 días.
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
      bajada="Preguntá lo que quieras: un precio, abrir más horas, tomar a alguien, una compra grande. Sami te dice qué supuso y desde cuánto te conviene."
    >
      {(negocio) => <Proyeccion negocio={negocio} />}
    </MarcoDeNegocio>
  );
}

function Proyeccion({ negocio }: { negocio: Negocio }) {
  const { datos, hoy } = negocio;
  const [inicial] = useState(() => interpretarPregunta(EJEMPLOS[0] ?? '', datos, hoy));
  const [supuestos, setSupuestos] = useState<Supuesto[]>(inicial.supuestos);
  const [pregunta, setPregunta] = useState('');
  const [preguntado, setPreguntado] = useState(EJEMPLOS[0] ?? '');
  const [entendida, setEntendida] = useState(inicial.entendida);
  const avisar = useSistemaStore((estado) => estado.avisar);
  const proyeccion = useMemo(() => proyectar(datos, hoy, supuestos), [datos, hoy, supuestos]);

  const preguntar = (texto: string) => {
    const decision = interpretarPregunta(texto, datos, hoy);
    setPreguntado(texto);
    setEntendida(decision.entendida);
    setSupuestos(decision.supuestos);
    setPregunta('');
  };

  const alEnviar = (evento: FormEvent) => {
    evento.preventDefault();
    if (pregunta.trim()) preguntar(pregunta.trim());
  };

  const cambiar = (cambiado: Supuesto) =>
    setSupuestos((lista) =>
      lista.map((supuesto) =>
        supuesto.id === cambiado.id ? { ...cambiado, origen: 'vos' } : supuesto,
      ),
    );
  const quitar = (id: string) =>
    setSupuestos((lista) => lista.filter((supuesto) => supuesto.id !== id));
  const sumar = (tipo: TipoDeSupuesto) =>
    setSupuestos((lista) => [...lista, supuestoNuevo(tipo, datos, hoy)]);

  const soloPrecios = supuestos.length > 0 && supuestos.every((item) => item.tipo === 'precio');
  const aplicar = () =>
    avisar(
      soloPrecios
        ? 'Listo, cambié el precio. En dos semanas te cuento si se cumplió la proyección.'
        : 'Anoté la decisión con estos supuestos. En dos semanas comparo lo que pasó con la proyección.',
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
            placeholder="¿Me conviene abrir los domingos?"
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
              aria-pressed={preguntado === ejemplo}
              onClick={() => preguntar(ejemplo)}
            >
              {ejemplo}
            </button>
          ))}
        </div>
      </Seccion>

      <Seccion
        titulo="Lo que supuso Sami"
        bajada={
          <>
            Para «{preguntado}». Cambiá cualquier número y la cuenta se rehace sola; cada uno dice
            de dónde salió.
          </>
        }
      >
        {!entendida && (
          <DiceSami pose="think">
            <p>
              No sé bien qué cambia en tu negocio con eso, así que armé la cuenta con lo básico.
              Poné cuánto te costaría por mes; lo que venderías de más no hace falta que lo sepas:
              te digo desde cuánto conviene. Si cambia algo más, sumalo abajo.
            </p>
          </DiceSami>
        )}
        {supuestos.length === 0 ? (
          <p className="gn-gris">Sin supuestos, la cuenta queda igual que hoy. Sumá alguno.</p>
        ) : (
          <ul className="gn-supuestos">
            {supuestos.map((supuesto) => (
              <EditorDeSupuesto
                key={supuesto.id}
                supuesto={supuesto}
                productos={datos.productos}
                umbral={proyeccion.umbral?.supuestoId === supuesto.id ? proyeccion.umbral : null}
                onCambiar={cambiar}
                onQuitar={() => quitar(supuesto.id)}
              />
            ))}
          </ul>
        )}
        <div className="gn-sumar">
          <span className="gn-gris">Sumar a la cuenta:</span>
          {PARA_SUMAR.map(({ tipo, etiqueta, icono }) => (
            <button key={tipo} type="button" className="gn-chip" onClick={() => sumar(tipo)}>
              <Icon name={icono} /> {etiqueta}
            </button>
          ))}
        </div>
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
              por mes
              {proyeccion.inversion > 0 && <>, después de poner {pesos(proyeccion.inversion)}</>}.
            </p>
          </div>
          <Btn cls="primary" onClick={aplicar}>
            <Icon name="check" /> {soloPrecios ? 'Cambiar el precio' : 'Anotar la decisión'}
          </Btn>
        </header>
        {proyeccion.umbral && (
          <p className="gn-umbral">
            <Icon name="bulb" /> <span>{proyeccion.umbral.frase}</span>
          </p>
        )}
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

const ICONOS: Record<TipoDeSupuesto, NombreDeIcono> = {
  precio: 'edit',
  producto: 'plus',
  ventas: 'chart',
  gastoFijo: 'wallet',
  inversion: 'box',
  aparte: 'note',
};

const numero = (valor: string) => Math.max(0, Number(valor.replace(',', '.')) || 0);

function Campo({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  return (
    <label className="gn-supuesto-campo">
      <span>{etiqueta}</span>
      {children}
    </label>
  );
}

/** Un supuesto de la cuenta: qué es, sus números para cambiar y de dónde salen. */
function EditorDeSupuesto({
  supuesto,
  productos,
  umbral,
  onCambiar,
  onQuitar,
}: {
  supuesto: Supuesto;
  productos: Producto[];
  umbral: Umbral | null;
  onCambiar: (supuesto: Supuesto) => void;
  onQuitar: () => void;
}) {
  const incognita =
    (supuesto.tipo === 'ventas' || supuesto.tipo === 'producto') && supuesto.incognita;

  let titulo: string;
  let campos: ReactNode;
  switch (supuesto.tipo) {
    case 'precio': {
      const producto = productos.find((item) => item.id === supuesto.productoId);
      titulo = `Cambiar el precio${producto ? ` de ${producto.nombre}` : ''}`;
      campos = (
        <>
          <Campo etiqueta="Producto">
            <select
              value={supuesto.productoId}
              onChange={(evento) => onCambiar({ ...supuesto, productoId: evento.target.value })}
            >
              {productos.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.nombre} · {pesos(item.precio)}
                </option>
              ))}
            </select>
          </Campo>
          <Campo etiqueta="Cambio del precio">
            <span className="gn-con-unidad">
              <input
                type="number"
                step={1}
                value={Math.round(supuesto.cambio * 100)}
                onChange={(evento) =>
                  onCambiar({ ...supuesto, cambio: (Number(evento.target.value) || 0) / 100 })
                }
              />
              <span>%</span>
            </span>
            {producto && (
              <small className="gn-gris">
                Queda a {pesos(producto.precio * (1 + supuesto.cambio))}
              </small>
            )}
          </Campo>
          <Campo etiqueta="Por cada 10 % que sube, la venta cae">
            <span className="gn-con-unidad">
              <input
                type="number"
                min={0}
                step={1}
                value={Math.round(supuesto.sensibilidad * 10)}
                onChange={(evento) =>
                  onCambiar({ ...supuesto, sensibilidad: numero(evento.target.value) / 10 })
                }
              />
              <span>%</span>
            </span>
          </Campo>
        </>
      );
      break;
    }
    case 'producto':
      titulo = 'Vender un producto nuevo';
      campos = (
        <>
          <Campo etiqueta="Qué">
            <input
              type="text"
              value={supuesto.nombre}
              onChange={(evento) => onCambiar({ ...supuesto, nombre: evento.target.value })}
            />
          </Campo>
          <Campo etiqueta="Te cuesta">
            <CampoDePlata
              valor={supuesto.costo}
              onCambiar={(costo) => onCambiar({ ...supuesto, costo })}
            />
          </Campo>
          <Campo etiqueta="Lo vendés a">
            <CampoDePlata
              valor={supuesto.precio}
              onCambiar={(precio) => onCambiar({ ...supuesto, precio })}
            />
          </Campo>
          <Campo etiqueta="Unidades por día">
            <input
              type="number"
              min={0}
              step={0.5}
              value={supuesto.unidadesPorDia}
              onChange={(evento) =>
                onCambiar({ ...supuesto, unidadesPorDia: numero(evento.target.value) })
              }
            />
          </Campo>
        </>
      );
      break;
    case 'ventas':
      titulo = supuesto.etiqueta;
      campos = (
        <Campo
          etiqueta={
            supuesto.cuando === 'por día' ? 'Por día' : `Cada vez (${supuesto.vecesPorMes} por mes)`
          }
        >
          <CampoDePlata
            valor={supuesto.monto}
            onCambiar={(monto) => onCambiar({ ...supuesto, monto })}
          />
        </Campo>
      );
      break;
    case 'gastoFijo':
      titulo = 'Un gasto por mes';
      campos = (
        <>
          <Campo etiqueta="Qué">
            <input
              type="text"
              value={supuesto.concepto}
              onChange={(evento) => onCambiar({ ...supuesto, concepto: evento.target.value })}
            />
          </Campo>
          <Campo etiqueta="Por mes">
            <CampoDePlata
              valor={supuesto.mensual}
              onCambiar={(mensual) => onCambiar({ ...supuesto, mensual })}
            />
          </Campo>
        </>
      );
      break;
    case 'inversion':
      titulo = 'Una compra grande, una sola vez';
      campos = (
        <>
          <Campo etiqueta="Qué">
            <input
              type="text"
              value={supuesto.concepto}
              onChange={(evento) => onCambiar({ ...supuesto, concepto: evento.target.value })}
            />
          </Campo>
          <Campo etiqueta="Cuánto">
            <CampoDePlata
              valor={supuesto.monto}
              onCambiar={(monto) => onCambiar({ ...supuesto, monto })}
            />
          </Campo>
        </>
      );
      break;
    case 'aparte':
      titulo = 'No cuenta como plata';
      campos = (
        <Campo etiqueta="Qué">
          <input
            type="text"
            value={supuesto.texto}
            onChange={(evento) => onCambiar({ ...supuesto, texto: evento.target.value })}
          />
        </Campo>
      );
      break;
  }

  return (
    <li className="gn-supuesto" data-tipo={supuesto.tipo} data-incognita={incognita || undefined}>
      <header>
        <Icon name={ICONOS[supuesto.tipo]} />
        <strong>{titulo}</strong>
        {supuesto.tipo !== 'aparte' && (
          <span className="tag outline" data-origen={supuesto.origen}>
            {incognita && supuesto.origen !== 'vos' ? 'No lo sabés' : ORIGENES[supuesto.origen]}
          </span>
        )}
        <button
          type="button"
          className="gn-quitar"
          aria-label={`Quitar «${titulo}» de la cuenta`}
          onClick={onQuitar}
        >
          <Icon name="close" />
        </button>
      </header>
      <div className="gn-supuesto-campos">{campos}</div>
      {umbral ? (
        <p className="gn-supuesto-umbral">
          <Icon name="bulb" /> {umbral.frase}
        </p>
      ) : (
        supuesto.nota && <small className="gn-gris">{supuesto.nota}</small>
      )}
    </li>
  );
}
