package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.useCase.plantilla.listarPlantillas.ListarPlantillas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlantillaUseCaseConfig {

    @Bean
    public ListarPlantillas listarPlantillas(PlantillaRepository plantillaRepository) {
        return new ListarPlantillas(plantillaRepository);
    }
}
