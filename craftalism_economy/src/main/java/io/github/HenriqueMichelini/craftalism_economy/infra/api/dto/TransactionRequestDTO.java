package io.github.HenriqueMichelini.craftalism_economy.infra.api.dto;

import java.util.UUID;

public record TransactionRequestDTO(UUID fromUuid, UUID toUuid, Long amount) {}