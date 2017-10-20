package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.domain.TransferRequest;

import java.util.Map;

public interface TransferRepository {

    /** Create a transfer into Transfer Repository
     * @param transferRequest
     * @return String
     *  */
    String createTransfer(TransferRequest transferRequest);

    /** Return a transfer from Repository from the account id send by parameter
     * @param transferId
     * @return Transfer
     *  */
    Transfer getTransfer(String transferId);

    /** Delete all records on the Repository */
    void clearTransfers();

    /** Return all the transfers stored on the Repository
     * @return Map<String, Transfer>
     * */
    Map<String, Transfer> getAllTransfers();
}
