package com.db.awmd.challenge.domain;

import com.db.awmd.challenge.enums.Status;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class Transfer {

    /**
     * Id for specific transfer
     */
    @NotNull
    private String id;

    /**
     * Request perform by the User
     */
    @NotNull
    private TransferRequest transfer;

    /**
     * Transaction status
     */
    @NotNull
    private Status status;
}
