import { useState } from 'react';
import { NOMBRES_DE_FORMATO } from '@/lib/carteles';
import { FORMATOS_DE_CARTEL, type ContenidoDeCartel, type FormatoDeCartel } from '@/types/carteles';

type Props = {
  contenido: ContenidoDeCartel;
  ocupado: boolean;
  onAplicar: (cambios: Partial<ContenidoDeCartel>) => void;
};

/** Sólo se editan los textos y el formato: colores, letras y márgenes quedan bloqueados. */
export function EditorDeTextos({ contenido, ocupado, onAplicar }: Props) {
  const [titular, setTitular] = useState(contenido.titular);
  const [bajada, setBajada] = useState(contenido.bajada);
  return (
    <form
      className="carteles-editor"
      onSubmit={(evento) => {
        evento.preventDefault();
        onAplicar({ titular, bajada });
      }}
    >
      <CampoDeTexto etiqueta="Titular" valor={titular} maximo={40} onCambio={setTitular} />
      <CampoDeTexto etiqueta="Bajada" valor={bajada} maximo={90} onCambio={setBajada} />
      <SelectorDeFormato formato={contenido.formato} onElegir={(formato) => onAplicar({ formato })} />
      <button type="submit" className="btn secondary" disabled={ocupado}>
        Actualizar vista previa
      </button>
    </form>
  );
}

type CampoProps = { etiqueta: string; valor: string; maximo: number; onCambio: (valor: string) => void };

function CampoDeTexto({ etiqueta, valor, maximo, onCambio }: CampoProps) {
  return (
    <label className="field">
      {etiqueta}
      <input value={valor} maxLength={maximo} onChange={(evento) => onCambio(evento.target.value)} />
    </label>
  );
}

function SelectorDeFormato({
  formato,
  onElegir,
}: {
  formato: FormatoDeCartel;
  onElegir: (formato: FormatoDeCartel) => void;
}) {
  return (
    <label className="field">
      Formato
      <select value={formato} onChange={(evento) => onElegir(evento.target.value as FormatoDeCartel)}>
        {FORMATOS_DE_CARTEL.map((codigo) => (
          <option key={codigo} value={codigo}>
            {NOMBRES_DE_FORMATO[codigo]}
          </option>
        ))}
      </select>
    </label>
  );
}
