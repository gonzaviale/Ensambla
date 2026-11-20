import { useEffect, useState, type FormEvent } from 'react';
import { leerErrorServidor } from '@/hooks/errores';
import {
  useConfirmarVerificacionWhatsapp,
  useSolicitarVerificacionWhatsapp,
} from '@/hooks/whatsapp/mutations';
import { useSistemaStore } from '@/stores/sistemaStore';
import { codigoVerificacionSchema, type VerificacionWhatsapp } from '@/types/whatsapp';
import { Btn } from '../../sistema/Btn';

// Segundos que faltan para poder pedir otro código; 0 cuando ya se puede.
function useEsperaParaReenviar(reenviarDesde: string) {
  const faltan = () =>
    Math.max(0, Math.ceil((new Date(reenviarDesde).getTime() - Date.now()) / 1000));
  const [segundos, setSegundos] = useState(faltan);
  useEffect(() => {
    setSegundos(faltan());
    const intervalo = window.setInterval(() => setSegundos(faltan()), 1000);
    return () => window.clearInterval(intervalo);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reenviarDesde]);
  return segundos;
}

export function IngresarCodigo({
  verificacion,
  onReenviada,
  onCambiarNumero,
}: {
  verificacion: VerificacionWhatsapp;
  onReenviada: (nueva: VerificacionWhatsapp) => void;
  onCambiarNumero: () => void;
}) {
  const confirmar = useConfirmarVerificacionWhatsapp();
  const reenviar = useSolicitarVerificacionWhatsapp();
  const avisar = useSistemaStore((estado) => estado.avisar);
  const espera = useEsperaParaReenviar(verificacion.reenviarDesde);
  const [codigo, setCodigo] = useState('');
  const [errorLocal, setErrorLocal] = useState<string>();
  const error = errorLocal ?? leerErrorServidor(confirmar.error ?? reenviar.error)?.mensaje;

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    const valido = codigoVerificacionSchema.safeParse(codigo);
    if (!valido.success) return setErrorLocal(valido.error.issues[0]?.message);
    setErrorLocal(undefined);
    confirmar.mutate(valido.data, {
      onSuccess: () => avisar('Vinculamos tu número de WhatsApp.'),
    });
  };

  return (
    <form onSubmit={enviar} aria-labelledby="cuenta-whatsapp-titulo" noValidate>
      <p role="status">
        Te enviamos un código de 6 dígitos por WhatsApp al +{verificacion.telefono}. Vence en 10
        minutos.
      </p>
      <div className="field">
        <label htmlFor="cuenta-whatsapp-codigo">Código</label>
        <input
          className="input"
          id="cuenta-whatsapp-codigo"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          autoFocus
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'cuenta-whatsapp-error' : undefined}
          value={codigo}
          onChange={(evento) => {
            setCodigo(evento.target.value.replace(/\D/g, ''));
            setErrorLocal(undefined);
          }}
        />
        {error ? (
          <p id="cuenta-whatsapp-error" role="alert" className="input-hint">
            {error}
          </p>
        ) : null}
      </div>
      <div className="cuenta-acciones">
        <Btn type="submit" cls="primary" disabled={confirmar.isPending}>
          Confirmar número
        </Btn>
        <Btn
          cls="secondary"
          disabled={espera > 0 || reenviar.isPending}
          onClick={() =>
            reenviar.mutate(verificacion.telefono, {
              onSuccess: (nueva) => {
                setCodigo('');
                onReenviada(nueva);
              },
            })
          }
        >
          {espera > 0 ? `Reenviar código (${espera} s)` : 'Reenviar código'}
        </Btn>
        <Btn cls="ghost" onClick={onCambiarNumero}>
          Cambiar número
        </Btn>
      </div>
    </form>
  );
}

// Paso 1: la persona escribe su número y le mandamos el código por WhatsApp.
