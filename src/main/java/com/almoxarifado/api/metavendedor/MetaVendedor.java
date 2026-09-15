package com.almoxarifado.api.metavendedor;

import com.almoxarifado.api.meta.Meta;
import com.almoxarifado.api.vendedor.Vendedor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** O valor de uma meta atribuído a um vendedor específico: quanto ele precisa bater e quanto já bateu. */
@Entity
@Table(name = "metas_vendedor", uniqueConstraints = @UniqueConstraint(columnNames = { "vendedor_id", "meta_id" }))
public class MetaVendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull(message = "Informe o vendedor")
    @ManyToOne(optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Vendedor vendedor;

    @NotNull(message = "Informe a meta")
    @ManyToOne(optional = false)
    @JoinColumn(name = "meta_id", nullable = false)
    private Meta meta;

    @DecimalMin(value = "0", message = "A meta não pode ser negativa")
    @Column(name = "valor_meta", nullable = false)
    private double valorMeta;

    @DecimalMin(value = "0", message = "O realizado não pode ser negativo")
    @Column(name = "valor_realizado", nullable = false)
    private double valorRealizado;

    public MetaVendedor() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Vendedor getVendedor() {
        return vendedor;
    }

    public void setVendedor(Vendedor vendedor) {
        this.vendedor = vendedor;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public double getValorMeta() {
        return valorMeta;
    }

    public void setValorMeta(double valorMeta) {
        this.valorMeta = valorMeta;
    }

    public double getValorRealizado() {
        return valorRealizado;
    }

    public void setValorRealizado(double valorRealizado) {
        this.valorRealizado = valorRealizado;
    }
}
