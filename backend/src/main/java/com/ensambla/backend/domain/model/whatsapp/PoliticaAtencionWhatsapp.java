package com.ensambla.backend.domain.model.whatsapp;

import java.time.Duration;

/**
 * Cómo atiende Sami los mensajes de WhatsApp.
 *
 * @param limiteDiarioSami    consultas de texto por negocio y por día (el mismo contador que el chat de la app)
 * @param limiteDiarioPaginas fotos por negocio y por día, además del cupo mensual del plan
 * @param umbralConfianza     debajo de este valor un dato se marca como dudoso y una tabla se confirma con la persona
 * @param vigenciaReclamo     cuánto tiempo tiene un mensaje reclamado antes de darse por perdido
 */
public record PoliticaAtencionWhatsapp(int limiteDiarioSami, int limiteDiarioPaginas, double umbralConfianza,
                                       Duration vigenciaReclamo) {
}
