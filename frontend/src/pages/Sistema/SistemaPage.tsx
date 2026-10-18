import { useEffect } from 'react';
import { getRouteApi } from '@tanstack/react-router';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';

const ruta = getRouteApi('/_app/sistemas/$sistemaId');

// Provisorio hasta la etapa del editor: deja el sistema de la URL como activo en el store.
export function SistemaPage() {
  const { sistemaId } = ruta.useParams();
  const { modo } = ruta.useSearch();
  const existe = useSistemaStore((estado) => estado.sistemas.some((item) => item.id === sistemaId));
  const activo = useSistemaStore(seleccionarSistemaActivo);

  useEffect(() => {
    const estado = useSistemaStore.getState();
    if (existe && estado.activeId !== sistemaId) {
      estado.abrirSistema(sistemaId, modo === 'construir' ? 'build' : 'use');
    }
  }, [existe, sistemaId, modo]);

  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="page-wrap reveal">
        <h1>{activo?.name ?? 'Este sistema no existe'}</h1>
      </main>
    </>
  );
}
