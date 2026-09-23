package com.almoxarifado.api.metarepresentante;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TotalVendidoMensalRepository extends JpaRepository<TotalVendidoMensal, String> {

    Optional<TotalVendidoMensal> findByRepresentanteIdAndMes(String representanteId, String mes);
}
