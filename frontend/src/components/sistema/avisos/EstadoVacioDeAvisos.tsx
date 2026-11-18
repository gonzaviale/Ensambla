import { Btn } from '../Btn';

type Props = {
  sinDatos: boolean;
  hayDemo: boolean;
  filtro: string;
  onCrearDemo: () => void;
};

// Cuando no hay avisos: o todavía no hay datos para mirar, o todo está en orden.
export function EstadoVacioDeAvisos({ sinDatos, hayDemo, filtro, onCrearDemo }: Props) {
  if (sinDatos) return <SinDatos hayDemo={hayDemo} onCrearDemo={onCrearDemo} />;
  return <TodoEnOrden filtro={filtro} />;
}

function SinDatos({ hayDemo, onCrearDemo }: { hayDemo: boolean; onCrearDemo: () => void }) {
  return (
    <div className="alertas-vacio">
      <h3>Todavía no tengo datos para mirar.</h3>
      <p>
        Cuando cargues stock, ventas o precios, te aviso lo que necesita atención y lo comparo con
        los datos de tu zona.
      </p>
      {!hayDemo && (
        <Btn cls="primary" onClick={onCrearDemo}>
          Probar con un almacén de ejemplo
        </Btn>
      )}
    </div>
  );
}

function TodoEnOrden({ filtro }: { filtro: string }) {
  return (
    <div className="alertas-vacio ok">
      <img className="alertas-vacio-img" src="/mockup/sami-guino.webp" alt="" />
      <h3>{filtro === 'todas' ? 'Todo en orden.' : 'Nada en esta categoría.'}</h3>
      <p>Sigo mirando tu stock, tus ventas y los precios de tu zona. Si algo cambia, te aviso.</p>
    </div>
  );
}
