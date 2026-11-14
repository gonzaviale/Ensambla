import { CompararNegocios } from '@/components/negocios/CompararNegocios';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { useRubros } from '@/hooks/negocio/queries';
import { useResumenConjunto } from '@/hooks/negocios/resumenConjunto';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { textoDelPeriodo } from '@/lib/negocios/comparar';

// «Comparar mis negocios» (ENS-284 · T-205): los totales de la cuenta y el comparativo, en una pantalla aparte del
// Inicio, que sigue siendo el de un negocio. Se llega desde el selector de negocio (T-202).
export function CompararNegociosPage() {
  useTituloPagina('Comparar mis negocios');
  const consulta = useResumenConjunto();
  const rubros = useRubros();
  const nombreDelRubro = (codigo: string) =>
    rubros.data?.rubros.find((rubro) => rubro.codigo === codigo)?.nombre;

  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="page-wrap comparar-negocios reveal">
        <header className="comparar-cabecera">
          <span className="eyebrow">MIS NEGOCIOS</span>
          <h1>Comparar mis negocios</h1>
          <p>
            {consulta.data
              ? `${textoDelPeriodo(consulta.data.desde, consulta.data.hasta)}: los últimos ${consulta.data.dias} días.`
              : 'Los últimos 30 días, negocio por negocio.'}
          </p>
        </header>
        {consulta.isPending ? (
          <p className="comparar-cargando" role="status">
            Juntando los números de tus negocios…
          </p>
        ) : consulta.isError ? (
          <p className="tablero-aviso" role="alert">
            No pudimos traer el resumen de tus negocios.{' '}
            <button type="button" onClick={() => void consulta.refetch()}>
              Probar de nuevo
            </button>
          </p>
        ) : (
          <CompararNegocios resumen={consulta.data} nombreDelRubro={nombreDelRubro} />
        )}
        <p className="comparar-nota">
          Los análisis de zona, como Mi zona y Dónde abrir, se miran dentro de cada negocio.
        </p>
      </main>
    </>
  );
}
