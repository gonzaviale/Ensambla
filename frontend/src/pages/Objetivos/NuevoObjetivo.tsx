import { useState } from 'react';
import { ChatCircleDotsIcon, CurrencyCircleDollarIcon } from '@phosphor-icons/react';
import { DefinirObjetivo } from './DefinirObjetivo';
import { ConversarConSami } from './sami/ConversarConSami';

type Props = {
  onDefinido: () => void;
  onCancelar?: () => void;
};

// Dos caminos para definir un objetivo: contárselo a Sami (cualquier objetivo, con o sin datos) o poner una meta en
// pesos por mes (el formulario de siempre, sin IA y sin cuota).
export function NuevoObjetivo({ onDefinido, onCancelar }: Props) {
  const [camino, setCamino] = useState<'sami' | 'pesos'>('sami');
  // Lo que la persona le había contado a Sami cuando resultó ser una meta en pesos.
  const [textoInicial, setTextoInicial] = useState('');

  if (camino === 'pesos') {
    return (
      <>
        <DefinirObjetivo
          key={textoInicial}
          textoInicial={textoInicial}
          onDefinido={onDefinido}
          onCancelar={onCancelar}
        />
        <button
          type="button"
          className="objetivos-cambiar-camino"
          onClick={() => setCamino('sami')}
        >
          <ChatCircleDotsIcon aria-hidden size={20} />
          Prefiero contárselo a Sami
        </button>
      </>
    );
  }
  return (
    <>
      <ConversarConSami
        onGuardado={onDefinido}
        onCancelar={onCancelar}
        onMetaEnPesos={(texto) => {
          setTextoInicial(texto);
          setCamino('pesos');
        }}
      />
      <button
        type="button"
        className="objetivos-cambiar-camino"
        onClick={() => {
          setTextoInicial('');
          setCamino('pesos');
        }}
      >
        <CurrencyCircleDollarIcon aria-hidden size={20} />
        Quiero poner una meta en pesos por mes
      </button>
    </>
  );
}
