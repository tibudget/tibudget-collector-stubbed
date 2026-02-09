package com.tibudget.plugins.stubbed;

import com.tibudget.api.exceptions.*;
import com.tibudget.dto.AccountDto;
import com.tibudget.dto.ItemDto;
import com.tibudget.dto.RecurringPaymentDto;
import com.tibudget.dto.TransactionDto;
import com.tibudget.plugins.stubbed.StubbedCollector.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class StubbedCollectorTest {

	private static final Logger LOG = Logger.getLogger(StubbedCollectorTest.class.getName());
	public static final double MAX_VALUE = 100000000000.0;

	private static LocalDate toLocalDate(Date date) {
		return date.toInstant()
				.atZone(ZoneId.of("Europe/Paris"))
				.toLocalDate();
	}

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
	void testTransactionRecurring() {
		StubbedCollector collector = new StubbedCollector();
		collector.setBeginDate(new Date(2026 - 1900, Calendar.FEBRUARY, 1));
		collector.setEndDate(new Date(2026 - 1900, Calendar.FEBRUARY, 7));
		collector.validate();

		RecurringPaymentConfig config = new RecurringPaymentConfig(
				"NETFLIX",
				"Streaming subscription",
				5.99,
				0.0,
				RecurringPaymentDto.RecurrenceUnit.MONTH,
				1,
				LocalDate.of(2020, 3, 6),
				null,
				null,
				null
		);

		List<TransactionDto> transactionDtos = collector.generateRecurringTransactions(config);
		assertNotNull(transactionDtos);
		assertEquals(1, transactionDtos.size());

		TransactionDto dto = transactionDtos.get(0);
		assertNotNull(dto);

		// ==== Recurring payment link ====
		assertNotNull(dto.getRecurrentPaymentUuid());

		// ==== Identity ====
		String expectedId = UUID.nameUUIDFromBytes(
				(config.seed + ":2026-02-06").getBytes()
		).toString();
		assertEquals(expectedId, dto.getId());

		// ==== Dates ====
		LocalDate expectedDate = LocalDate.of(2026, 2, 6);
		LocalDate actualDate = dto.getDateValue().toInstant()
				.atZone(ZoneId.of("Europe/Paris"))
				.toLocalDate();

		assertEquals(expectedDate, actualDate);

		LocalDate executionDate = dto.getDateTransaction().toInstant()
				.atZone(ZoneId.of("Europe/Paris"))
				.toLocalDate();
		assertEquals(expectedDate, executionDate);

		// ==== Label & details ====
		assertEquals(config.label, dto.getLabel());
		assertTrue(dto.getDetails().contains("2026-02-06"));

		// ==== Amount ====
		assertEquals(5.99, dto.getAmount(), 0.0001);

		// ==== Currency ====
		assertEquals("EUR", dto.getCurrencyCode());

		// ==== Type ====
		assertEquals(
				TransactionDto.TransactionDtoType.PAYMENT,
				dto.getType()
		);
	}

	@Test
	void testTransactionRecurringWithDefaultVariation() {
		StubbedCollector collector = new StubbedCollector();
		collector.setBeginDate(new Date(2026 - 1900, Calendar.FEBRUARY, 1));
		collector.setEndDate(new Date(2026 - 1900, Calendar.FEBRUARY, 7));
		collector.validate();

		RecurringPaymentConfig config = new RecurringPaymentConfig(
				"SPOTIFY",
				"Music subscription",
				10.00,
				null, // default ±30%
				RecurringPaymentDto.RecurrenceUnit.MONTH,
				1,
				LocalDate.of(2020, 3, 6),
				null,
				null,
				null
		);

		List<TransactionDto> transactions =
				collector.generateRecurringTransactions(config);

		assertNotNull(transactions);
		assertEquals(1, transactions.size());

		TransactionDto dto = transactions.get(0);
		assertNotNull(dto);

		// Date
		LocalDate expectedDate = LocalDate.of(2026, 2, 6);
		LocalDate actualDate = dto.getDateValue().toInstant()
				.atZone(ZoneId.of("Europe/Paris"))
				.toLocalDate();
		assertEquals(expectedDate, actualDate);

		// Amount must be within ±30%
		double min = 10.00 * 0.70;
		double max = 10.00 * 1.30;
		assertTrue(dto.getAmount() >= min);
		assertTrue(dto.getAmount() <= max);
	}

	@Test
	void testTransactionRecurringNegativeAmountWithRatio() {
		StubbedCollector collector = new StubbedCollector();
		collector.setBeginDate(new Date(2026 - 1900, Calendar.FEBRUARY, 1));
		collector.setEndDate(new Date(2026 - 1900, Calendar.FEBRUARY, 7));
		collector.validate();

		RecurringPaymentConfig config = new RecurringPaymentConfig(
				"LOAN",
				"Loan payment",
				-200.00,
				0.10, // ±10%
				RecurringPaymentDto.RecurrenceUnit.MONTH,
				1,
				LocalDate.of(2020, 3, 6),
				null,
				null,
				null
		);

		List<TransactionDto> transactions =
				collector.generateRecurringTransactions(config);

		assertNotNull(transactions);
		assertEquals(1, transactions.size());

		TransactionDto dto = transactions.get(0);
		assertNotNull(dto);

		double amount = dto.getAmount();

		// Sign must be preserved
		assertTrue(amount < 0);

		// Bounds: -180 to -220
		assertTrue(amount <= -180.00);
		assertTrue(amount >= -220.00);
	}

	@Test
	void testTransactionRecurringWithMonthRestriction() {
		StubbedCollector collector = new StubbedCollector();
		collector.setBeginDate(new Date(2026 - 1900, Calendar.FEBRUARY, 1));
		collector.setEndDate(new Date(2026 - 1900, Calendar.FEBRUARY, 28));
		collector.validate();

		RecurringPaymentConfig config = new RecurringPaymentConfig(
				"SCHOOL_FEES",
				"School fees",
				231.24,
				0.0,
				RecurringPaymentDto.RecurrenceUnit.MONTH,
				1,
				LocalDate.of(2020, 1, 6),
				null,
				Month.JANUARY,
				Month.OCTOBER
		);

		List<TransactionDto> transactions =
				collector.generateRecurringTransactions(config);

		assertNotNull(transactions);
		assertEquals(1, transactions.size());

		TransactionDto dto = transactions.get(0);
		assertNotNull(dto);

		LocalDate date = dto.getDateValue().toInstant()
				.atZone(ZoneId.of("Europe/Paris"))
				.toLocalDate();

		// February must be allowed
		assertEquals(Month.FEBRUARY, date.getMonth());

		// Fixed amount
		assertEquals(231.24, dto.getAmount(), 0.0001);
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

		ZoneId zone = ZoneId.of("Europe/Paris");

		LocalDate today = LocalDate.now(zone);
		Date beginDate = Date.from(today.atStartOfDay(zone).toInstant());
		Date endDate = Date.from(today.plusDays(7).atStartOfDay(zone).toInstant());

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
			assertTrue(opDto.getDateValue().getTime() >= beginDate.getTime(), opDto.getDateValue() + " < " + beginDate);
			assertTrue(opDto.getDateValue().getTime() <= endDate.getTime(), opDto.getDateValue() + " > " + endDate);

			assertNotNull(opDto.getDateTransaction());
			assertTrue(opDto.getDateTransaction().getTime() >= beginDate.getTime());
			assertTrue(opDto.getDateTransaction().getTime() <= endDate.getTime());

			assertNotNull(opDto.getLabel());
			assertFalse(opDto.getLabel().trim().isEmpty());

			assertTrue(opDto.getAmount() <= MAX_VALUE && opDto.getAmount() >= -MAX_VALUE);

			if (opDto.getRecurrentPaymentUuid() == null) {
				opCount++;
			}
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
		assertEquals(4, collector.getAccounts().size());
		collector.getAccounts().forEach(a -> {
			assertNotNull(a.getType());
			assertNotNull(a.getId());
			assertNotNull(a.getUuid());
			assertNotNull(a.getLabel());
			assertNotNull(a.getCurrencyCode());
			assertNotNull(a.getMetadatas());
		});

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
