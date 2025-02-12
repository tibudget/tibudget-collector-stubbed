package com.tibudget.plugins.stubbed;

import com.tibudget.api.CollectorPlugin;
import com.tibudget.api.Input;
import com.tibudget.api.OTPProvider;
import com.tibudget.api.exceptions.*;
import com.tibudget.dto.AccountDto;
import com.tibudget.dto.MessageDto;
import com.tibudget.dto.MessageDto.MessageType;
import com.tibudget.dto.OperationDto;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StubbedCollector implements CollectorPlugin {

	private static final Logger LOG = Logger.getLogger(StubbedCollector.class.getName());

	public enum Type {
		OPERATIONS, ERR_CollectError, ERR_AccessDeny, ERR_TemporaryUnavailable, ERR_ConnectionFailure, ERR_ParameterError, ERR_RuntimeCollect, ERR_RuntimeOperation, ERR_RuntimeAccount, ERR_RuntimeValidate
	}

	@Input(order = 0)
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

	@Input(order = 4, fieldset = "type_ERR_ParameterError", required = false)
	private String parameterErrorField = null;

	@Input(fieldset="type_OPERATIONS", order=1, required = false)
	private AccountDto account;

	private Double progress = 0.0;

	/**
	 * Random instance for generating random values
	 */
	private final Random rand = new Random();

	public void collect(Iterable<AccountDto> bankAccounts) throws CollectError, AccessDeny, TemporaryUnavailable, ConnectionFailure, ParameterError {
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

	public List<AccountDto> getAccounts() {
		if (type == Type.ERR_RuntimeAccount) {
			throw new RuntimeException("Simulated runtime exception in getBankAccounts()");
		}
		return Collections.singletonList(this.account);
	}

	public List<OperationDto> getOperations() {
		if (type == Type.ERR_RuntimeOperation) {
			throw new RuntimeException("Simulated runtime exception in getBankAccounts()");
		}
		if (type != Type.OPERATIONS) {
			return Collections.emptyList();
		}
		List<OperationDto> dtos = new ArrayList<>(this.correctOpCount + this.errorOpCount);
		for (int i = 0; i < this.correctOpCount; i++) {
			OperationDto opDto = generateBankOperation();
			this.account.setCurrentBalance(this.account.getCurrentBalance() + opDto.getValue());
			dtos.add(opDto);
		}
		for (int i = 0; i < this.errorOpCount; i++) {
			OperationDto opDto = generateBankOperation();
			addError(opDto);
			this.account.setCurrentBalance(this.account.getCurrentBalance() + opDto.getValue());
			dtos.add(opDto);
		}

		return dtos;
	}

	public int getProgress() {
		return progress.intValue();
	}

	@Override
	public void setOTPProvider(OTPProvider otpProvider) {
		
	}

	@Override
	public void setCookies(Map<String, String> map) {

	}

	@Override
	public Map<String, String> getCookies() {
		return Map.of();
	}

	public List<MessageDto> validate() {
		List<MessageDto> msg = new ArrayList<>();
		if (type == Type.ERR_RuntimeValidate) {
			throw new RuntimeException("Simulated runtime exception in validate()");
		}
		switch (type) {
			case OPERATIONS:
				if (this.account == null) {
					this.account = new AccountDto(UUID.randomUUID().toString(), AccountDto.AccountDtoType.PAYMENT, "Stubbed account", "Stubbed collector", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0);
				}
				if (beginDate == null) {
					beginDate = new Date();
					// one week
					endDate = new Date(beginDate.getTime() + (1000 * 60 * 60 * 24 * 7));
				}
				else if (endDate == null) {
					msg.add(new MessageDto("endDate", "form.error.endDate.null"));
				}
				if (endDate != null && beginDate.after(endDate)) {
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

	OperationDto generateBankOperation() {
		long dateValue = beginDate.getTime() + (long) (rand.nextDouble() * (endDate.getTime() - beginDate.getTime()));
		long dateOperation = dateValue + (long) (rand.nextDouble() * (endDate.getTime() - dateValue));
		OperationDto.OperationDtoType type = getOperationDtoType(dateOperation);
		return new OperationDto(
                account.getUuid(),
                UUID.randomUUID().toString(),
                type,
                new Date(dateOperation),
                new Date(dateValue),
				BankOperationLabelGenerator.generateRandomLabel(),
				BankOperationLabelGenerator.generateLoremIpsum(15),
                rand.nextDouble() * 1000 - 500
        );
	}

	private static OperationDto.OperationDtoType getOperationDtoType(long dateOperation) {
		OperationDto.OperationDtoType type = OperationDto.OperationDtoType.PAYMENT;
		if (dateOperation % 11 == 0) {
			type = OperationDto.OperationDtoType.PURCHASE;
		}
		else if (dateOperation % 7 == 0) {
			type = OperationDto.OperationDtoType.INTERNAL;
		}
		else if (dateOperation % 5 == 0) {
			type = OperationDto.OperationDtoType.TRANSFER;
		}
		return type;
	}

	void addError(OperationDto dto) {
		// Generate a int between 1 and 9 included (JAVA 8)
		int errorType = rand.nextInt( 9) + 1;
		switch (errorType) {
			case 1:
				LOG.log(Level.FINE, "Adding error: date operation = null");
				dto.setDateOperation(null);
				break;
			case 2:
				LOG.log(Level.FINE, "Adding error: date value = null");
				dto.setDateValue(null);
				break;
			case 3:
				LOG.log(Level.FINE, "Adding error: label = null");
				dto.setLabel(null);
				break;
			case 4:
				LOG.log(Level.FINE, "Adding error: label empty");
				dto.setLabel("");
				break;
			case 5:
				LOG.log(Level.FINE, "Adding error: label = ' '");
				dto.setLabel(" ");
				break;
			case 6:
				// Divide by 1000 to not have overflow into balance
				dto.setValue(Double.MAX_VALUE/1000);
				LOG.log(Level.FINE, "Adding error: value = " + dto.getValue());
				break;
			case 7:
				// Divide by 1000 to not have overflow into balance
				dto.setValue(-Double.MAX_VALUE/1000);
				LOG.log(Level.FINE, "Adding error: value = " + dto.getValue());
				break;
			case 8:
				LOG.log(Level.FINE, "Adding error: account = foo");
				dto.setAccountUuid("foo");
				break;
			case 9:
				dto.setValue(0.0);
				LOG.log(Level.FINE, "Adding error: value = " + dto.getValue());
				break;
			default:
				LOG.log(Level.FINE, "Adding error: date value = null");
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

	public void setAccount(AccountDto account) {
		this.account = account;
	}

	public void setDelayInSeconds(int delayInSeconds) {
		this.delayInSeconds = delayInSeconds;
	}

}
