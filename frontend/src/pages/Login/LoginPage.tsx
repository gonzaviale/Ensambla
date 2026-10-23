import { useRouter, useSearch } from '@tanstack/react-router';
import { AuthCard, EnlaceAuth } from '@/components/AuthForm/AuthCard';
import { LoginForm } from '@/components/AuthForm/LoginForm';
import { destinoSeguro } from '@/lib/destino';

export function LoginPage() {
  const { redirect } = useSearch({ from: '/_publica/login' });
  const router = useRouter();

  return (
    <AuthCard
      title="Ingresá a tu cuenta"
      lead="Retomá tu sistema donde lo dejaste."
      saludoSami="¡Qué bueno verte!"
      samiALaIzquierda
      footer={
        <>
          ¿Todavía no tenés cuenta? <EnlaceAuth to="/registro">Creá una</EnlaceAuth>
        </>
      }
    >
      <LoginForm onExito={() => router.history.push(destinoSeguro(redirect))} />
    </AuthCard>
  );
}
