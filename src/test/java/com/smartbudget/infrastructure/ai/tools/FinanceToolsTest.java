package com.smartbudget.infrastructure.ai.tools;

import com.smartbudget.application.support.InMemoryTransactionRepository;
import com.smartbudget.application.usecase.CreateTransactionUseCase;
import com.smartbudget.application.usecase.GetBalanceUseCase;
import com.smartbudget.application.usecase.GetExpensesByCategoryUseCase;
import com.smartbudget.application.usecase.GetExpensesByPeriodUseCase;
import com.smartbudget.application.usecase.GetLargestExpenseUseCase;
import com.smartbudget.application.usecase.GetMonthlySummaryUseCase;
import com.smartbudget.application.usecase.ListTransactionsUseCase;
import com.smartbudget.domain.exception.InvalidPeriodException;
import com.smartbudget.domain.exception.InvalidTransactionException;
import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FinanceTools - ponte entre o modelo e a aplicacao")
class FinanceToolsTest {
    private static final LocalDate TODAY = LocalDate.now();

    private InMemoryTransactionRepository repository;
    private FinanceTools tools;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        tools = new FinanceTools(
                new CreateTransactionUseCase(repository),
                new ListTransactionsUseCase(repository),
                new GetBalanceUseCase(repository),
                new GetExpensesByCategoryUseCase(repository),
                new GetExpensesByPeriodUseCase(repository),
                new GetMonthlySummaryUseCase(repository),
                new GetLargestExpenseUseCase(repository));
    }

    @Nested
    @DisplayName("createTransaction")
    class CreateTransaction {
        @Test
        @DisplayName("grava a transacao de verdade e devolve o registro persistido")
        void persistsForReal() {
            ToolResults.Transaction result = tools.createTransaction(
                    "Supermercado", new BigDecimal("120.00"), "EXPENSE", "FOOD", null);

            assertThat(result.amount()).isEqualTo("R$ 120,00");
            assertThat(result.category()).isEqualTo("Alimentação");
            assertThat(result.type()).isEqualTo("Despesa");
            assertThat(result.date()).isEqualTo(TODAY.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            List<Transaction> stored = repository.findRecent(10);
            assertThat(stored).hasSize(1);
            assertThat(stored.getFirst().description()).isEqualTo("Supermercado");
            assertThat(stored.getFirst().amount()).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("aceita tipo e categoria em minusculas, como o modelo costuma enviar")
        void acceptsLowerCaseEnums() {
            ToolResults.Transaction result = tools.createTransaction(
                    "Salario", new BigDecimal("5000"), "income", "salary", null);

            assertThat(result.type()).isEqualTo("Receita");
            assertThat(repository.findRecent(1).getFirst().type()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("respeita a data informada")
        void usesProvidedDate() {
            LocalDate yesterday = TODAY.minusDays(1);

            tools.createTransaction("Uber", new BigDecimal("35"), "EXPENSE", "TRANSPORT", yesterday.toString());

            assertThat(repository.findRecent(1).getFirst().occurredAt()).isEqualTo(yesterday);
        }

        @Test
        @DisplayName("valor invalido nao grava nada e produz mensagem que orienta o modelo")
        void invalidAmountPersistsNothing() {
            assertThatThrownBy(() -> tools.createTransaction(
                    "Uber", BigDecimal.ZERO, "EXPENSE", "TRANSPORT", null))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("maior que zero");

            assertThat(repository.findRecent(10)).isEmpty();
        }

        @Test
        @DisplayName("categoria desconhecida devolve a lista de categorias aceitas")
        void unknownCategoryGuidesTheModel() {
            assertThatThrownBy(() -> tools.createTransaction(
                    "Mercado", BigDecimal.TEN, "EXPENSE", "ALIMENTACAO", null))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("FOOD");
        }

        @Test
        @DisplayName("data em formato invalido explica o formato esperado")
        void invalidDateExplainsFormat() {
            assertThatThrownBy(() -> tools.createTransaction(
                    "Mercado", BigDecimal.TEN, "EXPENSE", "FOOD", "14/08/2026"))
                    .isInstanceOf(InvalidPeriodException.class)
                    .hasMessageContaining("AAAA-MM-DD");
        }
    }

    @Nested
    @DisplayName("consultas")
    class Queries {
        @BeforeEach
        void seed() {
            tools.createTransaction("Salario", new BigDecimal("5000"), "INCOME", "SALARY", null);
            tools.createTransaction("Supermercado", new BigDecimal("500"), "EXPENSE", "FOOD", null);
            tools.createTransaction("Restaurante", new BigDecimal("230"), "EXPENSE", "FOOD", null);
            tools.createTransaction("Uber", new BigDecimal("410"), "EXPENSE", "TRANSPORT", null);
        }

        @Test
        @DisplayName("getBalance devolve receitas, despesas e saldo formatados")
        void balance() {
            ToolResults.Balance balance = tools.getBalance(null, null);

            assertThat(balance.totalIncome()).isEqualTo("R$ 5.000,00");
            assertThat(balance.totalExpense()).isEqualTo("R$ 1.140,00");
            assertThat(balance.netBalance()).isEqualTo("R$ 3.860,00");
        }

        @Test
        @DisplayName("listTransactions devolve a contagem e os itens")
        void list() {
            ToolResults.TransactionList list = tools.listTransactions(null, null, null, null, null);

            assertThat(list.count()).isEqualTo(4);
            assertThat(list.transactions()).hasSize(4);
        }

        @Test
        @DisplayName("listTransactions filtra por categoria")
        void listFilteredByCategory() {
            ToolResults.TransactionList list = tools.listTransactions(null, null, "FOOD", null, null);

            assertThat(list.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("getExpensesByCategory sem categoria devolve a distribuicao ordenada")
        void categoryBreakdown() {
            List<ToolResults.CategoryExpense> breakdown = tools.getExpensesByCategory(null, null, null);

            assertThat(breakdown).extracting(ToolResults.CategoryExpense::category)
                    .containsExactly("Alimentação", "Transporte");
            assertThat(breakdown.getFirst().total()).isEqualTo("R$ 730,00");
        }

        @Test
        @DisplayName("getExpensesByCategory com categoria devolve apenas ela")
        void singleCategory() {
            List<ToolResults.CategoryExpense> result = tools.getExpensesByCategory("TRANSPORT", null, null);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().total()).isEqualTo("R$ 410,00");
        }

        @Test
        @DisplayName("getExpensesByPeriod soma o intervalo informado")
        void expensesByPeriod() {
            ToolResults.PeriodExpense today =
                    tools.getExpensesByPeriod(TODAY.toString(), TODAY.toString());

            assertThat(today.totalExpense()).isEqualTo("R$ 1.140,00");
            assertThat(today.expenseCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("getMonthlySummary devolve o resumo completo do mes atual")
        void monthlySummary() {
            ToolResults.MonthlySummary summary = tools.getMonthlySummary(null);

            assertThat(summary.month()).isEqualTo(YearMonth.now().toString());
            assertThat(summary.totalIncome()).isEqualTo("R$ 5.000,00");
            assertThat(summary.totalExpense()).isEqualTo("R$ 1.140,00");
            assertThat(summary.netBalance()).isEqualTo("R$ 3.860,00");
            assertThat(summary.transactionCount()).isEqualTo(4);
            assertThat(summary.largestExpense()).isNotNull();
            assertThat(summary.largestExpense().description()).isEqualTo("Supermercado");
            assertThat(summary.expensesByCategory()).hasSize(2);
        }

        @Test
        @DisplayName("getMonthlySummary de mes invalido explica o formato")
        void monthlySummaryInvalidMonth() {
            assertThatThrownBy(() -> tools.getMonthlySummary("agosto"))
                    .isInstanceOf(InvalidPeriodException.class)
                    .hasMessageContaining("AAAA-MM");
        }

        @Test
        @DisplayName("getLargestExpense devolve a maior despesa")
        void largestExpense() {
            List<ToolResults.Transaction> largest = tools.getLargestExpense(null, null);

            assertThat(largest).hasSize(1);
            assertThat(largest.getFirst().description()).isEqualTo("Supermercado");
            assertThat(largest.getFirst().amount()).isEqualTo("R$ 500,00");
        }
    }

    @Test
    @DisplayName("getLargestExpense devolve lista vazia quando nao ha despesas, sem inventar valor")
    void largestExpenseWhenEmpty() {
        assertThat(tools.getLargestExpense(null, null)).isEmpty();
    }

    @Test
    @DisplayName("todas as tools expostas tem descricao util para a selecao do modelo")
    void everyToolIsWellDescribed() {
        List<Method> annotated = Arrays.stream(FinanceTools.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .toList();

        assertThat(annotated).hasSize(7);
        assertThat(annotated).allSatisfy(method -> {
            Tool tool = method.getAnnotation(Tool.class);
            assertThat(tool.name()).isNotBlank();
            assertThat(tool.description()).hasSizeGreaterThan(40);
        });

        assertThat(annotated).extracting(method -> method.getAnnotation(Tool.class).name())
                .containsExactlyInAnyOrder(
                        "createTransaction", "getBalance", "listTransactions", "getExpensesByCategory",
                        "getExpensesByPeriod", "getMonthlySummary", "getLargestExpense");
    }

    @Test
    @DisplayName("categoria sem gastos devolve zero em vez de erro")
    void unusedCategoryReturnsZero() {
        List<ToolResults.CategoryExpense> health = tools.getExpensesByCategory("HEALTH", null, null);

        assertThat(health).hasSize(1);
        assertThat(health.getFirst().total()).isEqualTo("R$ 0,00");
    }

    @Test
    @DisplayName("tipo desconhecido na listagem orienta o modelo")
    void unknownTypeGuidesTheModel() {
        assertThatThrownBy(() -> tools.listTransactions(null, "SAIDA", null, null, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("EXPENSE");
    }

    @Test
    @DisplayName("categoria valida nao interfere no enum TransactionCategory")
    void categoryParsingStaysConsistent() {
        tools.createTransaction("Farmacia", new BigDecimal("45.90"), "EXPENSE", "health", null);

        assertThat(repository.findRecent(1).getFirst().category()).isEqualTo(TransactionCategory.HEALTH);
    }
}
