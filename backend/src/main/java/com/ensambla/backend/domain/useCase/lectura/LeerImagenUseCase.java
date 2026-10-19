package com.ensambla.backend.domain.useCase.lectura;

import com.ensambla.backend.domain.dto.lectura.LeerImagenItem;
import com.ensambla.backend.domain.dto.lectura.LeerImagenRequest;
import com.ensambla.backend.domain.dto.lectura.LeerImagenRequest.CampoImagen;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.service.procesamiento.PropuestaLecturaService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lee una foto contra un bloque y devuelve los registros y las columnas nuevas para revisar. Es la misma lectura que
 * el lote de fotos (el bloque elegido es el destino y se cruzan columnas por nombre y sinónimos), pero sin guardar
 * nada: la respuesta es inmediata y la persona decide después qué incorporar.
 */
@RequiredArgsConstructor
public class LeerImagenUseCase {

    static final long MAXIMO_BYTES = 5L * 1024 * 1024;
    static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final LectorCuadernoPort lectorCuadernoPort;
    private final PropuestaLecturaService propuestaLecturaService;

    public LeerImagenItem execute(LeerImagenRequest request) {
        validar(request);
        UUID destinoId = UUID.randomUUID();
        Map<UUID, String> clavesPorCampo = new HashMap<>();
        List<CampoContexto> contexto = new ArrayList<>();
        List<CampoConocido> conocidos = new ArrayList<>();
        for (CampoImagen campo : request.campos()) {
            UUID campoId = UUID.randomUUID();
            clavesPorCampo.put(campoId, campo.clave());
            contexto.add(new CampoContexto(campo.nombre(), campo.tipo(), campo.opciones()));
            conocidos.add(new CampoConocido(campoId, campo.nombre(), campo.tipo(), campo.opciones()));
        }
        LecturaCuaderno lectura = lectorCuadernoPort.leer(
                List.of(new PaginaCuaderno(1, request.contenido(), request.tipoContenido())),
                List.of(new BloqueConocido(destinoId, request.nombreBloque(), contexto)), destinoId);
        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                lectura, List.of(new BloqueLectura(destinoId, request.nombreBloque(), conocidos)), destinoId);
        return resultado(request, propuesta, clavesPorCampo);
    }

    private void validar(LeerImagenRequest request) {
        if (request.contenido() == null || request.contenido().length == 0) {
            throw new BusinessRuleException("La imagen está vacía");
        }
        if (request.contenido().length > MAXIMO_BYTES) {
            throw new BusinessRuleException("La imagen pesa más de 5 MB. Elegí una más liviana");
        }
        if (request.tipoContenido() == null || !TIPOS.contains(request.tipoContenido())) {
            throw new BusinessRuleException("Elegí una imagen JPG, PNG o WEBP");
        }
        if (request.campos().isEmpty()) {
            throw new BusinessRuleException("El bloque no tiene datos para leer");
        }
    }

    private LeerImagenItem resultado(LeerImagenRequest request, PropuestaLectura propuesta,
                                     Map<UUID, String> clavesPorCampo) {
        Set<String> usadas = new HashSet<>(clavesPorCampo.values());
        Map<String, String> clavesNuevas = new LinkedHashMap<>();
        List<CampoPropuesto> nuevos = new ArrayList<>();
        if (request.destinoExistente()) {
            for (CampoPropuesto nuevo : propuesta.camposNuevos()) {
                String clave = claveLibre(nuevo.nombre(), usadas);
                usadas.add(clave);
                clavesNuevas.put(nuevo.clave(), clave);
                nuevos.add(new CampoPropuesto(clave, nuevo.nombre(), nuevo.tipo(), nuevo.opciones()));
            }
        }
        List<Map<String, DatoLeido>> filas = propuesta.lectura().filas().stream().map(fila -> {
            Map<String, DatoLeido> datos = new LinkedHashMap<>();
            fila.datos().forEach((clave, dato) -> {
                UUID campoId = propuesta.mapeoCampos().get(clave);
                String destino = campoId == null ? clavesNuevas.get(clave) : clavesPorCampo.get(campoId);
                if (destino != null) {
                    datos.put(destino, dato);
                }
            });
            return datos;
        }).toList();
        return new LeerImagenItem(filas, nuevos, propuesta.lectura().advertencias());
    }

    /** Clave para una columna nueva a partir de su nombre, sin repetir las del bloque ni las de otras nuevas. */
    private String claveLibre(String nombre, Set<String> usadas) {
        String base = TextoNormalizado.de(nombre).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = "columna";
        }
        String clave = base;
        for (int numero = 2; usadas.contains(clave); numero++) {
            clave = base + "_" + numero;
        }
        return clave;
    }
}
