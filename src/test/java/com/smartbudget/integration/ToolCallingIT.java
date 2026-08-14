package com.smartbudget.integration;

import com.smartbudget.application.dto.BalanceView;
import com.smartbudget.application.usecase.GetBalanceUseCase;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;
import com.smartbudget.domain.repository.TransactionRepository;
import com.smartbudget.infrastructure.ai.service.AssistantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@DisplayName("[IT] Tool Calling ponta a ponta")
class ToolCallingIT {
    @Autowired
    private AssistantService assistantService;

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private GetBalanceUseCase getBalance;

    @Test
    @Transactional
    @DisplayName("um comando em linguagem natural grava a transacao no banco")
    void naturalLanguageCommandPersistsTransaction() {
        String resposta = assistantService.reply("Registre uma despesa de 137 reais com farmacia");

        assertThat(resposta).isNotBlank();

        var gravadas = repository.findRecent(50);
        assertThat(gravadas)
                .as("a tool precisa ter gravado de verdade, nao apenas o modelo dizer que gravou")
                .anySatisfy(transaction -> {
                    assertThat(transaction.amount()).isEqualByComparingTo(new BigDecimal("137.00"));
                    assertThat(transaction.type()).isEqualTo(TransactionType.EXPENSE);
                    assertThat(transaction.category()).isEqualTo(TransactionCategory.HEALTH);
                });
    }

    @Test
    @Transactional
    @DisplayName("uma consulta de saldo reporta o valor calculado pela aplicacao")
    void balanceQueryReportsRealNumber() {
        assistantService.reply("Registre uma receita de 1000 reais de salario");

        BalanceView esperado = getBalance.execute(null);
        String resposta = assistantService.reply("Qual e o meu saldo?");

        String semSeparadores = resposta.replace(".", "").replace(",", "");
        String valorInteiro = esperado.netBalance().toBigInteger().toString();

        assertThat(semSeparadores)
                .as("o saldo citado deve ser o que a aplicacao calculou (%s)", esperado.netBalance())
                .contains(valorInteiro);
    }

    @Test
    @DisplayName("informacao incompleta gera pergunta, nunca um valor inventado")
    void incompleteInformationAsksInsteadOfGuessing() {
        long antes = repository.findRecent(200).size();

        String resposta = assistantService.reply("Registre uma despesa de Uber");

        assertThat(repository.findRecent(200))
                .as("nada pode ser gravado quando o valor nao foi informado")
                .hasSize((int) antes);

        List<String> pistas = List.of("valor", "quanto", "qual");
        assertThat(pistas)
                .as("a resposta deveria pedir o valor, mas veio: %s", resposta)
                .anySatisfy(pista -> assertThat(resposta.toLowerCase(Locale.ROOT)).contains(pista));
    }
}
