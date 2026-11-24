-- V46: el plan que no cuesta nada pasa a llamarse «Básico» para poder mostrar su precio ($0 por mes) sin repetir la
-- palabra. El código `GRATUITO` no cambia: lo usan las reglas, las suscripciones y los tests.

UPDATE plan SET nombre = 'Básico', actualizado_en = now() WHERE codigo = 'GRATUITO';
