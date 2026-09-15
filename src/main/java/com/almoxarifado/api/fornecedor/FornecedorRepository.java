package com.almoxarifado.api.fornecedor;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository extends JpaRepository<Fornecedor, String> {

    Optional<Fornecedor> findByNomeIgnoreCase(String nome);
}
