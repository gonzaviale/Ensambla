import { useEffect, useState } from 'react';

export function useDebouncedValue<T>(valor: T, esperaMs: number) {
  const [debounced, setDebounced] = useState(valor);

  useEffect(() => {
    const espera = setTimeout(() => setDebounced(valor), esperaMs);
    return () => clearTimeout(espera);
  }, [valor, esperaMs]);

  return debounced;
}
