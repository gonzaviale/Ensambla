package com.ensambla.backend.infrastructure.presentation.mapper.zona;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirRequest;
import com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir.ExplorarDondeAbrirResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.DondeAbrirWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ExplorarDondeAbrirWebRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DondeAbrirPresentationMapper {

    public ExplorarDondeAbrirRequest toRequest(ExplorarDondeAbrirWebRequest request, UsuarioId usuarioId) {
        return new ExplorarDondeAbrirRequest(usuarioId, request.rubro(), request.publico(), request.importancia(),
                limites(request.limites()), alquileres(request));
    }

    private static ExplorarDondeAbrirRequest.Limites limites(
            ExplorarDondeAbrirWebRequest.LimitesWebRequest limites) {
        return limites == null ? null : new ExplorarDondeAbrirRequest.Limites(
                limites.distanciaMaxKm(), limites.competenciaMax(), limites.escuelasMin(), limites.parquesMin(),
                limites.transporteMin(), limites.alquilerMax());
    }

    private static Map<ZonaId, BigDecimal> alquileres(ExplorarDondeAbrirWebRequest request) {
        Map<ZonaId, BigDecimal> resultado = new LinkedHashMap<>();
        if (request.alquileres() == null) {
            return resultado;
        }
        request.alquileres().forEach(alquiler -> {
            ZonaId zonaId = ZonaId.de(alquiler.zonaId());
            if (resultado.putIfAbsent(zonaId, alquiler.valor()) != null) {
                throw new BusinessRuleException("No se puede informar dos veces el alquiler de la misma zona");
            }
        });
        return resultado;
    }

    public DondeAbrirWebResponse toResponse(ExplorarDondeAbrirResponse response) {
        ExplorarDondeAbrirResponse.NegocioActual negocio = response.negocioActual();
        return new DondeAbrirWebResponse(
                new DondeAbrirWebResponse.NegocioActualWebResponse(negocio.nombre(), negocio.rubro().name(),
                        negocio.direccion(), negocio.latitud(), negocio.longitud(), id(negocio.zonaId())),
                response.evaluadas(), response.ranking().stream().map(this::zona).toList(),
                response.descartadas().stream().map(descartada -> new DondeAbrirWebResponse.ZonaDescartadaWebResponse(
                        id(descartada.id()), descartada.nombre(), descartada.distanciaKm(), descartada.motivos()))
                        .toList(), response.avisos());
    }

    private DondeAbrirWebResponse.ZonaEvaluadaWebResponse zona(ExplorarDondeAbrirResponse.ZonaEvaluada zona) {
        return new DondeAbrirWebResponse.ZonaEvaluadaWebResponse(
                id(zona.id()), zona.nombre(), zona.localidad(), zona.jurisdiccion(), zona.latitud(), zona.longitud(),
                zona.distanciaKm(), zona.puntaje(), zona.coincidencia(),
                zona.aportes().stream().map(aporte -> new DondeAbrirWebResponse.AporteWebResponse(
                        aporte.factor().name(), aporte.etiqueta(), aporte.valor(), aporte.unidad(), aporte.peso(),
                        aporte.puntaje(), aporte.aporte(), fuente(aporte.fuente()), aporte.observadaEn())).toList(),
                zona.porQue(), zona.riesgos(), zona.puntos().stream().map(this::punto).toList());
    }

    private DondeAbrirWebResponse.PuntoWebResponse punto(DatosZonaDondeAbrir.Punto punto) {
        return new DondeAbrirWebResponse.PuntoWebResponse(punto.clave(), punto.tipo(), punto.rubro(), punto.nombre(),
                punto.latitud(), punto.longitud(), fuente(punto.fuente()), punto.observadaEn());
    }

    private static DondeAbrirWebResponse.FuenteWebResponse fuente(DatosZonaDondeAbrir.Fuente fuente) {
        return new DondeAbrirWebResponse.FuenteWebResponse(fuente.codigo(), fuente.nombre(), fuente.tipo(),
                fuente.url(), fuente.licencia());
    }

    private static String id(ZonaId id) {
        return id == null ? null : id.toString();
    }
}
