package com.ensambla.backend.infrastructure.whatsapp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/** Avisos con el formato real del webhook de Meta (WhatsApp Cloud API) y su firma. */
public final class WhatsappWebhookTestData {

    public static final String VERIFY_TOKEN = "token-de-prueba";
    public static final String APP_SECRET = "secreto-de-prueba";
    public static final WhatsappProperties PROPERTIES = new WhatsappProperties(VERIFY_TOKEN, APP_SECRET);

    public static final String AVISO_TEXTO = """
            {"object":"whatsapp_business_account","entry":[{"id":"102290129340398","changes":[{"value":{
            "messaging_product":"whatsapp",
            "metadata":{"display_phone_number":"15550783881","phone_number_id":"106540352242922"},
            "contacts":[{"profile":{"name":"Marta"},"wa_id":"5491122334455"}],
            "messages":[{"from":"5491122334455","id":"wamid.texto","timestamp":"1792411170","type":"text",
            "text":{"body":"Vendí 3 velas a Marta a 4500"}}]},"field":"messages"}]}]}
            """;

    public static final String AVISO_AUDIO = """
            {"object":"whatsapp_business_account","entry":[{"id":"102290129340398","changes":[{"value":{
            "messaging_product":"whatsapp",
            "metadata":{"display_phone_number":"15550783881","phone_number_id":"106540352242922"},
            "messages":[{"from":"5491122334455","id":"wamid.audio","timestamp":"1792411170","type":"audio",
            "audio":{"mime_type":"audio/ogg; codecs=opus","sha256":"abc","id":"1234567890123456","voice":true}}]},
            "field":"messages"}]}]}
            """;

    /** Meta también avisa cuando un mensaje enviado se entregó o se leyó: no trae {@code messages}. */
    public static final String AVISO_ESTADO = """
            {"object":"whatsapp_business_account","entry":[{"id":"102290129340398","changes":[{"value":{
            "messaging_product":"whatsapp",
            "statuses":[{"id":"wamid.enviado","status":"delivered","timestamp":"1792411170",
            "recipient_id":"5491122334455"}]},"field":"messages"}]}]}
            """;

    private WhatsappWebhookTestData() {
    }

    public static String aviso(String mensajes) {
        return "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"id\":\"1\",\"changes\":[{\"value\":"
                + "{\"messages\":[" + mensajes + "]},\"field\":\"messages\"}]}]}";
    }

    public static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /** La firma que pondría Meta en {@code X-Hub-Signature-256}. */
    public static String firma(byte[] cuerpo, String secreto) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(cuerpo));
    }
}
