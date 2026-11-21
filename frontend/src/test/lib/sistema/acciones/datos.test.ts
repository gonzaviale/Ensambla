import { describe, it } from 'vitest';
import {
  agregarCampo,
  cuandoSamiPide,
  dadoUnAlmacenDeEjemplo,
  editarCampo,
  entoncesNoCambioNada,
  entoncesSeAplico,
  entoncesSeOmitieron,
  entoncesSeOmitio,
  entoncesSoloSeHizo,
  entoncesSoloSeOmitio,
  quitarCampo,
} from '@/test/accionesDeSami';
import {
  dadoUnAlmacenConGastos,
  dadoUnInventarioConMinimosEscritosDeDistintasFormas,
  dadoUnInventarioQueYaUsaLaClaveRubro,
  dadosClientesConNotasQueEstanYQueNoEstanEnLaLista,
  dadosGastosConLaCategoriaComoListaPropia,
  entoncesLaCategoriaEsTextoSinOpciones,
  entoncesLaEtiquetaDeLaClave,
  entoncesLaEtiquetaDeLaClaveMide,
  entoncesLaPiezaYaNoTieneElDato,
  entoncesLaPrimeraEtiquetaEs,
  entoncesLaPrimeraFilaConserva,
  entoncesLasCategoriasDeLosGastosSon,
  entoncesLasEtiquetasSon,
  entoncesLasNotasQueNoEstanEnLaListaSeBorran,
  entoncesLosDatosSon,
  entoncesLosMinimosQuedanComoNumeroYLoQueNoSirveSeBorra,
  entoncesElUltimoDatoEs,
} from '@/test/accionesSobreDatos';

const SEIS_OPCIONES_CON_REPETIDAS = [
  'Mayorista',
  'MAYORISTA',
  'Minorista',
  'A',
  'B',
  'C',
  'D',
  'E',
  '',
];

describe('sumar y quitar datos', () => {
  it('suma un dato a la pieza', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(agregarCampo('inventory', 'Fecha de vencimiento', 'date'));

    entoncesSeAplico('Sumé Fecha de vencimiento a inventario.');
  });

  it('el dato nuevo tiene una clave propia y no es obligatorio', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(agregarCampo('inventory', 'Fecha de vencimiento', 'date'));

    entoncesElUltimoDatoEs('inventory', {
      label: 'Fecha de vencimiento',
      type: 'date',
      required: false,
      key: 'fecha_de_vencimiento',
    });
  });

  it('una clave que ya existe recibe un sufijo', () => {
    dadoUnInventarioQueYaUsaLaClaveRubro();

    cuandoSamiPide(agregarCampo('inventory', 'Rubro', 'text'));

    entoncesElUltimoDatoEs('inventory', { key: 'rubro_2' });
  });

  it('una etiqueta sólo de signos usa una clave genérica', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(agregarCampo('inventory', '???', 'text'));

    entoncesElUltimoDatoEs('inventory', { key: 'dato' });
  });

  it('suma un dato de lista con sus opciones sin espacios ni vacías', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      agregarCampo('sales', 'Forma de pago', 'select', { opciones: ['Efectivo', ' Tarjeta ', ''] }),
    );

    entoncesElUltimoDatoEs('sales', { type: 'select', options: ['Efectivo', 'Tarjeta'] });
  });

  it('omite datos repetidos, de tipo inválido, de lista sin opciones o de una pieza inexistente', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      agregarCampo('inventory', 'producto', 'text'),
      agregarCampo('inventory', 'Color', 'color'),
      agregarCampo('inventory', 'Rubro', 'select', { opciones: ['Una'] }),
      agregarCampo('inventory', 'Rubro', 'select'),
      agregarCampo('nada', 'X', 'text'),
    );

    entoncesSeOmitieron(5);
  });

  it('quita un dato', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(quitarCampo('clients', 'correo'));

    entoncesSeAplico('Quité Correo de clientes.');
  });

  it('al quitar un dato también se borra su valor en las filas', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(quitarCampo('clients', 'correo'));

    entoncesLaPiezaYaNoTieneElDato('clients', 'email');
  });

  it('no quita un dato obligatorio, una conexión ni uno que no existe', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      quitarCampo('inventory', 'Producto'),
      quitarCampo('sales', 'Cliente'),
      quitarCampo('inventory', 'Color'),
      quitarCampo('nada', 'Producto'),
    );

    entoncesSoloSeOmitio(
      'No se puede quitar «Producto»: Ensambla la necesita para funcionar.',
      'No se puede quitar «Cliente»: Ensambla la necesita para funcionar.',
      'No encontré ese dato.',
      'No encontré ese dato.',
    );
  });
});

