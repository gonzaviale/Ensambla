import type { ReactNode } from 'react';
import { CheckCircleIcon, ProhibitIcon } from '@phosphor-icons/react';
import { Sami } from '@/components/sistema/Sami';
import { Explicacion } from '@/pages/Objetivos/piezas';

// Las lecciones de texto de Sami te enseña. Los importes son los de la base de ejemplo («Almacén de barrio»).

export function LeccionResultado({ onObjetivo }: { onObjetivo: () => void }) {
  return (
    <div className="ensenanza-leccion">
      <div className="objetivos-contabilidad-layout">
        <article className="objetivos-estado-resultados">
          <div className="objetivos-estado-cabecera">
            <div>
              <p>Noviembre de 2026</p>
              <h3>Estado de resultados de gestión</h3>
            </div>
            <span>
              <CheckCircleIcon aria-hidden /> 86 operaciones
            </span>
          </div>
          <Linea
            nombre="Ventas registradas"
            ayuda="Incluye ventas cobradas y fiadas"
            valor="$1.200.000"
          />
          <Linea
            nombre="− Costo estimado de lo vendido"
            ayuda="Costo actual de los productos vendidos"
            valor="−$700.000"
          />
          <Linea
            nombre="= Resultado bruto"
            ayuda="Lo que queda para cubrir los demás gastos"
            valor="$500.000"
            destacada
          />
          <Linea
            nombre="− Gastos operativos"
            ayuda="Alquiler, servicios, transporte y otros"
            valor="−$320.000"
          />
          <Linea
            nombre="= Resultado operativo estimado"
            ayuda="Resultado después de costos y gastos"
            valor="$180.000"
            total
          />
          <p className="objetivos-formula">
            Ventas − costo de mercadería vendida − gastos = resultado operativo estimado.
          </p>
        </article>
        <aside className="objetivos-traductor">
          <div className="objetivos-traductor-sami">
            <Sami pose="think" />
          </div>
          <h3>En palabras simples</h3>
          <p>
            Vendiste $1.200.000. Esos productos costaron unos $700.000 y, después de los gastos, te
            quedaron unos $180.000.
          </p>
          <div>
            <strong>Importante</strong>
            <p>
              $96.000 son ventas fiadas: cuentan en el resultado, pero todavía no son plata en mano.
            </p>
          </div>
          <button type="button" className="btn primary" onClick={onObjetivo}>
            Convertirlo en un objetivo
          </button>
        </aside>
      </div>
      <div className="objetivos-calidad">
        <strong>Calidad del análisis: alta</strong>
        <span>
          94 % del importe vendido tiene costo · 86 ventas · gastos registrados durante todo el
          período
        </span>
      </div>
    </div>
  );
}

function Linea({
  nombre,
  ayuda,
  valor,
  destacada,
  total,
}: {
  nombre: string;
  ayuda: string;
  valor: string;
  destacada?: boolean;
  total?: boolean;
}) {
  return (
    <div
      className={`objetivos-linea-contable ${destacada ? 'destacada' : ''} ${total ? 'total' : ''}`.trim()}
    >
      <div>
        <strong>{nombre}</strong>
        <span>{ayuda}</span>
      </div>
      <b>{valor}</b>
    </div>
  );
}

const METRICAS = [
  {
    nombre: 'Resultado operativo',
    valor: '+$180.000',
    que: 'Lo que le queda al negocio después de pagar la mercadería vendida y los gastos del período.',
    cuenta: 'Ventas − costo de lo vendido − gastos operativos',
    leer: 'Si es positivo, el negocio cubre sus gastos. Si crece mes a mes, mejora.',
  },
  {
    nombre: 'Margen promedio',
    valor: '34 %',
    que: 'De cada $100 que vendés, cuántos te quedan después de pagar el producto.',
    cuenta: '(Precio − costo) ÷ precio, promediado entre tus productos',
    leer: 'Un margen bajo en un producto que vendés mucho es lo primero que conviene revisar.',
  },
  {
    nombre: 'Punto de equilibrio',
    valor: '$750.000',
    que: 'Lo mínimo que tenés que vender en el mes para cubrir todos tus gastos.',
    cuenta: 'Gastos operativos ÷ margen bruto',
    leer: 'Por debajo de ese número perdés plata; por encima, ganás. La distancia es tu margen de seguridad.',
  },
  {
    nombre: 'Ventas fiadas',
    valor: '$96.000',
    que: 'Ventas ya registradas que todavía no cobraste.',
    cuenta: 'Suma de las ventas marcadas como fiadas',
    leer: 'Cuentan en el resultado, pero no son dinero disponible: resultado y plata en mano no son lo mismo.',
  },
  {
    nombre: 'Calidad del análisis',
    valor: 'Alta',
    que: 'Qué tan completos son los datos con los que Sami hace las cuentas.',
    cuenta: '% del importe vendido que tiene costo informado, más cantidad de operaciones',
    leer: 'Cuando baja, Sami te lo avisa y te dice qué falta cargar para que sus cuentas sean más firmes.',
  },
] as const;

