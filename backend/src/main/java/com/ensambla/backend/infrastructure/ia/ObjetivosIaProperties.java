package com.ensambla.backend.infrastructure.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * La IA que redacta los mini-objetivos ({@code ensambla.objetivos.ia.*}). El proveedor y el modelo son los de Sami.
 *
 * @param habilitada  si se consulta a la IA para redactar; apagada, cada mini-objetivo lleva su texto de reserva
 * @param limiteDiario consultas por negocio y por día (argentino); comparte el contador con Sami
 * @param conversacionHabilitada si la persona puede armar un objetivo conversando con Sami; apagada, o sin acceso a la
 *                     IA, se ofrece el seguimiento manual de respaldo
 * @param maxMensajes  cuántos mensajes de la persona admite una conversación para armar un objetivo
 */
@ConfigurationProperties(prefix = "ensambla.objetivos.ia")
public record ObjetivosIaProperties(
        @DefaultValue("false") boolean habilitada,
        @DefaultValue("10") int limiteDiario,
        @DefaultValue("true") boolean conversacionHabilitada,
        @DefaultValue("6") int maxMensajes
) {
}
