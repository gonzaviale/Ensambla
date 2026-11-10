package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.zona.analizarZona.AnalizarZona;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrir;
import com.ensambla.backend.domain.useCase.zona.obtenerZona.ObtenerZona;
import com.ensambla.backend.infrastructure.presentation.dto.zona.AnalizarZonaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.zona.DondeAbrirWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ExplorarDondeAbrirWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.zona.ZonaPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.mapper.zona.DondeAbrirPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/zona")
@Tag(name = "Zona", description = "Datos agregados y anónimos de los comercios de una zona")
@RequiredArgsConstructor
public class ZonaController {

    private final ObtenerZona obtenerZona;

    private final AnalizarZona analizarZona;

    private final ExplorarDondeAbrir explorarDondeAbrir;

    private final ZonaPresentationMapper zonaPresentationMapper;

    private final DondeAbrirPresentationMapper dondeAbrirPresentationMapper;

    @GetMapping("/agregados")
    @Operation(
            summary = "Precios y ventas agregados de la zona del negocio de la sesión",
            description = "La vista indica la pantalla que pide los datos y define el orden y el cupo del plan gratuito: "
                    + "«precios» (por defecto) muestra primero los productos que el negocio ya tiene; «cerca» muestra "
                    + "primero los más vendidos que todavía no tiene. Cada vista gasta su propio cupo mensual.")
    public ResponseEntity<ZonaWebResponse> agregados(Authentication authentication,
                                                     @RequestParam(defaultValue = "precios") String vista) {
        return ResponseEntity.ok(zonaPresentationMapper.toResponse(obtenerZona.execute(
                zonaPresentationMapper.toObtenerRequest(UsuarioId.de(authentication.getName()), vista))));
    }

    @PostMapping("/analisis")
    @Operation(
            summary = "Precios y ventas agregados de otra zona, indicada por un punto del mapa, un barrio o una dirección",
            description = "Con latitud y longitud resuelve la zona precargada que contiene el punto; si no, busca el "
                    + "barrio o la dirección escritos. Si en la zona hay menos de 3 comercios devuelve la lista de "
                    + "productos vacía (no hay datos suficientes). No filtra por rubro. Los productos vienen ordenados "
                    + "por cuántos comercios los venden y el plan gratuito ve sólo los primeros de su cupo, que es "
                    + "aparte del de Mi zona.")
    public ResponseEntity<ZonaWebResponse> analizar(Authentication authentication,
                                                    @Valid @RequestBody AnalizarZonaWebRequest request) {
        return ResponseEntity.ok(zonaPresentationMapper.toResponse(
                analizarZona.execute(zonaPresentationMapper.toAnalizarRequest(
                        request, UsuarioId.de(authentication.getName())))));
    }

    @PostMapping("/donde-abrir")
    @Operation(
            summary = "Compara zonas para abrir un nuevo local",
            description = "Aplica primero los límites solicitados y luego puntúa las zonas con datos trazables. "
                    + "La ubicación del local actual siempre se toma del negocio de la sesión. Los factores sin "
                    + "datos redistribuyen su peso y el alquiler sólo participa cuando la persona carga cotizaciones.")
    public ResponseEntity<DondeAbrirWebResponse> dondeAbrir(
            Authentication authentication,
            @Valid @RequestBody ExplorarDondeAbrirWebRequest request) {
        return ResponseEntity.ok(dondeAbrirPresentationMapper.toResponse(explorarDondeAbrir.execute(
                dondeAbrirPresentationMapper.toRequest(request, UsuarioId.de(authentication.getName())))));
    }
}
