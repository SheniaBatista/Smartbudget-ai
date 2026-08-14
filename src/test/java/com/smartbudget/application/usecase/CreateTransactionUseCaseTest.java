package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.CreateTransactionCommand;
import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.application.support.InMemoryTransactionRepository;
import com.smartbudget.domain.exception.InvalidTransactionException;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionId;
import com.smartbudget.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CreateTransactionUseCase")
class CreateTransactionUseCaseTest {
    private InMemoryTransactionRepository repository;
    private CreateTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        useCase = new CreateTransactionUseCase(repository);
    }

    @Test
    @DisplayName("persiste a transacao e devolve a visao com identificador e rotulos")
    void persistsTransaction() {
        TransactionView view = useCase.execute(new CreateTransactionCommand(
                "Supermercado",
                new BigDecimal("120.00"),
                TransactionType.EXPENSE,
                TransactionCategory.FOOD,
                LocalDate.now()));

        assertThat(view.id()).isNotNull();
        assertThat(view.amount()).isEqualByComparingTo("120.00");
        assertThat(view.categoryLabel()).isEqualTo("Alimentação");
        assertThat(view.typeLabel()).isEqualTo("Despesa");

        assertThat(repository.findById(TransactionId.of(view.id()))).isPresent();
    }

    @Test
    @DisplayName("registra receita corretamente")
    void persistsIncome() {
        TransactionView view = useCase.execute(new CreateTransactionCommand(
                "Salario", new BigDecimal("5000"), TransactionType.INCOME,
                TransactionCategory.SALARY, null));

        assertThat(view.type()).isEqualTo(TransactionType.INCOME);
        assertThat(view.occurredAt()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("nao persiste nada quando o valor e invalido")
    void doesNotPersistInvalidAmount() {
        assertThatThrownBy(() -> useCase.execute(new CreateTransactionCommand(
                "Uber", BigDecimal.ZERO, TransactionType.EXPENSE, TransactionCategory.TRANSPORT, null)))
                .isInstanceOf(InvalidTransactionException.class);

        assertThat(repository.findRecent(10)).isEmpty();
    }

    @Test
    @DisplayName("rejeita comando nulo")
    void rejectsNullCommand() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(InvalidTransactionException.class);
    }
}
