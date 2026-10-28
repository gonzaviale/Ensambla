import { sinDatosInsuficientes, type ProductoDeZona, type Zona } from '@/lib/sistema/zona';
import { distanciaEnMetros, ZONA_DE_PRUEBA } from './ubicacion';

const producto = (
  nombre: string,
  comercios: number,
  minimo: number,
  promedio: number,
  maximo: number,
  vendenComercios: number,
  unidadesPorSemana: number,
): ProductoDeZona => ({
  nombre,
  comercios,
  precio: { minimo, promedio, maximo },
  ventas: { comercios: vendenComercios, unidadesPorSemana },
});

// Lo que el backend va a devolver ya agregado. Acá se arma "en crudo", con un producto que aportan muy
// pocos comercios (Fernet), para comprobar que el mínimo de tres se respeta antes de entregar nada.
const ZONA_EN_CRUDO: Zona = {
  nombre: ZONA_DE_PRUEBA.nombre,
  comercios: 12,
  productos: [
    producto('Yerba Playadito 1 kg', 11, 3500, 3900, 4300, 11, 30),
    producto('Aceite Natura 900 ml', 10, 2500, 2800, 3100, 10, 14),
    producto('Coca-Cola 2,25 L', 12, 2600, 2750, 2900, 12, 40),
    producto('Azúcar Ledesma 1 kg', 9, 1050, 1150, 1250, 9, 22),
    producto('Fideos Matarazzo 500 g', 10, 1150, 1300, 1450, 10, 25),
    producto('Lavandina Ayudín 1 L', 8, 1200, 1350, 1500, 8, 9),
    producto('Alfajor Jorgito', 7, 700, 800, 950, 7, 18),
    producto('Galletitas Criollitas 3 U', 6, 1100, 1250, 1400, 6, 10),
    producto('Leche La Serenísima 1 L', 12, 1500, 1650, 1800, 12, 60),
    producto('Pan lactal Bimbo 360 g', 10, 2300, 2500, 2700, 10, 26),
    producto('Agua Villavicencio 2 L', 11, 1000, 1100, 1250, 11, 30),
    producto('Cerveza Quilmes 1 L', 9, 2100, 2300, 2500, 9, 20),
    producto('Fernet Branca 750 ml', 2, 9800, 10400, 11200, 2, 4),
  ],
};

export const zonaDeLaPersona = (): Zona => sinDatosInsuficientes(ZONA_EN_CRUDO);

// Un número estable a partir de un texto: la misma dirección da siempre el mismo análisis.
const huella = (texto: string) => {
  let valor = 7;
  for (const letra of texto) valor = (valor * 31 + letra.charCodeAt(0)) % 100_003;
  return valor;
};

/** El análisis agregado de otra zona que la persona escribe (un barrio o una dirección). */
export function zonaParaTexto(texto: string): Zona {
  const limpio = texto.trim();
  const semilla = huella(limpio.toLowerCase());
  const comercios = 1 + (semilla % 15);
  const factor = 0.88 + (semilla % 25) / 100;
  const productos = ZONA_EN_CRUDO.productos.map((origen, indice) => {
    const ajuste = factor + ((semilla + indice * 7) % 9) / 100 - 0.04;
    const comerciosDelProducto = Math.max(1, Math.min(comercios, comercios - (indice % 3)));
    return {
      nombre: origen.nombre,
      comercios: comerciosDelProducto,
      precio: {
        minimo: Math.round((origen.precio.minimo * ajuste) / 10) * 10,
        promedio: Math.round((origen.precio.promedio * ajuste) / 10) * 10,
        maximo: Math.round((origen.precio.maximo * ajuste) / 10) * 10,
      },
      ventas: {
        comercios: comerciosDelProducto,
        unidadesPorSemana: Math.max(
          1,
          Math.round(origen.ventas.unidadesPorSemana * (0.7 + (semilla % 6) / 10)),
        ),
      },
    };
  });
  return sinDatosInsuficientes({ nombre: limpio, comercios, productos });
}

// Zonas precargadas que el servidor reconoce por punto: un centro, un radio y cuántos comercios aportan.
// La de Ramos Mejía tiene pocos comercios a propósito, para ver el caso «sin datos suficientes».
const ZONAS_POR_PUNTO = [
  { ...ZONA_DE_PRUEBA, comercios: 10 },
  {
    nombre: 'Ramos Mejía - Centro',
    latitud: -34.6419,
    longitud: -58.5666,
    radioMetros: 800,
    comercios: 2,
  },
];

/**
 * El análisis de la zona que contiene el punto, o `null` si el punto no cae en ninguna zona precargada.
 * Igual que el servidor: la de centro más cercano, con los productos ordenados por cuántos comercios los
 * venden y sin productos si aportan menos de tres comercios.
 */
export function zonaParaPunto(punto: { latitud: number; longitud: number }): Zona | null {
  const zona = ZONAS_POR_PUNTO.filter(
    (candidata) => distanciaEnMetros(candidata, punto) <= candidata.radioMetros,
  ).sort((a, b) => distanciaEnMetros(a, punto) - distanciaEnMetros(b, punto))[0];
  if (!zona) return null;
  const productos = ZONA_EN_CRUDO.productos
    .map((origen) => ({
      ...origen,
      comercios: Math.min(origen.comercios, zona.comercios),
      ventas: { ...origen.ventas, comercios: Math.min(origen.ventas.comercios, zona.comercios) },
    }))
    .sort(
      (a, b) =>
        b.ventas.comercios - a.ventas.comercios ||
        b.ventas.unidadesPorSemana - a.ventas.unidadesPorSemana,
    );
  return sinDatosInsuficientes({ nombre: zona.nombre, comercios: zona.comercios, productos });
}
