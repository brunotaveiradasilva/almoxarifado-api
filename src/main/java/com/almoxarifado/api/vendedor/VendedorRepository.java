package com.almoxarifado.api.vendedor;

import java.util.List;

import com.almoxarifado.api.fornecedor.Fornecedor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendedorRepository extends JpaRepository<Vendedor, String> {

    List<Vendedor> findByFornecedoresContaining(Fornecedor fornecedor);
}
