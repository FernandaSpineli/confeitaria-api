package com.confeitaria.demo;

import com.confeitaria.auth.api.AuthDtos.RegisterRequest;
import com.confeitaria.auth.application.AuthService;
import com.confeitaria.configuracao.api.ConfiguracaoDtos.ConfiguracaoRequest;
import com.confeitaria.configuracao.application.ConfiguracaoService;
import com.confeitaria.configuracao.persistence.Bandeira;
import com.confeitaria.configuracao.persistence.CriterioRateio;
import com.confeitaria.embalagem.api.EmbalagemDtos.EmbalagemRequest;
import com.confeitaria.embalagem.application.EmbalagemService;
import com.confeitaria.estoque.api.EstoqueDtos.CompraRequest;
import com.confeitaria.estoque.application.EstoqueService;
import com.confeitaria.insumo.api.InsumoDtos.InsumoRequest;
import com.confeitaria.insumo.application.InsumoService;
import com.confeitaria.receita.api.ReceitaDtos.ItemInsumoRequest;
import com.confeitaria.receita.api.ReceitaDtos.ReceitaRequest;
import com.confeitaria.receita.api.ReceitaDtos.TamanhoRequest;
import com.confeitaria.receita.application.ReceitaService;
import com.confeitaria.shared.domain.BaseQuantidade;
import com.confeitaria.shared.domain.UnidadeMedida;
import com.confeitaria.usuario.persistence.UsuarioRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conta de demonstração com a torta holandesa em três tamanhos. Só roda com {@code app.demo.enabled=true}
 * e só cria a conta se o e-mail ainda não existir.
 *
 * <pre>
 * receita inteira: produtos R$ 55,00 + trabalho R$ 10,00 (1 h a R$ 10/h), margem 40%
 * Grande  = 1 receita,   1 torta, caixa R$ 1,00  →  custo R$ 66,00  →  preço R$ 110,00
 * Pequena = ½ receita,   1 torta, caixa R$ 1,00  →  custo R$ 33,50  →  preço R$  55,83
 * Pote    = 1 receita,   8 potes, pote  R$ 1,00  →  custo R$  9,125 →  preço R$  15,21 cada
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class ContaDemonstracao implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContaDemonstracao.class);

    private final UsuarioRepository usuarios;
    private final AuthService auth;
    private final ConfiguracaoService configuracao;
    private final InsumoService insumos;
    private final EmbalagemService embalagens;
    private final ReceitaService receitas;
    private final EstoqueService estoque;
    private final String email;
    private final String senha;

    public ContaDemonstracao(UsuarioRepository usuarios, AuthService auth, ConfiguracaoService configuracao,
            InsumoService insumos, EmbalagemService embalagens, ReceitaService receitas, EstoqueService estoque,
            @Value("${app.demo.email:demo@exemplo.com}") String email,
            @Value("${app.demo.password:senha12345}") String senha) {
        this.usuarios = usuarios;
        this.auth = auth;
        this.configuracao = configuracao;
        this.insumos = insumos;
        this.embalagens = embalagens;
        this.receitas = receitas;
        this.estoque = estoque;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (criar(email, senha)) {
            log.info("Conta de demonstração criada: {}", email);
        }
    }

    /** Cria a conta completa. Devolve false (sem mudar nada) se o e-mail já existir. */
    public boolean criar(String emailConta, String senhaConta) {
        if (usuarios.existsByEmail(emailConta.toLowerCase())) {
            return false;
        }
        UUID dona = auth.registrar(new RegisterRequest("Clara Demonstração", emailConta, senhaConta)).id();

        // R$ 1.600 por 160 h = R$ 10 por hora; sem gás, sem custos fixos, sem canais.
        configuracao.atualizar(dona, new ConfiguracaoRequest(new BigDecimal("1600"), new BigDecimal("160"),
                Bandeira.VERDE, null, null, CriterioRateio.HORAS, false));

        // Produtos da receita inteira: 5 + 6 + 7 + 8 + 13 + 16 = R$ 55,00.
        List<ItemInsumoRequest> itens = new ArrayList<>();
        itens.add(insumo(dona, "Biscoito maisena", "200", "5.00", "200", "200", "400", "10.00"));
        itens.add(insumo(dona, "Manteiga sem sal", "200", "12.00", "100", "100", "200", "12.00"));
        itens.add(insumo(dona, "Leite condensado", "395", "7.00", "395", "395", "790", "14.00"));
        itens.add(insumo(dona, "Creme de leite", "200", "4.00", "400", "400", "800", "16.00"));
        itens.add(insumo(dona, "Chocolate meio amargo", "1000", "52.00", "250", "250", "500", "26.00"));
        itens.add(insumo(dona, "Biscoito Calipso", "130", "8.00", "260", "260", "520", "32.00"));

        UUID caixaGrande = embalagem(dona, "Caixa para torta grande", "10");
        UUID caixaPequena = embalagem(dona, "Caixa para torta pequena", "10");
        UUID pote = embalagem(dona, "Pote de 250 ml com tampa", "24");

        receitas.criar(dona, new ReceitaRequest("Torta holandesa", 1, 60, 0, new BigDecimal("40"), null, itens,
                List.of(), List.of(), List.of(
                        new TamanhoRequest("Torta grande", BigDecimal.ONE, 1, caixaGrande, BigDecimal.ONE),
                        new TamanhoRequest("Torta pequena", new BigDecimal("0.5"), 1, caixaPequena, BigDecimal.ONE),
                        new TamanhoRequest("Pote individual", BigDecimal.ONE, 8, pote, BigDecimal.ONE))));
        return true;
    }

    /** Cadastra o insumo (em gramas), compra estoque pelo mesmo preço e devolve o item da receita. */
    private ItemInsumoRequest insumo(UUID dona, String nome, String gramasEmbalagem, String precoEmbalagem,
            String minimo, String usoNaReceita, String gramasCompradas, String valorPago) {
        UUID id = insumos.criar(dona, new InsumoRequest(nome, UnidadeMedida.G, new BigDecimal(gramasEmbalagem),
                new BigDecimal(precoEmbalagem), new BigDecimal(minimo))).id();
        estoque.registrarCompra(dona, new CompraRequest(id, null, new BigDecimal(gramasCompradas), UnidadeMedida.G,
                new BigDecimal(valorPago), "Estoque inicial da demonstração"));
        return new ItemInsumoRequest(id, new BigDecimal(usoNaReceita), UnidadeMedida.G, BaseQuantidade.POR_LOTE);
    }

    /** Embalagem de R$ 1,00 a unidade, com estoque inicial. */
    private UUID embalagem(UUID dona, String nome, String quantidade) {
        UUID id = embalagens.criar(dona, new EmbalagemRequest(nome, BigDecimal.ONE, new BigDecimal("2"))).id();
        estoque.registrarCompra(dona, new CompraRequest(null, id, new BigDecimal(quantidade), UnidadeMedida.UN,
                new BigDecimal(quantidade), "Estoque inicial da demonstração"));
        return id;
    }
}
