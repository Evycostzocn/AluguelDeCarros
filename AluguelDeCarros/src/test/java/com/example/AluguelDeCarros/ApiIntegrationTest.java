package com.example.AluguelDeCarros;

import com.example.AluguelDeCarros.model.*;
import com.example.AluguelDeCarros.repository.*;
import com.example.AluguelDeCarros.service.*;
import com.example.AluguelDeCarros.dto.ApiDtos.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AgenteRepository agentes;
    @Autowired AutomovelRepository automoveis;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ClienteService clienteService;
    @Autowired AluguelService aluguelService;
    final JsonMapper json=JsonMapper.builder().build();
    String sufixo;
    String empresa;
    String banco;
    Long carro;

    @BeforeEach void preparar() {
        sufixo=UUID.randomUUID().toString().replace("-","").substring(0,12);
        empresa="empresa"+sufixo; banco="banco"+sufixo;
        Agente a=agentes.save(new Agente(empresa,encoder.encode("senha-teste-123"),"Empresa","11222333000181",TipoAgente.EMPRESA));
        agentes.save(new Agente(banco,encoder.encode("senha-teste-123"),"Banco","11444777000161",TipoAgente.BANCO));
        carro=automoveis.save(new Automovel("M"+sufixo,2024,"Marca","Modelo",sufixo.substring(0,7),null,a)).getId();
    }
    String cpf() {
        String base=String.format("%09d",ThreadLocalRandom.current().nextInt(100000000,999999999));
        for(int n=9;n<=10;n++) {
            int soma=0; for(int i=0;i<n;i++) soma+=(base.charAt(i)-'0')*(n+1-i);
            int d=11-soma%11; base+=(d>=10?0:d);
        }
        return base;
    }
    Map<String,Object> cadastro(String usuario,String cpf) {
        return Map.of("usuario",usuario,"senha","senha-teste-123","rg","12345","cpf",cpf,
            "nome","Cliente","endereco","Rua","profissao","Profissão");
    }
    Map<String,Object> pedido() { return Map.of("automovelId",carro,"dataInicio","2027-01-01","dataFim","2027-01-05"); }
    class Browser {
        MockHttpSession session;
        String token;
        Browser() throws Exception { csrf(); }
        void csrf() throws Exception {
            var req=get("/api/auth/csrf"); if(session!=null&&!session.isInvalid()) req.session(session).with(request -> {
                request.setRequestedSessionId(session.getId());
                request.setRequestedSessionIdValid(true);
                return request;
            });
            var result=mvc.perform(req).andReturn(); assertEquals(200,result.getResponse().getStatus());
            session=(MockHttpSession)result.getRequest().getSession(false);
            token=json.readTree(result.getResponse().getContentAsString()).get("token").asString();
        }
        MvcResult send(MockHttpServletRequestBuilder req,Object body,int status) throws Exception {
            if(session!=null&&!session.isInvalid()) req.session(session).with(request -> {
                request.setRequestedSessionId(session.getId());
                request.setRequestedSessionIdValid(true);
                return request;
            });
            req.header("X-CSRF-TOKEN",token);
            if(body!=null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
            MvcResult result=mvc.perform(req).andReturn();
            assertEquals(status,result.getResponse().getStatus(),result.getResponse().getContentAsString());
            return result;
        }
        JsonNode data(MockHttpServletRequestBuilder req,Object body,int status) throws Exception {
            return json.readTree(send(req,body,status).getResponse().getContentAsString());
        }
        void login(String nome) throws Exception {
            send(post("/api/auth/login"),Map.of("usuario",nome,"senha","senha-teste-123"),200); csrf();
        }
    }
    Browser cliente(String nome) throws Exception {
        Browser b=new Browser(); b.send(post("/api/clientes"),cadastro(nome,cpf()),201); b.login(nome); return b;
    }
    @Test void cadastroLoginSessaoCsrfLogoutEDesativacao() throws Exception {
        Browser b=new Browser(); String nome="c"+sufixo;
        mvc.perform(get("/api/auth/me")).andExpect(r -> assertEquals(401,r.getResponse().getStatus()));
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cadastro(nome,cpf()))))
            .andExpect(r -> assertEquals(403,r.getResponse().getStatus()));
        String documento=cpf(); var cad=cadastro(nome,documento);
        String idAntes=b.session.getId();
        JsonNode criado=b.data(post("/api/clientes"),cad,201);
        assertFalse(criado.has("senha")); assertFalse(criado.has("senhaHash"));
        var hash=usuarios.findByUsuario(nome).orElseThrow().getSenhaHash();
        assertNotEquals("senha-teste-123",hash); assertTrue(encoder.matches("senha-teste-123",hash));
        b.send(post("/api/clientes"),cad,409);
        b.send(post("/api/clientes"),cadastro(nome+"outro",documento),409);
        b.send(post("/api/auth/login"),Map.of("usuario",nome,"senha","errada"),401);
        b.login(nome); assertNotEquals(idAntes,b.session.getId());
        assertEquals("CLIENTE",b.data(get("/api/auth/me"),null,200).get("perfil").asString());
        b.send(post("/api/auth/logout"),null,204);
        b.send(get("/api/auth/me"),null,401);
        b.csrf(); b.login(nome);
        Browser outraSessao=new Browser(); outraSessao.login(nome);
        b.send(delete("/api/clientes/me"),null,204);
        outraSessao.send(get("/api/automoveis"),null,401);
        b.csrf(); b.send(post("/api/auth/login"),Map.of("usuario",nome,"senha","senha-teste-123"),401);
        assertFalse(usuarios.findByUsuario(nome).orElseThrow().isAtivo());
    }
    @Test void cadastroNaoAceitaPerfilCpfInvalidoOuCamposDeControle() throws Exception {
        Browser b=new Browser();
        var dados=new HashMap<>(cadastro("c"+sufixo,cpf())); dados.put("perfil","BANCO");
        b.send(post("/api/clientes"),dados,400);
        b.send(post("/api/clientes"),cadastro("c"+sufixo,"11111111111"),400);
        String valido=cpf(); String formatado=valido.substring(0,3)+"."+valido.substring(3,6)+"."+valido.substring(6,9)+"-"+valido.substring(9);
        assertEquals(valido,b.data(post("/api/clientes"),cadastro("c"+sufixo,formatado),201).get("cpf").asString());
        b.login("c"+sufixo);
        var pedido=new HashMap<>(pedido()); pedido.put("status","APROVADO");
        b.send(post("/api/pedidos"),pedido,400);
        pedido.remove("status"); pedido.put("clienteId",999);
        b.send(post("/api/pedidos"),pedido,400);
        pedido.remove("clienteId"); pedido.put("proprietarioId",999);
        b.send(post("/api/pedidos"),pedido,400);
        pedido.remove("proprietarioId"); pedido.put("dataFim","2026-12-01");
        b.send(post("/api/pedidos"),pedido,400);
        b.send(put("/api/automoveis/"+carro),Map.of("proprietarioClienteId",1),404);
    }
    @Test void rendaLimiteIsolamentoAtualizacaoERemocao() throws Exception {
        Browser a=cliente("a"+sufixo), b=cliente("b"+sufixo);
        Map<String,Object> renda=Map.of("entidadeEmpregadora","Empresa","rendimento",1000);
        long id=a.data(post("/api/clientes/me/vinculos-renda"),renda,201).get("id").asLong();
        a.send(post("/api/clientes/me/vinculos-renda"),renda,201);
        a.send(post("/api/clientes/me/vinculos-renda"),renda,201);
        a.send(post("/api/clientes/me/vinculos-renda"),renda,409);
        assertEquals(3,a.data(get("/api/clientes/me/vinculos-renda"),null,200).size());
        b.send(put("/api/clientes/me/vinculos-renda/"+id),renda,404);
        b.send(delete("/api/clientes/me/vinculos-renda/"+id),null,404);
        a.send(put("/api/clientes/me/vinculos-renda/"+id),Map.of("entidadeEmpregadora","Nova","rendimento",2000),200);
        a.send(delete("/api/clientes/me/vinculos-renda/"+id),null,204);
        assertEquals(0,jdbc.queryForObject("select count(*) from vinculos_renda where id=?",Integer.class,id));
        a.send(post("/api/clientes/me/vinculos-renda"),renda,201);
        b.send(post("/api/clientes/me/vinculos-renda"),Map.of("entidadeEmpregadora","Empresa","rendimento",0),400);
        Browser agente=new Browser(); agente.login(empresa); agente.send(get("/api/clientes/me"),null,403);
    }
    @Test void fluxoCompletoComAutorizacaoEContratos() throws Exception {
        Browser c=cliente("c"+sufixo), outro=cliente("outro"+sufixo);
        Browser e=new Browser(); e.login(empresa); Browser b=new Browser(); b.login(banco);
        c.send(get("/api/automoveis"),null,200);
        long id=c.data(post("/api/pedidos"),pedido(),201).get("id").asLong();
        outro.send(get("/api/pedidos/"+id),null,404);
        outro.send(put("/api/pedidos/"+id),pedido(),404);
        assertEquals(0,outro.data(get("/api/pedidos"),null,200).size());
        assertEquals(1,c.data(get("/api/pedidos"),null,200).size());
        e.send(get("/api/pedidos"),null,200);
        e.send(post("/api/pedidos"),pedido(),403);
        c.send(put("/api/pedidos/"+id),pedido(),200);
        e.send(put("/api/pedidos/"+id),pedido(),200);
        Map<String,Object> contrato=Map.of("dataCelebracao","2026-11-01","valor",1000);
        e.send(post("/api/pedidos/"+id+"/contrato"),contrato,409);
        c.send(post("/api/pedidos/"+id+"/avaliacao"),Map.of("parecer","OK","aprovada",true),403);
        assertEquals("APROVADO",e.data(post("/api/pedidos/"+id+"/avaliacao"),Map.of("parecer","OK","aprovada",true),201).get("status").asString());
        e.send(post("/api/pedidos/"+id+"/avaliacao"),Map.of("parecer","OK","aprovada",true),409);
        c.send(put("/api/pedidos/"+id),pedido(),409);
        long contratoId=e.data(post("/api/pedidos/"+id+"/contrato"),contrato,201).get("id").asLong();
        e.send(post("/api/pedidos/"+id+"/contrato"),contrato,409);
        assertEquals("CONTRATADO",c.data(get("/api/pedidos/"+id),null,200).get("status").asString());
        c.send(post("/api/pedidos/"+id+"/cancelamento"),null,409);
        outro.send(get("/api/contratos/"+contratoId),null,404);
        c.send(get("/api/contratos/"+contratoId),null,200);
        Map<String,Object> credito=Map.of("valor",500,"quantidadeParcelas",5);
        e.send(post("/api/contratos/"+contratoId+"/credito"),credito,403);
        c.send(post("/api/contratos/"+contratoId+"/credito"),credito,403);
        var resultado=b.data(post("/api/contratos/"+contratoId+"/credito"),credito,201);
        assertEquals(usuarios.findByUsuario(banco).orElseThrow().getId(),resultado.get("credito").get("bancoId").asLong());
        b.send(post("/api/contratos/"+contratoId+"/credito"),credito,409);
        assertEquals(usuarios.findByUsuario(empresa).orElseThrow().getId(),jdbc.queryForObject("select proprietario_agente_id from automoveis where id=?",Long.class,carro));
    }
    @Test void reprovacaoECancelamentosRespeitamTransicoes() throws Exception {
        Browser c=cliente("c"+sufixo); Browser e=new Browser();e.login(empresa);
        long id=c.data(post("/api/pedidos"),pedido(),201).get("id").asLong();
        assertEquals("REPROVADO",e.data(post("/api/pedidos/"+id+"/avaliacao"),Map.of("parecer","Renda insuficiente","aprovada",false),201).get("status").asString());
        c.send(post("/api/pedidos/"+id+"/cancelamento"),null,409);
        e.send(post("/api/pedidos/"+id+"/contrato"),Map.of("dataCelebracao","2026-11-01","valor",100),409);
        long novo=c.data(post("/api/pedidos"),pedido(),201).get("id").asLong();
        assertEquals("CANCELADO",c.data(post("/api/pedidos/"+novo+"/cancelamento"),null,200).get("status").asString());
        e.send(post("/api/pedidos/"+novo+"/avaliacao"),Map.of("parecer","OK","aprovada",true),409);
        long aprovado=c.data(post("/api/pedidos"),pedido(),201).get("id").asLong();
        e.send(post("/api/pedidos/"+aprovado+"/avaliacao"),Map.of("parecer","OK","aprovada",true),201);
        c.send(post("/api/pedidos/"+aprovado+"/cancelamento"),null,200);
    }
    @Test void migracoesRestricoesEConcorrencia() throws Exception {
        assertEquals(2,jdbc.queryForObject("select count(*) from flyway_schema_history where success",Integer.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
            "insert into automoveis(matricula,ano,marca,modelo,placa) values (?,2024,'M','M',?)","INVALID"+sufixo,"INV"+sufixo.substring(0,5)));
        String nome="c"+sufixo; cliente(nome);
        clienteService.adicionar(nome,new RendaEntrada("A",BigDecimal.ONE));
        clienteService.adicionar(nome,new RendaEntrada("B",BigDecimal.ONE));
        var resultados=concorrentes(() -> clienteService.adicionar(nome,new RendaEntrada("C",BigDecimal.ONE)));
        assertEquals(1,resultados.stream().filter(Boolean::booleanValue).count());
        assertEquals(3,clienteService.rendas(nome).size());
        long id=aluguelService.criar(nome,new PedidoEntrada(carro,java.time.LocalDate.of(2027,1,1),java.time.LocalDate.of(2027,1,5))).id();
        assertEquals(1,concorrentes(() -> aluguelService.avaliar(empresa,id,new AvaliacaoEntrada("OK",true))).stream().filter(Boolean::booleanValue).count());
        assertEquals(1,concorrentes(() -> aluguelService.contratar(empresa,id,new ContratoEntrada(java.time.LocalDate.now(),BigDecimal.TEN))).stream().filter(Boolean::booleanValue).count());
        long contratoId=aluguelService.detalhe(nome,id).contratoId();
        assertEquals(1,concorrentes(() -> aluguelService.concederCredito(banco,contratoId,new CreditoEntrada(BigDecimal.TEN,2))).stream().filter(Boolean::booleanValue).count());
    }
    List<Boolean> concorrentes(Callable<?> operacao) throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            CountDownLatch pronto=new CountDownLatch(2), iniciar=new CountDownLatch(1);
            Callable<Boolean> tarefa=() -> { pronto.countDown(); iniciar.await(); try { operacao.call();return true; }
                catch(IllegalStateException|ApiException ex) { return false; } };
            Future<Boolean> a=pool.submit(tarefa),b=pool.submit(tarefa);
            assertTrue(pronto.await(5,TimeUnit.SECONDS)); iniciar.countDown();
            return List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
        }
    }
}
