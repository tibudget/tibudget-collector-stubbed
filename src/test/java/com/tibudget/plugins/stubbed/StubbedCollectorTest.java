package com.tibudget.plugins.stubbed;

import com.tibudget.api.exceptions.*;
import com.tibudget.dto.AccountDto;
import com.tibudget.dto.ItemDto;
import com.tibudget.dto.LoyaltyCardDto;
import com.tibudget.dto.TransactionDto;
import com.tibudget.plugins.stubbed.StubbedCollector.Type;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StubbedCollectorTest {

	private static final Logger LOG = Logger.getLogger(StubbedCollectorTest.class.getName());

	public static final double MAX_VALUE = 100000000000.0;

	@Test
	public void testItem() {
		ItemDto itemDto = StubbedCollector.generateItem();
		Assert.assertNotNull(itemDto);
	}

	@Test
	public void testOperationPurchase() {
		StubbedCollector collector = new StubbedCollector();
		collector.validate();
		List<TransactionDto> TransactionDtos = collector.generateOperationPurchase();
		Assert.assertNotNull(TransactionDtos);
        Assert.assertEquals(2, TransactionDtos.size());
		for (TransactionDto TransactionDto : TransactionDtos) {
			Assert.assertNotNull(TransactionDto);
		}
	}

	@Test
	public void testOperationTransfer() {
		StubbedCollector collector = new StubbedCollector();
		collector.validate();
		List<TransactionDto> TransactionDtos = collector.generateOperationTransfer();
		Assert.assertNotNull(TransactionDtos);
		Assert.assertEquals(2, TransactionDtos.size());
		for (TransactionDto TransactionDto : TransactionDtos) {
			Assert.assertNotNull(TransactionDto);
		}
	}

	@Test
	public void testOperationInterne() {
		StubbedCollector collector = new StubbedCollector();
		collector.validate();
		List<TransactionDto> TransactionDtos = collector.generateOperationInterne();
		Assert.assertNotNull(TransactionDtos);
		Assert.assertEquals(1, TransactionDtos.size());
		for (TransactionDto TransactionDto : TransactionDtos) {
			Assert.assertNotNull(TransactionDto);
		}
	}

	@Test
	public void testRandom() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(AccountDto.AccountDtoType.PAYMENT, "my account", "StubbedCollectorTest", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0));
		Date beginDate = new Date();
		// one week
		Date endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
		collector.setBeginDate(beginDate);
		collector.setEndDate(endDate);
		collector.setCorrectOpCount(50);
		collector.setErrorOpCount(0);
		Assert.assertEquals("progress", 0, collector.getProgress());
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect();
		Assert.assertNotNull(collector.getAccounts());
		int opCount = 0;
		for (TransactionDto opDto : collector.getTransactions()) {
			LOG.log(Level.FINE, "dateOp=" + opDto.getDateTransaction() + " dateVal=" + opDto.getDateValue() + " val=" + opDto.getAmount());
			Assert.assertNotNull("value date", opDto.getDateValue());
			Assert.assertTrue(opDto.getDateValue().getTime() >= beginDate.getTime());
			Assert.assertTrue(opDto.getDateValue().getTime() <= endDate.getTime());
			Assert.assertNotNull("operation date", opDto.getDateTransaction());
			Assert.assertTrue(opDto.getDateTransaction().getTime() >= beginDate.getTime());
			Assert.assertTrue(opDto.getDateTransaction().getTime() <= endDate.getTime());
			Assert.assertNotNull("label", opDto.getLabel());
            Assert.assertFalse("label size", opDto.getLabel().trim().isEmpty());
			Assert.assertNotNull(opDto.getLabel());
			Assert.assertTrue("value limit", opDto.getAmount() <= MAX_VALUE && opDto.getAmount() >= -MAX_VALUE);
			opCount++;
		}
		Assert.assertEquals("operation count", 50 * 4 + 1, opCount);
		Assert.assertEquals("progress", 100, collector.getProgress());
	}

	@Test
	public void testDefaultAccount() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect();
		Assert.assertNotNull(collector.getAccounts());
		Assert.assertEquals(3, collector.getAccounts().size());
		Optional<AccountDto> firstShoppingAccount = collector.getAccounts().stream()
				.filter(account -> account.getType() == AccountDto.AccountDtoType.SHOPPING)
				.findFirst();
		Assert.assertTrue(firstShoppingAccount.isPresent());
		AccountDto accountDto = firstShoppingAccount.get();
		Assert.assertNotNull(accountDto.getLoyaltyCards());
		Assert.assertEquals(1, accountDto.getLoyaltyCards().size());
		LoyaltyCardDto loyaltyCardDto = accountDto.getLoyaltyCards().get(0);
		Assert.assertNotNull(loyaltyCardDto.getCover());
	}

	@Test(expected=AccessDeny.class)
	public void testAccessDeny() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_AccessDeny);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect();
	}

	@Test(expected=CollectError.class)
	public void testCollectError() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_CollectError);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect();
	}

	@Test(expected=TemporaryUnavailable.class)
	public void testTemporaryUnavailable() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_TemporaryUnavailable);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect();
	}

	@Test(expected=ConnectionFailure.class)
	public void testConnectionFailure() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ConnectionFailure);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect();
	}

	@Test(expected=ParameterError.class)
	public void testParameterError() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ParameterError);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect();
	}

	@Test(expected=ParameterError.class)
	public void testParameterErrorWithField() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ParameterError);
		collector.setParameterErrorField("toto");
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect();
	}

	@Test
	public void testParameterErrorEndDateNull() {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(com.tibudget.dto.AccountDto.AccountDtoType.PAYMENT, "my account", "StubbedCollectorTest", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0));
		Date beginDate = new Date();
		collector.setBeginDate(beginDate);
		collector.setEndDate(null);
		collector.setType(Type.OPERATIONS);
        Assert.assertFalse("validation messages size", collector.validate().isEmpty());
	}

	@Test
	public void testParameterErrorBadDates() {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(com.tibudget.dto.AccountDto.AccountDtoType.PAYMENT, "my account", "StubbedCollectorTest", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0));
		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
		collector.setBeginDate(endDate);
		collector.setEndDate(beginDate);
		collector.setType(Type.OPERATIONS);
        Assert.assertFalse("validation messages size", collector.validate().isEmpty());
	}

	@Test
	public void testParameterErrorCorrectCount() {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(com.tibudget.dto.AccountDto.AccountDtoType.PAYMENT, "my account", "StubbedCollectorTest", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0));
		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
		collector.setBeginDate(beginDate);
		collector.setEndDate(endDate);
		collector.setCorrectOpCount(-1);
		collector.setErrorOpCount(0);
		collector.setType(Type.OPERATIONS);
        Assert.assertFalse("validation messages size", collector.validate().isEmpty());
	}

	@Test
	public void testParameterErrorErrorCount() {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(com.tibudget.dto.AccountDto.AccountDtoType.PAYMENT, "my account", "StubbedCollectorTest", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0));
		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
		collector.setBeginDate(beginDate);
		collector.setEndDate(endDate);
		collector.setCorrectOpCount(0);
		collector.setErrorOpCount(-1);
		collector.setType(Type.OPERATIONS);
        Assert.assertFalse("validation messages size", collector.validate().isEmpty());
	}

	@Test
	public void testRuntimeAccount() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeAccount);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		Assert.assertNotNull("getTransactions", collector.getTransactions());
		try {
			collector.getAccounts();
		}
		catch (RuntimeException e) {
			Assert.assertTrue("Simulated RuntimeException", e.getMessage().contains("Simulated"));
		}
	}

	@Test
	public void testRuntimeOperation() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeOperation);
		Assert.assertNotNull("getAccounts", collector.getAccounts());
		try {
			collector.getTransactions();
		}
		catch (RuntimeException e) {
			Assert.assertTrue("Simulated RuntimeException", e.getMessage().contains("Simulated"));
		}
	}

	@Test
	public void testRuntimeCollect() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeCollect);
		Assert.assertNotNull("getAccounts", collector.getAccounts());
		Assert.assertNotNull("getTransactions", collector.getTransactions());
		try {
			collector.collect();
		}
		catch (RuntimeException e) {
			Assert.assertTrue("Simulated RuntimeException", e.getMessage().contains("Simulated"));
		}
	}

	@Test
	public void testRuntimeValidate() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeValidate);
		try {
			collector.validate();
		}
		catch (RuntimeException e) {
			Assert.assertTrue("Simulated RuntimeException", e.getMessage().contains("Simulated"));
		}
	}
}
