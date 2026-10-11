import { useState } from 'react';
import { useForm } from '@tanstack/react-form';
import { ApiError } from '@/api';
import { Button, InputField, PasswordField } from '@/components/ui';
import { useRegistro } from '@/hooks/auth/mutations';
import { primerError } from '@/lib/formErrors';
import { registroSchema } from '@/types/auth';

export function RegistroForm({ onExito }: { onExito: () => void }) {
  const registro = useRegistro();
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [erroresServidor, setErroresServidor] = useState<Record<string, string>>({});
  const form = useForm({
    defaultValues: { nombre: '', nombreNegocio: '', email: '', contrasenia: '' },
    validators: { onSubmit: registroSchema },
    onSubmit: async ({ value }) => {
      setErrorGeneral(null);
      setErroresServidor({});
      try {
        await registro.mutateAsync(registroSchema.parse(value));
        onExito();
      } catch (error) {
        if (error instanceof ApiError) {
          setErroresServidor(error.fieldErrors);
          setErrorGeneral(error.message);
        } else {
          setErrorGeneral('No pudimos crear tu cuenta. Probá de nuevo en unos minutos.');
        }
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
      <form.Field name="nombre">
        {(field) => (
          <InputField
            label="Tu nombre"
            autoComplete="name"
            maxLength={100}
            value={field.state.value}
            onBlur={field.handleBlur}
            onChange={(event) => {
              setErroresServidor({});
              field.handleChange(event.target.value);
            }}
            error={primerError(field.state.meta.errors) ?? erroresServidor.nombre}
          />
        )}
      </form.Field>
      <form.Field name="nombreNegocio">
        {(field) => (
          <InputField
            label="Nombre del negocio"
            autoComplete="organization"
            maxLength={120}
            value={field.state.value}
            onBlur={field.handleBlur}
            onChange={(event) => {
              setErroresServidor({});
              field.handleChange(event.target.value);
            }}
            error={primerError(field.state.meta.errors) ?? erroresServidor.nombreNegocio}
          />
        )}
      </form.Field>
      <form.Field name="email">
        {(field) => (
          <InputField
            label="Email"
            type="email"
            autoComplete="email"
            maxLength={254}
            value={field.state.value}
            onBlur={field.handleBlur}
            onChange={(event) => {
              setErroresServidor({});
              field.handleChange(event.target.value);
            }}
            error={primerError(field.state.meta.errors) ?? erroresServidor.email}
          />
        )}
      </form.Field>
      <form.Field name="contrasenia">
        {(field) => (
          <PasswordField
            label="Contraseña"
            autoComplete="new-password"
            hint="Usá al menos 8 caracteres."
            value={field.state.value}
            onBlur={field.handleBlur}
            onChange={(event) => {
              setErroresServidor({});
              field.handleChange(event.target.value);
            }}
            error={primerError(field.state.meta.errors) ?? erroresServidor.contrasenia}
          />
        )}
      </form.Field>
      {errorGeneral && (
        <p role="alert" className="text-hint text-bad">
          {errorGeneral}
        </p>
      )}
      <form.Subscribe selector={(estado) => estado.isSubmitting}>
        {(enviando) => (
          <Button type="submit" variant="solid" disabled={enviando} className="w-full">
            {enviando ? 'Creando tu cuenta…' : 'Crear cuenta'}
          </Button>
        )}
      </form.Subscribe>
    </form>
  );
}
