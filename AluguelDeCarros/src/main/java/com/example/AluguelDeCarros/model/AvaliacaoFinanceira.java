package com.example.AluguelDeCarros.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes_financeiras", schema = "public")
public class AvaliacaoFinanceira {
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

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDateTime dataAvaliacao;

    @Column(name = "parecer", nullable = false, length = 2000)
    private String parecer;

    @Column(name = "aprovada", nullable = false)
    private boolean aprovada;

    protected AvaliacaoFinanceira() { }

    public AvaliacaoFinanceira(PedidoAluguel pedido, Agente agente, String parecer, boolean aprovada) {
        pedido.avaliar(aprovada);
        this.pedido = pedido; this.agente = agente; this.parecer = parecer;
        this.aprovada = aprovada; this.dataAvaliacao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public PedidoAluguel getPedido() { return pedido; }
    public Agente getAgente() { return agente; }
    public LocalDateTime getDataAvaliacao() { return dataAvaliacao; }
    public String getParecer() { return parecer; }
    public boolean getAprovada() { return aprovada; }
}
