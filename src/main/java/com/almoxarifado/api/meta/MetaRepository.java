package com.almoxarifado.api.meta;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaRepository extends JpaRepository<Meta, String> {

    boolean existsByFornecedorId(String fornecedorId);
}
