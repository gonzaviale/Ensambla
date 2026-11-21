-- V37: métricas nuevas para los mini-objetivos medidos (H-60, ENS-340): productos en su mínimo o por debajo, capital
-- parado en mercadería y variedad de productos vendidos por semana. Ninguna es de un producto en particular.

ALTER TABLE mini_objetivo DROP CONSTRAINT mini_objetivo_tipo_check;

ALTER TABLE mini_objetivo ADD CONSTRAINT mini_objetivo_tipo_check CHECK (tipo IN (
    'SUBIR_PRECIO', 'MOVER_PARADO', 'PONER_A_LA_VISTA', 'RECORTAR_GASTO', 'MEJORAR_MARGEN', 'VENTAS_SEMANALES',
    'PRODUCTOS_BAJO_EL_MINIMO', 'CAPITAL_PARADO', 'VARIEDAD_VENDIDA'));
