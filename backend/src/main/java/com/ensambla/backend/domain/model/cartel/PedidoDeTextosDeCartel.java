package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.TonoDeMarca;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;

/**
 * Todo lo que se le cuenta a la IA para redactar: el negocio (nombre y rubro), cómo habla y lo que se anuncia. Nunca
 * lleva datos de personas: ni clientes, ni deudas, ni ventas, ni la dirección.
 */
public record PedidoDeTextosDeCartel(String negocio, RubroNegocio rubro, TonoDeMarca tono, DatosDePromocion datos) {
}
