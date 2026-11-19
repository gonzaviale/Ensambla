import type { Recorrido } from '@/hooks/dondeAbrir/useRecorridoDondeAbrir';

const ERROR_AL_MEDIR_UN_LUGAR =
  'No pudimos medir ese lugar. Probá con otro punto o volvé a intentar.';
const ERROR_AL_CALCULAR = 'No pudimos calcular el ranking. Tus respuestas siguen guardadas.';

function ErrorDeLaConsulta({ recorrido }: { recorrido: Recorrido }) {
  const { punto, consulta, quitarPunto } = recorrido;
  return (
    <div className="donde-estado error" role="alert">
      <p>{punto ? ERROR_AL_MEDIR_UN_LUGAR : ERROR_AL_CALCULAR}</p>
      {punto && (
        <button type="button" className="btn ghost" onClick={quitarPunto}>
          Quitar el lugar
        </button>
      )}
      <button type="button" className="btn secondary" onClick={() => void consulta.refetch()}>
        Volver a intentar
      </button>
    </div>
  );
}

function AvisosDeLosDatos({ avisos }: { avisos: string[] }) {
  return avisos.map((aviso) => (
    <p className="donde-aviso-datos" role="note" key={aviso}>
      {aviso}
    </p>
  ));
}

/** Lo que se cuenta mientras se calcula, si falló y los avisos sobre los datos usados. */
export function EstadoDeLaConsulta({ recorrido }: { recorrido: Recorrido }) {
  const { consulta } = recorrido;
  return (
    <>
      {consulta.isPending && (
        <p className="donde-estado" role="status">
          Estoy comparando las zonas…
        </p>
      )}
      {consulta.isError && <ErrorDeLaConsulta recorrido={recorrido} />}
      <AvisosDeLosDatos avisos={consulta.data?.avisos ?? []} />
    </>
  );
}
