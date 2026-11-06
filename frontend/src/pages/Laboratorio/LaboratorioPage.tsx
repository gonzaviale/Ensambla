import { useState } from 'react';
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  ChartLineUpIcon,
  CheckCircleIcon,
  FlaskIcon,
  TargetIcon,
  ShieldWarningIcon,
  SparkleIcon,
} from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Sami } from '@/components/sistema/Sami';
import { useTituloPagina } from '@/hooks/useTituloPagina';

const PASOS = ['Laboratorio', 'Definir meta', 'Caminos posibles', 'Probar escenario', 'Riesgos'] as const;

type Paso = 0 | 1 | 2 | 3 | 4;

export function LaboratorioPage() {
  useTituloPagina('Laboratorio de decisiones');
  const [paso, setPaso] = useState<Paso>(0);

  const ir = (destino: number) => setPaso(Math.max(0, Math.min(4, destino)) as Paso);

  return (
    <>
      <CabeceraComun active="laboratory" />
      <main id="main" tabIndex={-1} className="page-wrap laboratorio-page reveal">
        <nav className="laboratorio-pasos" aria-label="Recorrido del laboratorio">
          {PASOS.map((nombre, indice) => (
            <button
              type="button"
              key={nombre}
              className={paso === indice ? 'active' : ''}
              aria-current={paso === indice ? 'step' : undefined}
              onClick={() => ir(indice)}
            >
              <span>{indice + 1}</span>
              {nombre}
            </button>
          ))}
        </nav>

        {paso === 0 && <Portada onElegir={ir} />}
        {paso === 1 && <DefinirMeta onContinuar={() => ir(2)} />}
        {paso === 2 && <Caminos onSimular={() => ir(3)} />}
        {paso === 3 && <Escenario />}
        {paso === 4 && <Riesgos />}

        <div className="laboratorio-navegacion">
          <button type="button" className="laboratorio-volver" disabled={paso === 0} onClick={() => ir(paso - 1)}>
            <ArrowLeftIcon aria-hidden size={20} /> Pantalla anterior
          </button>
          <button type="button" className="btn primary" disabled={paso === 4} onClick={() => ir(paso + 1)}>
            Continuar recorrido <ArrowRightIcon aria-hidden size={20} />
          </button>
        </div>
      </main>
    </>
  );
}

function Encabezado({ etiqueta, titulo, texto }: { etiqueta: string; titulo: string; texto: string }) {
  return (
    <header className="laboratorio-encabezado">
      <div>
        <p className="laboratorio-etiqueta">{etiqueta}</p>
        <h1>{titulo}</h1>
        <p>{texto}</p>
      </div>
      <div className="laboratorio-sami-mini">
        <Sami pose="think" />
        <div><strong>Sami está listo</strong><span>Te explica cada cálculo</span></div>
      </div>
    </header>
  );
}

function Portada({ onElegir }: { onElegir: (paso: number) => void }) {
  return (
    <section>
      <Encabezado etiqueta="Gemelo económico" titulo="Probá una decisión antes de tomarla" texto="Ensambla usa la información contable de tu negocio para simular metas, decisiones y riesgos sin modificar tus datos reales." />
      <div className="laboratorio-acciones">
        <TarjetaAccion color="naranja" icono={<TargetIcon />} titulo="Quiero alcanzar una meta" texto="Decinos cuánto querés ganar, invertir o ahorrar. Buscamos caminos posibles para lograrlo." accion="Definir una meta" onClick={() => onElegir(1)} />
        <TarjetaAccion color="petroleo" icono={<FlaskIcon />} titulo="Quiero probar una decisión" texto="Simulá un aumento de precios, una contratación, una compra o un cambio en tus gastos." accion="Probar una decisión" onClick={() => onElegir(3)} />
        <TarjetaAccion color="coral" icono={<ShieldWarningIcon />} titulo="Quiero anticipar problemas" texto="Revisá qué podría afectar tu rentabilidad, tus cobros o el cumplimiento de tus metas." accion="Ver riesgos" onClick={() => onElegir(4)} />
      </div>
      <div className="laboratorio-base">
        <section className="laboratorio-panel">
          <h2>Tu base económica actual</h2>
          <Dato etiqueta="Resultado mensual estimado" valor="+$180.000" positivo />
          <Dato etiqueta="Margen promedio" valor="34%" />
          <Dato etiqueta="Punto de equilibrio" valor="$750.000" />
          <Dato etiqueta="Ventas fiadas" valor="$96.000" />
        </section>
        <aside className="laboratorio-aviso"><SparkleIcon aria-hidden size={25} /><div><strong>Sami observa</strong><p>El resultado es positivo, pero parte de las ventas todavía está pendiente de cobro. Resultado y dinero disponible no significan lo mismo.</p></div></aside>
      </div>
    </section>
  );
}

