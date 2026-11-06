import { useState } from 'react';
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  ChartLineUpIcon,
  CheckCircleIcon,
  ClipboardTextIcon,
  FlaskIcon,
  MapPinIcon,
  TargetIcon,
  ShieldWarningIcon,
  SparkleIcon,
} from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Sami } from '@/components/sistema/Sami';
import { useTituloPagina } from '@/hooks/useTituloPagina';

const PASOS = [
  'Inicio',
  'Aviso económico',
  'Fundamento contable',
  'Definir meta',
  'Caminos posibles',
  'Probar escenario',
  'Mi zona',
  'Plan de acción',
  'Seguimiento',
] as const;

type Paso = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8;

export function LaboratorioPage() {
  useTituloPagina('Laboratorio de decisiones');
  const [paso, setPaso] = useState<Paso>(0);

  const ir = (destino: number) =>
    setPaso(Math.max(0, Math.min(PASOS.length - 1, destino)) as Paso);

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
        {paso === 1 && <AvisoEconomico onEntender={() => ir(2)} onSimular={() => ir(5)} />}
        {paso === 2 && <SaludContable onMeta={() => ir(3)} />}
        {paso === 3 && <DefinirMeta onContinuar={() => ir(4)} />}
        {paso === 4 && <Caminos onSimular={() => ir(5)} />}
        {paso === 5 && <Escenario />}
        {paso === 6 && <CruceConZona onPlan={() => ir(7)} />}
        {paso === 7 && <PlanDeAccion onSeguimiento={() => ir(8)} />}
        {paso === 8 && <Seguimiento />}

        <div className="laboratorio-navegacion">
          <button type="button" className="laboratorio-volver" disabled={paso === 0} onClick={() => ir(paso - 1)}>
            <ArrowLeftIcon aria-hidden size={20} /> Pantalla anterior
          </button>
          <button type="button" className="btn primary" disabled={paso === PASOS.length - 1} onClick={() => ir(paso + 1)}>
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
        <TarjetaAccion color="naranja" icono={<SparkleIcon />} titulo="Ver el recorrido completo" texto="Empezá en un aviso económico y seguí el análisis hasta convertirlo en una decisión medible." accion="Comenzar por el aviso" onClick={() => onElegir(1)} />
        <TarjetaAccion color="petroleo" icono={<TargetIcon />} titulo="Quiero alcanzar una meta" texto="Decinos cuánto querés ganar, invertir o ahorrar. Buscamos caminos posibles para lograrlo." accion="Definir una meta" onClick={() => onElegir(3)} />
        <TarjetaAccion color="coral" icono={<FlaskIcon />} titulo="Quiero probar una decisión" texto="Simulá un aumento de precios, una contratación, una compra o un cambio en tus gastos." accion="Probar una decisión" onClick={() => onElegir(5)} />
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

function AvisoEconomico({ onEntender, onSimular }: { onEntender: () => void; onSimular: () => void }) {
  return (
    <section>
      <Encabezado etiqueta="Avisos + motor económico" titulo="Vendiste más, pero tu margen bajó" texto="Sami detectó un cambio importante y lo conecta con el Laboratorio para entenderlo antes de tomar una decisión." />
      <Demuestra texto="Cómo Ensambla pasa de registrar operaciones a detectar un problema económico con evidencia y ofrecer una acción concreta." />
      <article className="laboratorio-aviso-economico">
        <div className="laboratorio-aviso-prioridad"><ShieldWarningIcon aria-hidden size={28} /><span>Prioridad alta</span></div>
        <div className="laboratorio-aviso-contenido">
          <p className="laboratorio-etiqueta">Rentabilidad · Almacén de barrio</p>
          <h2>Las ventas crecieron 12 %, pero el resultado bruto bajó 4 %</h2>
          <p>El costo de los productos aumentó más que los precios. Aceite, leche y yerba explican la mayor parte de la diferencia.</p>
          <div className="laboratorio-evidencias">
            <Dato etiqueta="Ventas" valor="+12%" positivo />
            <Dato etiqueta="Resultado bruto" valor="−4%" />
            <Dato etiqueta="Productos involucrados" valor="3" />
            <Dato etiqueta="Confianza" valor="Alta" positivo />
          </div>
          <div className="laboratorio-acciones-aviso">
            <button type="button" className="btn primary" onClick={onEntender}>Entender por qué</button>
            <button type="button" className="btn secondary" onClick={onSimular}>Probar una solución</button>
            <button type="button" className="laboratorio-textual">Ver productos involucrados</button>
          </div>
        </div>
      </article>
      <Explicacion texto="No estoy suponiendo que vender más siempre es mejor. Comparé ventas, costos y margen entre períodos. El aviso tiene confianza alta porque el 94 % del importe vendido tiene costo informado." />
    </section>
  );
}

function SaludContable({ onMeta }: { onMeta: () => void }) {
  return (
    <section>
      <Encabezado etiqueta="Fundamento contable" titulo="Así se forma el resultado del negocio" texto="Cada importe puede explicarse con sus operaciones de origen. La contabilidad está por debajo; Sami la traduce a lenguaje cotidiano." />
      <Demuestra texto="La diferencia entre vender, obtener margen y tener un resultado positivo. También muestra límites y calidad de la información." />
      <div className="laboratorio-contabilidad-layout">
        <article className="laboratorio-estado-resultados">
          <div className="laboratorio-estado-cabecera"><div><p>Noviembre de 2026</p><h2>Estado de resultados de gestión</h2></div><span><CheckCircleIcon aria-hidden /> 86 operaciones</span></div>
          <LineaContable nombre="Ventas registradas" ayuda="Incluye ventas cobradas y fiadas" valor="$1.200.000" />
          <LineaContable nombre="− Costo estimado de lo vendido" ayuda="Costo actual de los productos vendidos" valor="−$700.000" />
          <LineaContable nombre="= Resultado bruto" ayuda="Lo que queda para cubrir los demás gastos" valor="$500.000" destacada />
          <LineaContable nombre="− Gastos operativos" ayuda="Alquiler, servicios, transporte y otros" valor="−$320.000" />
          <LineaContable nombre="= Resultado operativo estimado" ayuda="Resultado después de costos y gastos" valor="$180.000" total />
          <p className="laboratorio-formula">Ventas − costo de mercadería vendida − gastos = resultado operativo estimado.</p>
        </article>
        <aside className="laboratorio-traductor">
          <div className="laboratorio-traductor-sami"><Sami pose="think" /></div>
          <h2>En palabras simples</h2>
          <p>Vendiste por $1.200.000. Los productos que salieron habían costado aproximadamente $700.000. Después de los gastos registrados, el negocio generó un resultado estimado de $180.000.</p>
          <div><strong>Importante</strong><p>$96.000 corresponden a ventas fiadas. Forman parte del resultado, pero no representan dinero disponible.</p></div>
          <button type="button" className="btn primary" onClick={onMeta}>Convertirlo en una meta</button>
        </aside>
      </div>
      <div className="laboratorio-calidad"><strong>Calidad del análisis: alta</strong><span>94 % del importe vendido tiene costo · 86 ventas · gastos registrados durante todo el período</span></div>
    </section>
  );
}

function Demuestra({ texto }: { texto: string }) {
  return <aside className="laboratorio-demuestra"><strong>Qué demuestra esta pantalla</strong><p>{texto}</p></aside>;
}

function LineaContable({ nombre, ayuda, valor, destacada, total }: { nombre: string; ayuda: string; valor: string; destacada?: boolean; total?: boolean }) {
  return <div className={`laboratorio-linea-contable ${destacada ? 'destacada' : ''} ${total ? 'total' : ''}`}><div><strong>{nombre}</strong><span>{ayuda}</span></div><b>{valor}</b></div>;
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

function CruceConZona({ onPlan }: { onPlan: () => void }) {
  return (
    <section>
      <Encabezado etiqueta="Laboratorio + Mi zona" titulo="Compará la decisión con tu contexto" texto="El precio de la zona no decide por vos: funciona como una referencia agregada para evaluar si una alternativa es razonable." />
      <Demuestra texto="Cómo Ensambla combina información interna del negocio con estadísticas agrupadas de Mi zona sin mostrar comercios individuales." />
      <div className="laboratorio-zona-layout">
        <article className="laboratorio-producto-zona">
          <div className="laboratorio-producto-titulo"><div><p className="laboratorio-etiqueta">Producto involucrado</p><h2>Aceite de girasol 1,5 L</h2></div><MapPinIcon aria-hidden size={28} /></div>
          <div className="laboratorio-precios-zona"><Metrica etiqueta="Tu precio actual" valor="$3.500" /><Metrica etiqueta="Promedio de la zona" valor="$3.820" /><Metrica etiqueta="Precio para margen de 25%" valor="$3.873" /></div>
          <div className="laboratorio-rango"><span style={{ left: '16%' }}>$3.420 mínimo</span><i /><b style={{ left: '42%' }}>Tu precio</b><em style={{ left: '67%' }}>Promedio</em><span style={{ right: '0' }}>$4.120 máximo</span></div>
          <p className="laboratorio-privacidad">Datos ficticios y agrupados de 8 comercios para esta demostración. No se muestran nombres ni direcciones.</p>
        </article>
        <aside className="laboratorio-alternativas-zona">
          <h2>Tres alternativas</h2>
          <AlternativaZona nombre="Mantener" precio="$3.500" margen="17%" aporte="$47.600" />
          <AlternativaZona nombre="Acercarse al promedio" precio="$3.820" margen="24%" aporte="$66.240" recomendada />
          <AlternativaZona nombre="Alcanzar margen objetivo" precio="$3.873" margen="25%" aporte="$69.328" />
          <button type="button" className="btn primary" onClick={onPlan}>Usar en mi plan</button>
        </aside>
      </div>
      <Explicacion texto="El promedio de la zona es una referencia, no una orden. Si llevás el precio a $3.820 y las unidades bajan menos de 8 %, el aporte seguiría mejorando. Podés cambiar esa caída supuesta." />
    </section>
  );
}

function AlternativaZona({ nombre, precio, margen, aporte, recomendada }: { nombre: string; precio: string; margen: string; aporte: string; recomendada?: boolean }) {
  return <div className={`laboratorio-alternativa-zona ${recomendada ? 'recomendada' : ''}`}><div><strong>{nombre}</strong>{recomendada && <span>Equilibrada</span>}</div><dl><div><dt>Precio</dt><dd>{precio}</dd></div><div><dt>Margen</dt><dd>{margen}</dd></div><div><dt>Aporte estimado</dt><dd>{aporte}</dd></div></dl></div>;
}

function PlanDeAccion({ onSeguimiento }: { onSeguimiento: () => void }) {
  return (
    <section>
      <Encabezado etiqueta="Decisión convertida en gestión" titulo="Un plan pequeño, medible y revisable" texto="La simulación no cambia precios ni registros. El usuario elige una estrategia y Ensambla la convierte en objetivos semanales." />
      <Demuestra texto="Cómo la aplicación pasa de recomendar a acompañar una decisión sin ejecutarla automáticamente ni ocultar sus supuestos." />
      <div className="laboratorio-plan-layout">
        <article className="laboratorio-plan">
          <header><ClipboardTextIcon aria-hidden size={28} /><div><p className="laboratorio-etiqueta">Plan noviembre</p><h2>Mejorar el resultado sin depender solo de vender más</h2></div></header>
          <TareaPlan numero="1" titulo="Revisar el precio del aceite" detalle="Probar $3.820 durante una semana" impacto="+$18.640 estimados" />
          <TareaPlan numero="2" titulo="Reducir gastos extraordinarios" detalle="Meta: $20.000 menos esta semana" impacto="+$20.000 estimados" />
          <TareaPlan numero="3" titulo="Aumentar ventas con margen sano" detalle="Objetivo: 12 ventas adicionales" impacto="+$14.400 estimados" />
        </article>
        <aside className="laboratorio-meta-plan"><p>Meta mensual</p><strong>$500.000</strong><span>Resultado operativo estimado</span><div><i style={{ width: '64%' }} /></div><b>64 % del objetivo proyectado</b><small>Se revisa cada semana. Nada se aplica sin confirmación.</small><button type="button" className="btn primary" onClick={onSeguimiento}>Ver cómo se mediría</button></aside>
      </div>
    </section>
  );
}

function TareaPlan({ numero, titulo, detalle, impacto }: { numero: string; titulo: string; detalle: string; impacto: string }) {
  return <div className="laboratorio-tarea"><span>{numero}</span><div><strong>{titulo}</strong><p>{detalle}</p></div><b>{impacto}</b></div>;
}

function Seguimiento() {
  return (
    <section>
      <Encabezado etiqueta="Proyectado frente a real" titulo="Ensambla también controla qué ocurrió" texto="Después de una decisión, el sistema compara el escenario elegido con los resultados registrados y mejora los próximos supuestos." />
      <Demuestra texto="El cierre del ciclo de gestión: registrar, interpretar, detectar, simular, decidir y medir, manteniendo trazabilidad." />
      <div className="laboratorio-seguimiento">
        <Metrica etiqueta="Resultado proyectado" valor="$500.000" />
        <Metrica etiqueta="Resultado observado" valor="$462.000" />
        <Metrica etiqueta="Diferencia" valor="−$38.000" alerta />
      </div>
      <div className="laboratorio-cadena"><span>Se detectó un cambio<b>Aviso económico</b></span><ArrowRightIcon /><span>Se probaron alternativas<b>Laboratorio + Mi zona</b></span><ArrowRightIcon /><span>Se midió el resultado<b>Aprendizaje para el próximo plan</b></span></div>
      <article className="laboratorio-cierre"><div className="laboratorio-cierre-sami"><Sami pose="hello" /></div><div><p className="laboratorio-etiqueta">Sami cierra el ciclo</p><h2>La idea funcionó, pero las ventas crecieron menos de lo supuesto</h2><p>Esperábamos un crecimiento de 12 % y ocurrió uno de 7 %. El margen mejoró como se había proyectado. Para el próximo escenario voy a conservar el margen observado y usar un crecimiento de ventas más prudente.</p><div><span>Supuesto ajustado</span><strong>Crecimiento esperado: 7–9 %</strong></div></div></article>
    </section>
  );
}

function Explicacion({ texto }: { texto: string }) { return <aside className="laboratorio-explicacion"><div className="laboratorio-sami-cara"><Sami pose="think" /></div><div><strong>Sami te lo explica</strong><p>{texto}</p></div></aside>; }
