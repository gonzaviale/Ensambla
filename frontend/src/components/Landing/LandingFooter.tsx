import { Logo } from '@/components/ui/Logo';

export function LandingFooter() {
  return (
    <footer>
      <div className="ens-wrap ens-foot">
        <a href="#inicio" aria-label="Ensambla, volver al inicio">
          <Logo />
        </a>
        <p>Un cuaderno digital para el día a día de tu negocio.</p>
        <a className="ens-link" href="#tus-dudas">
          Resolver mis dudas
        </a>
      </div>
      <div className="ens-legal">
        <p>© {new Date().getFullYear()} Ensambla · Trabajo Práctico Integrador · UNLaM</p>
      </div>
    </footer>
  );
}
