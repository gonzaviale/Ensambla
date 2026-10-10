import { AuthCard, EnlaceAuth } from '@/components/AuthForm/AuthCard';
import { PendingTask } from '@/components/ui/PendingTask';

// T-008: formulario de registro con el campo "nombre del negocio". Usar LoginForm como modelo y
// useRegistro (hooks/auth/mutations) para crear la cuenta y abrir sesión.
export function RegistroPage() {
  return (
    <AuthCard
      title="Creá tu cuenta"
      lead="Empezá a armar el sistema de tu negocio."
      footer={
        <>
          ¿Ya tenés cuenta? <EnlaceAuth to="/login">Ingresá</EnlaceAuth>
        </>
      }
    >
      <PendingTask tarea="T-008" title="Formulario de registro" />
    </AuthCard>
  );
}
