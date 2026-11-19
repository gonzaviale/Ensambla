import { useConfiguracionNegocio, useRubros } from '@/hooks/negocio/queries';
import { detalleDelNegocio } from '@/lib/negocios/presentacion';
import { allBlocks } from '@/lib/sistema/core';
import { useSistemaStore } from '@/stores/sistemaStore';

/** El rubro que la cuenta eligió en su perfil, con el nombre que da el servidor. */
export function useRubroDelPerfil() {
  const configuracion = useConfiguracionNegocio();
  const rubros = useRubros();
  const codigo = configuracion.data?.rubro;
  return rubros.data?.rubros.find((item) => item.codigo === codigo)?.nombre;
}

/** «Kiosco · 4 tablas» de cada negocio de la cuenta: cada negocio es un cuaderno y sus tablas son sus secciones. */
export function useDetalleDeNegocios() {
  const rubro = useRubroDelPerfil();
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  return (negocioId: string) => {
    const negocio = sistemas.find((sistema) => sistema.id === negocioId);
    return negocio ? detalleDelNegocio(rubro, allBlocks(negocio).length) : rubro;
  };
}