function TarjetaAccion({ color, icono, titulo, texto, accion, onClick }: { color: string; icono: React.ReactNode; titulo: string; texto: string; accion: string; onClick: () => void }) {
  return <button type="button" className={`laboratorio-accion ${color}`} onClick={onClick}><span className="laboratorio-accion-icono">{icono}</span><strong>{titulo}</strong><p>{texto}</p><span className="laboratorio-accion-enlace">{accion} <ArrowRightIcon aria-hidden /></span></button>;
}

function Dato({ etiqueta, valor, positivo }: { etiqueta: string; valor: string; positivo?: boolean }) {
  return <div className="laboratorio-dato"><span>{etiqueta}</span><strong className={positivo ? 'positivo' : ''}>{valor}</strong></div>;
}

function DefinirMeta({ onContinuar }: { onContinuar: () => void }) {
  return <section><Encabezado etiqueta="Meta económica" titulo="¿Qué querés lograr?" texto="Escribilo con tus palabras. Sami lo transforma en una meta medible usando información real del negocio." /><div className="laboratorio-meta"><label htmlFor="meta-demo">Contale tu objetivo a Sami</label><textarea id="meta-demo" defaultValue="Quiero obtener una ganancia de $500.000 por mes sin aumentar demasiado los precios." /><div className="laboratorio-meta-pie"><span>Vamos a separar resultado, cobranzas y riesgo.</span><button type="button" className="btn primary" onClick={onContinuar}>Analizar mi meta</button></div></div><div className="laboratorio-ejemplos"><span>“Quiero contratar a una persona”</span><span>“Quiero recuperar mis gastos antes del día 20”</span><span>“¿Cuánto puedo retirar sin afectar el negocio?”</span></div><aside className="laboratorio-fundamento"><ChartLineUpIcon aria-hidden size={28} /><div><strong>Fundamento del análisis</strong><p>Ventas − costo de mercadería − gastos = resultado. También revisamos margen, punto de equilibrio, stock y ventas pendientes de cobro.</p></div></aside></section>;
}

function Caminos({ onSimular }: { onSimular: () => void }) {
  return <section><Encabezado etiqueta="Objetivo analizado" titulo="Tres caminos para llegar a $500.000" texto="No son promesas: son combinaciones calculadas sobre tu estructura actual de ventas, costos y gastos." /><div className="laboratorio-metricas"><Metrica etiqueta="Resultado actual" valor="$180.000" /><Metrica etiqueta="Objetivo elegido" valor="$500.000" /><Metrica etiqueta="Diferencia por cubrir" valor="$320.000" alerta /></div><div className="laboratorio-caminos"><Camino recomendado titulo="Camino combinado" bajada="Reparte el esfuerzo entre ventas, margen y gastos." datos={['Vender más|+12%', 'Reducir gastos|−$80.000', 'Mejorar margen|+3 puntos']} onClick={onSimular} /><Camino titulo="Vender más" bajada="Mantiene costos y margen actuales." datos={['Aumento de ventas|+27%', 'Ventas adicionales|≈145', 'Cambio de precio|0%']} /><Camino titulo="Ganar más por venta" bajada="No exige vender más unidades." datos={['Margen actual|34%', 'Margen necesario|43%', 'Productos a revisar|8']} /></div><Explicacion texto="Recomiendo combinar porque aumentar solamente las ventas exige muchas operaciones adicionales y aumentar solo los precios podría reducir la demanda." /></section>;
}

function Metrica({ etiqueta, valor, alerta }: { etiqueta: string; valor: string; alerta?: boolean }) { return <div className={`laboratorio-metrica ${alerta ? 'alerta' : ''}`}><span>{etiqueta}</span><strong>{valor}</strong></div>; }

function Camino({ titulo, bajada, datos, recomendado, onClick }: { titulo: string; bajada: string; datos: string[]; recomendado?: boolean; onClick?: () => void }) { return <article className={`laboratorio-camino ${recomendado ? 'recomendado' : ''}`}>{recomendado && <span className="laboratorio-recomendado">Recomendado por equilibrio</span>}<h2>{titulo}</h2><p>{bajada}</p><dl>{datos.map((dato) => { const [nombre, valor] = dato.split('|'); return <div key={nombre}><dt>{nombre}</dt><dd>{valor}</dd></div>; })}</dl><small><CheckCircleIcon aria-hidden /> {recomendado ? 'Confianza media-alta' : 'Requiere revisar sus supuestos'}</small><button type="button" className={recomendado ? 'btn primary' : 'btn secondary'} onClick={onClick}>{recomendado ? 'Simular este camino' : 'Ver en detalle'}</button></article>; }

