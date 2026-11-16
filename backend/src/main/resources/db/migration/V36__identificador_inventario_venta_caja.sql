UPDATE venta_caja
SET datos = jsonb_set(datos, '{inventarioId}', jsonb_build_object('valor', inventario_id::text))
WHERE jsonb_typeof(datos->'inventarioId') = 'string';
