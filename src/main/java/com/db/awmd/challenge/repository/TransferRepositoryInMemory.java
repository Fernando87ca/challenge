package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.enums.Status;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TransferRepositoryInMemory implements TransferRepository {

    private final Map<String, Transfer> transfers = new ConcurrentHashMap<>();

    @Override
    public String createTransfer(TransferRequest transferRequest) {
        String id = UUID.randomUUID().toString();

        Transfer transfer = new Transfer()
                .setId(UUID.randomUUID().toString())
                .setTransfer(transferRequest)
                .setStatus(Status.CREATED);
        this.transfers.putIfAbsent(id, transfer);

        return id;
    }

    @Override
    public Transfer getTransfer(String transferId) {
        return this.transfers.get(transferId);
    }

    @Override
    public void clearTransfers() {
        this.transfers.clear();
    }

    @Override
    public Map<String, Transfer> getAllTransfers() {
        return this.transfers;
    }
}
