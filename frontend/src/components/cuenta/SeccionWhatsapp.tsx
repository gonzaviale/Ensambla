import { useEffect, useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { leerErrorServidor } from '@/hooks/errores';
import {
  useConfirmarVerificacionWhatsapp,
  useDesvincularWhatsapp,
  useGenerarCodigoVinculacion,
  useSolicitarVerificacionWhatsapp,
} from '@/hooks/whatsapp/mutations';
import { useVinculacionWhatsapp } from '@/hooks/whatsapp/queries';
import { useSistemaStore } from '@/stores/sistemaStore';
import {
  codigoVerificacionSchema,
  telefonoWhatsappSchema,
  type VerificacionWhatsapp,
  type VinculoWhatsapp,
} from '@/types/whatsapp';
import { Btn } from '../sistema/Btn';

const fecha = (valor: string) =>
  new Intl.DateTimeFormat('es-AR', { dateStyle: 'long' }).format(new Date(valor));

const enlaceDeSami = (numeroBot: string) => `https://wa.me/${numeroBot}`;

// Segundos que faltan para poder pedir otro código; 0 cuando ya se puede.
function useEsperaParaReenviar(reenviarDesde: string) {
  const faltan = () => Math.max(0, Math.ceil((new Date(reenviarDesde).getTime() - Date.now()) / 1000));
  const [segundos, setSegundos] = useState(faltan);
  useEffect(() => {
    setSegundos(faltan());
    const intervalo = window.setInterval(() => setSegundos(faltan()), 1000);
    return () => window.clearInterval(intervalo);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reenviarDesde]);
  return segundos;
}

function NumeroVinculado({ vinculo }: { vinculo: VinculoWhatsapp }) {
  const desvincular = useDesvincularWhatsapp();
  const avisar = useSistemaStore((estado) => estado.avisar);
  const [confirmando, setConfirmando] = useState(false);

  return (
    <>
      <p role="status" className="cuenta-plan-actual">
        <strong>+{vinculo.telefono}</strong>
        <span>Vinculado el {fecha(vinculo.vinculadoEn)}</span>
      </p>
      <p>
        Mandale a Sami una foto de tu cuaderno o un texto como «gasté 5000 en harina»: arma las filas, te las muestra
        y las guarda cuando le respondés que sí. Las fotos descuentan de las páginas con IA de{' '}
        <Link to="/planes">tu plan</Link>.
      </p>
      {desvincular.isError ? <p role="alert">{leerErrorServidor(desvincular.error)?.mensaje}</p> : null}
      <div className="cuenta-acciones">
        {vinculo.numeroBot ? (
          <a className="btn primary" href={enlaceDeSami(vinculo.numeroBot)} target="_blank" rel="noreferrer">
            Escribirle a Sami
          </a>
        ) : null}
        {confirmando ? (
          <>
            <Btn
              cls="danger"
              disabled={desvincular.isPending}
              onClick={() =>
                desvincular.mutate(undefined, {
                  onSuccess: () => avisar('Desvinculamos tu número de WhatsApp.'),
                })
              }
            >
              Sí, desvincular
            </Btn>
            <Btn cls="ghost" onClick={() => setConfirmando(false)}>
              Cancelar
            </Btn>
          </>
        ) : (
          <Btn cls="secondary" onClick={() => setConfirmando(true)}>
            Desvincular
          </Btn>
        )}
      </div>
    </>
  );
}

// Paso 2: el código llegó por WhatsApp; se escribe acá. Se puede pedir otro pasada la espera o cambiar el número.
function IngresarCodigo({
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
    confirmar.mutate(valido.data, { onSuccess: () => avisar('Listo, vinculamos tu número de WhatsApp.') });
  };

  return (
    <form onSubmit={enviar} aria-labelledby="cuenta-whatsapp-titulo" noValidate>
      <p role="status">
        Te mandamos un código de 6 dígitos por WhatsApp al +{verificacion.telefono}. Vence en 10 minutos.
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
function CargarNumero({ onEnviada }: { onEnviada: (verificacion: VerificacionWhatsapp) => void }) {
  const solicitar = useSolicitarVerificacionWhatsapp();
  const [telefono, setTelefono] = useState('');
  const [errorLocal, setErrorLocal] = useState<string>();
  const error = errorLocal ?? leerErrorServidor(solicitar.error)?.mensaje;

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    const valido = telefonoWhatsappSchema.safeParse(telefono);
    if (!valido.success) return setErrorLocal(valido.error.issues[0]?.message);
    setErrorLocal(undefined);
    solicitar.mutate(valido.data, { onSuccess: onEnviada });
  };

  return (
    <form onSubmit={enviar} aria-labelledby="cuenta-whatsapp-titulo" noValidate>
      <p>
        Verificá tu número para usar a Sami por WhatsApp: te mandamos un código y lo escribís acá. Es para estar seguros
        de que el número es tuyo.
      </p>
      <div className="field">
        <label htmlFor="cuenta-whatsapp-telefono">Tu número de WhatsApp</label>
        <input
          className="input"
          id="cuenta-whatsapp-telefono"
          type="tel"
          inputMode="tel"
          autoComplete="tel"
          placeholder="+54 9 11 2233 4455"
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'cuenta-whatsapp-error' : 'cuenta-whatsapp-ayuda'}
          value={telefono}
          onChange={(evento) => {
            setTelefono(evento.target.value);
            setErrorLocal(undefined);
          }}
        />
        <span id="cuenta-whatsapp-ayuda" className="input-hint">
          Con código de país, por ejemplo +54 9 11 2233 4455.
        </span>
        {error ? (
          <p id="cuenta-whatsapp-error" role="alert" className="input-hint">
            {error}
          </p>
        ) : null}
      </div>
      <div className="cuenta-acciones">
        <Btn type="submit" cls="primary" disabled={solicitar.isPending}>
          Enviarme el código
        </Btn>
      </div>
    </form>
  );
}

// Plan B si el código no llega: se vincula mandando «VINCULAR <código>» desde el teléfono al número de Sami.
function VincularConCodigo() {
  const generar = useGenerarCodigoVinculacion();
  const resultado = generar.data;

  return (
    <details className="cuenta-alternativa">
      <summary>¿No te llega el código?</summary>
      <p>También podés vincular el número desde tu teléfono: generá un código y mandáselo a Sami.</p>
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
          {new Intl.DateTimeFormat('es-AR', { timeStyle: 'short' }).format(new Date(resultado.expiraEn))}.
        </p>
      ) : null}
      <Btn cls="secondary small" disabled={generar.isPending} onClick={() => generar.mutate()}>
        {resultado ? 'Generar otro código' : 'Generar código'}
      </Btn>
    </details>
  );
}

