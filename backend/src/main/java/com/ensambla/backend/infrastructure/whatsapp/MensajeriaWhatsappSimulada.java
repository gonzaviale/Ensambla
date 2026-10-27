package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** No envía nada: deja las respuestas en el log y en memoria. Sirve para desarrollar y para los tests. */
@Slf4j
public class MensajeriaWhatsappSimulada implements MensajeriaWhatsappPort {

    public record Enviado(String telefono, String texto) {
    }

    /** Los primeros bytes de un JPEG: alcanza para que la foto de ejemplo pase la validación de formato. */
    private static final byte[] FOTO_DE_EJEMPLO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private final List<Enviado> enviados = new CopyOnWriteArrayList<>();
    private final Map<String, ArchivoWhatsapp> medias = new ConcurrentHashMap<>();
    private final String numeroBot;
    private volatile boolean fallarEnvios;

    public MensajeriaWhatsappSimulada(String numeroBot) {
        this.numeroBot = numeroBot;
    }

    @Override
    public boolean enviarTexto(String telefono, String texto) {
        if (fallarEnvios) {
            log.info("WhatsApp simulado → {}: falló el envío", telefono);
            return false;
        }
        log.info("WhatsApp simulado → {}: {}", telefono, texto);
        enviados.add(new Enviado(telefono, texto));
        return true;
    }

    /** Sin una foto cargada con {@link #ponerMedia} devuelve una foto de ejemplo. */
    @Override
    public ArchivoWhatsapp descargarMedia(String idExterno) {
        ArchivoWhatsapp cargada = medias.get(idExterno);
        return cargada != null ? cargada : fotoDeEjemplo();
    }

    @Override
    public String numeroBot() {
        return numeroBot;
    }

    public List<Enviado> enviados() {
        return List.copyOf(enviados);
    }

    /** Para los tests: hace que los próximos envíos devuelvan {@code false}. */
    public void fallarEnvios(boolean fallar) {
        this.fallarEnvios = fallar;
    }

    /** Para los tests: la foto que se descarga para ese mensaje. */
    public void ponerMedia(String idExterno, ArchivoWhatsapp archivo) {
        medias.put(idExterno, archivo);
    }

    private static ArchivoWhatsapp fotoDeEjemplo() {
        return new ArchivoWhatsapp(FOTO_DE_EJEMPLO.clone(), "image/jpeg");
    }
}
