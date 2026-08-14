package com.smartbudget.infrastructure.web.request;

import com.smartbudget.application.dto.CreateTransactionCommand;
import com.smartbudget.domain.model.TransactionCategory;
import com.smartbudget.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(

        @NotBlank(message = "A descricao e obrigatoria.")
        @Size(max = 255, message = "A descricao nao pode ultrapassar 255 caracteres.")
        String description,

        @NotNull(message = "O valor e obrigatorio.")
        @DecimalMin(value = "0.01", message = "O valor da transacao deve ser maior que zero.")
        @DecimalMax(value = "999999999.99", message = "O valor informado excede o limite suportado.")
        BigDecimal amount,

        @NotNull(message = "O tipo e obrigatorio (INCOME ou EXPENSE).")
        TransactionType type,

        @NotNull(message = "A categoria e obrigatoria.")
        TransactionCategory category,

        @PastOrPresent(message = "A data da transacao nao pode estar no futuro.")
        LocalDate occurredAt) {
    public CreateTransactionCommand toCommand() {
        return new CreateTransactionCommand(description, amount, type, category, occurredAt);
    }
}
