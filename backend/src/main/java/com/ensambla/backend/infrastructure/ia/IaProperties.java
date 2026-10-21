package com.ensambla.backend.infrastructure.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Configuración de la IA ({@code ensambla.ia.*}). {@code proveedor} elige quién atiende la lectura de cuadernos y, si
 * Sami no lo pisa ({@code ensambla.sami.ia.proveedor}), también a Sami. Las credenciales y los modelos de cada
 * proveedor van en un solo lugar: {@code ensambla.ia.proveedores.<nombre>.*}.
 *
 * @param proveedor            {@code simulado} (por defecto, sin costo), {@code anthropic} o {@code bedrock}; en blanco
 *                             es {@code simulado}
 * @param modelo               modelo de visión que pisa el del proveedor ({@code ENSAMBLA_IA_MODELO}); vacío usa
 *                             {@code proveedores.<nombre>.modelo-vision}
 * @param timeout              tiempo máximo de espera de una lectura
 * @param maxPaginasPorLectura cuántas páginas se mandan juntas como máximo
 * @param umbralConfianza      debajo de este valor un dato se marca como dudoso
 * @param proveedores          credenciales y modelos de cada proveedor, por nombre
 */
@ConfigurationProperties(prefix = "ensambla.ia")
public record IaProperties(
        @DefaultValue("simulado") String proveedor,
        String modelo,
        @DefaultValue("60s") Duration timeout,
        @DefaultValue("10") int maxPaginasPorLectura,
        @DefaultValue("0.8") double umbralConfianza,
        Map<String, Proveedor> proveedores
) {

    static final String SIMULADO = "simulado";
    private static final Proveedor SIN_DATOS = new Proveedor(null, null, null, null, null, null);

    public IaProperties {
        proveedor = AjustesIa.elegir(proveedor, SIMULADO).strip().toLowerCase(Locale.ROOT);
        proveedores = proveedores == null ? Map.of() : Map.copyOf(proveedores);
    }

    /**
     * Credenciales y modelos de un proveedor; lo que no usa queda vacío. Las claves van sólo por variable de entorno o
     * por el archivo de secretos.
     *
     * @param apiKey         clave de la API (Anthropic)
     * @param region         región (Bedrock)
     * @param perfilAws      perfil de {@code ~/.aws}; vacío usa la cadena de credenciales por defecto (Bedrock)
     * @param modeloRapido   modelo chico (taller y primera consulta de Sami)
     * @param modeloAvanzado modelo capaz (escalado de Sami); vacío no escala
     * @param modeloVision   modelo que lee las fotos del cuaderno
     */
    public record Proveedor(String apiKey, String region, String perfilAws, String modeloRapido,
                            String modeloAvanzado, String modeloVision) {
    }

    /** Lo configurado para ese proveedor o, si no hay nada (el simulado), todo vacío. */
    public Proveedor de(String nombre) {
        return proveedores.getOrDefault(nombre, SIN_DATOS);
    }

    /** Los ajustes del proveedor global, con el modelo de visión que pisa {@code modelo}. */
    public AjustesIa ajustes() {
        Proveedor datos = de(proveedor);
        return new AjustesIa(proveedor, datos.apiKey(), datos.region(), datos.perfilAws(), datos.modeloRapido(),
                datos.modeloAvanzado(), AjustesIa.elegir(modelo, datos.modeloVision()), timeout);
    }

    /** El modelo que lee los cuadernos; nulo si no hay ninguno configurado (el simulado) o está en blanco. */
    public String modeloVision() {
        String modeloVision = ajustes().modeloVision();
        return AjustesIa.vacio(modeloVision) ? null : modeloVision;
    }
}