// Sami por WhatsApp: verificar el número, ver cuál está vinculado y desvincularlo.
export function SeccionWhatsapp() {
  const vinculacion = useVinculacionWhatsapp();
  const [verificacion, setVerificacion] = useState<VerificacionWhatsapp>();
  const vinculado = !!vinculacion.data;

  // Con el número ya vinculado la verificación terminó: si después se desvincula se arranca de cero.
  useEffect(() => {
    if (vinculado) setVerificacion(undefined);
  }, [vinculado]);

  return (
    <section className="cuenta-tarjeta" aria-labelledby="cuenta-whatsapp-titulo">
      <h2 id="cuenta-whatsapp-titulo">WhatsApp</h2>
      {vinculacion.isPending ? (
        <p role="status">Consultando tu número…</p>
      ) : vinculacion.isError ? (
        <div role="alert">
          <p>{leerErrorServidor(vinculacion.error)?.mensaje}</p>
          <Btn cls="secondary small" onClick={() => void vinculacion.refetch()}>
            Reintentar
          </Btn>
        </div>
      ) : vinculacion.data ? (
        <NumeroVinculado vinculo={vinculacion.data} />
      ) : (
        <>
          {verificacion ? (
            <IngresarCodigo
              verificacion={verificacion}
              onReenviada={setVerificacion}
              onCambiarNumero={() => setVerificacion(undefined)}
            />
          ) : (
            <CargarNumero onEnviada={setVerificacion} />
          )}
          <VincularConCodigo />
        </>
      )}
    </section>
  );
}
