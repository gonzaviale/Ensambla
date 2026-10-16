import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { CheckCircleIcon } from '@phosphor-icons/react';
import { AuthCard, EnlaceAuth } from '@/components/AuthForm/AuthCard';
import { enfocarPrimerError } from '@/components/Cuenta/enfocarPrimerError';
import { Badge, Button, InputField, Panel, buttonClasses } from '@/components/ui';
import { validarEmail } from './validaciones';

// Pedir el enlace (T-039/T-040). Hoy no hay endpoint: se simula y se dice claro. El mensaje es el
// mismo exista o no el correo, para no revelar quién tiene cuenta.
export function OlvideClavePage() {
  const formulario = useRef<HTMLFormElement>(null);
  const resultado = useRef<HTMLDivElement>(null);
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | undefined>();
  const [enviado, setEnviado] = useState(false);

  useEffect(() => {
    if (enviado) resultado.current?.focus();
  }, [enviado]);

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const problema = validarEmail(email);
    setError(problema);
    if (problema) {
      enfocarPrimerError(formulario.current);
      return;
    }
    setEnviado(true);
  }

  return (
    <AuthCard
      title="Recuperá tu contraseña"
      lead="Escribí tu email y te enviamos un enlace para elegir una contraseña nueva."
      footer={<EnlaceAuth to="/login">Volver a ingresar</EnlaceAuth>}
    >
      {enviado ? (
        <div ref={resultado} tabIndex={-1} className="flex flex-col gap-4 outline-none">
          <p role="status" className="m-0 flex items-start gap-3 text-body font-semibold">
            <CheckCircleIcon aria-hidden size={28} weight="fill" className="shrink-0 text-ok" />
            Si el correo existe, te enviamos un enlace. Revisá también la carpeta de correo no
            deseado.
          </p>
          <Panel flat className="flex flex-col gap-3">
            <Badge tone="warn" className="self-start">
              Demostración
            </Badge>
            <p className="m-0 text-body text-soft">
              En la versión real, el enlace te llega por mail y vence a las pocas horas. En esta
              demostración no se envía ningún correo: usá el enlace de ejemplo.
            </p>
            <Link
              to="/restablecer-contrasena"
              search={{ token: 'demo' }}
              className={buttonClasses({ variant: 'solid', size: 'lg', fullWidth: true })}
            >
              Abrir el enlace de ejemplo
            </Link>
          </Panel>
          <Button variant="ghost" className="self-start" onClick={() => setEnviado(false)}>
            Usar otro email
          </Button>
        </div>
      ) : (
        <form ref={formulario} noValidate onSubmit={enviar} className="flex flex-col gap-4">
          <InputField
            label="Email"
            type="email"
            autoComplete="email"
            maxLength={254}
            value={email}
            error={error}
            onChange={(evento) => {
              setError(undefined);
              setEmail(evento.target.value);
            }}
          />
          <Button type="submit" variant="solid" size="lg" fullWidth>
            Enviarme el enlace
          </Button>
        </form>
      )}
    </AuthCard>
  );
}
