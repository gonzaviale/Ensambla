import { useState, type FormEvent } from 'react';
import { leerErrorServidor } from '@/hooks/errores';
import { useSolicitarVerificacionWhatsapp } from '@/hooks/whatsapp/mutations';
import { telefonoWhatsappSchema, type VerificacionWhatsapp } from '@/types/whatsapp';
import { Btn } from '../../sistema/Btn';

export function CargarNumero({
  onEnviada,
}: {
  onEnviada: (verificacion: VerificacionWhatsapp) => void;
}) {
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
        Verificá tu número para usar a Sami por WhatsApp: te enviamos un código y lo escribís en
        esta pantalla. Es para estar seguros de que el número es tuyo.
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
