package com.tibudget.plugins.stubbed;

import com.tibudget.api.exceptions.*;
import com.tibudget.dto.AccountDto;
import com.tibudget.dto.ItemDto;
import com.tibudget.dto.TransactionDto;
import com.tibudget.plugins.stubbed.StubbedCollector.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class StubbedCollectorTest {

	private static final Logger LOG = Logger.getLogger(StubbedCollectorTest.class.getName());
	public static final double MAX_VALUE = 100000000000.0;

	@Test
	void testItem() {
		ItemDto itemDto = StubbedCollector.generateItem();
		assertNotNull(itemDto);
	}

	@Test
	void testOperationPurchase() {
		StubbedCollector collector = new StubbedCollector();
		collector.validate();

		List<TransactionDto> transactionDtos = collector.generateOperationPurchase();
		assertNotNull(transactionDtos);
		assertEquals(2, transactionDtos.size());

		transactionDtos.forEach(Assertions::assertNotNull);
	}

	@Test
	void testOperationTransfer() {
		StubbedCollector collector = new StubbedCollector();
		collector.validate();

		List<TransactionDto> transactionDtos = collector.generateOperationTransfer();
		assertNotNull(transactionDtos);
		assertEquals(2, transactionDtos.size());

		transactionDtos.forEach(Assertions::assertNotNull);
	}

	@Test
	void testOperationInterne() {
		StubbedCollector collector = new StubbedCollector();
		collector.validate();

		List<TransactionDto> transactionDtos = collector.generateOperationInterne();
		assertNotNull(transactionDtos);
		assertEquals(1, transactionDtos.size());

		transactionDtos.forEach(Assertions::assertNotNull);
	}

	@Test
	void testRandom() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(
				AccountDto.AccountDtoType.PAYMENT,
				"my account",
				"StubbedCollectorTest",
				Currency.getInstance(Locale.getDefault()).getCurrencyCode(),
				0.0
		));

		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + 1000L * 60 * 60 * 24 * 7);

		collector.setBeginDate(beginDate);
		collector.setEndDate(endDate);
		collector.setCorrectOpCount(50);
		collector.setErrorOpCount(0);

		assertEquals(0, collector.getProgress());
		assertEquals(0, collector.validate().size());

		collector.collect();

		assertNotNull(collector.getAccounts());

		int opCount = 0;
		for (TransactionDto opDto : collector.getTransactions()) {
			LOG.log(Level.FINE,
					"dateOp=" + opDto.getDateTransaction()
							+ " dateVal=" + opDto.getDateValue()
							+ " val=" + opDto.getAmount());

			assertNotNull(opDto.getDateValue());
			assertTrue(opDto.getDateValue().getTime() >= beginDate.getTime());
			assertTrue(opDto.getDateValue().getTime() <= endDate.getTime());

			assertNotNull(opDto.getDateTransaction());
			assertTrue(opDto.getDateTransaction().getTime() >= beginDate.getTime());
			assertTrue(opDto.getDateTransaction().getTime() <= endDate.getTime());

			assertNotNull(opDto.getLabel());
			assertFalse(opDto.getLabel().trim().isEmpty());

			assertTrue(opDto.getAmount() <= MAX_VALUE && opDto.getAmount() >= -MAX_VALUE);

			opCount++;
		}

		assertEquals(50 * 4 + 1, opCount);
		assertEquals(100, collector.getProgress());
	}

	@Test
	void testDefaultAccount() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		assertEquals(0, collector.validate().size());

		collector.collect();

		assertNotNull(collector.getAccounts());
		assertEquals(3, collector.getAccounts().size());

		Optional<AccountDto> shoppingAccount = collector.getAccounts().stream()
				.filter(a -> a.getType() == AccountDto.AccountDtoType.SHOPPING)
				.findFirst();

		assertTrue(shoppingAccount.isPresent());
	}

	@Test
	void testAccessDeny() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_AccessDeny);
		assertEquals(0, collector.validate().size());

		assertThrows(AccessDeny.class, collector::collect);
	}

	@Test
	void testCollectError() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_CollectError);
		assertEquals(0, collector.validate().size());

		assertThrows(CollectError.class, collector::collect);
	}

	@Test
	void testTemporaryUnavailable() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_TemporaryUnavailable);
		assertEquals(0, collector.validate().size());

		assertThrows(TemporaryUnavailable.class, collector::collect);
	}

	@Test
	void testConnectionFailure() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ConnectionFailure);
		assertEquals(0, collector.validate().size());

		assertThrows(ConnectionFailure.class, collector::collect);
	}

	@Test
	void testParameterError() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ParameterError);
		assertEquals(0, collector.validate().size());

		assertThrows(ParameterError.class, collector::collect);
	}

	@Test
	void testParameterErrorWithField() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ParameterError);
		collector.setParameterErrorField("toto");
		assertEquals(0, collector.validate().size());

		assertThrows(ParameterError.class, collector::collect);
	}

	@Test
	void testParameterErrorEndDateNull() {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(
				AccountDto.AccountDtoType.PAYMENT,
				"my account",
				"StubbedCollectorTest",
				Currency.getInstance(Locale.getDefault()).getCurrencyCode(),
				0.0
		));

		collector.setBeginDate(new Date());
		collector.setEndDate(null);
		collector.setType(Type.OPERATIONS);

		assertFalse(collector.validate().isEmpty());
	}

	@Test
	void testParameterErrorBadDates() {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(
				AccountDto.AccountDtoType.PAYMENT,
				"my account",
				"StubbedCollectorTest",
				Currency.getInstance(Locale.getDefault()).getCurrencyCode(),
				0.0
		));

		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + 1000L * 60 * 60 * 24 * 7);

		collector.setBeginDate(endDate);
		collector.setEndDate(beginDate);
		collector.setType(Type.OPERATIONS);

		assertFalse(collector.validate().isEmpty());
	}

	@Test
	void testParameterErrorCorrectCount() {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(
				AccountDto.AccountDtoType.PAYMENT,
				"my account",
				"StubbedCollectorTest",
				Currency.getInstance(Locale.getDefault()).getCurrencyCode(),
				0.0
		));

		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + 1000L * 60 * 60 * 24 * 7);

		collector.setBeginDate(beginDate);
		collector.setEndDate(endDate);
		collector.setCorrectOpCount(-1);
		collector.setErrorOpCount(0);
		collector.setType(Type.OPERATIONS);

		assertFalse(collector.validate().isEmpty());
	}

	@Test
	void testParameterErrorErrorCount() {
		StubbedCollector collector = new StubbedCollector();
		collector.setAccountPayment(new AccountDto(
				AccountDto.AccountDtoType.PAYMENT,
				"my account",
				"StubbedCollectorTest",
				Currency.getInstance(Locale.getDefault()).getCurrencyCode(),
				0.0
		));

		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + 1000L * 60 * 60 * 24 * 7);

		collector.setBeginDate(beginDate);
		collector.setEndDate(endDate);
		collector.setCorrectOpCount(0);
		collector.setErrorOpCount(-1);
		collector.setType(Type.OPERATIONS);

		assertFalse(collector.validate().isEmpty());
	}

	@Test
	void testRuntimeAccount() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeAccount);

		assertEquals(0, collector.validate().size());
		assertNotNull(collector.getTransactions());

		RuntimeException ex = assertThrows(RuntimeException.class, collector::getAccounts);
		assertTrue(ex.getMessage().contains("Simulated"));
	}

	@Test
	void testRuntimeOperation() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeOperation);

		assertNotNull(collector.getAccounts());

		RuntimeException ex = assertThrows(RuntimeException.class, collector::getTransactions);
		assertTrue(ex.getMessage().contains("Simulated"));
	}

	@Test
	void testRuntimeCollect() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeCollect);

		assertNotNull(collector.getAccounts());
		assertNotNull(collector.getTransactions());

		RuntimeException ex = assertThrows(RuntimeException.class, collector::collect);
		assertTrue(ex.getMessage().contains("Simulated"));
	}

	@Test
	void testRuntimeValidate() {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeValidate);

		RuntimeException ex = assertThrows(RuntimeException.class, collector::validate);
		assertTrue(ex.getMessage().contains("Simulated"));
	}
}
