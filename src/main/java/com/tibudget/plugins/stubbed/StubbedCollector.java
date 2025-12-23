package com.tibudget.plugins.stubbed;

import com.tibudget.api.*;
import com.tibudget.api.exceptions.*;
import com.tibudget.dto.*;
import com.tibudget.dto.MessageDto.MessageType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StubbedCollector implements CollectorPlugin {

	private static final Logger LOG = Logger.getLogger(StubbedCollector.class.getName());

	private static final String COUNTERPARTY_UUID = "12345678-1234-1234-1245-123456789012";

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

	@Input(required = false)
	private boolean askForCode = false;

	private OTPProvider otpProvider;

	private Double progress = 0.0;

	private final List<TransactionDto> operations = new ArrayList<>();

	private final List<AccountDto> accounts = new ArrayList<>();

	/**
	 * Random instance for generating random values
	 */
	private static final Random RANDOM = new Random();

	@Override
	public void init(InternetProvider internetProvider,
					 CounterpartyProvider counterpartyProvider,
					 OTPProvider otpProvider,
					 PDFToolsProvider pdfToolsProvider,
					 Map<String, String> settings,
					 Map<String, String> previousCookies,
					 List<AccountDto> previousAccounts) {
		this.otpProvider = otpProvider;
		this.accounts.addAll(previousAccounts);
	}

	@Override
	public String initConnection(URI uri) {
		return "";
	}

	@Override
	public String getOpenIdJSONConfiguration() {
		return CollectorPlugin.super.getOpenIdJSONConfiguration();
	}

	public List<MessageDto> validate() {
		List<MessageDto> msg = new ArrayList<>();
		if (type == Type.ERR_RuntimeValidate) {
			throw new RuntimeException("Simulated runtime exception in validate()");
		}
		switch (type) {
			case OPERATIONS:
				if (this.accountPayment == null) {
					this.accountPayment = new AccountDto(AccountDto.AccountDtoType.PAYMENT, "My checking account", COUNTERPARTY_UUID, Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0);
					this.accountPayment.addPaymentMethod(new PaymentMethodDto(PaymentDto.PaymentDtoType.CARD, "1234"));
					this.accountPayment.addPaymentMethod(new PaymentMethodDto(PaymentDto.PaymentDtoType.TRANSFER));
					this.accountPayment.addPaymentMethod(new PaymentMethodDto(PaymentDto.PaymentDtoType.CHECK));
					this.accountPayment.setMetadata(AccountDto.METADATA_IBAN, "FR1234567891234567891234567");
					this.accounts.add(this.accountPayment);
				}
				if (this.accountSaving == null) {
					this.accountSaving = new AccountDto(AccountDto.AccountDtoType.SAVING, "My saving account", COUNTERPARTY_UUID, Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0);
					this.accountSaving.addPaymentMethod(new PaymentMethodDto(PaymentDto.PaymentDtoType.TRANSFER));
					this.accounts.add(this.accountSaving);
				}
				if (this.accountShopping == null) {
					this.accountShopping = new AccountDto(AccountDto.AccountDtoType.SHOPPING, "My shopping account", COUNTERPARTY_UUID, Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 12.32);
					LoyaltyCardDto card = new LoyaltyCardDto();
					card.setBarcodeType(LoyaltyCardDto.BarcodeType.CODE_128);
					card.setReference("123456789012");
					card.setIssuer("Myshop.com");
                    try {
                        card.setCover(new FileDto(FileDto.FileDtoType.IMAGE, "Card cover", "image/png", FileGenerator.copyResourceToTempFile("loyalty-card.png")));
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Cannot load loyalty card cover: " + e.getMessage(), e);
                    }
                    this.accountShopping.addLoyaltyCard(card);
					this.accounts.add(this.accountShopping);
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

	@Override
	public void collect() throws CollectError, AccessDeny, TemporaryUnavailable, ConnectionFailure, ParameterError {
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
					TransactionDto opDto = generateOperation();
					addError(opDto);
					this.accountPayment.setCurrentBalance(this.accountPayment.getCurrentBalance() + opDto.getAmount());
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

	@Override
	public Map<String, String> getSettings() {
		return Map.of();
	}

	public List<AccountDto> getAccounts() {
		if (type == Type.ERR_RuntimeAccount) {
			throw new RuntimeException("Simulated runtime exception in getAccounts()");
		}
		return accounts;
	}

	@Override
	public List<TransactionDto> getTransactions() {
		if (type == Type.ERR_RuntimeOperation) {
			throw new RuntimeException("Simulated runtime exception in getOperations()");
		}
		return operations;
	}

	public int getProgress() {
		return progress.intValue();
	}

	@Override
	public Map<String, String> getCookies() {
		// No cookies needed by this collector
		return Map.of();
	}

	public List<TransactionDto> generateOperationPurchase() {
		List<TransactionDto> operationsDtos = new ArrayList<>();
		Date datePurchase = new Date(beginDate.getTime() + (long) (RANDOM.nextDouble() * (endDate.getTime() - beginDate.getTime())));
		TransactionDto purchase = new TransactionDto(
				UUID.randomUUID().toString(),
				accountShopping.getUuid(),
				TransactionDto.TransactionDtoType.PURCHASE,
				datePurchase,
				datePurchase,
				OperationLabelGenerator.generateOperationLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
				RANDOM.nextDouble() * 1000 - 500,
				"EUR"
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
		purchase.setAmount(amount);
		purchase.addPayment(new PaymentDto(
				PaymentDto.PaymentDtoType.CARD,
				"Visa",
				datePurchase,
				amount,
				"EUR",
				null,
				"1234"
		));
		if (randomYes(60)) {
			try {
				purchase.addFile(new FileDto(
						FileDto.FileDtoType.INVOICE,
						"Invoice",
						"application/pdf",
						FileGenerator.getRandomInvoiceFile()
				));
			} catch (IOException e) {
				// Ignore
			}
		}
		operationsDtos.add(purchase);

		TransactionDto checkOp = new TransactionDto(
				UUID.randomUUID().toString(),
				accountPayment.getUuid(),
				TransactionDto.TransactionDtoType.PAYMENT,
				datePurchase,
				datePurchase,
				"Purchase of " + purchase.getLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
				-amount,
				"EUR"
		);
		accountPayment.setCurrentBalance(accountPayment.getCurrentBalance() - amount);
		operationsDtos.add(checkOp);

		return operationsDtos;
	}

	public List<TransactionDto> generateOperationTransfer() {
		List<TransactionDto> operationsDtos = new ArrayList<>();
		Date dateOperation = new Date(beginDate.getTime() + (long) (RANDOM.nextDouble() * (endDate.getTime() - beginDate.getTime())));
		double amount = randomPrice();
		TransactionDto checkingOp = new TransactionDto(
				UUID.randomUUID().toString(),
				accountPayment.getUuid(),
				TransactionDto.TransactionDtoType.TRANSFER,
				dateOperation,
				dateOperation,
				"Transfer to " + accountSaving.getLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
				-amount,
				"EUR"
		);
		accountPayment.setCurrentBalance(accountPayment.getCurrentBalance() - amount);
		operationsDtos.add(checkingOp);

		TransactionDto savingOp = new TransactionDto(
				UUID.randomUUID().toString(),
				accountSaving.getUuid(),
				TransactionDto.TransactionDtoType.TRANSFER,
				dateOperation,
				dateOperation,
				"Transfer from " + accountPayment.getLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
				amount,
				"EUR"
		);
		accountSaving.setCurrentBalance(accountSaving.getCurrentBalance() + amount);
		operationsDtos.add(savingOp);

		return operationsDtos;
	}

	public List<TransactionDto> generateOperationInterne() {
		List<TransactionDto> operationsDtos = new ArrayList<>();
		Date dateOperation = new Date(beginDate.getTime() + (long) (RANDOM.nextDouble() * (endDate.getTime() - beginDate.getTime())));
		double amount = randomPrice();
		TransactionDto savingOp = new TransactionDto(
				UUID.randomUUID().toString(),
				accountSaving.getUuid(),
				TransactionDto.TransactionDtoType.INTERNAL,
				dateOperation,
				dateOperation,
				"Interest 2%",
				OperationLabelGenerator.generateOperationDetails(15),
				amount,
				"EUR"
		);
		accountSaving.setCurrentBalance(accountSaving.getCurrentBalance() + amount);
		operationsDtos.add(savingOp);

		return operationsDtos;
	}

	TransactionDto generateOperation() {
		long dateValue = beginDate.getTime() + (long) (RANDOM.nextDouble() * (endDate.getTime() - beginDate.getTime()));
		long dateOperation = dateValue + (long) (RANDOM.nextDouble() * (endDate.getTime() - dateValue));
		TransactionDto.TransactionDtoType type = getTransactionDtoType(dateOperation);
		return new TransactionDto(
				UUID.randomUUID().toString(),
				accountPayment.getUuid(),
                type,
                new Date(dateOperation),
                new Date(dateValue),
				OperationLabelGenerator.generateOperationLabel(),
				OperationLabelGenerator.generateOperationDetails(15),
                RANDOM.nextDouble() * 1000 - 500,
				"EUR"
        );
	}

	public static ItemDto generateItem() {
		ItemDto dto = new ItemDto(
				ItemLabelGenerator.generateProductName(),
				randomPrice(),
				randomQuantity(),
				ItemDto.QuantityUnit.UNIT
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

	private static TransactionDto.TransactionDtoType getTransactionDtoType(long dateOperation) {
		TransactionDto.TransactionDtoType type = TransactionDto.TransactionDtoType.PAYMENT;
		if (dateOperation % 11 == 0) {
			type = TransactionDto.TransactionDtoType.PURCHASE;
		}
		else if (dateOperation % 7 == 0) {
			type = TransactionDto.TransactionDtoType.INTERNAL;
		}
		else if (dateOperation % 5 == 0) {
			type = TransactionDto.TransactionDtoType.TRANSFER;
		}
		return type;
	}

	void addError(TransactionDto dto) {
		// Generate a int between 1 and 9 included (JAVA 8)
		int errorType = RANDOM.nextInt( 9) + 1;
		switch (errorType) {
			case 1:
				LOG.log(Level.FINE, "Adding error: date operation = null");
				dto.setDateTransaction(null);
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
				dto.setAmount(Double.MAX_VALUE/1000);
				LOG.log(Level.FINE, "Adding error: value = " + dto.getAmount());
				break;
			case 7:
				// Divide by 1000 to not have overflow into balance
				dto.setAmount(-Double.MAX_VALUE/1000);
				LOG.log(Level.FINE, "Adding error: value = " + dto.getAmount());
				break;
			case 8:
				LOG.log(Level.FINE, "Adding error: account = foo");
				dto.setAccountUuid("foo");
				break;
			case 9:
				dto.setAmount(0.0);
				LOG.log(Level.FINE, "Adding error: value = " + dto.getAmount());
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

	public void setOtpProvider(OTPProvider otpProvider) {
		this.otpProvider = otpProvider;
	}

	public void setProgress(Double progress) {
		this.progress = progress;
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
