package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.InsufficientAmountException;
import com.db.awmd.challenge.repository.AccountsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransferService {

    public static final String INSUFFICIENT_AMOUNT = "insufficient Amount in Origin Account";
    public static final String MONEY_SENT = "Money sent";
    public static final String MONEY_RECEIVED = "Money received";


    @Autowired
    private AccountsRepository accountsRepository;

    @Autowired
    private NotificationService notificationService;

    public void makeTransfer(TransferRequest transferRequest) {
        Account originAccount = this.accountsRepository.getAccount(transferRequest.getAccountFromId());
        if(originAccount == null) {
            throw new AccountNotFoundException("Account " + transferRequest.getAccountFromId() + "doesn't exist");
        }

        Account destinationAccount = this.accountsRepository.getAccount(transferRequest.getAccountToId());
        if(destinationAccount == null) {
            throw new AccountNotFoundException("Account " + transferRequest.getAccountToId() + "doesn't exist");
        }

        executeTransfer(transferRequest, originAccount, destinationAccount);

        this.notificationService.notifyAboutTransfer(originAccount, MONEY_SENT);
        this.notificationService.notifyAboutTransfer(destinationAccount, MONEY_RECEIVED);
    }

    private void executeTransfer(TransferRequest transferRequest, Account originAccount, Account destinationAccount) {
        originAccount.setBalance(originAccount.getBalance().subtract(transferRequest.getAmountTransfer()));

        if (originAccount.getBalance().compareTo(new BigDecimal(0)) >= 0) {
            destinationAccount.setBalance(destinationAccount.getBalance().add(transferRequest.getAmountTransfer()));
        } else {
            originAccount.setBalance(originAccount.getBalance().add(transferRequest.getAmountTransfer()));
            throw new InsufficientAmountException(INSUFFICIENT_AMOUNT);
        }
    }

}
