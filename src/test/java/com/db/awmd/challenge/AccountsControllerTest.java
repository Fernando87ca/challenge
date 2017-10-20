package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.enums.Status;
import com.db.awmd.challenge.repository.TransferRepository;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import java.util.Map;

import com.db.awmd.challenge.service.NotificationService;
import com.db.awmd.challenge.service.TransferService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private TransferService transferService;

  @Autowired
  private TransferRepository transferRepository;

  @MockBean
  private NotificationService notificationService;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
    transferRepository.clearTransfers();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  public void makeTransfer() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"1\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"2\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"from\":\"1\",\"to\":\"2\",\"amount\":90}")).andExpect(status().isOk());

    Account accountFrom = accountsService.getAccount("1");
    assertThat(accountFrom.getAccountId()).isEqualTo("1");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("10");

    Account accountTo = accountsService.getAccount("2");
    assertThat(accountTo.getAccountId()).isEqualTo("2");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("190");

    verify(this.notificationService,atLeastOnce()).notifyAboutTransfer(eq(accountFrom), anyString());
    verify(this.notificationService,atLeastOnce()).notifyAboutTransfer(eq(accountTo), anyString());

    assertThat(transferRepository.getAllTransfers().size()).isEqualTo(1);

    Map<String, Transfer> allTransfers = this.transferRepository.getAllTransfers();
    final String key = allTransfers.entrySet().iterator().next().getKey();
    final TransferRequest transfer = allTransfers.get(key).getTransfer();
    assertThat(transfer.getAccountFromId()).isEqualTo("1");
    assertThat(transfer.getAccountToId()).isEqualTo("2");
    assertThat(transfer.getAmountTransfer()).isEqualTo(new BigDecimal(90));
    assertThat(allTransfers.get(key).getStatus()).isEqualTo(Status.COMPLETED);
  }

  @Test
  public void insufficientAmount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"1\",\"balance\":10}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"2\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"from\":\"1\",\"to\":\"2\",\"amount\":90}")).andExpect(status().isBadRequest());

    Account accountFrom = accountsService.getAccount("1");
    assertThat(accountFrom.getAccountId()).isEqualTo("1");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("10");

    Account accountTo = accountsService.getAccount("2");
    assertThat(accountTo.getAccountId()).isEqualTo("2");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("100");

    assertThat(transferRepository.getAllTransfers().size()).isEqualTo(1);

    Map<String, Transfer> allTransfers = this.transferRepository.getAllTransfers();
    final String key = allTransfers.entrySet().iterator().next().getKey();
    final TransferRequest transfer = allTransfers.get(key).getTransfer();
    assertThat(transfer.getAccountFromId()).isEqualTo("1");
    assertThat(transfer.getAccountToId()).isEqualTo("2");
    assertThat(transfer.getAmountTransfer()).isEqualTo(new BigDecimal(90));
    assertThat(allTransfers.get(key).getStatus()).isEqualTo(Status.ERROR);
  }

  @Test
  public void sendBadTransferRequestObject() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"1\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"2\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"fail\":\"1\",\"fail\":\"2\",\"amount\":90}")).andExpect(status().isBadRequest());

    Account accountFrom = accountsService.getAccount("1");
    assertThat(accountFrom.getAccountId()).isEqualTo("1");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("100");

    Account accountTo = accountsService.getAccount("2");
    assertThat(accountTo.getAccountId()).isEqualTo("2");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("100");

    assertThat(transferRepository.getAllTransfers().size()).isEqualTo(0);
  }


  @Test
  public void senderMissing() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"1\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"2\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"to\":\"2\",\"amount\":90}")).andExpect(status().isBadRequest());

    Account accountFrom = accountsService.getAccount("1");
    assertThat(accountFrom.getAccountId()).isEqualTo("1");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("100");

    Account accountTo = accountsService.getAccount("2");
    assertThat(accountTo.getAccountId()).isEqualTo("2");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("100");

    assertThat(transferRepository.getAllTransfers().size()).isEqualTo(0);
  }

  @Test
  public void receiverMissing() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"1\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"2\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"from\":\"1\",\"amount\":90}")).andExpect(status().isBadRequest());

    Account accountFrom = accountsService.getAccount("1");
    assertThat(accountFrom.getAccountId()).isEqualTo("1");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("100");

    Account accountTo = accountsService.getAccount("2");
    assertThat(accountTo.getAccountId()).isEqualTo("2");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("100");

    assertThat(transferRepository.getAllTransfers().size()).isEqualTo(0);
  }

  @Test
  public void transferWrongAmount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"1\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"2\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"from\":\"1\",\"to\":\"2\",\"amount\":-90}")).andExpect(status().isBadRequest());

    Account accountFrom = accountsService.getAccount("1");
    assertThat(accountFrom.getAccountId()).isEqualTo("1");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("100");

    Account accountTo = accountsService.getAccount("2");
    assertThat(accountTo.getAccountId()).isEqualTo("2");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("100");

    assertThat(transferRepository.getAllTransfers().size()).isEqualTo(0);
  }

  @Test
  public void senderNotExist() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"1\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"2\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"from\":\"3\",\"to\":\"2\",\"amount\":90}")).andExpect(status().isBadRequest());

    Account accountFrom = accountsService.getAccount("1");
    assertThat(accountFrom.getAccountId()).isEqualTo("1");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("100");

    Account accountTo = accountsService.getAccount("2");
    assertThat(accountTo.getAccountId()).isEqualTo("2");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("100");

    Map<String, Transfer> allTransfers = this.transferRepository.getAllTransfers();
    final String key = allTransfers.entrySet().iterator().next().getKey();
    final TransferRequest transfer = allTransfers.get(key).getTransfer();
    assertThat(transfer.getAccountFromId()).isEqualTo("3");
    assertThat(transfer.getAccountToId()).isEqualTo("2");
    assertThat(transfer.getAmountTransfer()).isEqualTo(new BigDecimal(90));
    assertThat(allTransfers.get(key).getStatus()).isEqualTo(Status.ERROR);
  }

  @Test
  public void receiverNotExist() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"1\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"2\",\"balance\":100}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"from\":\"1\",\"to\":\"3\",\"amount\":90}")).andExpect(status().isBadRequest());

    Account accountFrom = accountsService.getAccount("1");
    assertThat(accountFrom.getAccountId()).isEqualTo("1");
    assertThat(accountFrom.getBalance()).isEqualByComparingTo("100");

    Account accountTo = accountsService.getAccount("2");
    assertThat(accountTo.getAccountId()).isEqualTo("2");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("100");

    Map<String, Transfer> allTransfers = this.transferRepository.getAllTransfers();
    final String key = allTransfers.entrySet().iterator().next().getKey();
    final TransferRequest transfer = allTransfers.get(key).getTransfer();
    assertThat(transfer.getAccountFromId()).isEqualTo("1");
    assertThat(transfer.getAccountToId()).isEqualTo("3");
    assertThat(transfer.getAmountTransfer()).isEqualTo(new BigDecimal(90));
    assertThat(allTransfers.get(key).getStatus()).isEqualTo(Status.ERROR);
  }
}
