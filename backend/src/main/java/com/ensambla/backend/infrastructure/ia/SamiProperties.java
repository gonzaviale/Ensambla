package com.ensambla.backend.infrastructure.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración de Sami ({@code ensambla.sami.*}).
 *
 * @param umbralConfianza confianza mínima para proponer sin preguntar más ni consultar a la IA
 * @param maxPreguntas    cuántas preguntas de aclaración se le hacen como máximo a la persona
 * @param limiteDiarioIa  consultas con IA por negocio y por día (UTC)
 * @param ia              proveedor y modelos del respaldo con IA
 */
@ConfigurationProperties(prefix = "ensambla.sami")
public record SamiProperties(
        @DefaultValue("0.7") double umbralConfianza,
        @DefaultValue("3") int maxPreguntas,
        @DefaultValue("20") int limiteDiarioIa,
        @DefaultValue Ia ia
) {

    /**
     * @param proveedor     {@code simulado} (por defecto, sin costo) o {@code bedrock}
     * @param region        región de AWS donde está habilitado el acceso a los modelos
     * @param perfilAws     perfil de {@code ~/.aws}; vacío usa la cadena de credenciales por defecto (variables de
     *                      entorno, rol de la instancia, etc.)
     * @param modeloRapido  perfil de inferencia del modelo chico, que atiende todo lo que consulta a la IA
     * @param modeloAvanzado perfil de inferencia del modelo capaz, que sólo se consulta si el chico no alcanza;
     *                      vacío desactiva el escalado
     * @param umbralEscalar confianza que tiene que alcanzar un modelo para no pasar al siguiente
     */
    public record Ia(
            @DefaultValue("simulado") String proveedor,
            @DefaultValue("us-east-1") String region,
            String perfilAws,
            @DefaultValue("us.anthropic.claude-haiku-4-5-20251001-v1:0") String modeloRapido,
            @DefaultValue("us.anthropic.claude-sonnet-4-6") String modeloAvanzado,
            @DefaultValue("30s") Duration timeout,
            @DefaultValue("700") int maxTokens,
            @DefaultValue("0.6") double umbralEscalar
    ) {
    }
}
