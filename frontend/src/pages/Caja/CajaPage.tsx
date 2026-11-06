import { useMemo, useState } from 'react';
import {
  ArrowRightIcon,
  CheckCircleIcon,
  ClockIcon,
  CrownIcon,
  InfoIcon,
  LockKeyIcon,
  MoneyIcon,
  ReceiptIcon,
  WarningCircleIcon,
} from '@phosphor-icons/react';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Sami } from '@/components/sistema/Sami';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';

type Vista = 'estado' | 'arqueo' | 'diferencia' | 'cerrada';

const MOVIMIENTOS = [
  { hora: '09:12', tipo: 'Venta cobrada', origen: 'Ventas del almacén', medio: 'Efectivo', importe: 18500 },
  { hora: '11:40', tipo: 'Cobro de fiado', origen: 'Cuenta de Marta', medio: 'Transferencia', importe: 12000 },
  { hora: '13:05', tipo: 'Gasto operativo', origen: 'Flete de mercadería', medio: 'Efectivo', importe: -6500 },
  { hora: '15:30', tipo: 'Retiro del dueño', origen: 'Retiro personal', medio: 'Efectivo', importe: -10000 },
] as const;

export function CajaPage() {
  useTituloPagina('Caja');
  const navegacion = useNavegacion();
  const [vista, setVista] = useState<Vista>('estado');
  const [efectivoContado, setEfectivoContado] = useState('65000');
  const [transferencias, setTransferencias] = useState('80000');
  const [motivo, setMotivo] = useState('');

  const diferencia = useMemo(
    () => Number(efectivoContado || 0) + Number(transferencias || 0) - 148000,
    [efectivoContado, transferencias],
  );

  const comenzarArqueo = () => setVista('arqueo');
  const revisar = () => setVista(diferencia === 0 ? 'cerrada' : 'diferencia');

  return (
    <>
      <CabeceraComun active="cash" />
      <main id="main" tabIndex={-1} className="page-wrap caja-page reveal">
        <header className="caja-hero">
          <div>
            <p className="caja-etiqueta">Control diario · datos de demostración</p>
            <h1>{vista === 'cerrada' ? 'Caja cerrada y explicada' : 'Caja del negocio'}</h1>
            <p>Reuní lo que pasó en todos tus cuadernos, comprobá el dinero real y entendé cada diferencia antes de cerrar.</p>
          </div>
          <div className={`caja-estado ${vista === 'cerrada' ? 'cerrada' : ''}`}>
            {vista === 'cerrada' ? <CheckCircleIcon /> : <ClockIcon />}
            <div><span>Estado</span><strong>{vista === 'cerrada' ? 'Cerrada hoy, 18:42' : 'Abierta desde hoy, 08:47'}</strong></div>
          </div>
        </header>

        {vista === 'estado' && <EstadoCaja onCerrar={comenzarArqueo} onLaboratorio={() => void navegacion.irALaboratorio()} />}
        {vista === 'arqueo' && (
          <Arqueo
            efectivo={efectivoContado}
            transferencias={transferencias}
            diferencia={diferencia}
            onEfectivo={setEfectivoContado}
            onTransferencias={setTransferencias}
            onRevisar={revisar}
          />
        )}
        {vista === 'diferencia' && (
          <Diferencia diferencia={diferencia} motivo={motivo} onMotivo={setMotivo} onVolver={() => setVista('arqueo')} onCerrar={() => setVista('cerrada')} />
        )}
        {vista === 'cerrada' && <Cierre diferencia={diferencia} motivo={motivo} onLaboratorio={() => void navegacion.irALaboratorio()} onReabrir={() => setVista('estado')} />}
      </main>
    </>
  );
}

