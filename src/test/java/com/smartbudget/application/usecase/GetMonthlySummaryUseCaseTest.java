package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.MonthlySummaryView;
import com.smartbudget.application.support.InMemoryTransactionRepository;
import com.smartbudget.domain.exception.InvalidPeriodException;
import com.smartbudget.domain.model.TransactionCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static com.smartbudget.application.support.TransactionFixtures.expense;
import static com.smartbudget.application.support.TransactionFixtures.income;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GetMonthlySummaryUseCase - Financial Insights")
class GetMonthlySummaryUseCaseTest {
    private static final LocalDate TODAY = LocalDate.now();
    private static final YearMonth CURRENT_MONTH = YearMonth.from(TODAY);
    private static final LocalDate FIRST_DAY = CURRENT_MONTH.atDay(1);

    private InMemoryTransactionRepository repository;
    private GetMonthlySummaryUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        useCase = new GetMonthlySummaryUseCase(repository);
    }

    @Test
    @DisplayName("consolida receitas, despesas, saldo, contagem, maior gasto e categorias")
    void buildsCompleteSummary() {
        repository.save(income("Salario", "5000.00", TransactionCategory.SALARY, FIRST_DAY));
        repository.save(expense("Supermercado", "500.00", TransactionCategory.FOOD, FIRST_DAY));
        repository.save(expense("Restaurante", "230.00", TransactionCategory.FOOD, TODAY));
        repository.save(expense("Uber", "410.00", TransactionCategory.TRANSPORT, TODAY));
        repository.save(expense("Cinema", "160.00", TransactionCategory.ENTERTAINMENT, TODAY));

        MonthlySummaryView summary = useCase.execute(CURRENT_MONTH);

        assertThat(summary.period()).isEqualTo(CURRENT_MONTH.toString());
        assertThat(summary.totalIncome()).isEqualByComparingTo("5000.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("1300.00");
        assertThat(summary.netBalance()).isEqualByComparingTo("3700.00");
        assertThat(summary.transactionCount()).isEqualTo(5);

        assertThat(summary.largestExpense()).isNotNull();
        assertThat(summary.largestExpense().description()).isEqualTo("Supermercado");
        assertThat(summary.largestExpense().amount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("agrupa despesas por categoria em ordem decrescente com percentual")
    void groupsCategoriesOrderedByTotal() {
        repository.save(expense("Supermercado", "500.00", TransactionCategory.FOOD, FIRST_DAY));
        repository.save(expense("Restaurante", "230.00", TransactionCategory.FOOD, TODAY));
        repository.save(expense("Uber", "410.00", TransactionCategory.TRANSPORT, TODAY));
        repository.save(expense("Cinema", "160.00", TransactionCategory.ENTERTAINMENT, TODAY));

        MonthlySummaryView summary = useCase.execute(CURRENT_MONTH);

        assertThat(summary.expensesByCategory())
                .extracting(view -> view.category().name())
                .containsExactly("FOOD", "TRANSPORT", "ENTERTAINMENT");

        assertThat(summary.expensesByCategory().getFirst().total()).isEqualByComparingTo("730.00");
        assertThat(summary.expensesByCategory().getFirst().transactionCount()).isEqualTo(2);

        assertThat(summary.expensesByCategory().getFirst().percentageOfExpenses())
                .isEqualByComparingTo("56.15");

        assertThat(summary.topCategory()).isNotNull();
        assertThat(summary.topCategory().category()).isEqualTo(TransactionCategory.FOOD);
    }

    @Test
    @DisplayName("percentuais das categorias somam aproximadamente cem por cento")
    void percentagesAddUp() {
        repository.save(expense("A", "100.00", TransactionCategory.FOOD, TODAY));
        repository.save(expense("B", "300.00", TransactionCategory.TRANSPORT, TODAY));

        MonthlySummaryView summary = useCase.execute(CURRENT_MONTH);

        assertThat(summary.expensesByCategory())
                .extracting(view -> view.percentageOfExpenses().toPlainString())
                .containsExactly("75.00", "25.00");
    }

    @Test
    @DisplayName("ignora transacoes de outros meses")
    void ignoresOtherMonths() {
        repository.save(expense("Mes passado", "999.00", TransactionCategory.OTHER,
                CURRENT_MONTH.minusMonths(1).atDay(15)));
        repository.save(expense("Deste mes", "100.00", TransactionCategory.FOOD, TODAY));

        MonthlySummaryView summary = useCase.execute(CURRENT_MONTH);

        assertThat(summary.totalExpense()).isEqualByComparingTo("100.00");
        assertThat(summary.transactionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("mes sem movimento devolve zeros e nenhuma maior despesa")
    void emptyMonth() {
        MonthlySummaryView summary = useCase.execute(CURRENT_MONTH);

        assertThat(summary.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("0.00");
        assertThat(summary.netBalance()).isEqualByComparingTo("0.00");
        assertThat(summary.transactionCount()).isZero();
        assertThat(summary.largestExpense()).isNull();
        assertThat(summary.topCategory()).isNull();
        assertThat(summary.expensesByCategory()).isEmpty();
    }

    @Test
    @DisplayName("mes nulo e rejeitado")
    void rejectsNullMonth() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(InvalidPeriodException.class);
    }
}
