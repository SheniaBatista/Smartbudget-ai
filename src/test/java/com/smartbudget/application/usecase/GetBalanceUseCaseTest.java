package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.BalanceView;
import com.smartbudget.application.support.InMemoryTransactionRepository;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.TransactionCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.smartbudget.application.support.TransactionFixtures.expense;
import static com.smartbudget.application.support.TransactionFixtures.income;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetBalanceUseCase")
class GetBalanceUseCaseTest {
    private static final LocalDate TODAY = LocalDate.now();

    private InMemoryTransactionRepository repository;
    private GetBalanceUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        useCase = new GetBalanceUseCase(repository);
    }

    @Test
    @DisplayName("saldo e receitas menos despesas em toda a base")
    void computesOverallBalance() {
        repository.save(income("Salario", "5000.00", TransactionCategory.SALARY, TODAY));
        repository.save(expense("Mercado", "730.00", TransactionCategory.FOOD, TODAY));
        repository.save(expense("Uber", "410.00", TransactionCategory.TRANSPORT, TODAY));

        BalanceView balance = useCase.execute(null);

        assertThat(balance.totalIncome()).isEqualByComparingTo("5000.00");
        assertThat(balance.totalExpense()).isEqualByComparingTo("1140.00");
        assertThat(balance.netBalance()).isEqualByComparingTo("3860.00");
    }

    @Test
    @DisplayName("respeita o periodo informado")
    void respectsPeriod() {
        repository.save(expense("Antigo", "100.00", TransactionCategory.OTHER, TODAY.minusDays(30)));
        repository.save(expense("Recente", "50.00", TransactionCategory.OTHER, TODAY));

        BalanceView balance = useCase.execute(DateRange.of(TODAY.minusDays(2), TODAY));

        assertThat(balance.totalExpense()).isEqualByComparingTo("50.00");
        assertThat(balance.netBalance()).isEqualByComparingTo("-50.00");
    }

    @Test
    @DisplayName("base vazia devolve zeros em vez de nulo")
    void emptyRepositoryReturnsZeros() {
        BalanceView balance = useCase.execute(null);

        assertThat(balance.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(balance.totalExpense()).isEqualByComparingTo("0.00");
        assertThat(balance.netBalance()).isEqualByComparingTo("0.00");
    }
}
