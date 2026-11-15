import { useQueryClient } from '@tanstack/react-query';
import { useRouter, useSearch } from '@tanstack/react-router';
import { AuthCard, EnlaceAuth } from '@/components/AuthForm/AuthCard';
import { LoginForm } from '@/components/AuthForm/LoginForm';
import { negociosQuery } from '@/hooks/negocios/queries';
import { destinoSeguro } from '@/lib/destino';

export function LoginPage() {
  const { redirect } = useSearch({ from: '/_publica/login' });
  const router = useRouter();
  const queryClient = useQueryClient();

  // Con más de un negocio, después de ingresar se elige con cuál trabajar (T-204). Si la lista no llega se entra
  // igual, con el último que se usó: no saber cuántos negocios hay no tiene que trabar el inicio de sesión.
  const alIniciarSesion = async () => {
    const lista = await queryClient.fetchQuery(negociosQuery()).catch(() => null);
    if (lista && lista.negocios.length > 1) {
      await router.navigate({ to: '/elegir-negocio', search: { redirect } });
    } else {
      router.history.push(destinoSeguro(redirect));
    }
  };

  return (
    <AuthCard
      title="Ingresá a tu cuenta"
      lead="Retomá tu negocio donde lo dejaste."
      saludoSami="¡Qué bueno verte!"
      samiALaIzquierda
      footer={
        <>
          ¿Todavía no tenés cuenta? <EnlaceAuth to="/registro">Creá una</EnlaceAuth>
        </>
      }
    >
      <LoginForm onExito={alIniciarSesion} />
    </AuthCard>
  );
}
