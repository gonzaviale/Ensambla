import { useState } from 'react';

export function useCopiarAlPortapapeles(aviso: string) {
  const [resultado, setResultado] = useState('');

  const copiar = async (texto: string) => {
    try {
      await navigator.clipboard.writeText(texto);
      setResultado(aviso);
    } catch {
      setResultado('No pudimos copiarlo. Escribilo a mano.');
    }
  };

  return { copiar, resultado };
}
