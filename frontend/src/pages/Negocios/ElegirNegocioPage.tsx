import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useRouter, useSearch } from '@tanstack/react-router';
import { ElegirNegocioAlEntrar } from '@/components/negocios/ElegirNegocioAlEntrar';
import { negociosQuery } from '@/hooks/negocios/queries';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { destinoSeguro } from '@/lib/destino';

// Lo primero que se ve después de ingresar con más de un negocio (T-204): la pantalla en blanco, así ya se siente
// adentro, y arriba el pop-up para elegir. Con un solo negocio, o si la lista no llega, se sigue de largo.
export function ElegirNegocioPage() {
  useTituloPagina('Elegí tu negocio');
  const { redirect } = useSearch({ from: '/elegir-negocio' });
  const router = useRouter();
  const consulta = useQuery({ ...negociosQuery(), retry: false });
  const destino = destinoSeguro(redirect);
  const hayQueElegir = (consulta.data?.negocios.length ?? 0) > 1;
  const seguirDeLargo = consulta.isError || (consulta.isSuccess && !hayQueElegir);

  useEffect(() => {
    if (seguirDeLargo) router.history.replace(destino);
  }, [seguirDeLargo, router, destino]);

  return (
    <div className="min-h-dvh bg-white">
      {consulta.data && hayQueElegir && (
        <ElegirNegocioAlEntrar
          lista={consulta.data}
          onListo={() => router.history.replace(destino)}
        />
      )}
    </div>
  );
}
