package com.ensambla.backend.infrastructure.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * La IA que redacta los carteles ({@code ensambla.carteles.ia.*}).
 *
 * @param proveedor {@code simulado} (por defecto) arma los textos de reserva sin llamar a nadie; {@code real} consulta
 *                  al proveedor y al modelo rápido de Sami
 */
@ConfigurationProperties(prefix = "ensambla.carteles.ia")
public record CartelesIaProperties(@DefaultValue("simulado") String proveedor) {
}
