package com.example.AluguelDeCarros.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contratos_credito", schema = "public")
public class ContratoCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_aluguel_id", nullable = false, unique = true)
    private ContratoAluguel contrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banco_id", nullable = false)
    private Agente banco;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "quantidade_parcelas", nullable = false)
    private Integer quantidadeParcelas;

    protected ContratoCredito() { }

    public ContratoCredito(ContratoAluguel contrato, Agente banco, BigDecimal valor, Integer parcelas) {
        if (banco.getTipo() != TipoAgente.BANCO) throw new IllegalArgumentException("Somente banco pode conceder crédito");
        if (valor == null || valor.signum() <= 0 || parcelas == null || parcelas <= 0)
            throw new IllegalArgumentException("Valor e parcelas devem ser positivos");
        this.contrato = contrato; this.banco = banco; this.valor = valor; this.quantidadeParcelas = parcelas;
    }

    public Long getId() { return id; }
    public ContratoAluguel getContrato() { return contrato; }
    public Agente getBanco() { return banco; }
    public BigDecimal getValor() { return valor; }
    public Integer getQuantidadeParcelas() { return quantidadeParcelas; }
}