describe('renombrar un dato', () => {
  it('lo busca sin importar mayúsculas ni tildes y recorta el nombre nuevo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('Clientes', 'telefono', { etiqueta: ' Celular ' }));

    entoncesSoloSeHizo('Renombré Teléfono a Celular en clientes.');
  });

  it('la clave y el tipo no cambian', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('Clientes', 'telefono', { etiqueta: ' Celular ' }));

    entoncesLosDatosSon('clients', { Celular: { key: 'phone', type: 'tel' } });
  });

  it('las filas conservan sus valores', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('Clientes', 'telefono', { etiqueta: ' Celular ' }));

    entoncesLaPrimeraFilaConserva('clients', 'phone', '11 5555-0101');
  });

  it('recorta un nombre nuevo demasiado largo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('clients', 'Notas', { etiqueta: 'x'.repeat(80) }));

    entoncesLaEtiquetaDeLaClaveMide('clients', 'notes', 60);
  });

  it('permite cambiar sólo las mayúsculas', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('clients', 'Correo', { etiqueta: 'CORREO' }));

    entoncesLaEtiquetaDeLaClave('clients', 'email', 'CORREO');
  });

  it('no repite el nombre de otro dato de la pieza', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('clients', 'Correo', { etiqueta: 'teléfono' }));

    entoncesSeOmitio('Ya hay un dato «teléfono» en clientes.');
  });

  it('un nombre repetido deja el dato como estaba', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('clients', 'Correo', { etiqueta: 'teléfono' }));

    entoncesLaEtiquetaDeLaClave('clients', 'email', 'Correo');
  });

  it('un vínculo y el dato principal no se pueden renombrar: son datos fijos', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('Ventas', 'Cliente', { etiqueta: 'Clienta' }),
      editarCampo('clients', 'Nombre', { etiqueta: 'Nombre y apellido' }),
    );

    entoncesSoloSeOmitio(
      'No se puede cambiar ni renombrar «Cliente»: Ensambla la necesita para funcionar.',
      'No se puede cambiar ni renombrar «Nombre»: Ensambla la necesita para funcionar.',
    );
  });

  it('un dato fijo que se quiso renombrar queda con su nombre', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('Ventas', 'Cliente', { etiqueta: 'Clienta' }));

    entoncesLosDatosSon('sales', { Cliente: { key: 'customer', type: 'relation' } });
  });
});

