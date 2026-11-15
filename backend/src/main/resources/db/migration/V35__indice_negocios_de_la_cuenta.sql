-- T-199: lista y bloqueo de los negocios de una cuenta en el mismo orden estable que usa el selector.
CREATE INDEX ix_negocio_duenio_creado ON negocio (duenio_id, creado_en, id);
