package com.almoxarifado.api.metarepresentante;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaRepresentanteRepository extends JpaRepository<MetaRepresentante, String> {

    List<MetaRepresentante> findByRepresentanteId(String representanteId);

    Optional<MetaRepresentante> findByRepresentanteIdAndMetaId(String representanteId, String metaId);
}
