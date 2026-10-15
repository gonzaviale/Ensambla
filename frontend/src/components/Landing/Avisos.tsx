import { EJEMPLO, REGLA_INICIAL, calcularDemo } from '@/hooks/landing/ejemploLista';
import { pesos, porcentaje } from '@/lib/red/formato';
import { variables } from './estilos';

type Aviso = {
  clave: string;
  tipo: string;
  icono: string;
  tono: 'naranja' | 'coral' | 'arena' | 'pizarra';
  mensaje: string;
  porque: string;
  toque: string;
};

function avisos(): Aviso[] {
  const aceite = calcularDemo(REGLA_INICIAL);
  return [
    {
      clave: 'precio-en-riesgo',
      tipo: 'Precio en riesgo',
      icono: '!',
      tono: 'coral',
      mensaje: `El aceite Natura 900 ml quedó con ${porcentaje(aceite.margenHoy)} de margen. ¿Lo paso a ${pesos(aceite.precioSugerido)}?`,
      porque: `Su costo subió de ${pesos(EJEMPLO.costoAnterior)} a ${pesos(aceite.costoNuevo)}.`,
      toque: 'Aplicar',
    },
    {
      clave: 'lista-nueva',
      tipo: 'Lista nueva',
      icono: '↗',
      tono: 'naranja',
      mensaje:
        'Subieron 31 productos, 8 % en promedio. Tus reglas ya calcularon los precios nuevos.',
      porque: 'Llegó la lista de Distribuidora Sur.',
      toque: 'Revisar y aplicar',
    },
    {
      clave: 'stock',
      tipo: 'Stock por agotarse',
      icono: '▤',
      tono: 'arena',
      mensaje: 'Al ritmo de esta semana, la yerba se termina el jueves.',
      porque: 'Te quedan 12 paquetes y vendés 4 por día.',
      toque: 'Agregar al pedido',
    },
    {
      clave: 'oferta',
      tipo: 'Oferta que sirve',
      icono: '%',
      tono: 'naranja',
      mensaje: 'Distribuidora Sur tiene la Coca 2,25 L 8 % más barata hasta el viernes.',
      porque: 'Es un producto que vendés todas las semanas.',
      toque: 'Ver oferta',
    },
    {
      clave: 'compras-caro',
      tipo: 'Comprás caro',
      icono: '$',
      tono: 'coral',
      mensaje: 'La harina Pureza 1 kg te sale 14 % más que a otros almacenes del barrio.',
      porque: 'Comparación anónima, con 9 comercios de tu zona.',
      toque: 'Ver proveedores',
    },
    {
      clave: 'mejor-proveedor',
      tipo: 'Mejor proveedor',
      icono: '⇄',
      tono: 'pizarra',
      mensaje: `El azúcar Ledesma está ${pesos(120)} más barato en Mayorista Norte.`,
      porque: `Ahí cuesta ${pesos(900)}; hoy pagás ${pesos(1020)}.`,
      toque: 'Comparar',
    },
    {
      clave: 'cambio-raro',
      tipo: 'Cambio raro',
      icono: '?',
      tono: 'coral',
      mensaje: 'La lavandina Ayudín subió 35 % de golpe. ¿Error de la lista?',
      porque: 'Es mucho más de lo que suele cambiar.',
      toque: 'Confirmar o ignorar',
    },
    {
      clave: 'resumen',
      tipo: 'Resumen del día',
      icono: '◷',
      tono: 'pizarra',
      mensaje: `Vendiste ${pesos(184000)}. El aceite lo vendés casi al costo desde el martes. Faltan yerba y azúcar.`,
      porque: 'Te llega a la noche, en una sola pantalla.',
      toque: 'Compartir precios',
    },
  ];
}

const REGLAS = [
  'Pocos avisos, y los que más plata mueven primero.',
  'Vos elegís desde qué margen te avisamos.',
  'Cada aviso dice por qué. Si lo ignorás siempre, se apaga solo.',
] as const;

export function Avisos() {
  return (
    <section className="seccion" id="avisos" aria-labelledby="avisos-titulo">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Avisos que importan</p>
          <h2 id="avisos-titulo">No buscás reportes: Ensambla te avisa.</h2>
          <p>
            Cada aviso es una línea, con el porqué y una acción de un toque. Nada se aplica sin que
            lo confirmes.
          </p>
        </header>

        <ul className="avisos">
          {avisos().map(({ clave, tipo, icono, tono, mensaje, porque, toque }, i) => (
            <li
              key={clave}
              className="aviso revelar"
              data-tono={tono}
              style={variables({ '--d': `${(i % 4) * 0.08}s` })}
            >
              <h3 className="aviso__tipo">
                <span className="aviso__icono" aria-hidden="true">
                  {icono}
                </span>
                {tipo}
              </h3>
              <p className="aviso__mensaje">{mensaje}</p>
              <p className="aviso__porque">{porque}</p>
              <p className="aviso__toque">
                <span className="aviso__toque-rotulo">Un toque</span>
                <b>{toque}</b>
              </p>
            </li>
          ))}
        </ul>

        <ul className="avisos__reglas">
          {REGLAS.map((regla) => (
            <li key={regla}>{regla}</li>
          ))}
        </ul>
      </div>
    </section>
  );
}
