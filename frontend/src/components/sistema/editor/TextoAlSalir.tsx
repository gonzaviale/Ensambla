import { useEffect, useState } from 'react';

// Un texto que se guarda al salir del campo («change» en el mockup), no con cada tecla.
export function TextoAlSalir({
  valor,
  alGuardar,
  area = false,
  ...resto
}: {
  valor: string;
  alGuardar: (valor: string) => void;
  area?: boolean;
  id: string;
  maxLength: number;
  'data-tour'?: string;
}) {
  const [escrito, setEscrito] = useState(valor);
  useEffect(() => setEscrito(valor), [valor]);
  const salir = () => {
    const limpio = escrito.trim();
    if (limpio === valor) return;
    alGuardar(limpio);
  };
  return area ? (
    <textarea
      {...resto}
      value={escrito}
      onChange={(e) => setEscrito(e.target.value)}
      onBlur={salir}
    />
  ) : (
    <input
      {...resto}
      className="input"
      value={escrito}
      onChange={(e) => setEscrito(e.target.value)}
      onBlur={salir}
    />
  );
}
