package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TransferRequest implements Serializable {

    public static final String AMOUNT_CAN_NOT_BE_EMPTY = "amount can not be empty";

    @NotEmpty
    @JsonProperty("from")
    private String accountFromId;

    @NotEmpty
    @JsonProperty("to")
    private String accountToId;

    @NotNull
    @JsonProperty("amount")
    @Min(value = 0, message = "Initial balance must be positive.")
    private BigDecimal amountTransfer;
}
