import { useState, type FormEvent } from 'react';
import { BotonParaAgregar } from '@/components/sistema/metricas/agregar/BotonParaAgregar';
import type { PropsDeLaPestana } from '@/components/sistema/metricas/agregar/tipos';
import { VistaPrevia } from '@/components/sistema/metricas/agregar/VistaPrevia';
import { LoQueEntendi } from '@/components/sistema/metricas/LoQueEntendi';
import { leerErrorServidor } from '@/hooks/errores';
import { useEntenderPedido } from '@/hooks/metricas/mutations';
import { resumenDeLaReceta } from '@/lib/metricas/metricasAMedida';
import type { PedidoInterpretado } from '@/types/metricas';

const EJEMPLOS_DE_PEDIDOS = [
  'Cuánto vendí esta semana',
  'Cuánto gané este mes',
  'Qué producto vendo más este mes',
  'Cuánto me deben por cliente',
  'En qué gasté más este mes',
];

// Se escribe lo que se quiere ver y se entiende con un diccionario fijo (sin IA): la métrica se muestra con su
// gráfico real antes de guardarla, y si no se entendió o no se puede calcular, se dice por qué.
export function PedilaConTusPalabras({ sistemaId, guardando, onAgregar }: PropsDeLaPestana) {
  const { pedido, setPedido, entender, propuesta, aviso, entendiendo, descartar } =
    usePedidoEnPalabras(sistemaId);
  return (
    <form className="panel-agregar-pedido" onSubmit={entender} noValidate>
      <p className="panel-agregar-pedido-intro">
        Contame qué querés ver, como se lo dirías a alguien del negocio. Yo lo traduzco y te muestro
        cómo queda antes de guardarlo.
      </p>
      <label>
        ¿Qué querés ver?
        <input
          type="text"
          value={pedido}
          maxLength={200}
          placeholder="Por ejemplo: cuánto vendí esta semana"
          onChange={(evento) => setPedido(evento.target.value)}
        />
      </label>
      <EjemplosParaProbar onElegir={setPedido} />
      <button type="submit" className="agregar-metrica-ver" disabled={entendiendo}>
        Ver qué entendí
      </button>
      {aviso && <AvisoDelPedido texto={aviso} />}
      {propuesta && <LoQueEntendi propuesta={propuesta} />}
      {propuesta && (
        <PropuestaDelPedido
          propuesta={propuesta}
          guardando={guardando}
          onGuardar={() => onAgregar(propuesta.definicion, 'PROPIA')}
          onDescartar={descartar}
        />
      )}
    </form>
  );
}

function EjemplosParaProbar({ onElegir }: { onElegir: (pedido: string) => void }) {
  return (
    <div className="panel-agregar-ejemplos" role="group" aria-label="Pedidos de ejemplo">
      {EJEMPLOS_DE_PEDIDOS.map((ejemplo) => (
        <button key={ejemplo} type="button" onClick={() => onElegir(ejemplo)}>
          {ejemplo}
        </button>
      ))}
    </div>
  );
}

/** Lo que la persona escribe y lo que respondió el servidor: la propuesta o por qué no se pudo. */
function usePedidoEnPalabras(sistemaId: string) {
  const entendido = useEntenderPedido(sistemaId);
  const [pedido, setPedido] = useState('');
  const [faltaElPedido, setFaltaElPedido] = useState(false);
  const entender = (evento: FormEvent) => {
    evento.preventDefault();
    const limpio = pedido.trim();
    setFaltaElPedido(limpio === '');
    if (limpio !== '') entendido.mutate(limpio);
  };
  return {
    pedido,
    setPedido,
    entender,
    propuesta: entendido.data,
    aviso: avisoDelPedido(faltaElPedido, entendido.error),
    entendiendo: entendido.isPending,
    descartar: entendido.reset,
  };
}

function avisoDelPedido(faltaElPedido: boolean, error: unknown) {
  if (faltaElPedido) return 'Contanos qué querés ver.';
  if (!error) return null;
  const delServidor = leerErrorServidor(error);
  return delServidor && delServidor.status > 0
    ? delServidor.mensaje
    : 'No pudimos entenderlo ahora. Probá de nuevo.';
}

function AvisoDelPedido({ texto }: { texto: string }) {
  return (
    <div className="agregar-metrica-no-entendi" role="alert">
      <p>{texto}</p>
      <p>Podés probar con:</p>
      <ul>
        {EJEMPLOS_DE_PEDIDOS.map((ejemplo) => (
          <li key={ejemplo}>«{ejemplo}»</li>
        ))}
      </ul>
    </div>
  );
}

type PropsDeLaPropuesta = {
  propuesta: PedidoInterpretado;
  guardando: boolean;
  onGuardar: () => void;
  onDescartar: () => void;
};

function PropuestaDelPedido({ propuesta, guardando, onGuardar, onDescartar }: PropsDeLaPropuesta) {
  const { definicion, resultado } = propuesta;
  return (
    <section className="panel-agregar-propuesta" aria-label="Así quedaría">
      <h3>{definicion.nombre}</h3>
      <p className="metrica-medida-receta">{resumenDeLaReceta(definicion)}</p>
      <VistaPrevia definicion={definicion} resultado={resultado} />
      <div className="agregar-metrica-acciones">
        <BotonParaAgregar
          yaLaTenes={propuesta.yaLaTenes}
          guardando={guardando}
          onAgregar={onGuardar}
          texto="Guardar métrica"
        />
        <button type="button" className="agregar-metrica-ver" onClick={onDescartar}>
          Descartar
        </button>
      </div>
    </section>
  );
}
