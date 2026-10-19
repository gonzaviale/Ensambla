import { AuthCard, EnlaceAuth } from '@/components/AuthForm/AuthCard';
import { RegistroForm } from '@/components/AuthForm/RegistroForm';
import { useRouter } from '@tanstack/react-router';

export function RegistroPage() {
  const router = useRouter();
  return (
    <AuthCard
      title="Creá tu cuenta"
      lead="Empezá a ordenar las anotaciones de tu negocio."
      footer={
        <>
          ¿Ya tenés cuenta? <EnlaceAuth to="/login">Ingresá</EnlaceAuth>
        </>
      }
    >
      <RegistroForm onExito={() => void router.navigate({ to: '/inicio' })} />
    </AuthCard>
  );
}
