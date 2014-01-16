package com.tibudget.plugins.stubbed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.math.random.RandomDataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibudget.api.ICollectorPlugin;
import com.tibudget.api.Input;
import com.tibudget.api.exceptions.AccessDeny;
import com.tibudget.api.exceptions.CollectError;
import com.tibudget.api.exceptions.ConnectionFailure;
import com.tibudget.api.exceptions.ParameterError;
import com.tibudget.api.exceptions.TemporaryUnavailable;
import com.tibudget.dto.BankAccountDto;
import com.tibudget.dto.BankOperationDto;
import com.tibudget.dto.MessageDto;
import com.tibudget.dto.MessageDto.MessageType;

public class StubbedCollector implements ICollectorPlugin {

	private static Logger LOG = LoggerFactory.getLogger(StubbedCollector.class);

	public enum Type {
		OPERATIONS, ERR_CollectError, ERR_AccessDeny, ERR_TemporaryUnavailable, ERR_ConnectionFailure, ERR_ParameterError, ERR_RuntimeCollect, ERR_RuntimeBankOperation, ERR_RuntimeBankAccount, ERR_RuntimeValidate
	}

	@Input(order = 0, required = true)
	private Type type = Type.OPERATIONS;

	@Input(order = 2, fieldset = "type_OPERATIONS")
	private int correctOpCount = 10;

	@Input(order = 3, fieldset = "type_OPERATIONS")
	private int errorOpCount = 0;

	@Input(order = 4, fieldset = "type_OPERATIONS")
	private Date beginDate = null;

	@Input(order = 5, fieldset = "type_OPERATIONS")
	private Date endDate = null;

	@Input(order = 6, fieldset = "type_OPERATIONS")
	private int delayInSeconds = 1;

	@Input(order = 4, fieldset = "type_ERR_ParameterError")
	private String parameterErrorField = null;

	@Input(fieldset="type_OPERATIONS", order=1)
	private BankAccountDto bankAccount;
	
	private Double progress = 0.0;

	/**
	 * You must use the same instance if you want the random stuff to work
	 */
	private RandomDataImpl rand = new RandomDataImpl();

	public void collect(Iterable<BankAccountDto> bankAccounts) throws CollectError, AccessDeny, TemporaryUnavailable, ConnectionFailure, ParameterError {
		progress = 0.0;
		switch (type) {
		case ERR_CollectError:
			throw new CollectError("error.CollectError", new Date());
		case ERR_AccessDeny:
			throw new AccessDeny("error.AccessDeny", new Date());
		case ERR_TemporaryUnavailable:
			throw new TemporaryUnavailable("error.TemporaryUnavailable", new Date());
		case ERR_ConnectionFailure:
			throw new ConnectionFailure("error.ConnectionFailure", new Date());
		case ERR_ParameterError:
			throw new ParameterError(parameterErrorField, "error.ParameterError", new Date());
		case ERR_RuntimeCollect:
			throw new RuntimeException("Simulated runtime exception in collect()");
		default:
			for (int i = 0; i < this.delayInSeconds; i++) {
				try {
					Thread.sleep(1000);
					progress = (i + 1) * (100.0 / delayInSeconds);
				} catch (InterruptedException e) {
					throw new RuntimeException("Cannot sleep anymore :-(", e);
				}
			}
		}
		progress = 100.0;
	}

	public Iterable<BankAccountDto> getBankAccounts() {
		if (type == Type.ERR_RuntimeBankAccount) {
			throw new RuntimeException("Simulated runtime exception in getBankAccounts()");
		}
		return Collections.singleton(this.bankAccount);
	}

	public Iterable<BankOperationDto> getBankOperations() {
		if (type == Type.ERR_RuntimeBankOperation) {
			throw new RuntimeException("Simulated runtime exception in getBankAccounts()");
		}
		if (type != Type.OPERATIONS) {
			return Collections.emptyList();
		}
		List<BankOperationDto> dtos = new ArrayList<BankOperationDto>(this.correctOpCount + this.errorOpCount);
		for (int i = 0; i < this.correctOpCount; i++) {
			BankOperationDto opDto = generateBankOperation();
			this.bankAccount.setCurrentBalance(this.bankAccount.getCurrentBalance() + opDto.getValue());
			dtos.add(opDto);
		}
		for (int i = 0; i < this.errorOpCount; i++) {
			BankOperationDto opDto = generateBankOperation();
			addError(opDto);
			this.bankAccount.setCurrentBalance(this.bankAccount.getCurrentBalance() + opDto.getValue());
			dtos.add(opDto);
		}
		
		return dtos;
	}

	public int getProgress() {
		return progress.intValue();
	}

