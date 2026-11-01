package com.example.AluguelDeCarros.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos_aluguel", schema = "public")
public class PedidoAluguel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "automovel_id", nullable = false)
    private Automovel automovel;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPedido status;

    protected PedidoAluguel() { }

    public PedidoAluguel(Cliente cliente, Automovel automovel, LocalDate inicio, LocalDate fim) {
        this.cliente = cliente; this.status = StatusPedido.EM_ANALISE;
        this.dataCriacao = LocalDateTime.now();
        alterar(automovel, inicio, fim);
    }

    public void alterar(Automovel automovel, LocalDate inicio, LocalDate fim) {
        exigir(StatusPedido.EM_ANALISE);
        if (inicio == null || fim == null || !fim.isAfter(inicio))
            throw new IllegalArgumentException("Data final deve ser posterior à inicial");
        this.automovel = java.util.Objects.requireNonNull(automovel);
        this.dataInicio = inicio; this.dataFim = fim;
    }
    public void cancelar() {
        if (status != StatusPedido.EM_ANALISE && status != StatusPedido.APROVADO)
            throw new IllegalStateException("Pedido não pode ser cancelado neste status");
        status = StatusPedido.CANCELADO;
    }
    public void avaliar(boolean aprovada) {
        exigir(StatusPedido.EM_ANALISE);
        status = aprovada ? StatusPedido.APROVADO : StatusPedido.REPROVADO;
    }
    public void contratar() { exigir(StatusPedido.APROVADO); status = StatusPedido.CONTRATADO; }
    private void exigir(StatusPedido esperado) {
        if (status != esperado) throw new IllegalStateException("Operação incompatível com o status do pedido");
    }

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Automovel getAutomovel() { return automovel; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public StatusPedido getStatus() { return status; }
}
