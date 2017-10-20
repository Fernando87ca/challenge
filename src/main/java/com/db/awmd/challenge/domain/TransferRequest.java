package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class TransferRequest implements Serializable {

    public static final String AMOUNT_CAN_NOT_BE_EMPTY = "Initial balance must be positive";

    /**
     * Origin Account ID
     */
    @NotEmpty
    @JsonProperty("from")
    private String accountFromId;

    /**
     * Destination Account ID
     */
    @NotEmpty
    @JsonProperty("to")
    private String accountToId;

    /**
     * Amount transfer
     */
    @NotNull
    @JsonProperty("amount")
    @Min(value = 0, message = AMOUNT_CAN_NOT_BE_EMPTY)
    private BigDecimal amountTransfer;
}