export function LeccionMetricas() {
  return (
    <div className="ensenanza-leccion">
      <div className="ensenanza-metricas">
        {METRICAS.map((metrica) => (
          <article key={metrica.nombre} className="ensenanza-metrica">
            <header>
              <h3>{metrica.nombre}</h3>
              <strong>{metrica.valor}</strong>
            </header>
            <p>{metrica.que}</p>
            <dl>
              <div>
                <dt>Cómo se calcula</dt>
                <dd>{metrica.cuenta}</dd>
              </div>
              <div>
                <dt>Cómo leerla</dt>
                <dd>{metrica.leer}</dd>
              </div>
            </dl>
          </article>
        ))}
      </div>
    </div>
  );
}

function Bloque({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section className="ensenanza-bloque">
      <h3>{titulo}</h3>
      {children}
    </section>
  );
}

export function LeccionRecomendaciones() {
  return (
    <div className="ensenanza-leccion">
      <ul className="ensenanza-pasos">
        <li>
          <strong>Junta tus datos.</strong>
          <span>
            Ventas, costo de tus productos, gastos registrados y ventas fiadas del período.
          </span>
        </li>
        <li>
          <strong>Calcula la brecha.</strong>
          <span>La diferencia entre tu resultado de hoy y la meta de tu objetivo.</span>
        </li>
        <li>
          <strong>Arma tres caminos.</strong>
          <span>Vender más, ganar más por venta, o combinar ambos con menos gastos.</span>
        </li>
        <li>
          <strong>Recomienda uno y dice por qué.</strong>
          <span>
            Prefiere el que reparte el esfuerzo, porque depende menos de un solo supuesto.
          </span>
        </li>
      </ul>
      <div className="ensenanza-columnas">
        <Bloque titulo="Qué datos usa">
          <ul>
            <li>Tus ventas, sus costos y los gastos que registraste.</li>
            <li>Las ventas que todavía no cobraste.</li>
            <li>Estadísticas agrupadas de Mi zona, sin mostrar comercios individuales.</li>
          </ul>
        </Bloque>
        <Bloque titulo="Cuánto confía">
          <ul>
            <li>
              <b>Alta:</b> casi todo lo vendido tiene costo y hay muchas operaciones.
            </li>
            <li>
              <b>Media:</b> faltan algunos costos o hay pocas operaciones.
            </li>
            <li>
              <b>Baja:</b> faltan datos importantes. Sami te dice cuáles.
            </li>
          </ul>
        </Bloque>
      </div>
      <Bloque titulo="Qué nunca hace">
        <ul className="ensenanza-nunca">
          {[
            'Cambiar precios, gastos o registros sin que lo confirmes.',
            'Prometer un resultado: los caminos son estimaciones, y muestra sus supuestos.',
            'Reemplazar la contabilidad legal: trabaja con un estado de resultados de gestión.',
            'Inventar importes: usa los mismos números que ves en pantalla.',
          ].map((texto) => (
            <li key={texto}>
              <ProhibitIcon aria-hidden size={18} /> {texto}
            </li>
          ))}
        </ul>
      </Bloque>
      <Explicacion>
        Hoy los importes son de demostración. Con tus ventas reales la cuenta es la misma, y la
        podés revisar.
      </Explicacion>
    </div>
  );
}

const CONSEJOS = [
  {
    titulo: 'Cargá el costo de tus productos',
    texto:
      'Sin costo, Sami no puede calcular tu margen y baja la confianza de sus recomendaciones.',
  },
  {
    titulo: 'Registrá los gastos todas las semanas',
    texto: 'Un gasto que falta hace que el resultado parezca mejor de lo que es.',
  },
  {
    titulo: 'Marcá cuándo cobrás lo fiado',
    texto: 'Así Sami separa lo que ganaste de lo que todavía tenés que cobrar.',
  },
  {
    titulo: 'Mirá tus objetivos una vez por semana',
    texto:
      'Es lo que Sami necesita para comparar lo proyectado con lo real y ajustar sus supuestos.',
  },
  {
    titulo: 'Usá Mi zona como referencia, no como orden',
    texto: 'El precio de la zona sirve para saber si estás lejos o cerca, no para decidir por vos.',
  },
] as const;

export function LeccionConsejos({ onObjetivo }: { onObjetivo: () => void }) {
  return (
    <div className="ensenanza-leccion">
      <ul className="ensenanza-consejos">
        {CONSEJOS.map((consejo) => (
          <li key={consejo.titulo}>
            <strong>{consejo.titulo}</strong>
            <span>{consejo.texto}</span>
          </li>
        ))}
      </ul>
      <button type="button" className="btn primary" onClick={onObjetivo}>
        Ir a mis objetivos
      </button>
    </div>
  );
}
