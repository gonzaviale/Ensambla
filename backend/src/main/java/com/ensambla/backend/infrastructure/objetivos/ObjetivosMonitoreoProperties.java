package com.ensambla.backend.infrastructure.objetivos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * El monitoreo de objetivos ({@code ensambla.objetivos.monitoreo.*}). Si está habilitado y cada cuánto corre lo
 * resuelve {@code MonitoreoObjetivosWorker}.
 *
 * @param tamanoTanda cuántos negocios se cargan por vez
 */
@ConfigurationProperties(prefix = "ensambla.objetivos.monitoreo")
public record ObjetivosMonitoreoProperties(@DefaultValue("50") int tamanoTanda) {
}