function EstadoCaja({ onCerrar, onLaboratorio }: { onCerrar: () => void; onLaboratorio: () => void }) {
  return <>
    <section className="caja-resumen" aria-label="Resumen de caja">
      <Metrica titulo="Saldo esperado" valor="$148.000" detalle="Según los movimientos registrados" />
      <Metrica titulo="Efectivo esperado" valor="$68.000" detalle="Incluye fondo inicial de $36.000" />
      <Metrica titulo="Transferencias" valor="$80.000" detalle="Cobros informados en cuadernos" />
      <Metrica titulo="Resultado del día" valor="+$91.500" detalle="No incluye retiros del dueño" positiva />
    </section>
    <section className="caja-grilla">
      <article className="caja-panel caja-movimientos">
        <div className="caja-panel-cabecera"><div><p className="caja-etiqueta">Trazabilidad</p><h2>Últimos movimientos</h2></div><span>4 de 17</span></div>
        {MOVIMIENTOS.map((movimiento) => <div className="caja-movimiento" key={`${movimiento.hora}-${movimiento.tipo}`}>
          <time>{movimiento.hora}</time><div><strong>{movimiento.tipo}</strong><span>{movimiento.origen} · {movimiento.medio}</span></div><b className={movimiento.importe < 0 ? 'egreso' : ''}>{formatear(movimiento.importe)}</b>
        </div>)}
        <div className="caja-clasificacion"><InfoIcon /><p><strong>Un retiro baja la caja, pero no es un gasto del negocio.</strong> Sami lo separa para no empeorar falsamente el resultado.</p></div>
      </article>
      <aside className="caja-panel caja-sami">
        <div className="caja-sami-personaje"><Sami pose="think" /></div>
        <p className="caja-etiqueta">Sami analiza</p><h2>La caja está ordenada</h2>
        <p>Hay $148.000 esperados entre efectivo y transferencias. Antes de cerrar, contá el dinero real: registrar una venta no garantiza que el saldo coincida.</p>
        <ul><li>17 movimientos con origen identificado</li><li>1 retiro separado del resultado</li><li>Sin movimientos sin clasificar</li></ul>
        <button type="button" className="btn primary" onClick={onCerrar}>Empezar cierre de caja <ArrowRightIcon /></button>
      </aside>
    </section>
    <Premium onLaboratorio={onLaboratorio} />
  </>;
}

function Arqueo({ efectivo, transferencias, diferencia, onEfectivo, onTransferencias, onRevisar }: { efectivo: string; transferencias: string; diferencia: number; onEfectivo: (valor: string) => void; onTransferencias: (valor: string) => void; onRevisar: () => void }) {
  return <section className="caja-paso-layout">
    <article className="caja-panel caja-formulario">
      <p className="caja-etiqueta">Paso 1 de 2 · arqueo</p><h2>¿Cuánto dinero hay realmente?</h2><p>Contá cada medio por separado. Ensambla lo compara con lo que debería haber según tus registros.</p>
      <label>Efectivo contado <span>Esperado: $68.000</span><div><b>$</b><input aria-label="Efectivo contado" inputMode="numeric" value={efectivo} onChange={(e) => onEfectivo(e.target.value)} /></div></label>
      <label>Transferencias comprobadas <span>Esperado: $80.000</span><div><b>$</b><input aria-label="Transferencias comprobadas" inputMode="numeric" value={transferencias} onChange={(e) => onTransferencias(e.target.value)} /></div></label>
      <button type="button" className="btn primary" onClick={onRevisar}>Comparar y revisar cierre</button>
    </article>
    <aside className="caja-panel caja-comparacion"><MoneyIcon /><h2>Comparación automática</h2><Fila nombre="Total esperado" valor="$148.000" /><Fila nombre="Total comprobado" valor={formatear(Number(efectivo || 0) + Number(transferencias || 0))} /><Fila nombre="Diferencia de caja" valor={formatear(diferencia)} destacada /><p>La diferencia no cambia el resultado contable por sí sola. Primero hay que entender su origen y documentarla.</p></aside>
  </section>;
}

function Diferencia({ diferencia, motivo, onMotivo, onVolver, onCerrar }: { diferencia: number; motivo: string; onMotivo: (valor: string) => void; onVolver: () => void; onCerrar: () => void }) {
  return <section className="caja-paso-layout">
    <article className="caja-panel caja-formulario">
      <p className="caja-etiqueta">Paso 2 de 2 · explicación</p><h2>Hay una diferencia de {formatear(diferencia)}</h2><p>Podés cerrar igualmente, pero dejar una explicación mejora la trazabilidad y los análisis futuros.</p>
      <fieldset><legend>¿Qué pudo haber pasado?</legend>{['Gasto todavía no registrado', 'Cobro o vuelto mal anotado', 'Retiro sin registrar', 'Todavía no lo sé'].map((opcion) => <label className="caja-opcion" key={opcion}><input type="radio" name="motivo" value={opcion} checked={motivo === opcion} onChange={(e) => onMotivo(e.target.value)} /><span>{opcion}</span></label>)}</fieldset>
      <div className="caja-botones"><button type="button" className="btn secondary" onClick={onVolver}>Volver a contar</button><button type="button" className="btn primary" onClick={onCerrar}>Cerrar con diferencia justificada</button></div>
    </article>
    <aside className="caja-panel caja-sami caja-sami-alerta"><div className="caja-sami-personaje"><Sami pose="think" /></div><WarningCircleIcon /><h2>No voy a corregirlo solo</h2><p>Una diferencia puede deberse a un registro faltante o a dinero real que no coincide. Yo te ayudo a investigarla, pero no invento movimientos ni cambio importes.</p><small>La justificación queda asociada al cierre y puede revisarse después.</small></aside>
  </section>;
}

