package com.almoxarifado.api.agendamento;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Uma retirada agendada de um material (mesmo formato do types.ts do front-end).
 * Não há relação de chave estrangeira com Material de propósito: o front-end permite
 * excluir um material mantendo os agendamentos que já existiam com ele.
 */
@Entity
@Table(name = "agendamentos")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Informe o material")
    @Column(name = "material_id", nullable = false)
    private String materialId;

    @Min(value = 1, message = "A quantidade deve ser maior que zero")
    @Column(nullable = false)
    private int qtd;

    @NotBlank(message = "Informe o responsável")
    @Column(nullable = false)
    private String responsavel;

    /** Cliente para quem o material vai. Vazio em retiradas de uso interno. */
    @Column
    private String cliente;

    @NotNull(message = "Informe a data de retirada")
    @Column(nullable = false)
    private LocalDate retirada;

    @NotNull(message = "Informe a data de devolução")
    @Column(nullable = false)
    private LocalDate devolucao;

    @NotBlank(message = "Informe o status")
    @Pattern(regexp = "agendado|retirado|devolvido", message = "Status deve ser agendado, retirado ou devolvido")
    @Column(nullable = false)
    private String status;

    @Column(length = 2000)
    private String obs;

    public Agendamento() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public int getQtd() {
        return qtd;
    }

    public void setQtd(int qtd) {
        this.qtd = qtd;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public LocalDate getRetirada() {
        return retirada;
    }

    public void setRetirada(LocalDate retirada) {
        this.retirada = retirada;
    }

    public LocalDate getDevolucao() {
        return devolucao;
    }

    public void setDevolucao(LocalDate devolucao) {
        this.devolucao = devolucao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObs() {
        return obs;
    }

    public void setObs(String obs) {
        this.obs = obs;
    }
}