	public Collection<MessageDto> validate() {
		List<MessageDto> msg = new ArrayList<MessageDto>();
		if (type == Type.ERR_RuntimeValidate) {
			throw new RuntimeException("Simulated runtime exception in validate()");
		}
		switch (type) {
		case OPERATIONS:
			if (this.bankAccount == null) {
				this.bankAccount = new BankAccountDto(com.tibudget.dto.BankAccountDto.Type.BANK_CHECKING, "Stubbed account", "Stubbed collector", 0.0);
			}
			if (beginDate == null) {
				beginDate = new Date();
				// one week
				endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
			}
			else if (endDate == null) {
				msg.add(new MessageDto("endDate", "form.error.endDate.null"));
			}
			if (endDate != null && beginDate != null && beginDate.after(endDate)) {
				msg.add(new MessageDto("beginDate", "form.error.beginAfterEndDate"));
			}
			if (errorOpCount < 0) {
				msg.add(new MessageDto("errorOpCount", "form.error.errorOpCount"));
			}
			if (correctOpCount < 0) {
				msg.add(new MessageDto("correctOpCount", "form.error.correctOpCount"));
			}
			if (delayInSeconds < 0 || delayInSeconds > 3600) {
				msg.add(new MessageDto(MessageType.WARN, "delayInSeconds", "form.warn.delayInSeconds.ignored", delayInSeconds));
				delayInSeconds = 1;
			}
			break;
		case ERR_CollectError:
		case ERR_AccessDeny:
		case ERR_TemporaryUnavailable:
		case ERR_ConnectionFailure:
		case ERR_ParameterError:
		case ERR_RuntimeCollect:
		default:
		}
		return msg;
	}
	
	BankOperationDto generateBankOperation() {
		long dateValue = rand.nextLong(beginDate.getTime(), endDate.getTime());
		long dateOperation = rand.nextLong(dateValue, endDate.getTime());
		com.tibudget.dto.BankOperationDto.Type type = com.tibudget.dto.BankOperationDto.Type.OTHER;
		if (dateOperation % 11 == 0) {
			type = com.tibudget.dto.BankOperationDto.Type.CHECK;
		}
		else if (dateOperation % 7 == 0) {
			type = com.tibudget.dto.BankOperationDto.Type.BANK;
		}
		else if (dateOperation % 5 == 0) {
			type = com.tibudget.dto.BankOperationDto.Type.TRANSFERT;
		}
		else if (dateOperation % 3 == 0) {
			type = com.tibudget.dto.BankOperationDto.Type.CASH;
		}
		else if (dateOperation % 2 == 0) {
			type = com.tibudget.dto.BankOperationDto.Type.DEBIT_CARD;
		}
		BankOperationDto dto = new BankOperationDto(type, new Date(dateValue), new Date(dateOperation), UUID.randomUUID().toString(), rand.nextUniform(-500, 500));
		dto.setAccountId(bankAccount.getTitle());
		return dto;
	}
	
	void addError(BankOperationDto dto) {
		int errorType = rand.nextInt(1, 8);
		switch (errorType) {
		case 1:
			LOG.debug("Adding error: date operation = null");
			dto.setDateOperation(null);
			break;
		case 2:
			LOG.debug("Adding error: date value = null");
			dto.setDateValue(null);
			break;
		case 3:
			LOG.debug("Adding error: label = null");
			dto.setLabel(null);
			break;
		case 4:
			LOG.debug("Adding error: label empty");
			dto.setLabel("");
			break;
		case 5:
			LOG.debug("Adding error: label = ' '");
			dto.setLabel(" ");
			break;
		case 6:
			// Divide by 1000 to not have overflow into balance 
			dto.setValue(Double.MAX_VALUE/1000);
			LOG.debug("Adding error: value = " + dto.getValue());
			break;
		case 7:
			// Divide by 1000 to not have overflow into balance 
			dto.setValue(-Double.MAX_VALUE/1000);
			LOG.debug("Adding error: value = " + dto.getValue());
			break;
		case 8:
			LOG.debug("Adding error: account = foo");
			dto.setAccountId("foo");
			break;
		case 9:
			dto.setValue(0.0);
			LOG.debug("Adding error: value = " + dto.getValue());
			break;
		default:
			LOG.debug("Adding error: date value = null");
			dto.setDateValue(null);
			break;
		}
	}

	public void setType(Type type) {
		this.type = type;
	}

	public void setCorrectOpCount(int correctOpCount) {
		this.correctOpCount = correctOpCount;
	}

	public void setErrorOpCount(int errorOpCount) {
		this.errorOpCount = errorOpCount;
	}

	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public void setParameterErrorField(String parameterErrorField) {
		this.parameterErrorField = parameterErrorField;
	}

	public void setBankAccount(BankAccountDto bankAccount) {
		this.bankAccount = bankAccount;
	}

	public void setDelayInSeconds(int delayInSeconds) {
		this.delayInSeconds = delayInSeconds;
	}

}
