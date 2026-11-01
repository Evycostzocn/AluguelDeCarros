package com.example.AluguelDeCarros.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contratos_aluguel", schema = "public")
public class ContratoAluguel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private PedidoAluguel pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false)
    private Agente agente;

    @Column(name = "data_celebracao", nullable = false)
    private LocalDate dataCelebracao;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    protected ContratoAluguel() { }

    public ContratoAluguel(PedidoAluguel pedido, Agente agente, LocalDate data, BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException("Valor deve ser positivo");
        pedido.contratar();
        this.pedido = pedido; this.agente = agente;
        this.dataCelebracao = java.util.Objects.requireNonNull(data); this.valor = valor;
    }

    public Long getId() { return id; }
    public PedidoAluguel getPedido() { return pedido; }
    public Agente getAgente() { return agente; }
    public LocalDate getDataCelebracao() { return dataCelebracao; }
    public BigDecimal getValor() { return valor; }
}
