package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.application.support.InMemoryTransactionRepository;
import com.smartbudget.domain.exception.TransactionNotFoundException;
import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.smartbudget.application.support.TransactionFixtures.expense;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Consulta por id e remocao de transacao")
class TransactionLifecycleUseCasesTest {
    private InMemoryTransactionRepository repository;
    private GetTransactionByIdUseCase getById;
    private DeleteTransactionUseCase delete;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        getById = new GetTransactionByIdUseCase(repository);
        delete = new DeleteTransactionUseCase(repository);
    }

    @Test
    @DisplayName("recupera uma transacao existente pelo identificador")
    void findsExisting() {
        Transaction saved = repository.save(
                expense("Farmacia", "45.90", TransactionCategory.HEALTH, LocalDate.now()));

        TransactionView view = getById.execute(saved.id());

        assertThat(view.id()).isEqualTo(saved.id().uuid());
        assertThat(view.description()).isEqualTo("Farmacia");
    }

    @Test
    @DisplayName("identificador inexistente resulta em erro de nao encontrado")
    void unknownIdFails() {
        assertThatThrownBy(() -> getById.execute(TransactionId.generate()))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    @DisplayName("remove uma transacao existente")
    void deletesExisting() {
        Transaction saved = repository.save(
                expense("Cinema", "35.00", TransactionCategory.ENTERTAINMENT, LocalDate.now()));

        delete.execute(saved.id());

        assertThat(repository.findById(saved.id())).isEmpty();
    }

    @Test
    @DisplayName("remover transacao inexistente resulta em erro de nao encontrado")
    void deleteUnknownFails() {
        assertThatThrownBy(() -> delete.execute(TransactionId.generate()))
                .isInstanceOf(TransactionNotFoundException.class);
    }
}
