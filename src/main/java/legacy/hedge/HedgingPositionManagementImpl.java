package legacy.hedge;

import legacy.dto.Modif;
import legacy.security.User;
import legacy.error.ARPSystemException;
import legacy.error.CheckResult;
import legacy.security.UserSessionsManager;
import legacy.service.DataAccessService;
import legacy.service.IHedgingPositionDataAccessService;
import legacy.service.ITradingDataAccessService;
import legacy.service.ITransactionManagerService;
import legacy.dto.Transaction;
import legacy.persistence.StorageActionEnum;
import org.apache.commons.lang3.SerializationUtils;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HedgingPositionManagementImpl implements IHedgingPositionManagement {

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
            hp.setHedgeMsg(hp.getErrorLevel().createHMsgFromError());
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


    private CheckResult<HedgingPosition> hedgePositionBySendTo3rdParty(HedgingPosition hp) {
        CheckResult<HedgingPosition> result = hedginPositionMgrInvoker.invoke(hp);
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

		final double price = hpdas.getPriceQuote(dId, transaction);
        tradingDataAccessService.computeDPSOnTheGrid(transaction.getOuterEdge());
		final String combck = dId + " " + transaction.getId() + " CONTROL: [" + hpdas.getControl() + "]";
		Date valueDate;
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
        PositionFiller positionFiller;
        switch (hp.getType()) {
            case INI:
                positionFiller = new IniPositionFiller(hp, hpdas, transaction, combck, valueDate);
                positionFiller.fillPosition();

            case CANCEL_TRANSACTION:
                final Date finalValueDate = valueDate;
                positionFiller = new CancelTransactionPositionFiller(hp, hpdas, transaction, finalValueDate);
                positionFiller.fillPosition();

                break;
			case EXT:
                final Date finalValueDate2 = valueDate;
                positionFiller = new ExtPositionFiller(hp, hpdas, transaction, price, combck, finalValueDate2);
                positionFiller.fillPosition();
				break;
			case CANCEL_POSITION:
                final Date finalValueDate1 = valueDate;
                final String finalHedgingTransactionId = hedgingTransactionId;
                positionFiller = new CancelPositionFiller(hp, finalValueDate1, finalHedgingTransactionId);
                positionFiller.fillPosition();

                break;
		}

		return hp;
 	}



    private String getUser() {
		User user = UserSessionsManager.getInstance().getCurrentUser();
		if (user !=null) {
			return user.getName();
		} else {
			return "autobot";
		}
	}

    public void setHedginPositionMgrInvoker(HedginPositionMgrInvoker hedginPositionMgrInvoker) {
        this.hedginPositionMgrInvoker = hedginPositionMgrInvoker;
    }

    public void setTradingDataAccessService(ITradingDataAccessService tradingDataAccessService) {
        this.tradingDataAccessService = tradingDataAccessService;
    }

}
