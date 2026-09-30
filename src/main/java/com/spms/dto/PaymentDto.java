package com.spms.dto;

import java.math.BigDecimal;

public record PaymentDto(
        Long id,
        BigDecimal amount,
        String paymentDate,
        String status,
        String transactionReference
) {
}
