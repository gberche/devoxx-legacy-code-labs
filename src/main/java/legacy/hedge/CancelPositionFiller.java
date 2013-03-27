package legacy.hedge;

import java.util.Date;

/**
*
*/
class CancelPositionFiller implements HedgingPositionManagementImpl.PositionFiller {
    private final HedgingPosition hp;
    private final Date finalValueDate1;
    private final String finalHedgingTransactionId;

    public CancelPositionFiller(HedgingPosition hp, Date finalValueDate1, String finalHedgingTransactionId) {
        this.hp = hp;
        this.finalValueDate1 = finalValueDate1;
        this.finalHedgingTransactionId = finalHedgingTransactionId;
    }

    @Override
    public void fillPosition() {
        /*********************************** INPUT DEAL DATA *********************/
        hp.setCodetyptkt(20);
        hp.setHedgingTransactionId(finalHedgingTransactionId);
        /*********************************** INPUT EVENT DATA *********************/
        hp.setValueDate(finalValueDate1);
    }
}
