package com.almoxarifado.api.tipometa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoMetaRepository extends JpaRepository<TipoMeta, String> {

    Optional<TipoMeta> findByNomeIgnoreCase(String nome);
}
