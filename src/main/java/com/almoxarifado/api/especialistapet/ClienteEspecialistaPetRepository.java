package com.almoxarifado.api.especialistapet;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteEspecialistaPetRepository extends JpaRepository<ClienteEspecialistaPet, String> {

    List<ClienteEspecialistaPet> findByMes(String mes);

    List<ClienteEspecialistaPet> findByMesOrderByRepresentanteAscNomeAsc(String mes);

    List<ClienteEspecialistaPet> findAllByOrderByRepresentanteAscNomeAsc();

    void deleteByMes(String mes);
}
