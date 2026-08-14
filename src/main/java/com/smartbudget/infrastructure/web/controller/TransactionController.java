package com.smartbudget.infrastructure.web.controller;

import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.application.usecase.CreateTransactionUseCase;
import com.smartbudget.application.usecase.DeleteTransactionUseCase;
import com.smartbudget.application.usecase.GetTransactionByIdUseCase;
import com.smartbudget.application.usecase.ListTransactionsQuery;
import com.smartbudget.application.usecase.ListTransactionsUseCase;
import com.smartbudget.domain.model.DateRange;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionId;
import com.smartbudget.domain.model.TransactionType;
import com.smartbudget.infrastructure.web.request.CreateTransactionRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final CreateTransactionUseCase createTransaction;
    private final ListTransactionsUseCase listTransactions;
    private final GetTransactionByIdUseCase getTransactionById;
    private final DeleteTransactionUseCase deleteTransaction;

    public TransactionController(CreateTransactionUseCase createTransaction,
                                 ListTransactionsUseCase listTransactions,
                                 GetTransactionByIdUseCase getTransactionById,
                                 DeleteTransactionUseCase deleteTransaction) {
        this.createTransaction = createTransaction;
        this.listTransactions = listTransactions;
        this.getTransactionById = getTransactionById;
        this.deleteTransaction = deleteTransaction;
    }

    @PostMapping
    public ResponseEntity<TransactionView> create(@Valid @RequestBody CreateTransactionRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        TransactionView created = createTransaction.execute(request.toCommand());
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/transactions/{id}").build(created.id()))
                .body(created);
    }

    @GetMapping
    public List<TransactionView> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(defaultValue = "20") int limit) {
        DateRange range = (from != null || to != null)
                ? DateRange.of(from != null ? from : LocalDate.of(1970, 1, 1),
                               to != null ? to : LocalDate.now())
                : null;

        return listTransactions.execute(new ListTransactionsQuery(range, type, category, limit));
    }

    @GetMapping("/{id}")
    public TransactionView getById(@PathVariable UUID id) {
        return getTransactionById.execute(TransactionId.of(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteTransaction.execute(TransactionId.of(id));
        return ResponseEntity.noContent().build();
    }
}
