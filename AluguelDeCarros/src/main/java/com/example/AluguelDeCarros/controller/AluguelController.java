package com.example.AluguelDeCarros.controller;

import com.example.AluguelDeCarros.dto.ApiDtos.*;
import com.example.AluguelDeCarros.service.AluguelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AluguelController {
    private final AluguelService service;
    public AluguelController(AluguelService service) { this.service=service; }
    @GetMapping("/automoveis") public List<AutomovelSaida> automoveis(Authentication a) { return service.automoveis(a.getName()); }
    @PostMapping("/pedidos") @ResponseStatus(HttpStatus.CREATED)
    public PedidoSaida criar(Authentication a,@Valid @RequestBody PedidoEntrada d) { return service.criar(a.getName(),d); }
    @GetMapping("/pedidos") public List<PedidoSaida> listar(Authentication a) { return service.listar(a.getName()); }
    @GetMapping("/pedidos/{id}") public PedidoSaida detalhe(Authentication a,@PathVariable Long id) { return service.detalhe(a.getName(),id); }
    @PutMapping("/pedidos/{id}") public PedidoSaida alterar(Authentication a,@PathVariable Long id,@Valid @RequestBody PedidoEntrada d) { return service.alterar(a.getName(),id,d); }
    @PostMapping("/pedidos/{id}/cancelamento") public PedidoSaida cancelar(Authentication a,@PathVariable Long id) { return service.cancelar(a.getName(),id); }
    @PostMapping("/pedidos/{id}/avaliacao") @ResponseStatus(HttpStatus.CREATED)
    public PedidoSaida avaliar(Authentication a,@PathVariable Long id,@Valid @RequestBody AvaliacaoEntrada d) { return service.avaliar(a.getName(),id,d); }
    @PostMapping("/pedidos/{id}/contrato") @ResponseStatus(HttpStatus.CREATED)
    public ContratoSaida contratar(Authentication a,@PathVariable Long id,@Valid @RequestBody ContratoEntrada d) { return service.contratar(a.getName(),id,d); }
    @GetMapping("/contratos/{id}") public ContratoSaida contrato(Authentication a,@PathVariable Long id) { return service.contrato(a.getName(),id); }
    @PostMapping("/contratos/{id}/credito") @ResponseStatus(HttpStatus.CREATED)
    public ContratoSaida credito(Authentication a,@PathVariable Long id,@Valid @RequestBody CreditoEntrada d) { return service.concederCredito(a.getName(),id,d); }
}