describe('cambiar el tipo de un dato', () => {
  it('cambia el tipo', () => {
    dadoUnInventarioConMinimosEscritosDeDistintasFormas();

    cuandoSamiPide(
      editarCampo('inventory', 'Stock mínimo', { tipo: 'text' }),
      editarCampo('inventory', 'Stock mínimo', { tipo: 'number' }),
    );

    entoncesSeAplico(
      'Cambié Stock mínimo a texto en inventario.',
      'Cambié Stock mínimo a número en inventario.',
    );
  });

  it('convierte los valores de las filas y borra lo que no sirve', () => {
    dadoUnInventarioConMinimosEscritosDeDistintasFormas();

    cuandoSamiPide(
      editarCampo('inventory', 'Stock mínimo', { tipo: 'text' }),
      editarCampo('inventory', 'Stock mínimo', { tipo: 'number' }),
    );

    entoncesLosMinimosQuedanComoNumeroYLoQueNoSirveSeBorra();
  });

  it('al pasar a lista avisa el cambio', () => {
    dadosClientesConNotasQueEstanYQueNoEstanEnLaLista();

    cuandoSamiPide(
      editarCampo('clients', 'Notas', { tipo: 'select', opciones: SEIS_OPCIONES_CON_REPETIDAS }),
    );

    entoncesSeAplico('Cambié Notas a lista de opciones en clientes.');
  });

  it('al pasar a lista usa hasta seis opciones, sin repetir', () => {
    dadosClientesConNotasQueEstanYQueNoEstanEnLaLista();

    cuandoSamiPide(
      editarCampo('clients', 'Notas', { tipo: 'select', opciones: SEIS_OPCIONES_CON_REPETIDAS }),
    );

    entoncesLosDatosSon('clients', {
      Notas: { type: 'select', options: ['Mayorista', 'Minorista', 'A', 'B', 'C', 'D'] },
    });
  });

  it('al pasar a lista descarta los valores que no están entre las opciones', () => {
    dadosClientesConNotasQueEstanYQueNoEstanEnLaLista();

    cuandoSamiPide(
      editarCampo('clients', 'Notas', { tipo: 'select', opciones: SEIS_OPCIONES_CON_REPETIDAS }),
    );

    entoncesLasNotasQueNoEstanEnLaListaSeBorran();
  });

  it('cambia las opciones de una lista', () => {
    dadoUnAlmacenConGastos();

    cuandoSamiPide(editarCampo('expenses', 'Categoría', { opciones: ['Mercadería', 'Impuestos'] }));

    entoncesSeAplico('Cambié las opciones de Categoría en gastos.');
  });

  it('con opciones nuevas, los valores que ya no están se borran', () => {
    dadoUnAlmacenConGastos();

    cuandoSamiPide(editarCampo('expenses', 'Categoría', { opciones: ['Mercadería', 'Impuestos'] }));

    entoncesLasCategoriasDeLosGastosSon(['Mercadería', undefined]);
  });

  it('al dejar de ser lista se le sacan las opciones', () => {
    dadosGastosConLaCategoriaComoListaPropia();

    cuandoSamiPide(editarCampo('expenses', 'Categoría', { tipo: 'text' }));

    entoncesLaCategoriaEsTextoSinOpciones();
  });

  it('no pasa a lista sin al menos dos opciones válidas', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('clients', 'Notas', { tipo: 'select' }),
      editarCampo('clients', 'Notas', { tipo: 'select', opciones: ['Sola', ' ', 'sola'] }),
    );

    entoncesSeOmitio(
      'Una lista necesita al menos dos opciones.',
      'Una lista necesita al menos dos opciones.',
    );
  });

  it('una lista sin opciones deja el dato con su tipo de antes', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('clients', 'Notas', { tipo: 'select' }));

    entoncesLosDatosSon('clients', { Notas: { type: 'textarea' } });
  });

  it('no cambia el tipo de un vínculo, del dato principal ni a un tipo desconocido', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('sales', 'Cliente', { tipo: 'text' }),
      editarCampo('clients', 'Nombre', { tipo: 'number' }),
      editarCampo('clients', 'Notas', { tipo: 'relation' }),
      editarCampo('clients', 'Notas', { tipo: 'color' }),
    );

    entoncesSoloSeOmitio(
      '«Cliente» conecta con otra pieza: no le puedo cambiar el tipo.',
      '«Nombre» es el dato principal de clientes: no le puedo cambiar el tipo.',
      'No conozco el tipo «relation».',
      'No conozco el tipo «color».',
    );
  });

  it('el vínculo y el dato principal conservan su tipo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('sales', 'Cliente', { tipo: 'text' }),
      editarCampo('clients', 'Nombre', { tipo: 'number' }),
    );

    entoncesLosDatosSon('clients', { Nombre: { type: 'text' } });
  });

  it('el tipo del dato principal se ignora y lo demás se aplica', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('clients', 'Nombre', { etiqueta: 'Razón social', tipo: 'number' }));

    entoncesSoloSeHizo('Renombré Nombre a Razón social en clientes.');
  });

  it('el dato principal renombrado sigue siendo texto', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('clients', 'Nombre', { etiqueta: 'Razón social', tipo: 'number' }));

    entoncesLosDatosSon('clients', { 'Razón social': { type: 'text' } });
  });
});

