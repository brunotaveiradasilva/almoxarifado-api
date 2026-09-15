package com.almoxarifado.api.vendedor;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendedorRepository extends JpaRepository<Vendedor, String> {

    Optional<Vendedor> findByCodigoIgnoreCase(String codigo);
}
