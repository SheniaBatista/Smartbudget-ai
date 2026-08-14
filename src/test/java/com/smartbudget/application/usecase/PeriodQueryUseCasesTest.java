package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.PeriodExpenseView;
import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.application.support.InMemoryTransactionRepository;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.TransactionCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static com.smartbudget.application.support.TransactionFixtures.expense;
import static com.smartbudget.application.support.TransactionFixtures.income;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Consultas por periodo e maior despesa")
class PeriodQueryUseCasesTest {
    private static final LocalDate TODAY = LocalDate.now();

    private InMemoryTransactionRepository repository;
    private GetExpensesByPeriodUseCase byPeriod;
    private GetLargestExpenseUseCase largest;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        byPeriod = new GetExpensesByPeriodUseCase(repository);
        largest = new GetLargestExpenseUseCase(repository);
    }

    @Test
    @DisplayName("soma apenas as despesas do dia consultado")
    void sumsExpensesOfTheDay() {
        repository.save(expense("Almoco", "38.00", TransactionCategory.FOOD, TODAY));
        repository.save(expense("Uber", "22.50", TransactionCategory.TRANSPORT, TODAY));
        repository.save(expense("Ontem", "999.00", TransactionCategory.OTHER, TODAY.minusDays(1)));
        repository.save(income("Salario", "5000.00", TransactionCategory.SALARY, TODAY));

        PeriodExpenseView today = byPeriod.execute(DateRange.ofDay(TODAY));

        assertThat(today.totalExpense()).isEqualByComparingTo("60.50");
        assertThat(today.transactionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("periodo sem despesas devolve zero")
    void emptyPeriodReturnsZero() {
        PeriodExpenseView result = byPeriod.execute(DateRange.ofDay(TODAY));

        assertThat(result.totalExpense()).isEqualByComparingTo("0.00");
        assertThat(result.transactionCount()).isZero();
    }

    @Test
    @DisplayName("encontra a maior despesa do periodo, ignorando receitas maiores")
    void findsLargestExpense() {
        repository.save(expense("Mercado", "120.00", TransactionCategory.FOOD, TODAY));
        repository.save(expense("Notebook", "4500.00", TransactionCategory.SHOPPING, TODAY));
        repository.save(income("Salario", "9000.00", TransactionCategory.SALARY, TODAY));

        Optional<TransactionView> result = largest.execute(null);

        assertThat(result).isPresent();
        assertThat(result.get().description()).isEqualTo("Notebook");
        assertThat(result.get().amount()).isEqualByComparingTo("4500.00");
    }

    @Test
    @DisplayName("sem despesas devolve vazio em vez de inventar um valor")
    void noExpensesReturnsEmpty() {
        repository.save(income("Salario", "5000.00", TransactionCategory.SALARY, TODAY));

        assertThat(largest.execute(null)).isEmpty();
    }
}
