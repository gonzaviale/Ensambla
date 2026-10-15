import { useMemo, useState } from 'react';
import { REGLA_INICIAL, calcularDemo } from './ejemploLista';

export type PasoDemo = 'llego' | 'revisado' | 'confirmado';

// Estado de la demo «Llegó la lista de tu proveedor»: todo en el navegador, sin pedir nada al servidor.
export function useDemoLista() {
  const [paso, setPaso] = useState<PasoDemo>('llego');
  const [regla, setRegla] = useState<number>(REGLA_INICIAL);
  const resultado = useMemo(() => calcularDemo(regla), [regla]);

  return {
    paso,
    regla,
    resultado,
    revisar: () => setPaso('revisado'),
    confirmar: () => setPaso('confirmado'),
    elegirRegla: setRegla,
    reiniciar: () => {
      setPaso('llego');
      setRegla(REGLA_INICIAL);
    },
  };
}