function Cierre({ diferencia, motivo, onLaboratorio, onReabrir }: { diferencia: number; motivo: string; onLaboratorio: () => void; onReabrir: () => void }) {
  return <>
    <section className="caja-cierre-ok"><CheckCircleIcon /><div><p className="caja-etiqueta">Cierre n.º 28</p><h2>El día quedó controlado</h2><p>Se guardó el dinero comprobado, la diferencia y su explicación sin modificar los movimientos originales.</p></div></section>
    <section className="caja-resumen"><Metrica titulo="Dinero comprobado" valor="$145.000" detalle="Efectivo + transferencias" /><Metrica titulo="Diferencia" valor={formatear(diferencia)} detalle={motivo || 'Pendiente de investigar'} /><Metrica titulo="Resultado operativo" valor="+$91.500" detalle="Ventas − costos − gastos" positiva /><Metrica titulo="Retiros personales" valor="$10.000" detalle="Afectan caja, no resultado" /></section>
    <section className="caja-grilla"><article className="caja-panel caja-explicacion"><ReceiptIcon /><h2>Lectura contable del cierre</h2><p>El negocio generó un resultado estimado de <strong>$91.500</strong>, pero terminó con <strong>$145.000 disponibles</strong>. No son el mismo número porque la caja incluye el fondo inicial, cobranzas de fiado y retiros personales.</p><div className="caja-formula">Saldo inicial + cobros − pagos − retiros = saldo de caja<br/>Ventas − costos − gastos = resultado operativo</div></article><aside className="caja-panel caja-sami"><div className="caja-sami-personaje"><Sami pose="wink" /></div><h2>Ahora podemos decidir mejor</h2><p>Este cierre alimenta el Laboratorio. Podés analizar cuánto retirar, cuándo recuperás tus gastos o qué pasa si tus cobros se atrasan.</p><button type="button" className="btn primary" onClick={onLaboratorio}>Probar en el Laboratorio</button><button type="button" className="caja-enlace" onClick={onReabrir}>Volver a la demostración</button></aside></section>
  </>;
}

function Premium({ onLaboratorio }: { onLaboratorio: () => void }) { return <section className="caja-premium"><div><CrownIcon /><p className="caja-etiqueta">Caja inteligente · Premium</p><h2>Pasá del cierre aislado a una gestión que aprende</h2><p>Compará cierres, detectá diferencias repetidas, recibí recordatorios por WhatsApp y usá la liquidez real en escenarios pesimistas, esperados y optimistas.</p></div><div className="caja-premium-beneficios"><span><LockKeyIcon /> Historial completo y patrones</span><span><LockKeyIcon /> Avisos inteligentes y WhatsApp</span><span><LockKeyIcon /> Escenarios ilimitados</span></div><button type="button" className="btn primary" onClick={onLaboratorio}>Conocer análisis Premium</button></section>; }
function Metrica({ titulo, valor, detalle, positiva }: { titulo: string; valor: string; detalle: string; positiva?: boolean }) { return <article className="caja-metrica"><span>{titulo}</span><strong className={positiva ? 'positiva' : ''}>{valor}</strong><small>{detalle}</small></article>; }
function Fila({ nombre, valor, destacada }: { nombre: string; valor: string; destacada?: boolean }) { return <div className={`caja-fila ${destacada ? 'destacada' : ''}`}><span>{nombre}</span><strong>{valor}</strong></div>; }
function formatear(valor: number) { return `${valor < 0 ? '−' : valor > 0 ? '+' : ''}$${Math.abs(valor).toLocaleString('es-AR')}`; }
