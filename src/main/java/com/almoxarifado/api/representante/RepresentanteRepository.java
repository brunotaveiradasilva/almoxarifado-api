package com.almoxarifado.api.representante;

import java.util.List;

import com.almoxarifado.api.fornecedor.Fornecedor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepresentanteRepository extends JpaRepository<Representante, String> {

    List<Representante> findByFornecedoresContaining(Fornecedor fornecedor);
}
