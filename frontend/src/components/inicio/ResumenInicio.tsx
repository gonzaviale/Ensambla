import { useState } from 'react';
import { useUsuario } from '@/hooks/auth/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { metricasInicio } from '@/lib/sistema/metricasInicio';
import { allBlocks } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { TarjetaMetrica } from './TarjetaMetrica';

export function ResumenInicio({ sistemas }: { sistemas: Sistema[] }) {
  const usuario = useUsuario();
  const navegacion = useNavegacion();
  const [elegido, setElegido] = useState(sistemas[0]?.id ?? '');
  const sistema = sistemas.find((s) => s.id === elegido) ?? sistemas[0];
  if (!sistema) return null;
  const metricas = metricasInicio(sistema);
  const filas = allBlocks(sistema).flatMap((b) => b.rows);
  const soloEjemplos =
    !!sistema.demoAlmacen || (filas.length > 0 && filas.every((f) => f.__example));
  return (
    <section className="inicio-resumen" aria-labelledby="inicio-resumen-titulo">
      <div className="inicio-resumen-cabecera">
        <div>
          <p className="eyebrow">TU NEGOCIO, A LA VISTA</p>
          <h1 id="inicio-resumen-titulo">Cómo viene tu negocio</h1>
          <p>Los números de {sistema.name}, con un objetivo claro.</p>
        </div>
        <div className="inicio-resumen-control">
          <label htmlFor="inicio-sistema">Sistema</label>
          <select
            id="inicio-sistema"
            value={sistema.id}
            onChange={(e) => setElegido(e.target.value)}
          >
            {sistemas.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
          <button
            type="button"
            className="text-link"
            onClick={() => void navegacion.abrirSistema(sistema.id)}
          >
            Cargar y ver →
          </button>
        </div>
      </div>
      {soloEjemplos ? (
        <p className="inicio-datos-nota" role="status">
          Estas tablas todavía tienen solo ejemplos. Cargá tus datos para ver tus números; los
          ejemplos no se cuentan.
        </p>
      ) : filas.length === 0 ? (
        <p className="inicio-datos-nota">
          Tus tablas están listas. Cargá la primera fila para empezar a ver movimiento.
        </p>
      ) : null}
      <div className="inicio-metricas" key={`${usuario.id}:${sistema.id}`}>
        {metricas.map((m) => (
          <TarjetaMetrica key={m.clave} metrica={m} usuarioId={usuario.id} sistemaId={sistema.id} />
        ))}
      </div>
      <p className="inicio-metas-nota">
        Las metas se guardan en este navegador para tu cuenta y sistema. El avance se muestra al
        visitar esta pantalla.
      </p>
    </section>
  );
}
