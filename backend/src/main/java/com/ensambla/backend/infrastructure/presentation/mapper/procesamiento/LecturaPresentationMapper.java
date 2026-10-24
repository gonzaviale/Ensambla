package com.ensambla.backend.infrastructure.presentation.mapper.procesamiento;

import com.ensambla.backend.domain.useCase.procesamiento.ConsumoItem;
import com.ensambla.backend.domain.useCase.procesamiento.UsuarioLoteRequest;
import com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura.ConfirmarLecturaRequest;
import com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura.ConfirmarLecturaResponse;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenItem.DatoItem;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenItem;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenRequest;
import com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura.LecturaItem;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.ConfirmacionRequest;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.ConfirmacionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.ConsumoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.LecturaImagenResponse.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.LecturaImagenResponse.DudaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.LecturaImagenResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.LecturaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.PedidoLecturaRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class LecturaPresentationMapper {

    static final String MOTIVO_DUDA = "No estamos seguros de este dato.";

    /** Tipo de campo de la pantalla → nombre del {@code TipoCampo} del dominio. */
    private static final Map<String, String> TIPOS_DE_DOMINIO = Map.ofEntries(
            Map.entry("text", "TEXTO"), Map.entry("textarea", "TEXTO"), Map.entry("email", "TEXTO"),
            Map.entry("tel", "TEXTO"), Map.entry("time", "TEXTO"), Map.entry("number", "NUMERO"),
            Map.entry("currency", "NUMERO"), Map.entry("date", "FECHA"), Map.entry("select", "OPCION"),
            Map.entry("checkbox", "SI_NO"));

    /** Nombre del {@code TipoCampo} del dominio → tipo de campo de la pantalla. */
    private static final Map<String, String> TIPOS_DE_PANTALLA = Map.ofEntries(
            Map.entry("TEXTO", "text"), Map.entry("FORMULA", "text"), Map.entry("NUMERO", "number"),
            Map.entry("FECHA", "date"), Map.entry("OPCION", "select"), Map.entry("SI_NO", "checkbox"),
            Map.entry("RELACION", "relation"), Map.entry("TEXTO_LARGO", "textarea"), Map.entry("MONEDA", "currency"),
            Map.entry("EMAIL", "email"), Map.entry("TELEFONO", "tel"), Map.entry("HORA", "time"));

    public UsuarioLoteRequest toRequest(UUID usuarioId, UUID loteId) {
        return new UsuarioLoteRequest(usuarioId, loteId);
    }

    public ConfirmarLecturaRequest toConfirmarRequest(UUID usuarioId, UUID loteId, ConfirmacionRequest body) {
        return new ConfirmarLecturaRequest(
                usuarioId,
                loteId,
                body.proyectoId(),
                body.nombreProyecto(),
                body.bloqueId(),
                body.nombreBloque(),
                body.campos().stream()
                        .map(campo -> new ConfirmarLecturaRequest.CampoItem(
                                campo.clave(), campo.campoId(), campo.nombre(), campo.tipo(), campo.opciones()))
                        .toList(),
                body.filas().stream()
                        .map(fila -> new ConfirmarLecturaRequest.FilaItem(fila.pagina(), fila.valores()))
                        .toList());
    }

    public ConfirmacionResponse toResponse(ConfirmarLecturaResponse response) {
        return new ConfirmacionResponse(response.proyectoId(), response.bloqueId(), response.registros());
    }

    public LecturaResponse toResponse(LecturaItem item) {
        return new LecturaResponse(item.loteId(), item.estado(), item.motivoError(), item.intentos(), item.propuesta());
    }

    public ConsumoResponse toResponse(ConsumoItem item) {
        return new ConsumoResponse(item.fecha(), item.limite(), item.paginas(), item.reservadas(), item.disponibles(),
                item.tokensEntrada(), item.tokensSalida(), item.costoEstimadoUsd(), item.consumoCompleto());
    }

    /** Sólo se leen los tipos que la IA sabe leer; un campo que apunta a otro bloque ({@code relation}) lo completa la persona. */
    public LeerImagenRequest toRequest(UUID usuarioId, MultipartFile imagen, PedidoLecturaRequest pedido) {
        List<LeerImagenRequest.CampoItem> campos = pedido.campos().stream()
                .filter(campo -> TIPOS_DE_DOMINIO.containsKey(campo.type()))
                .map(campo -> new LeerImagenRequest.CampoItem(
                        campo.key(), campo.label(), TIPOS_DE_DOMINIO.get(campo.type()), campo.options()))
                .toList();
        return new LeerImagenRequest(usuarioId, contenido(imagen), pedido.nombre(), campos, pedido.destinoExistente());
    }

    public LecturaImagenResponse toResponse(LeerImagenItem item, PedidoLecturaRequest pedido) {
        Map<String, String> tipos = new LinkedHashMap<>();
        pedido.campos().forEach(campo -> tipos.put(campo.key(), campo.type()));
        List<CampoResponse> nuevos = item.camposNuevos().stream().map(this::toResponse).toList();
        nuevos.forEach(campo -> tipos.put(campo.key(), campo.type()));

        List<Map<String, Object>> filas = new ArrayList<>();
        List<DudaResponse> dudas = new ArrayList<>();
        for (Map<String, DatoItem> datos : item.filas()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            tipos.forEach((clave, tipo) -> {
                DatoItem dato = datos.get(clave);
                fila.put(clave, valor(dato == null ? null : dato.valor(), tipo));
                if (dato != null && dato.dudoso()) {
                    dudas.add(new DudaResponse(filas.size(), clave, MOTIVO_DUDA));
                }
            });
            filas.add(fila);
        }
        return new LecturaImagenResponse(filas, "", nuevos, dudas);
    }

    private CampoResponse toResponse(LeerImagenItem.CampoItem campo) {
        String tipo = TIPOS_DE_PANTALLA.getOrDefault(campo.tipo(), "text");
        return new CampoResponse(campo.clave(), campo.nombre(), tipo, false,
                "select".equals(tipo) ? campo.opciones() : null);
    }

    /** Sin dato, el valor vacío del campo: sí/no en falso y el resto en blanco, para que la persona lo complete. */
    private Object valor(String valor, String tipo) {
        if ("checkbox".equals(tipo)) {
            return Boolean.parseBoolean(valor);
        }
        if (valor == null) {
            return "";
        }
        if ("number".equals(tipo) || "currency".equals(tipo)) {
            return numero(valor);
        }
        return valor;
    }

    /** Un número mal formado queda en blanco en vez de romper la respuesta: la persona lo completa. */
    private Object numero(String valor) {
        try {
            return new BigDecimal(valor).doubleValue();
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private byte[] contenido(MultipartFile imagen) {
        try {
            return imagen.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen recibida", e);
        }
    }
}
