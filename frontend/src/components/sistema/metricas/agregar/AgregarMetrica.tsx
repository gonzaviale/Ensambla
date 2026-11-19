import { useState, type ReactNode } from 'react';
import { ArmalaVos } from '@/components/sistema/metricas/agregar/ArmalaVos';
import { PedilaConTusPalabras } from '@/components/sistema/metricas/agregar/PedilaConTusPalabras';
import { Sugeridas } from '@/components/sistema/metricas/agregar/Sugeridas';
import type { PropsDeLaPestana } from '@/components/sistema/metricas/agregar/tipos';
import { VentanaFlotante } from '@/components/sistema/metricas/VentanaFlotante';
import { leerErrorServidor } from '@/hooks/errores';
import { useGuardarMetrica } from '@/hooks/metricas/mutations';
import type { AutoriaDeMetrica, DefinicionDeMetrica } from '@/types/metricas';

type Pestana = 'sugeridas' | 'armala' | 'palabras';

const PESTANAS: Record<
  Pestana,
  { etiqueta: string; Contenido: (props: PropsDeLaPestana) => ReactNode }
> = {
  sugeridas: { etiqueta: 'Sugeridas', Contenido: Sugeridas },
  armala: { etiqueta: 'Armala vos', Contenido: ArmalaVos },
  palabras: { etiqueta: 'Pedila con tus palabras', Contenido: PedilaConTusPalabras },
};

const ORDEN_DE_LAS_PESTANAS: Pestana[] = ['sugeridas', 'armala', 'palabras'];

type Props = { sistemaId: string; onCerrar: () => void; onAgregada: (metricaId: string) => void };

// Una ventana centrada, encima del inicio: no ocupa lugar en «Tu negocio». Tres formas de sumar una métrica sin
// escribir fórmulas, y en todas se ve cómo queda antes de guardarla.
export function AgregarMetrica({ sistemaId, onCerrar, onAgregada }: Props) {
  const [pestana, setPestana] = useState<Pestana>('sugeridas');
  const guardar = useGuardarMetrica(sistemaId);
  const { Contenido } = PESTANAS[pestana];
  const guardarMetrica = (definicion: DefinicionDeMetrica, autoria: AutoriaDeMetrica) =>
    guardar.mutate({ ...definicion, autoria }, { onSuccess: (creada) => onAgregada(creada.id) });
  return (
    <VentanaFlotante titulo="Agregar una métrica" tamano="mediana" onCerrar={onCerrar}>
      <PestanasDelPanel pestana={pestana} onElegir={setPestana} />
      <ErrorAlGuardar error={guardar.isError ? guardar.error : null} />
      <div className="panel-agregar-cuerpo" role="tabpanel" id="panel-agregar-cuerpo">
        <Contenido sistemaId={sistemaId} guardando={guardar.isPending} onAgregar={guardarMetrica} />
      </div>
    </VentanaFlotante>
  );
}

function PestanasDelPanel({
  pestana,
  onElegir,
}: {
  pestana: Pestana;
  onElegir: (pestana: Pestana) => void;
}) {
  return (
    <div className="panel-agregar-pestanas" role="tablist" aria-label="Cómo querés agregarla">
      {ORDEN_DE_LAS_PESTANAS.map((opcion) => (
        <button
          key={opcion}
          type="button"
          role="tab"
          aria-selected={pestana === opcion}
          aria-controls="panel-agregar-cuerpo"
          onClick={() => onElegir(opcion)}
        >
          {PESTANAS[opcion].etiqueta}
        </button>
      ))}
    </div>
  );
}

const NO_SE_PUDO_GUARDAR = 'No pudimos guardarla. Probá de nuevo.';

// Si el servidor dijo por qué (por ejemplo, que ya está), se muestra eso; si no respondió, el aviso genérico.
function ErrorAlGuardar({ error }: { error: unknown }) {
  if (!error) return null;
  const delServidor = leerErrorServidor(error);
  return (
    <p className="metrica-medida-error" role="alert">
      {delServidor && delServidor.status > 0 ? delServidor.mensaje : NO_SE_PUDO_GUARDAR}
    </p>
  );
}
