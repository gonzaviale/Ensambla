package com.ensambla.backend.infrastructure.presentation.mapper.importacion;

import com.ensambla.backend.domain.dto.lectura.LeerImagenItem;
import com.ensambla.backend.domain.dto.lectura.LeerImagenRequest;
import com.ensambla.backend.domain.dto.lectura.LeerImagenRequest.CampoImagen;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.infrastructure.ia.IaProperties;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.LecturaImagenResponse;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.LecturaImagenResponse.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.LecturaImagenResponse.DudaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.PedidoLecturaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce entre los tipos de campo de la pantalla ({@code text}, {@code number}...) y los del dominio, y convierte
 * los valores leídos (siempre texto) a lo que espera cada campo.
 */
@Component
@RequiredArgsConstructor
public class ImportacionPresentationMapper {

    static final String MOTIVO_DUDA = "No estamos seguros de este dato.";

    /** Los tipos que la IA sabe leer; un campo que apunta a otro bloque ({@code relation}) lo completa la persona. */
    private static final Map<String, TipoCampo> TIPOS = Map.of(
            "text", TipoCampo.TEXTO, "textarea", TipoCampo.TEXTO, "email", TipoCampo.TEXTO,
            "tel", TipoCampo.TEXTO, "time", TipoCampo.TEXTO,
            "number", TipoCampo.NUMERO, "currency", TipoCampo.NUMERO,
            "date", TipoCampo.FECHA, "select", TipoCampo.OPCION, "checkbox", TipoCampo.SI_NO);

    private final IaProperties iaProperties;

    public LeerImagenRequest toRequest(MultipartFile imagen, PedidoLecturaRequest pedido) {
        List<CampoImagen> campos = pedido.campos().stream()
                .filter(campo -> TIPOS.containsKey(campo.type()))
                .map(campo -> new CampoImagen(campo.key(), campo.label(), TIPOS.get(campo.type()), campo.options()))
                .toList();
        return new LeerImagenRequest(
                contenido(imagen), imagen.getContentType(), pedido.nombre(), campos, pedido.destinoExistente());
    }

    public LecturaImagenResponse toResponse(LeerImagenItem item, PedidoLecturaRequest pedido) {
        Map<String, String> tipos = new LinkedHashMap<>();
        pedido.campos().forEach(campo -> tipos.put(campo.key(), campo.type()));
        List<CampoResponse> nuevos = item.camposNuevos().stream().map(this::toResponse).toList();
        nuevos.forEach(campo -> tipos.put(campo.key(), campo.type()));

        List<Map<String, Object>> filas = new ArrayList<>();
        List<DudaResponse> dudas = new ArrayList<>();
        for (Map<String, DatoLeido> datos : item.filas()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            tipos.forEach((clave, tipo) -> {
                DatoLeido dato = datos.get(clave);
                fila.put(clave, valor(dato == null ? null : dato.valor(), tipo));
                if (dato != null && dato.esDudoso(iaProperties.umbralConfianza())) {
                    dudas.add(new DudaResponse(filas.size(), clave, MOTIVO_DUDA));
                }
            });
            filas.add(fila);
        }
        return new LecturaImagenResponse(filas, "", nuevos, dudas);
    }

    private CampoResponse toResponse(CampoPropuesto campo) {
        boolean conOpciones = campo.tipo() == TipoCampo.OPCION;
        return new CampoResponse(campo.clave(), campo.nombre(), tipoDePantalla(campo.tipo()), false,
                conOpciones ? campo.opciones() : null);
    }

    private String tipoDePantalla(TipoCampo tipo) {
        return switch (tipo) {
            case NUMERO -> "number";
            case FECHA -> "date";
            case OPCION -> "select";
            case SI_NO -> "checkbox";
            case RELACION -> "relation";
            case TEXTO_LARGO -> "textarea";
            case MONEDA -> "currency";
            case EMAIL -> "email";
            case TELEFONO -> "tel";
            case HORA -> "time";
            case TEXTO, FORMULA -> "text";
        };
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
            return new BigDecimal(valor).doubleValue();
        }
        return valor;
    }

    private byte[] contenido(MultipartFile imagen) {
        try {
            return imagen.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen recibida", e);
        }
    }
}
