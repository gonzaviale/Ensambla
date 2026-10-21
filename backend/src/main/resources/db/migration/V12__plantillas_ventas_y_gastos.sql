-- ENS-176 / T-115: Sami sugiere plantillas por rubro; se suman las de ventas y gastos que faltaban.
INSERT INTO plantilla (id, nombre, descripcion, icono, estructura)
VALUES
    ('00000000-0000-4000-8000-0000000000a4', 'Ventas', 'Anotá lo que vendés, a quién y cuánto cobraste.', 'shopping-cart', '{"bloques":[{"nombre":"Ventas","campos":[{"nombre":"Fecha","tipo":"FECHA","obligatorio":true},{"nombre":"Producto","tipo":"TEXTO","obligatorio":true},{"nombre":"Cantidad","tipo":"NUMERO","valorInicial":"1"},{"nombre":"Monto","tipo":"NUMERO","obligatorio":true},{"nombre":"Cliente","tipo":"TEXTO"}]}]}'::jsonb),
    ('00000000-0000-4000-8000-0000000000a5', 'Gastos', 'Registrá lo que gastás en el negocio, en qué y cuánto.', 'wallet', '{"bloques":[{"nombre":"Gastos","campos":[{"nombre":"Fecha","tipo":"FECHA","obligatorio":true},{"nombre":"Detalle","tipo":"TEXTO","obligatorio":true},{"nombre":"Monto","tipo":"NUMERO","obligatorio":true}]}]}'::jsonb);
