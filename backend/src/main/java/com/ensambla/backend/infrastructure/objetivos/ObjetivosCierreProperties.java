package com.ensambla.backend.infrastructure.objetivos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * El cierre semanal de objetivos ({@code ensambla.objetivos.cierre.*}). Si está habilitado y cuándo corre lo resuelve
 * {@code CierreSemanalObjetivosWorker}.
 *
 * @param maxSemanasPendientes cuántas semanas sin cerrar se recuperan como mucho por objetivo
 * @param tamanoTanda          cuántos negocios se cargan por vez
 */
@ConfigurationProperties(prefix = "ensambla.objetivos.cierre")
public record ObjetivosCierreProperties(
        @DefaultValue("8") int maxSemanasPendientes,
        @DefaultValue("50") int tamanoTanda) {
}
