package legacy.hedge;

import legacy.dto.Transaction;
import legacy.service.IHedgingPositionDataAccessService;

import java.util.Date;

/**
*
*/
class CancelTransactionPositionFiller implements HedgingPositionManagementImpl.PositionFiller {
    private final HedgingPosition hp;
    private final IHedgingPositionDataAccessService hpdas;
    private final Transaction transaction;
    private final Date finalValueDate;

    public CancelTransactionPositionFiller(HedgingPosition hp, IHedgingPositionDataAccessService hpdas, Transaction transaction, Date finalValueDate) {
        this.hp = hp;
        this.hpdas = hpdas;
        this.transaction = transaction;
        this.finalValueDate = finalValueDate;
    }

    @Override
    public void fillPosition() {
        /*********************************** INPUT DEAL DATA *********************/
        String hedgingPositionId = hpdas.getHedgingPositionIdByPositionKey(transaction.getPositionKey());

        hp.setCodetyptkt(20);
        /*********************************** INPUT EVENT DATA *********************/
        hp.setValueDate(finalValueDate);
    }
}
