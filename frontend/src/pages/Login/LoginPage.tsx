import { useQueryClient } from '@tanstack/react-query';
import { useRouter, useSearch } from '@tanstack/react-router';
import { AuthCard, EnlaceAuth } from '@/components/AuthForm/AuthCard';
import { LoginForm } from '@/components/AuthForm/LoginForm';
import { sistemasQuery } from '@/hooks/sistemas/queries';
import { destinoSeguro } from '@/lib/destino';

export function LoginPage() {
  const { redirect } = useSearch({ from: '/_publica/login' });
  const router = useRouter();
  const queryClient = useQueryClient();

  // Con más de un negocio (cada cuaderno es uno), después de ingresar se elige con cuál trabajar (T-204). Si la lista
  // no llega se entra igual, con el último que se usó: no saber cuántos hay no tiene que trabar el inicio de sesión.
  const alIniciarSesion = async () => {
    const sistemas = await queryClient
      .fetchQuery({ ...sistemasQuery(), retry: false })
      .catch(() => null);
    if (sistemas && sistemas.length > 1) {
      await router.navigate({ to: '/elegir-negocio', search: { redirect } });
    } else {
      router.history.push(destinoSeguro(redirect));
    }
  };

  return (
    <AuthCard
      title="Entrá a tu cuenta"
      lead="Retomá tu negocio donde lo dejaste."
      saludoSami="Hola de nuevo."
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
