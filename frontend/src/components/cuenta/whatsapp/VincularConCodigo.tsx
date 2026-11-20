import { enlaceDeSami } from './formatos';
import { leerErrorServidor } from '@/hooks/errores';
import { useGenerarCodigoVinculacion } from '@/hooks/whatsapp/mutations';
import { Btn } from '../../sistema/Btn';

export function VincularConCodigo() {
  const generar = useGenerarCodigoVinculacion();
  const resultado = generar.data;

  return (
    <details className="cuenta-alternativa">
      <summary>¿No te llega el código?</summary>
      <p>
        También podés vincular el número desde tu teléfono: generá un código y enviáselo a Sami.
      </p>
      {generar.isError ? <p role="alert">{leerErrorServidor(generar.error)?.mensaje}</p> : null}
      {resultado ? (
        <p role="status">
          Mandá <strong>VINCULAR {resultado.codigo}</strong>
          {resultado.numeroBot ? (
            <>
              {' '}
              al{' '}
              <a href={enlaceDeSami(resultado.numeroBot)} target="_blank" rel="noreferrer">
                +{resultado.numeroBot}
              </a>
            </>
          ) : null}
          . Vale hasta las{' '}
          {new Intl.DateTimeFormat('es-AR', { timeStyle: 'short' }).format(
            new Date(resultado.expiraEn),
          )}
          .
        </p>
      ) : null}
      <Btn cls="secondary small" disabled={generar.isPending} onClick={() => generar.mutate()}>
        {resultado ? 'Generar otro código' : 'Generar código'}
      </Btn>
    </details>
  );
}

// Sami por WhatsApp: verificar el número, ver cuál está vinculado y desvincularlo.
