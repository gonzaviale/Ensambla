package com.ensambla.backend.infrastructure.ia;

import java.time.Duration;

/**
 * Con qué proveedor de IA se habla y cómo, ya resuelto: el global ({@link IaProperties#ajustes()}) o el de Sami con lo
 * que pisa ({@link SamiProperties.Ia#ajustes(IaProperties)}). Lo que un proveedor no usa queda vacío (Anthropic no
 * tiene región; Bedrock no tiene clave).
 *
 * @param proveedor      nombre del {@link ProveedorIa} que arma los clientes
 * @param apiKey         credencial del proveedor (Anthropic)
 * @param region         región del proveedor (Bedrock)
 * @param perfilAws      perfil de {@code ~/.aws}; vacío usa la cadena de credenciales por defecto (Bedrock)
 * @param modeloRapido   modelo chico: atiende el taller y la primera consulta de Sami
 * @param modeloAvanzado modelo capaz: Sami escala a él si el rápido no está seguro; vacío no escala
 * @param modeloVision   modelo que lee las fotos del cuaderno
 * @param timeout        tiempo máximo de una llamada
 */
public record AjustesIa(
        String proveedor,
        String apiKey,
        String region,
        String perfilAws,
        String modeloRapido,
        String modeloAvanzado,
        String modeloVision,
        Duration timeout
) {

    /** {@code valor} si trae algo; si es nulo o está en blanco, {@code siNoHay}. */
    static String elegir(String valor, String siNoHay) {
        return vacio(valor) ? siNoHay : valor;
    }

    static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