describe('obligatorio y lugar de un dato', () => {
  it('lo hace obligatorio o no', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('clients', 'Teléfono', { obligatorio: true }),
      editarCampo('clients', 'Teléfono', { obligatorio: false }),
    );

    entoncesSeAplico(
      'Hice obligatorio Teléfono en clientes.',
      'Teléfono ya no es obligatorio en clientes.',
    );
  });

  it('el dato queda con lo pedido', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('clients', 'Teléfono', { obligatorio: true }),
      editarCampo('clients', 'Correo', { obligatorio: true }),
      editarCampo('clients', 'Teléfono', { obligatorio: false }),
    );

    entoncesLosDatosSon('clients', { Teléfono: { required: false }, Correo: { required: true } });
  });

  it('mueve el dato al lugar pedido, acotado a los que hay', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('Ventas', 'Cliente', { posicion: 1 }),
      editarCampo('clients', 'Nombre', { posicion: 99 }),
      editarCampo('clients', 'Correo', { posicion: '0' }),
    );

    entoncesSeAplico(
      'Moví Cliente al lugar 1 en ventas.',
      'Moví Nombre al lugar 4 en clientes.',
      'Moví Correo al lugar 1 en clientes.',
    );
  });

  it('el dato movido queda en su lugar nuevo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('Ventas', 'Cliente', { posicion: 1 }));

    entoncesLasEtiquetasSon('sales', [
      'Cliente',
      'Producto',
      'Cantidad',
      'Precio',
      'Fecha',
      'Fiado',
    ]);
  });

  it('los lugares fuera de rango se acotan al primero y al último', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('clients', 'Nombre', { posicion: 99 }),
      editarCampo('clients', 'Correo', { posicion: '0' }),
    );

    entoncesLasEtiquetasSon('clients', ['Correo', 'Teléfono', 'Notas', 'Nombre']);
  });

  it('renombra y mueve en la misma acción', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('sales', 'Cliente', { etiqueta: 'Clienta', posicion: 1 }));

    entoncesSeAplico('Renombré Cliente a Clienta en ventas.', 'Moví Clienta al lugar 1 en ventas.');
  });

  it('el dato renombrado y movido queda primero con su nombre nuevo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('sales', 'Cliente', { etiqueta: 'Clienta', posicion: 1 }));

    entoncesLaPrimeraEtiquetaEs('sales', 'Clienta');
  });

  it('omite un lugar que no es un número', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(editarCampo('clients', 'Notas', { posicion: 'arriba' }));

    entoncesSeOmitio('No entendí a qué lugar mover ese dato.');
  });
});

describe('lo que no cambia un dato', () => {
  it('si no cambia nada, o si falta la pieza o el dato, se omite', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('clients', 'Teléfono', {
        etiqueta: 'Teléfono',
        tipo: 'tel',
        obligatorio: false,
        posicion: 2,
      }),
      editarCampo('clients', 'Teléfono'),
      editarCampo('clients', 'Teléfono', { obligatorio: 'sí' }),
      editarCampo('clients', 'Edad', { etiqueta: 'Años' }),
      editarCampo('proveedores', 'Nombre', { etiqueta: 'Razón' }),
    );

    entoncesSoloSeOmitio(
      '«Teléfono» ya estaba así.',
      '«Teléfono» ya estaba así.',
      'No entendí si «Teléfono» es obligatorio.',
      'No encontré ese dato.',
      'No encontré ese dato.',
    );
  });

  it('lo omitido deja el sistema igual', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('clients', 'Teléfono', {
        etiqueta: 'Teléfono',
        tipo: 'tel',
        obligatorio: false,
        posicion: 2,
      }),
      editarCampo('clients', 'Teléfono', { obligatorio: 'sí' }),
    );

    entoncesNoCambioNada();
  });

  it('es todo o nada: si una parte no vale, no se aplica ninguna', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('clients', 'Notas', { etiqueta: 'Comentarios', tipo: 'color', posicion: 1 }),
      editarCampo('clients', 'Notas', { etiqueta: 'Comentarios', obligatorio: 'sí' }),
    );

    entoncesSeAplico();
  });

  it('es todo o nada: el resto de los datos queda igual', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      editarCampo('clients', 'Notas', { etiqueta: 'Comentarios', tipo: 'color', posicion: 1 }),
      editarCampo('clients', 'Notas', { etiqueta: 'Comentarios', obligatorio: 'sí' }),
    );

    entoncesLasEtiquetasSon('clients', ['Nombre', 'Teléfono', 'Correo', 'Notas']);
  });

  it('una lista vacía, nulos o textos en blanco cuentan como no pedidos', () => {
    dadoUnAlmacenConGastos();

    cuandoSamiPide(
      editarCampo('expenses', 'Categoría', {
        etiqueta: ' ',
        tipo: null,
        opciones: [],
        obligatorio: null,
        posicion: '',
      }),
    );

    entoncesSeOmitio('«Categoría» ya estaba así.');
  });
});
