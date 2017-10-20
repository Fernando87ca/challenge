package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.InsufficientAmountException;
import com.db.awmd.challenge.exception.TransferNotCompletedException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.repository.TransferRepository;
import com.db.awmd.challenge.enums.Status;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class TransferService {

    public static final String INSUFFICIENT_AMOUNT = "insufficient Amount in Origin Account";
    public static final String MONEY_SENT = "Money sent";
    public static final String MONEY_RECEIVED = "Money received";
    public static final String TRANSFER_CAN_NOT_BE_PERFORM = "Transfer can not be perform";
    public static final BigDecimal ZERO_VALUE = new BigDecimal(0);
    public static final int FIVE_SECONDS = 5;

    private Map<String, Lock> transferLocks = new ConcurrentHashMap<>();

    @Autowired
    private AccountsRepository accountsRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TransferRepository transferRepository;

    /**
     * Create a new transfer base on transfer request, validates all the inputs and executes the transfer
     *  <br /> while a transfer is being executed, locks both to prevent corruption of balance state.
     *
     * @param transferRequest the request to be executed
     * @exception AccountNotFoundException when one of accounts is not found
     * @exception TransferNotCompletedException when there is an unexpected error executing the transfer
     * */
    public void makeTransfer(TransferRequest transferRequest) {
        log.info("transfer between {} and {} with amount {} started",
                transferRequest.getAccountFromId(),
                transferRequest.getAccountToId(),
                transferRequest.getAmountTransfer());

        String transferId = transferRepository.createTransfer(transferRequest);
        final Account originAccount = getAccount(transferRequest.getAccountFromId());
        final Account destinationAccount = getAccount(transferRequest.getAccountToId());
        final Transfer transfer = this.transferRepository.getTransfer(transferId);

        if (originAccount == null || destinationAccount == null) {
            markTransferAsError(transferId);
            throw new AccountNotFoundException("Origin or destination Account " + transferId + " does not exist");
        }

        Lock fromLock = getAccountLock(originAccount);
        try {
            if (fromLock.tryLock(FIVE_SECONDS, TimeUnit.SECONDS)) {
                Lock toLock = getAccountLock(destinationAccount);

                try {
                    if (toLock.tryLock(FIVE_SECONDS, TimeUnit.SECONDS)) {
                        executeTransfer(transferRequest, originAccount, destinationAccount, transferId);
                    } else {
                        markTransferAsError(transferId);
                        throw new TransferNotCompletedException(TRANSFER_CAN_NOT_BE_PERFORM);
                    }
                } finally {
                    toLock.unlock();
                }
                transfer.setStatus(Status.COMPLETED);
            } else {
                markTransferAsError(transferId);
                throw new TransferNotCompletedException(TRANSFER_CAN_NOT_BE_PERFORM);
            }
        } catch (InterruptedException e) {
            markTransferAsError(transferId);
            throw new TransferNotCompletedException(TRANSFER_CAN_NOT_BE_PERFORM);
        } finally {
            fromLock.unlock();
        }

        if(transfer.getStatus().equals(Status.COMPLETED)) {
            notifyTransfer(originAccount, destinationAccount);
        }

        log.info("transfer between {} and {} with amount {} ended",
                transferRequest.getAccountFromId(),
                transferRequest.getAccountToId(),
                transferRequest.getAmountTransfer());
    }

    /**
     * Receives transferId as parameter and mark this transfer on TransferRepository as Error.
     * @param transferId
     */
    private void markTransferAsError(String transferId) {
        this.transferRepository.getTransfer(transferId).setStatus(Status.ERROR);
    }

    /**
     * Add into transferLocks map the account received by parameter using the account id as key and the lock as value.
     * Then return the lock for this account.
     * @param account
     * @return Lock
     */
    private Lock getAccountLock(Account account) {
        this.transferLocks.putIfAbsent(account.getAccountId(), new ReentrantLock());
        return transferLocks.get(account.getAccountId());
    }

    /**
     * Find and return the account saved into Account repository finding by account Id received as parameter.
     * @param accountId
     * @return Account
     */
    private Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    /**
     * Notify at Origin & Destination account about the transfer completed.
     * @param originAccount
     * @param destinationAccount
     */
    private void notifyTransfer(Account originAccount, Account destinationAccount) {
        this.notificationService.notifyAboutTransfer(originAccount, MONEY_SENT);
        this.notificationService.notifyAboutTransfer(destinationAccount, MONEY_RECEIVED);
    }

    /**
     * Perform the transaction amount between accounts.
     * @param transferRequest
     * @param originAccount
     * @param destinationAccount
     * @param transferInRepoId
     */
    private void executeTransfer(TransferRequest transferRequest,
                                 Account originAccount,
                                 Account destinationAccount,
                                 String transferInRepoId) {
        log.info("executing transfer {}", transferRequest);

        if (originAccount.getBalance().subtract(transferRequest.getAmountTransfer()).compareTo(ZERO_VALUE) >= 0) {
            originAccount.setBalance(originAccount.getBalance().subtract(transferRequest.getAmountTransfer()));
            destinationAccount.setBalance(destinationAccount.getBalance().add(transferRequest.getAmountTransfer()));
        } else {
            markTransferAsError(transferInRepoId);
            throw new InsufficientAmountException(INSUFFICIENT_AMOUNT);
        }
    }

}
