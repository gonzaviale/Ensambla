import { useState } from 'react';
import { useForm } from '@tanstack/react-form';
import { ApiError } from '@/api';
import { Button, InputField, PasswordField } from '@/components/ui';
import { useLogin } from '@/hooks/auth/mutations';
import { primerError } from '@/lib/formErrors';
import { loginSchema } from '@/types/auth';

const MENSAJE_CREDENCIALES = 'El email o la contraseña no son correctos.';
const MENSAJE_GENERICO = 'No pudimos iniciar sesión. Probá de nuevo en unos minutos.';

function mensajeDeError(error: unknown) {
  if (error instanceof ApiError) {
    return error.status === 401 ? MENSAJE_CREDENCIALES : error.message;
  }
  return MENSAJE_GENERICO;
}

// Formulario de referencia: TanStack Form + zod (validators.onSubmit) + mutación de TanStack Query.
export function LoginForm({ onExito }: { onExito: () => void }) {
  const login = useLogin();
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [erroresServidor, setErroresServidor] = useState<Record<string, string>>({});

  const form = useForm({
    defaultValues: { email: '', contrasenia: '' },
    validators: { onSubmit: loginSchema },
    onSubmit: async ({ value }) => {
      setErrorGeneral(null);
      setErroresServidor({});
      try {
        await login.mutateAsync({ ...value, email: value.email.trim() });
        onExito();
      } catch (error) {
        if (error instanceof ApiError) setErroresServidor(error.fieldErrors);
        setErrorGeneral(mensajeDeError(error));
      }
    },
  });

  return (
    <form
      noValidate
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        void form.handleSubmit();
      }}
    >
      <form.Field name="email">
        {(field) => (
          <InputField
            label="Email"
            type="email"
            autoComplete="email"
            value={field.state.value}
            onBlur={field.handleBlur}
            onChange={(event) => field.handleChange(event.target.value)}
            error={primerError(field.state.meta.errors) ?? erroresServidor.email}
          />
        )}
      </form.Field>
      <form.Field name="contrasenia">
        {(field) => (
          <PasswordField
            label="Contraseña"
            autoComplete="current-password"
            value={field.state.value}
            onBlur={field.handleBlur}
            onChange={(event) => field.handleChange(event.target.value)}
            error={primerError(field.state.meta.errors) ?? erroresServidor.contrasenia}
          />
        )}
      </form.Field>
      {errorGeneral && (
        <p role="alert" className="m-0 text-base font-semibold text-bad">
          {errorGeneral}
        </p>
      )}
      <form.Subscribe selector={(estado) => estado.isSubmitting}>
        {(enviando) => (
          <Button type="submit" variant="solid" size="lg" fullWidth loading={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </Button>
        )}
      </form.Subscribe>
    </form>
  );
}
