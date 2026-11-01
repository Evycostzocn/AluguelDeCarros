package com.example.AluguelDeCarros.service;

import com.example.AluguelDeCarros.dto.ApiDtos.*;
import com.example.AluguelDeCarros.model.*;
import com.example.AluguelDeCarros.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Transactional
public class ClienteService {
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final VinculoRendaRepository vinculos;
    private final IdentidadeService identidade;
    private final PasswordEncoder encoder;
    public ClienteService(ClienteRepository clientes, UsuarioRepository usuarios, IdentidadeService identidade, PasswordEncoder encoder, VinculoRendaRepository vinculos) {
        this.clientes=clientes; this.usuarios=usuarios; this.identidade=identidade; this.encoder=encoder; this.vinculos=vinculos;
    }
    public ClienteSaida cadastrar(Cadastro d) {
        if (d.senha().getBytes(StandardCharsets.UTF_8).length > 72) throw new ApiException(400,"Senha excede 72 bytes");
        if (usuarios.existsByUsuario(d.usuario())) throw new ApiException(409,"Usuário ou CPF já cadastrado");
        Cliente c = new Cliente(d.usuario(), encoder.encode(d.senha()), d.rg(), Cpf.normalizar(d.cpf()), d.nome(), d.endereco(), d.profissao());
        return saida(clientes.saveAndFlush(c));
    }
    @Transactional(readOnly=true)
    public ClienteSaida consultar(String nome) { return saida(identidade.cliente(nome)); }
    private Cliente bloquear(String nome) {
        identidade.cliente(nome);
        return clientes.bloquearPorUsuario(nome).orElseThrow(ApiException::naoEncontrado);
    }
    public ClienteSaida atualizar(String nome, DadosCliente d) {
        Cliente c=bloquear(nome); String cpf=Cpf.normalizar(d.cpf());
        if (clientes.existsByCpfAndIdNot(cpf,c.getId())) throw new ApiException(409,"Usuário ou CPF já cadastrado");
        c.atualizar(d.rg(),cpf,d.nome(),d.endereco(),d.profissao());
        clientes.flush(); return saida(c);
    }
    public void desativar(String nome) { bloquear(nome).desativar(); }
    @Transactional(readOnly=true)
    public List<RendaSaida> rendas(String nome) { return consultar(nome).vinculosRenda(); }
    public RendaSaida adicionar(String nome, RendaEntrada d) {
        Cliente c=bloquear(nome);
        VinculoRenda v=new VinculoRenda(d.entidadeEmpregadora(),d.rendimento());
        c.adicionarVinculoRenda(v);
        return renda(vinculos.saveAndFlush(v));
    }
    public RendaSaida atualizarRenda(String nome, Long id, RendaEntrada d) {
        Cliente c=bloquear(nome); VinculoRenda v=buscarRenda(c,id);
        v.atualizar(d.entidadeEmpregadora(),d.rendimento());
        clientes.flush(); return renda(v);
    }
    public void removerRenda(String nome, Long id) {
        Cliente c=bloquear(nome); c.removerVinculoRenda(buscarRenda(c,id));
    }
    private VinculoRenda buscarRenda(Cliente c, Long id) {
        return c.getVinculosRenda().stream().filter(v -> v.getId().equals(id)).findFirst().orElseThrow(ApiException::naoEncontrado);
    }
    private static RendaSaida renda(VinculoRenda v) { return new RendaSaida(v.getId(),v.getEntidadeEmpregadora(),v.getRendimento()); }
    private static ClienteSaida saida(Cliente c) {
        return new ClienteSaida(c.getId(),c.getUsuario(),c.getRg(),c.getCpf(),c.getNome(),c.getEndereco(),c.getProfissao(),c.isAtivo(),
            c.getVinculosRenda().stream().map(ClienteService::renda).toList());
    }
}
