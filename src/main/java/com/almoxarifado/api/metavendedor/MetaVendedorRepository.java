package com.almoxarifado.api.metavendedor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaVendedorRepository extends JpaRepository<MetaVendedor, String> {

    List<MetaVendedor> findByVendedorId(String vendedorId);

    Optional<MetaVendedor> findByVendedorIdAndMetaId(String vendedorId, String metaId);
}
