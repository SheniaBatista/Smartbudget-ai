package com.smartbudget.application.usecase;

import com.smartbudget.application.dto.TransactionView;
import com.smartbudget.domain.model.Transaction;
import com.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListTransactionsUseCase {
    private final TransactionRepository repository;

    public ListTransactionsUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TransactionView> execute(ListTransactionsQuery query) {
        ListTransactionsQuery effective = query != null
                ? query
                : ListTransactionsQuery.recent(ListTransactionsQuery.DEFAULT_LIMIT);

        List<Transaction> transactions = hasNoFilter(effective)
                ? repository.findRecent(effective.limit())
                : repository.search(effective.range(), effective.type(), effective.category(), effective.limit());

        return transactions.stream().map(TransactionView::from).toList();
    }

    private boolean hasNoFilter(ListTransactionsQuery query) {
        return query.range() == null && query.type() == null && query.category() == null;
    }
}
