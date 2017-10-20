package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@Slf4j
public class ConcurrencyTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    public void concurrencyByCallTest() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"1\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"2\",\"balance\":1000}")).andExpect(status().isCreated());

        final AtomicInteger atomicInteger = new AtomicInteger(1000);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        IntStream.range(0, 1000)
                .forEach(i -> executor.submit(() -> {
                    try {
                        this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"from\":\"1\",\"to\":\"2\",\"amount\":10}"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        atomicInteger.decrementAndGet();
                    }
                }));

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        assertThat(atomicInteger.get()).isEqualTo(0);

        Account accountFrom = accountsService.getAccount("1");
        assertThat(accountFrom.getBalance()).isEqualTo(new BigDecimal(0));
    }

    @Test
    public void concurrencyByServiceTest() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"1\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"2\",\"balance\":1000}")).andExpect(status().isCreated());

        TransferRequest transferRequest = new TransferRequest()
                .setAccountFromId("1")
                .setAccountToId("2")
                .setAmountTransfer(new BigDecimal(10));

        final AtomicInteger atomicInteger = new AtomicInteger(1000);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        IntStream.range(0, 1000)
                .forEach(i -> executor.submit(() -> {
                    try {
                        transferService.makeTransfer(transferRequest);
                    } finally {
                        atomicInteger.decrementAndGet();
                    }
                }));

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        assertThat(atomicInteger.get()).isEqualTo(0);

        Account accountFrom = accountsService.getAccount("1");
        assertThat(accountFrom.getBalance()).isEqualTo(new BigDecimal(0));
    }

    /*
    The objective of this test is generate concurrency transactions that depends each other, with the objective
    to test if the code can manage death lock situation without problem.
     */
    @Test
    public void multipleConcurrencyTransferTest() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"1\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"2\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"3\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"4\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"5\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"6\",\"balance\":1000}")).andExpect(status().isCreated());

        TransferRequest firstTransfer = new TransferRequest()
                .setAccountFromId("1")
                .setAccountToId("2")
                .setAmountTransfer(new BigDecimal(1));

        TransferRequest secondTransfer = new TransferRequest()
                .setAccountFromId("3")
                .setAccountToId("4")
                .setAmountTransfer(new BigDecimal(1));

        TransferRequest thirdTransfer = new TransferRequest()
                .setAccountFromId("5")
                .setAccountToId("6")
                .setAmountTransfer(new BigDecimal(1));

        TransferRequest fourthTransfer = new TransferRequest()
                .setAccountFromId("1")
                .setAccountToId("3")
                .setAmountTransfer(new BigDecimal(1));

        TransferRequest fifthTransfer = new TransferRequest()
                .setAccountFromId("6")
                .setAccountToId("1")
                .setAmountTransfer(new BigDecimal(10));

        final AtomicInteger atomicInteger = new AtomicInteger(100);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        IntStream.range(0, 100)
                .forEach(i -> executor.submit(() -> {
                    transferService.makeTransfer(firstTransfer);
                    transferService.makeTransfer(secondTransfer);
                    transferService.makeTransfer(thirdTransfer);
                    transferService.makeTransfer(fourthTransfer);
                    transferService.makeTransfer(fifthTransfer);
                    atomicInteger.getAndDecrement();
                }));

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        assertThat(atomicInteger.get()).isEqualTo(0);

        assertThat(accountsService.getAccount("1").getBalance()).isEqualTo(new BigDecimal(1800));
        assertThat(accountsService.getAccount("2").getBalance()).isEqualTo(new BigDecimal(1100));
        assertThat(accountsService.getAccount("3").getBalance()).isEqualTo(new BigDecimal(1000));
        assertThat(accountsService.getAccount("4").getBalance()).isEqualTo(new BigDecimal(1100));
        assertThat(accountsService.getAccount("5").getBalance()).isEqualTo(new BigDecimal(900));
        assertThat(accountsService.getAccount("6").getBalance()).isEqualTo(new BigDecimal(100));
    }
}
