package com.tibudget.plugins.stubbed;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibudget.api.exceptions.AccessDeny;
import com.tibudget.api.exceptions.CollectError;
import com.tibudget.api.exceptions.ConnectionFailure;
import com.tibudget.api.exceptions.MessagesException;
import com.tibudget.api.exceptions.ParameterError;
import com.tibudget.api.exceptions.TemporaryUnavailable;
import com.tibudget.dto.BankAccountDto;
import com.tibudget.dto.BankOperationDto;
import com.tibudget.plugins.stubbed.StubbedCollector.Type;

public class StubbedCollectorTest {

	private static Logger LOG = LoggerFactory.getLogger(StubbedCollectorTest.class);

	public static final double MAX_VALUE = 100000000000.0;

	@Test
	public void testRandom() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setBankAccount(new BankAccountDto(UUID.randomUUID().toString(), com.tibudget.dto.BankAccountDto.Type.BANK_CHECKING, "my account", "StubbedCollectorTest", 0.0));
		Date beginDate = new Date();
		// one week
		Date endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
		collector.setBeginDate(beginDate);
		collector.setEndDate(endDate);
		collector.setCorrectOpCount(50);
		collector.setErrorOpCount(0);
		Assert.assertEquals("progress", 0, collector.getProgress());
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.<BankAccountDto>emptyList());
		Assert.assertNotNull(collector.getBankAccounts());
		int opCount = 0;
		BankOperationDto lastOpDto = null;
		for (BankOperationDto opDto : collector.getBankOperations()) {
			LOG.debug("dateOp=" + opDto.getDateOperation() + " dateVal=" + opDto.getDateValue() + " val=" + opDto.getValue());
			Assert.assertNotNull("value date", opDto.getDateValue());
			Assert.assertTrue(opDto.getDateValue().getTime() >= beginDate.getTime());
			Assert.assertTrue(opDto.getDateValue().getTime() <= endDate.getTime());
			Assert.assertNotNull("operation date", opDto.getDateOperation());
			Assert.assertTrue(opDto.getDateOperation().getTime() >= beginDate.getTime());
			Assert.assertTrue(opDto.getDateOperation().getTime() <= endDate.getTime());
			Assert.assertNotNull("label", opDto.getLabel());
			Assert.assertTrue("label size", opDto.getLabel().trim().length() > 0);
			Assert.assertNotNull(opDto.getLabel());
			Assert.assertTrue("value limit", opDto.getValue() <= MAX_VALUE && opDto.getValue() >= -MAX_VALUE);
			if (lastOpDto != null) {
				Assert.assertFalse("not so randomized value date", lastOpDto.getDateValue().equals(opDto.getDateValue()));
				Assert.assertFalse("not so randomized operation date", lastOpDto.getDateOperation().equals(opDto.getDateOperation()));
				Assert.assertFalse("not so randomized label", lastOpDto.getLabel().equals(opDto.getLabel()));
				Assert.assertFalse("not so randomized value", lastOpDto.getValue() == opDto.getValue());
			}
			opCount++;
		}
		Assert.assertEquals("operation count", 50, opCount);
		Assert.assertEquals("progress", 100, collector.getProgress());
	}

	@Test
	public void testDefaultAccount() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.OPERATIONS);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.<BankAccountDto>emptyList());
		Assert.assertNotNull(collector.getBankAccounts());
		Assert.assertTrue("getBankAccounts() must have element", collector.getBankAccounts().iterator().hasNext());
	}

	@Test(expected=AccessDeny.class)
	public void testAccessDeny() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_AccessDeny);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.<BankAccountDto>emptyList());
	}

	@Test(expected=CollectError.class)
	public void testCollectError() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_CollectError);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.<BankAccountDto>emptyList());
	}

	@Test(expected=TemporaryUnavailable.class)
	public void testTemporaryUnavailable() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_TemporaryUnavailable);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.<BankAccountDto>emptyList());
	}

	@Test(expected=ConnectionFailure.class)
	public void testConnectionFailure() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ConnectionFailure);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.<BankAccountDto>emptyList());
	}

	@Test(expected=ParameterError.class)
	public void testParameterError() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ParameterError);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.<BankAccountDto>emptyList());
	}

	@Test(expected=ParameterError.class)
	public void testParameterErrorWithField() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_ParameterError);
		collector.setParameterErrorField("toto");
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		collector.collect(Collections.<BankAccountDto>emptyList());
	}

	@Test
	public void testParameterErrorEndDateNull() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setBankAccount(new BankAccountDto(UUID.randomUUID().toString(), com.tibudget.dto.BankAccountDto.Type.BANK_CHECKING, "my account", "StubbedCollectorTest", 0.0));
		Date beginDate = new Date();
		collector.setBeginDate(beginDate);
		collector.setEndDate(null);
		collector.setType(Type.OPERATIONS);
		Assert.assertTrue("validation messages size", collector.validate().size() > 0);
	}

	@Test
	public void testParameterErrorBadDates() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setBankAccount(new BankAccountDto(UUID.randomUUID().toString(), com.tibudget.dto.BankAccountDto.Type.BANK_CHECKING, "my account", "StubbedCollectorTest", 0.0));
		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
		collector.setBeginDate(endDate);
		collector.setEndDate(beginDate);
		collector.setType(Type.OPERATIONS);
		Assert.assertTrue("validation messages size", collector.validate().size() > 0);
	}

	@Test
	public void testParameterErrorCorrectCount() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setBankAccount(new BankAccountDto(UUID.randomUUID().toString(), com.tibudget.dto.BankAccountDto.Type.BANK_CHECKING, "my account", "StubbedCollectorTest", 0.0));
		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
		collector.setBeginDate(beginDate);
		collector.setEndDate(endDate);
		collector.setCorrectOpCount(-1);
		collector.setErrorOpCount(0);
		collector.setType(Type.OPERATIONS);
		Assert.assertTrue("validation messages size", collector.validate().size() > 0);
	}

	@Test
	public void testParameterErrorErrorCount() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setBankAccount(new BankAccountDto(UUID.randomUUID().toString(), com.tibudget.dto.BankAccountDto.Type.BANK_CHECKING, "my account", "StubbedCollectorTest", 0.0));
		Date beginDate = new Date();
		Date endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
		collector.setBeginDate(beginDate);
		collector.setEndDate(endDate);
		collector.setCorrectOpCount(0);
		collector.setErrorOpCount(-1);
		collector.setType(Type.OPERATIONS);
		Assert.assertTrue("validation messages size", collector.validate().size() > 0);
	}

	@Test
	public void testRuntimeBankAccount() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeBankAccount);
		Assert.assertEquals("validation messages size", 0, collector.validate().size());
		Assert.assertNotNull("getBankOperations", collector.getBankOperations());
		try {
			collector.getBankAccounts();
		}
		catch (RuntimeException e) {
			Assert.assertTrue("Simulated RuntimeException", e.getMessage().contains("Simulated"));
		}
	}

	@Test
	public void testRuntimeBankOperation() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeBankOperation);
		Assert.assertNotNull("getBankAccounts", collector.getBankAccounts());
		try {
			collector.getBankOperations();
		}
		catch (RuntimeException e) {
			Assert.assertTrue("Simulated RuntimeException", e.getMessage().contains("Simulated"));
		}
	}

	@Test
	public void testRuntimeCollect() throws MessagesException {
		StubbedCollector collector = new StubbedCollector();
		collector.setType(Type.ERR_RuntimeCollect);
		Assert.assertNotNull("getBankAccounts", collector.getBankAccounts());
		Assert.assertNotNull("getBankOperations", collector.getBankOperations());
		try {
			collector.collect(Collections.<BankAccountDto>emptyList());
		}
		catch (RuntimeException e) {
			Assert.assertTrue("Simulated RuntimeException", e.getMessage().contains("Simulated"));
		}
	}

	@Test
	public void testRuntimeValidate() throws MessagesException {
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
