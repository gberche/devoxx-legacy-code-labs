package legacy.hedge;

import legacy.DateTimeUtils;
import legacy.dto.Modif;
import legacy.security.User;
import legacy.error.ARPSystemException;
import legacy.error.CheckResult;
import legacy.dto.Book;
import legacy.security.UserSessionsManager;
import legacy.service.DataAccessService;
import legacy.service.IHedgingPositionDataAccessService;
import legacy.service.ITradingDataAccessService;
import legacy.service.ITransactionManagerService;
import legacy.service.TradingOrder;
import legacy.dto.Transaction;
import legacy.persistence.StorageActionEnum;
import org.apache.commons.lang3.SerializationUtils;

import java.math.BigInteger;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HedgingPositionManagementImpl implements IHedgingPositionManagement {

	private static int MAX_DECIMALS = 4;
	private static Logger LOGGER = Logger.getLogger(HedgingPositionManagementImpl.class.getName());
    private ITradingDataAccessService tradingDataAccessService = DataAccessService.getTradingDataAccessService();
    private ITransactionManagerService transactionManagerService = DataAccessService.getTransactionManagerService();



    @Override
	public CheckResult<HedgingPosition> initAndSendHedgingPosition(HedgingPosition hp) throws ARPSystemException {
		CheckResult<HedgingPosition> result = new CheckResult<HedgingPosition>();
		try {
			hp = initHedgingPosition(hp);
		} catch (Exception e) {
			String errorMsg = "TECHNICAL ERROR, cannot initialize HP to send";
			LOGGER.log(Level.SEVERE, errorMsg, e);
			String msg = hp.getErrorLevel().createHMsgFromError();
			hp.setHedgeMsg(msg);
			result.setCheckIsOk(false);
			try {
				updateHedgingPosition(hp);
			} catch (ARPSystemException e1) {
				LOGGER.log(Level.SEVERE, errorMsg, e1);
			}
			return result;
		}
		try {
			result = hedgePositionBySendTo3rdParty(hp);
			if(result.isCheckIsOk()) {
				hp = result.getResult();
				hp.setStatus(HedgingPositionStatusConst.HEDGED);
				updateHedgingPosition(hp);
			} else {
				hp = result.getResult();
				switch(hp.getErrorLevel()){
					case FUNCTIONAL_ERROR:{
						hp.setStatus(HedgingPositionStatusConst.REJECTED);
						break;
					}
					case CONNECT_ERROR: {
						hp.setStatus(HedgingPositionStatusConst.REJECTED);
						break;
					}
					case BOOKING_MALFUNCTION: {
						//TO DO
						break;
					}
					default: {
						break;
					}
				}
				updateHedgingPosition(hp);
			}
		} catch(ARPSystemException e) {
			LOGGER.log(Level.SEVERE,e.getMessage(), e);
		}
		return result;
	}

    public interface HedginPositionMgrInvoker {
        CheckResult<HedgingPosition> invoke(HedgingPosition hp);
    }

    HedginPositionMgrInvoker hedginPositionMgrInvoker = new HedginPositionMgrInvoker() {

        @Override
        public CheckResult<HedgingPosition> invoke(HedgingPosition hp) {
            return HedgingPositionMgt.hedgingPositionMgt(hp);
        }
    };

    public void setHedginPositionMgrInvoker(HedginPositionMgrInvoker hedginPositionMgrInvoker) {
        this.hedginPositionMgrInvoker = hedginPositionMgrInvoker;
    }

    public void setTradingDataAccessService(ITradingDataAccessService tradingDataAccessService) {
        this.tradingDataAccessService = tradingDataAccessService;
    }

    private CheckResult<HedgingPosition> hedgePositionBySendTo3rdParty(HedgingPosition hp) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.log(Level.FINEST,"Begin 3r party processing. stand by");
		}
		CheckResult<HedgingPosition> result;
        result = hedginPositionMgrInvoker.invoke(hp);
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.log(Level.FINEST,"3r party processing is now finished, thank you for your patience"); // t'es con michel
		}
		return result;
	}

	private HedgingPosition updateHedgingPosition(HedgingPosition hp)  {
		HedgingPosition hpUpdate = SerializationUtils.clone(hp);
			if (hp.getType().equals(HedgingPositionTypeConst.INI)) {
				hpUpdate.setTransactionId(hp.getTransactionId());
				Modif modif = new Modif();
				modif.setCreDate(new Date());
				hp.setLastModification(modif);
				hp.setStorageUpdate(StorageActionEnum.CREATE);
			} else {
				hp.setStorageUpdate(StorageActionEnum.UPDATE);
			}
            hpUpdate = transactionManagerService.classStorageAction(hp);
        return hpUpdate;
	}

    interface PositionFiller {
        void fillPosition();
    }

	private HedgingPosition initHedgingPosition(final HedgingPosition hp) {
        final IHedgingPositionDataAccessService hpdas = DataAccessService.getHedgingPositionDataAccessService();
		final Transaction transaction = tradingDataAccessService.getTransactionById(hp.getId());
		long dId = tradingDataAccessService.getOptionalIdFromTransaction(transaction);

		double price = hpdas.getPriceQuote(dId, transaction);
        tradingDataAccessService.computeDPSOnTheGrid(transaction.getOuterEdge());
		String combck = dId + " " + transaction.getId() + " CONTROL: [" + hpdas.getControl() + "]";
		Date valueDate = new Date();
		try {
			valueDate = hp.getValueDate();
		} catch(Exception e) {
			valueDate = transaction.getValueDate();
		}

		String hedgingTransactionId = new String();
		if (!HedgingPositionTypeConst.INI.equals(hp.getType())) {
			hedgingTransactionId = hpdas.getHedgingTransactionIdByTransactionId(transaction.getId());
		}
		String userIni = getUser();
		hp.setIkRtH(userIni);
		switch (hp.getType()) {
			case INI:
                PositionFiller positionFiller = new IniPositionFiller(hp, hpdas, transaction, combck, valueDate);
                positionFiller.fillPosition();

            case CANCEL_TRANSACTION:
                final Date finalValueDate = valueDate;
                PositionFiller positionFiller1 = new PositionFiller() {
                    @Override
                    public void fillPosition() {
                        handleCancelCase(hp, hpdas, transaction, finalValueDate);
                    }
                };
                positionFiller1.fillPosition();

                break;
			case EXT:
                handleExtCase(hp, hpdas, transaction, price, combck, valueDate);
				break;
			case CANCEL_POSITION:
                handleCancelCase(hp, valueDate, hedgingTransactionId);

                break;
		}

		return hp;
 	}

    private void handleCancelCase(HedgingPosition hp, Date valueDate, String hedgingTransactionId) {
        /*********************************** INPUT DEAL DATA *********************/
        hp.setCodetyptkt(20);
        hp.setHedgingTransactionId(hedgingTransactionId);
        /*********************************** INPUT EVENT DATA *********************/
        hp.setValueDate(valueDate);
    }

    private void handleExtCase(HedgingPosition hp, IHedgingPositionDataAccessService hpdas, Transaction transaction, double price, String combck, Date valueDate) {
        TradingOrder evt = hpdas.getTrade(transaction.getId());
        double fxprice = -1d;
        if (evt !=null ){
            price = evt.getPrice().getPrice();
            fxprice = evt.getPrice().getFxPrice();
        }
        if (price > 0) {
            price = price * fxprice;
        }
        /*********************************** INPUT DEAL DATA *********************/
        hp.setBasprx(price / 100);
        hp.setPrxref(price);
        hp.setCodetyptkt(42);
        hp.setQuantity(String.valueOf(evt.getPrice().getQuantity()));
        /*********************************** INPUT EVENT DATA *********************/
        Date issueDate = transaction.getIssueDate();
        Date tradeDate = transaction.getTradeDate();
        if (DateTimeUtils.compareDate(issueDate, tradeDate)) {
            hp.setCreDate(issueDate);
            hp.setDaprx(tradeDate);
            hp.setDatefinthe(valueDate);
        } else {
            hp.setCreDate(issueDate);
            hp.setDaprx(tradeDate);
            hp.setDatefinthe(tradeDate);
            hp.setCombck(combck);
        }
        hp.setValueDate(valueDate);
    }

    private void handleCancelCase(HedgingPosition hp, IHedgingPositionDataAccessService hpdas, Transaction transaction, Date valueDate) {
        /*********************************** INPUT DEAL DATA *********************/
        String hedgingPositionId = hpdas.getHedgingPositionIdByPositionKey(transaction.getPositionKey());

        hp.setCodetyptkt(20);
        /*********************************** INPUT EVENT DATA *********************/
        hp.setValueDate(valueDate);
    }

    private void handleIniCase(HedgingPosition hp, IHedgingPositionDataAccessService hpdas, Transaction transaction, String combck, Date valueDate) {
        String transactionWay = convertWayToTransactionWayCode(transaction);

        int bodCode = 0;
        Integer stock = DataAccessService.getAnalyticalService().getRetrieveStockByActiveGK(transaction.getId(), transactionWay);
        TradingOrder evt = hpdas.getTrade(transaction.getId());
        boolean isStockForbidden = false;
        if (stock == null) {
            isStockForbidden = true;
        }
        if (!isStockForbidden) {
            Book book = DataAccessService.getAnalyticalService().getBookByName(transaction.getBookName());
            bodCode = book.getCode();
        } else {
            Book book = DataAccessService.getAnalyticalService().getBookByName(transaction.getBookName() + "-instock");
            bodCode = Integer.parseInt(book.getPortfolioIdFromRank());
        }
        /*********************************** INPUT DEAL DATA *********************/
        hp.setTransactionWay(transactionWay);
        hp.setCodetyptkt(34);
        hp.setCodtyptra(BigInteger.valueOf(bodCode));
        hp.setQuantity(String.valueOf(evt.getPrice().getQuantity()));
        hp.setBasprx(evt.getPrice().getFxPrice() / 100);
        hp.setPrxref(evt.getPrice().getFxPrice());
        hp.setCombck(combck);
        /*********************************** INPUT EVENT DATA *********************/
        hp.setTransactionId(transaction.getId());
        hp.setValueDate(valueDate);
    }

    private String convertWayToTransactionWayCode(Transaction transaction) {
        String transactionWay = new String();
        switch (transaction.getWay()) {
            case LONG:
                transactionWay = "L";
                break;
            case SHORT:
                transactionWay = "S";
                break;
            default:
                break;
        }
        return transactionWay;
    }

    private String getUser() {
		User user = UserSessionsManager.getInstance().getCurrentUser();
		if (user !=null) {
			return user.getName();
		} else {
			return "autobot";
		}
	}


    private class IniPositionFiller implements PositionFiller {
        private final HedgingPosition hp;
        private final IHedgingPositionDataAccessService hpdas;
        private final Transaction transaction;
        private final String combck;
        private final Date valueDate;

        public IniPositionFiller(HedgingPosition hp, IHedgingPositionDataAccessService hpdas, Transaction transaction, String combck, Date valueDate) {
            this.hp = hp;
            this.hpdas = hpdas;
            this.transaction = transaction;
            this.combck = combck;
            this.valueDate = valueDate;
        }

        @Override
        public void fillPosition() {
            handleIniCase(hp, hpdas, transaction, combck, valueDate);
        }
    }
}
