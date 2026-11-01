package com.example.AluguelDeCarros.service;

import com.example.AluguelDeCarros.dto.ApiDtos.*;
import com.example.AluguelDeCarros.model.*;
import com.example.AluguelDeCarros.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class AluguelService {
    private final IdentidadeService identidade;
    private final AutomovelRepository automoveis;
    private final PedidoAluguelRepository pedidos;
    private final AvaliacaoFinanceiraRepository avaliacoes;
    private final ContratoAluguelRepository contratos;
    private final ContratoCreditoRepository creditos;
    public AluguelService(IdentidadeService identidade, AutomovelRepository automoveis, PedidoAluguelRepository pedidos,
        AvaliacaoFinanceiraRepository avaliacoes, ContratoAluguelRepository contratos, ContratoCreditoRepository creditos) {
        this.identidade=identidade; this.automoveis=automoveis; this.pedidos=pedidos;
        this.avaliacoes=avaliacoes; this.contratos=contratos; this.creditos=creditos;
    }
    @Transactional(readOnly=true)
    public List<AutomovelSaida> automoveis(String nome) {
        identidade.usuario(nome); return automoveis.findAll().stream().map(this::automovelSaida).toList();
    }
    private Automovel automovel(Long id) { return automoveis.findById(id).orElseThrow(ApiException::naoEncontrado); }
    public PedidoSaida criar(String nome, PedidoEntrada d) {
        PedidoAluguel p = new PedidoAluguel(identidade.cliente(nome),automovel(d.automovelId()),d.dataInicio(),d.dataFim());
        return pedidoSaida(pedidos.saveAndFlush(p));
    }
    @Transactional(readOnly=true)
    public List<PedidoSaida> listar(String nome) {
        Usuario u=identidade.usuario(nome);
        var lista=u instanceof Agente ? pedidos.findAllByOrderByIdDesc() : pedidos.findByClienteIdOrderByIdDesc(u.getId());
        return lista.stream().map(this::pedidoSaida).toList();
    }
    private void autorizar(Usuario u, PedidoAluguel p) {
        if (!(u instanceof Agente) && !p.getCliente().getId().equals(u.getId())) throw ApiException.naoEncontrado();
    }
    private PedidoAluguel buscar(String nome, Long id, boolean bloquear) {
        Usuario u=identidade.usuario(nome);
        PedidoAluguel p=(bloquear ? pedidos.bloquear(id) : pedidos.findById(id)).orElseThrow(ApiException::naoEncontrado);
        autorizar(u,p); return p;
    }
    @Transactional(readOnly=true)
    public PedidoSaida detalhe(String nome, Long id) { return pedidoSaida(buscar(nome,id,false)); }
    public PedidoSaida alterar(String nome, Long id, PedidoEntrada d) {
        PedidoAluguel p=buscar(nome,id,true);
        p.alterar(automovel(d.automovelId()),d.dataInicio(),d.dataFim());
        return pedidoSaida(p);
    }
    public PedidoSaida cancelar(String nome, Long id) {
        identidade.cliente(nome);
        PedidoAluguel p=buscar(nome,id,true); p.cancelar(); return pedidoSaida(p);
    }
    public PedidoSaida avaliar(String nome, Long id, AvaliacaoEntrada d) {
        Agente a=identidade.agente(nome); PedidoAluguel p=buscar(nome,id,true);
        avaliacoes.saveAndFlush(new AvaliacaoFinanceira(p,a,d.parecer(),d.aprovada()));
        return pedidoSaida(p);
    }
    public ContratoSaida contratar(String nome, Long id, ContratoEntrada d) {
        Agente a=identidade.agente(nome); PedidoAluguel p=buscar(nome,id,true);
        return contratoSaida(contratos.saveAndFlush(new ContratoAluguel(p,a,d.dataCelebracao(),d.valor())));
    }
    @Transactional(readOnly=true)
    public ContratoSaida contrato(String nome, Long id) {
        ContratoAluguel c=contratos.findById(id).orElseThrow(ApiException::naoEncontrado);
        autorizar(identidade.usuario(nome),c.getPedido()); return contratoSaida(c);
    }
    public ContratoSaida concederCredito(String nome, Long id, CreditoEntrada d) {
        Agente banco=identidade.agente(nome);
        if (banco.getTipo()!=TipoAgente.BANCO) throw new ApiException(403,"Operação exclusiva de banco");
        ContratoAluguel c=contratos.bloquear(id).orElseThrow(ApiException::naoEncontrado);
        if (creditos.findByContratoId(id).isPresent()) throw new ApiException(409,"Contrato já possui crédito");
        creditos.saveAndFlush(new ContratoCredito(c,banco,d.valor(),d.quantidadeParcelas()));
        return contratoSaida(c);
    }
    private AutomovelSaida automovelSaida(Automovel a) {
        boolean cliente=a.getProprietarioCliente()!=null;
        return new AutomovelSaida(a.getId(),a.getMatricula(),a.getAno(),a.getMarca(),a.getModelo(),a.getPlaca(),
            cliente ? "CLIENTE" : "AGENTE", cliente ? a.getProprietarioCliente().getId() : a.getProprietarioAgente().getId());
    }
    private PedidoSaida pedidoSaida(PedidoAluguel p) {
        AvaliacaoSaida av=avaliacoes.findByPedidoId(p.getId()).map(a -> new AvaliacaoSaida(a.getId(),a.getAgente().getId(),a.getDataAvaliacao(),a.getParecer(),a.getAprovada())).orElse(null);
        Long contratoId=contratos.findByPedidoId(p.getId()).map(ContratoAluguel::getId).orElse(null);
        return new PedidoSaida(p.getId(),p.getCliente().getId(),automovelSaida(p.getAutomovel()),p.getDataCriacao(),p.getDataInicio(),p.getDataFim(),p.getStatus(),av,contratoId);
    }
    private ContratoSaida contratoSaida(ContratoAluguel c) {
        CreditoSaida cr=creditos.findByContratoId(c.getId()).map(v -> new CreditoSaida(v.getId(),v.getBanco().getId(),v.getValor(),v.getQuantidadeParcelas())).orElse(null);
        return new ContratoSaida(c.getId(),c.getPedido().getId(),c.getAgente().getId(),c.getDataCelebracao(),c.getValor(),cr);
    }
}
