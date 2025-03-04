package com.tibudget.plugins.stubbed;

import com.tibudget.api.exceptions.*;
import com.tibudget.dto.AccountDto;
import com.tibudget.dto.ItemDto;
import com.tibudget.dto.OperationDto;
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
		List<OperationDto> operationDtos = collector.generateOperationPurchase();
		Assert.assertNotNull(operationDtos);
        Assert.assertEquals(2, operationDtos.size());
		for (OperationDto operationDto : operationDtos) {
			Assert.assertNotNull(operationDto);
		}
	}

	@Test
	public void testOperationTransfer() {
		StubbedCollector collector = new StubbedCollector();
		collector.validate();
		List<OperationDto> operationDtos = collector.generateOperationTransfer();
		Assert.assertNotNull(operationDtos);
		Assert.assertEquals(2, operationDtos.size());
		for (OperationDto operationDto : operationDtos) {
			Assert.assertNotNull(operationDto);
		}
	}

	@Test
	public void testOperationInterne() {
		StubbedCollector collector = new StubbedCollector();
		collector.validate();
		List<OperationDto> operationDtos = collector.generateOperationInterne();
		Assert.assertNotNull(operationDtos);
		Assert.assertEquals(1, operationDtos.size());
		for (OperationDto operationDto : operationDtos) {
			Assert.assertNotNull(operationDto);
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
		collector.collect(Collections.emptyList());
		Assert.assertNotNull(collector.getAccounts());
		int opCount = 0;
		for (OperationDto opDto : collector.getOperations()) {
			LOG.log(Level.FINE, "dateOp=" + opDto.getDateOperation() + " dateVal=" + opDto.getDateValue() + " val=" + opDto.getAmount());
			Assert.assertNotNull("value date", opDto.getDateValue());
			Assert.assertTrue(opDto.getDateValue().getTime() >= beginDate.getTime());
			Assert.assertTrue(opDto.getDateValue().getTime() <= endDate.getTime());
			Assert.assertNotNull("operation date", opDto.getDateOperation());
			Assert.assertTrue(opDto.getDateOperation().getTime() >= beginDate.getTime());
			Assert.assertTrue(opDto.getDateOperation().getTime() <= endDate.getTime());
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
		collector.collect(Collections.emptyList());
		Assert.assertNotNull(collector.getAccounts());
		Assert.assertEquals(3, collector.getAccounts().size());
	}

	@Test(expected=AccessDeny.class)
	public void testAccessDeny() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_AccessDeny);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.emptyList());
	}

	@Test(expected=CollectError.class)
	public void testCollectError() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_CollectError);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.emptyList());
	}

	@Test(expected=TemporaryUnavailable.class)
	public void testTemporaryUnavailable() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_TemporaryUnavailable);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.emptyList());
	}

	@Test(expected=ConnectionFailure.class)
	public void testConnectionFailure() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ConnectionFailure);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.emptyList());
	}

	@Test(expected=ParameterError.class)
	public void testParameterError() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ParameterError);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.emptyList());
	}

	@Test(expected=ParameterError.class)
	public void testParameterErrorWithField() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ParameterError);
		collector.setParameterErrorField("toto");
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.emptyList());
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
		Assert.assertNotNull("getOperations", collector.getOperations());
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
			collector.getOperations();
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
		Assert.assertNotNull("getOperations", collector.getOperations());
		try {
			collector.collect(Collections.emptyList());
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