function Escenario() {
  return <section><Encabezado etiqueta="Simulación económica" titulo="¿Qué pasa si aplicás este camino?" texto="Modificá los supuestos y observá el efecto sobre resultado y punto de equilibrio. Esta vista todavía usa datos de demostración." /><div className="laboratorio-simulador"><div className="laboratorio-controles"><h2>Cambios a probar</h2><Control etiqueta="Ventas mensuales" valor="+12%" posicion="62%" /><Control etiqueta="Margen promedio" valor="34% → 37%" posicion="57%" /><Control etiqueta="Gastos mensuales" valor="−$80.000" posicion="45%" /><p><strong>Supuesto:</strong> la cantidad vendida no cae al modificar precios.</p></div><div className="laboratorio-resultados"><Metrica etiqueta="Resultado proyectado" valor="$512.000" /><Metrica etiqueta="Punto de equilibrio" valor="$645.000" /><Metrica etiqueta="Margen de seguridad" valor="9%" /><div className="laboratorio-grafico"><h2>Comparación de escenarios</h2><div><Barra clase="coral" alto="42%" nombre="Si vendo menos" valor="$210.000" /><Barra clase="celeste" alto="68%" nombre="Ritmo esperado" valor="$398.000" /><Barra clase="naranja" alto="88%" nombre="Camino elegido" valor="$512.000" /></div></div></div></div><Explicacion texto="La meta se alcanzaría principalmente por la mejora del margen y la reducción de gastos. Si las ventas bajaran más de 9%, el resultado quedaría por debajo del objetivo." /></section>;
}

function Control({ etiqueta, valor, posicion }: { etiqueta: string; valor: string; posicion: string }) { return <label className="laboratorio-control"><span>{etiqueta}<strong>{valor}</strong></span><input aria-label={etiqueta} type="range" defaultValue={Number.parseInt(posicion)} /></label>; }
function Barra({ clase, alto, nombre, valor }: { clase: string; alto: string; nombre: string; valor: string }) { return <figure><div className={`laboratorio-barra ${clase}`} style={{ height: alto }} /><figcaption>{nombre}<strong>{valor}</strong></figcaption></figure>; }

function Riesgos() {
  return <section><Encabezado etiqueta="Alerta temprana" titulo="Qué podría afectar tu negocio" texto="Sami conecta rentabilidad, cobranzas, gastos y stock para señalar problemas antes de que se vuelvan difíciles de resolver." /><div className="laboratorio-cadena"><span>Ventas fiadas crecen<b>+18% este mes</b></span><ArrowRightIcon /><span>Los cobros se demoran<b>Promedio: 24 días</b></span><ArrowRightIcon /><span>Resultado y caja se separan<b>Requiere seguimiento</b></span></div><div className="laboratorio-riesgos"><Riesgo prioridad="Prioridad alta" titulo="Las ventas pendientes representan una parte importante" texto="El resultado contable incluye $96.000 en ventas fiadas. Ensambla no las presenta como dinero disponible." fundamento="Principio de devengado y separación entre resultado y flujo de efectivo." /><Riesgo prioridad="Prioridad media" titulo="El margen baja aunque las ventas crecen" texto="Las ventas crecieron 9%, pero el resultado bruto solo creció 2%. Tres productos concentran la diferencia." fundamento="Comparación del margen bruto y el costo estimado de lo vendido." /></div><Explicacion texto="Podés simular una mejora en las cobranzas, revisar el margen de los productos o reducir un gasto. Te mostraré qué alternativa protege mejor el resultado." /></section>;
}

function Riesgo({ prioridad, titulo, texto, fundamento }: { prioridad: string; titulo: string; texto: string; fundamento: string }) { return <article className="laboratorio-riesgo"><span>{prioridad}</span><h2>{titulo}</h2><p>{texto}</p><small><strong>Fundamento:</strong> {fundamento}</small></article>; }
function Explicacion({ texto }: { texto: string }) { return <aside className="laboratorio-explicacion"><div className="laboratorio-sami-cara"><Sami pose="think" /></div><div><strong>Sami te lo explica</strong><p>{texto}</p></div></aside>; }
