import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useSearch } from '@tanstack/react-router';
import { CheckCircleIcon } from '@phosphor-icons/react';
import { AuthCard, EnlaceAuth } from '@/components/AuthForm/AuthCard';
import { enfocarPrimerError } from '@/components/Cuenta/enfocarPrimerError';
import { Badge, Button, Panel, PasswordField, buttonClasses } from '@/components/ui';
import { validarClaveNueva, type ErroresClave } from './validaciones';

// Elegir una contraseña nueva (T-040). Simulado: el enlace de ejemplo trae `?token=demo`; sin token
// (o con uno vacío) se explica que el enlace no sirve y se ofrece pedir otro.
export function RestablecerClavePage() {
  const busqueda = useSearch({ strict: false }) as { token?: unknown };
  const token =
    typeof busqueda.token === 'string' || typeof busqueda.token === 'number'
      ? String(busqueda.token).trim()
      : '';

  const formulario = useRef<HTMLFormElement>(null);
  const resultado = useRef<HTMLDivElement>(null);
  const [contrasenia, setContrasenia] = useState('');
  const [repetida, setRepetida] = useState('');
  const [errores, setErrores] = useState<ErroresClave>({});
  const [listo, setListo] = useState(false);

  useEffect(() => {
    if (listo) resultado.current?.focus();
  }, [listo]);

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const nuevos = validarClaveNueva(contrasenia, repetida);
    setErrores(nuevos);
    if (nuevos.contrasenia || nuevos.repetida) {
      enfocarPrimerError(formulario.current);
      return;
    }
    setListo(true);
  }

  if (!token) {
    return (
      <AuthCard
        title="El enlace venció o no es válido"
        lead="Los enlaces duran poco tiempo y se pueden usar una sola vez. Pedí uno nuevo y listo."
        footer={<EnlaceAuth to="/login">Volver a ingresar</EnlaceAuth>}
      >
        <Link
          to="/olvide-contrasena"
          className={buttonClasses({ variant: 'solid', size: 'lg', fullWidth: true })}
        >
          Pedir un enlace nuevo
        </Link>
      </AuthCard>
    );
  }

  if (listo) {
    return (
      <AuthCard
        title="Listo, ya podés ingresar"
        lead="Tu contraseña nueva quedó guardada."
        footer={<EnlaceAuth to="/login">Volver a ingresar</EnlaceAuth>}
      >
        <div ref={resultado} tabIndex={-1} className="flex flex-col gap-4 outline-none">
          <p role="status" className="m-0 flex items-start gap-3 text-body font-semibold">
            <CheckCircleIcon aria-hidden size={28} weight="fill" className="shrink-0 text-ok" />
            Cambiamos tu contraseña. Usala la próxima vez que ingreses.
          </p>
          <Panel flat className="flex flex-col gap-2">
            <Badge tone="warn" className="self-start">
              Simulación
            </Badge>
            <p className="m-0 text-body text-soft">
              Esto es una demostración: en esta versión no se cambió ninguna contraseña real.
            </p>
          </Panel>
          <Link
            to="/login"
            search={{ redirect: undefined }}
            className={buttonClasses({ variant: 'solid', size: 'lg', fullWidth: true })}
          >
            Ir a ingresar
          </Link>
        </div>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      title="Elegí una contraseña nueva"
      lead="Escribila dos veces para estar seguros de que es la que querés."
      footer={<EnlaceAuth to="/login">Volver a ingresar</EnlaceAuth>}
    >
      <form ref={formulario} noValidate onSubmit={enviar} className="flex flex-col gap-4">
        <Panel flat tight className="flex flex-wrap items-center gap-2 text-hint text-soft">
          <Badge tone="warn">Simulación</Badge>
          En esta versión de demostración no se cambia ninguna contraseña real.
        </Panel>
        <PasswordField
          label="Contraseña nueva"
          autoComplete="new-password"
          hint="Mínimo 8 caracteres. Mejor si mezclás letras y números."
          value={contrasenia}
          error={errores.contrasenia}
          onChange={(evento) => {
            setErrores((actuales) => ({ ...actuales, contrasenia: undefined }));
            setContrasenia(evento.target.value);
          }}
        />
        <PasswordField
          label="Repetí la contraseña nueva"
          autoComplete="new-password"
          value={repetida}
          error={errores.repetida}
          onChange={(evento) => {
            setErrores((actuales) => ({ ...actuales, repetida: undefined }));
            setRepetida(evento.target.value);
          }}
        />
        <Button type="submit" variant="solid" size="lg" fullWidth>
          Guardar la contraseña nueva
        </Button>
      </form>
    </AuthCard>
  );
}
