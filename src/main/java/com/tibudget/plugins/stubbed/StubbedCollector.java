package com.tibudget.plugins.stubbed;

import com.tibudget.api.CollectorPlugin;
import com.tibudget.api.Input;
import com.tibudget.api.OTPProvider;
import com.tibudget.api.exceptions.*;
import com.tibudget.dto.*;
import com.tibudget.dto.MessageDto.MessageType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
	private AccountDto accountPayment;

	@Input(fieldset="type_OPERATIONS", order=1, required = false)
	private AccountDto accountSaving;

	@Input(fieldset="type_OPERATIONS", order=1, required = false)
	private AccountDto accountShopping;

	@Input
	private boolean askForCode = false;

	private OTPProvider otpProvider;

	private Double progress = 0.0;

	private final List<OperationDto> operations = new ArrayList<>();

	/**
	 * Random instance for generating random values
	 */
	private static final Random RANDOM = new Random();

	public List<MessageDto> validate() {
		List<MessageDto> msg = new ArrayList<>();
		if (type == Type.ERR_RuntimeValidate) {
			throw new RuntimeException("Simulated runtime exception in validate()");
		}
		switch (type) {
			case OPERATIONS:
				if (this.accountPayment == null) {
					this.accountPayment = new AccountDto(UUID.randomUUID().toString(), AccountDto.AccountDtoType.PAYMENT, "My checking account", "Stubbed collector", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0);
				}
				if (this.accountSaving == null) {
					this.accountSaving = new AccountDto(UUID.randomUUID().toString(), AccountDto.AccountDtoType.SAVING, "My saving account", "Stubbed collector", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0);
				}
				if (this.accountShopping == null) {
					this.accountShopping = new AccountDto(UUID.randomUUID().toString(), AccountDto.AccountDtoType.SHOPPING, "My shopping account", "Stubbed collector", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0);
				}
				if (beginDate == null) {
					// Default is past 7 days
					endDate = new Date();
					beginDate = new Date(endDate.getTime() - (1000 * 60 * 60 * 24 * 7));
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

	public void collect(Iterable<AccountDto> accounts) throws CollectError, AccessDeny, TemporaryUnavailable, ConnectionFailure, ParameterError {
		progress = 0.0;
		if (askForCode && otpProvider != null) {
			String otpCode = otpProvider.getCode(OTPProvider.Channel.SMS, "the keyword", OTPProvider.PATTERN_6_DIGIT, "The stubbed collector need a code, please provide one");
			if (otpCode == null || otpCode.isEmpty()) {
				throw new AccessDeny("Access denied, no OTP code provided");
			}
		}
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
			case OPERATIONS:
			default:
				operations.addAll(generateOperationInterne());
				for (int i = 0; i < this.correctOpCount; i++) {
					operations.addAll(generateOperationPurchase());
					operations.addAll(generateOperationTransfer());
				}
				for (int i = 0; i < this.errorOpCount; i++) {
					OperationDto opDto = generateOperation();
					addError(opDto);
					this.accountPayment.setCurrentBalance(this.accountPayment.getCurrentBalance() + opDto.getValue());
					operations.add(opDto);
				}
				break;
		}
		for (int i = 0; i < this.delayInSeconds; i++) {
			try {
				Thread.sleep(1000);
				progress = (i + 1) * (100.0 / delayInSeconds);
			} catch (InterruptedException e) {
				throw new RuntimeException("Cannot sleep anymore :-(", e);
			}
		}
		progress = 100.0;
	}

	public List<AccountDto> getAccounts() {
		List<AccountDto> accounts = new ArrayList<>();
		if (type == Type.ERR_RuntimeAccount) {
			throw new RuntimeException("Simulated runtime exception in getAccounts()");
		}
		if (accountPayment != null) {
			accounts.add(accountPayment);
		}
		if (accountSaving != null) {
			accounts.add(accountSaving);
		}
		if (accountShopping != null) {
			accounts.add(accountShopping);
		}
		return accounts;
	}

	public List<OperationDto> getOperations() {
		if (type == Type.ERR_RuntimeOperation) {
			throw new RuntimeException("Simulated runtime exception in getOperations()");
		}
		return operations;
	}

	public int getProgress() {
		return progress.intValue();
	}

	@Override
	public void setOTPProvider(OTPProvider otpProvider) {
		this.otpProvider = otpProvider;
	}

	@Override
	public void setCookies(Map<String, String> cookies) {
		// No cookies needed by this collector
	}

	@Override
	public Map<String, String> getCookies() {
		// No cookies needed by this collector
		return Map.of();
	}

	public List<OperationDto> generateOperationPurchase() {
		List<OperationDto> operationsDtos = new ArrayList<>();
		Date datePurchase = new Date(beginDate.getTime() + (long) (RANDOM.nextDouble() * (endDate.getTime() - beginDate.getTime())));
		OperationDto purchase = new OperationDto(
				accountShopping.getUuid(),
				UUID.randomUUID().toString(),
				OperationDto.OperationDtoType.PURCHASE,
				datePurchase,
				datePurchase,
				OperationLabelGenerator.generateOperationLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
				RANDOM.nextDouble() * 1000 - 500
		);
		double amount = 0.0;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < randomItemQuantity(); i++) {
			ItemDto itemDto = generateItem();
			amount += itemDto.getPrice();
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(itemDto.getLabel());
			purchase.addItem(itemDto);
		}
		purchase.setDetails(sb.toString());
		purchase.setLabel(sb.toString());
		purchase.setValue(amount);
		purchase.addPaiment(new PaymentDto(
				PaymentDto.PaymentDtoType.CARD,
				"",
				datePurchase,
				amount,
				"EUR",
				null,
				"*-1234"
		));
		if (randomYes(60)) {
			try {
				purchase.addFile(new FileDto(
						FileDto.FileDtoType.INVOICE,
						"Invoice " + purchase.getIdForCollector(),
						FileGenerator.getRandomInvoiceFile()
				));
			} catch (IOException e) {
				// Ignore
			}
		}
		operationsDtos.add(purchase);

		OperationDto checkOp = new OperationDto(
				accountPayment.getUuid(),
				UUID.randomUUID().toString(),
				OperationDto.OperationDtoType.PAYMENT,
				datePurchase,
				datePurchase,
				"Purchase of " + purchase.getLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
				-amount
		);
		accountPayment.setCurrentBalance(accountPayment.getCurrentBalance() - amount);
		operationsDtos.add(checkOp);

		return operationsDtos;
	}

	public List<OperationDto> generateOperationTransfer() {
		List<OperationDto> operationsDtos = new ArrayList<>();
		Date dateOperation = new Date(beginDate.getTime() + (long) (RANDOM.nextDouble() * (endDate.getTime() - beginDate.getTime())));
		double amount = randomPrice();
		OperationDto checkingOp = new OperationDto(
				accountPayment.getUuid(),
				UUID.randomUUID().toString(),
				OperationDto.OperationDtoType.TRANSFER,
				dateOperation,
				dateOperation,
				"Transfer to " + accountSaving.getLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
				-amount
		);
		accountPayment.setCurrentBalance(accountPayment.getCurrentBalance() - amount);
		operationsDtos.add(checkingOp);

		OperationDto savingOp = new OperationDto(
				accountSaving.getUuid(),
				UUID.randomUUID().toString(),
				OperationDto.OperationDtoType.TRANSFER,
				dateOperation,
				dateOperation,
				"Transfer from " + accountPayment.getLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
				amount
		);
		accountSaving.setCurrentBalance(accountSaving.getCurrentBalance() + amount);
		operationsDtos.add(savingOp);

		return operationsDtos;
	}

	public List<OperationDto> generateOperationInterne() {
		List<OperationDto> operationsDtos = new ArrayList<>();
		Date dateOperation = new Date(beginDate.getTime() + (long) (RANDOM.nextDouble() * (endDate.getTime() - beginDate.getTime())));
		double amount = randomPrice();
		OperationDto savingOp = new OperationDto(
				accountSaving.getUuid(),
				UUID.randomUUID().toString(),
				OperationDto.OperationDtoType.INTERNAL,
				dateOperation,
				dateOperation,
				"Interest 2%",
				OperationLabelGenerator.generateOperationDetails(15),
				amount
		);
		accountSaving.setCurrentBalance(accountSaving.getCurrentBalance() + amount);
		operationsDtos.add(savingOp);

		return operationsDtos;
	}

	OperationDto generateOperation() {
		long dateValue = beginDate.getTime() + (long) (RANDOM.nextDouble() * (endDate.getTime() - beginDate.getTime()));
		long dateOperation = dateValue + (long) (RANDOM.nextDouble() * (endDate.getTime() - dateValue));
		OperationDto.OperationDtoType type = getOperationDtoType(dateOperation);
		return new OperationDto(
				accountPayment.getUuid(),
                UUID.randomUUID().toString(),
                type,
                new Date(dateOperation),
                new Date(dateValue),
				OperationLabelGenerator.generateOperationLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
                RANDOM.nextDouble() * 1000 - 500
        );
	}

	public static ItemDto generateItem() {
		ItemDto dto = new ItemDto(
				ItemLabelGenerator.generateProductName(),
				randomPrice(),
				randomQuantity()
		);
		if (randomYes(40)) {
			dto.setReference(ItemDto.ProductReferenceType.ASIN, "ABCDEFGHIJ");
		}
		if (randomYes(70)) {
			dto.setReference(ItemDto.ProductReferenceType.SKU, "ABC-1234-XY");
		}
		if (randomYes(80)) {
            try {
                dto.setUrl(new URL("https://tibu.com"));
            } catch (MalformedURLException e) {
                // Ignore
            }
        }
		if (randomYes(50)) {
            try {
                dto.addFile(new FileDto(
                        FileDto.FileDtoType.IMAGE,
                        "Cover of the image",
                        FileGenerator.getRandomImageFile()
                ));
            } catch (IOException e) {
                // Ignore
            }
        }
		return dto;
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
		int errorType = RANDOM.nextInt( 9) + 1;
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

	public static boolean randomYes(int percent) {
		return RANDOM.nextInt(100) < percent;
	}

	public static double randomPrice() {
		return 1 + (RANDOM.nextDouble() * 499);
	}

	public static int randomQuantity() {
		int randInt = RANDOM.nextInt(10); // Génère un nombre entre 0 et 9
		if (randInt < 7) { // 70% de chance d'obtenir 1
			return 1;
		} else if (randInt < 9) { // 20% de chance d'obtenir 2
			return 2;
		} else { // 10% de chance d'obtenir 3
			return 3;
		}
	}

	public static int randomItemQuantity() {
		int randInt = RANDOM.nextInt(100);
		if (randInt < 70) {
			return 1;
		} else if (randInt < 80) {
			return 2 + RANDOM.nextInt(5);
		} else if (randInt < 95) {
			return 5 + RANDOM.nextInt(10);
		} else {
			return 10 + RANDOM.nextInt(50);
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

	public void setAccountPayment(AccountDto accountPayment) {
		this.accountPayment = accountPayment;
	}

	public void setAccountSaving(AccountDto accountSaving) {
		this.accountSaving = accountSaving;
	}

	public void setAccountShopping(AccountDto accountShopping) {
		this.accountShopping = accountShopping;
	}

	public void setDelayInSeconds(int delayInSeconds) {
		this.delayInSeconds = delayInSeconds;
	}

	public boolean isAskForCode() {
		return askForCode;
	}

	public void setAskForCode(boolean askForCode) {
		this.askForCode = askForCode;
	}
}
