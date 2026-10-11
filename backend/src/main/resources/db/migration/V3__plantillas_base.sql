-- ENS-41 / T-013: plantillas globales copiables. V1 y V2 permanecen intactas.
INSERT INTO plantilla (id, nombre, descripcion, icono, estructura)
VALUES
    ('00000000-0000-4000-8000-0000000000a1', 'Inventario', 'Controlá tus productos, su stock y el costo de cada uno.', 'box', '{"bloques":[{"nombre":"Productos","campos":[{"nombre":"Nombre","tipo":"TEXTO","obligatorio":true},{"nombre":"Stock","tipo":"NUMERO","obligatorio":true,"valorInicial":"0"},{"nombre":"Rubro","tipo":"OPCION","opciones":["Insumos","Terminados"]}]}]}'::jsonb),
    ('00000000-0000-4000-8000-0000000000a2', 'Pedidos', 'Anotá los pedidos de tus clientes y seguí su estado.', 'list-checks', '{"bloques":[{"nombre":"Pedidos","campos":[{"nombre":"Cliente","tipo":"TEXTO","obligatorio":true},{"nombre":"Entrega","tipo":"FECHA"},{"nombre":"Entregado","tipo":"SI_NO","valorInicial":"false"}]}]}'::jsonb),
    ('00000000-0000-4000-8000-0000000000a3', 'Clientes', 'Tu agenda de clientes con sus datos de contacto.', 'users', '{"bloques":[{"nombre":"Clientes","campos":[{"nombre":"Nombre","tipo":"TEXTO","obligatorio":true},{"nombre":"Teléfono","tipo":"TEXTO"}]}]}'::jsonb);

