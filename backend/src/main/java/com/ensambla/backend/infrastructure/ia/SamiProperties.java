package com.ensambla.backend.infrastructure.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Locale;

/**
 * Configuración de Sami ({@code ensambla.sami.*}).
 *
 * @param umbralConfianza confianza mínima para proponer sin preguntar más ni consultar a la IA
 * @param maxPreguntas    cuántas preguntas de aclaración se le hacen como máximo a la persona
 * @param limiteDiarioIa  consultas con IA por negocio y por día (UTC) del asistente de plantillas
 * @param limiteDiarioTaller consultas con IA por negocio y por día (UTC) del taller conversacional; comparte el
 *                        contador con el asistente de plantillas
 * @param ia              proveedor y modelos del respaldo con IA
 */
@ConfigurationProperties(prefix = "ensambla.sami")
public record SamiProperties(
        @DefaultValue("0.7") double umbralConfianza,
        @DefaultValue("3") int maxPreguntas,
        @DefaultValue("20") int limiteDiarioIa,
        @DefaultValue("60") int limiteDiarioTaller,
        @DefaultValue Ia ia
) {

    /**
     * Lo que Sami pisa de la configuración global ({@code ensambla.ia.*}); todo lo que quede vacío sale de ahí.
     *
     * @param proveedor     vacío usa {@code ensambla.ia.proveedor}; si tiene valor ({@code simulado},
     *                      {@code anthropic}, {@code bedrock}) manda ése
     * @param region        región que pisa la del proveedor (Bedrock)
     * @param perfilAws     perfil de {@code ~/.aws} que pisa el del proveedor (Bedrock)
     * @param modeloRapido  modelo chico que pisa el del proveedor: atiende todo lo que consulta a la IA
     * @param modeloAvanzado modelo capaz que pisa el del proveedor: sólo se consulta si el chico no alcanza; si los dos
     *                      quedan vacíos no hay escalado
     * @param umbralEscalar confianza que tiene que alcanzar un modelo para no pasar al siguiente
     * @param maxTokensTaller tokens de salida máximos de un turno del taller (usa el modelo rápido)
     */
    public record Ia(
            String proveedor,
            String region,
            String perfilAws,
            String modeloRapido,
            String modeloAvanzado,
            @DefaultValue("30s") Duration timeout,
            @DefaultValue("700") int maxTokens,
            @DefaultValue("0.6") double umbralEscalar,
            @DefaultValue("900") int maxTokensTaller
    ) {

        /** Los ajustes con los que habla Sami: los del proveedor que corresponda, con lo que Sami pisa. */
        public AjustesIa ajustes(IaProperties global) {
            String nombre = AjustesIa.vacio(proveedor) ? global.proveedor() : proveedor.strip().toLowerCase(Locale.ROOT);
            IaProperties.Proveedor datos = global.de(nombre);
            return new AjustesIa(nombre, datos.apiKey(),
                    AjustesIa.elegir(region, datos.region()),
                    AjustesIa.elegir(perfilAws, datos.perfilAws()),
                    AjustesIa.elegir(modeloRapido, datos.modeloRapido()),
                    AjustesIa.elegir(modeloAvanzado, datos.modeloAvanzado()),
                    datos.modeloVision(),
                    timeout);
        }
    }
}
