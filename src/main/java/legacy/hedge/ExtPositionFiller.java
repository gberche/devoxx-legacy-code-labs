package legacy.hedge;

import legacy.DateTimeUtils;
import legacy.dto.Transaction;
import legacy.service.IHedgingPositionDataAccessService;
import legacy.service.TradingOrder;

import java.util.Date;

/**
*
*/
class ExtPositionFiller implements HedgingPositionManagementImpl.PositionFiller {
    private final HedgingPosition hp;
    private final IHedgingPositionDataAccessService hpdas;
    private final Transaction transaction;
    private final double price;
    private final String combck;
    private final Date finalValueDate2;

    public ExtPositionFiller(HedgingPosition hp, IHedgingPositionDataAccessService hpdas, Transaction transaction, double price, String combck, Date finalValueDate2) {
        this.hp = hp;
        this.hpdas = hpdas;
        this.transaction = transaction;
        this.price = price;
        this.combck = combck;
        this.finalValueDate2 = finalValueDate2;
    }

    @Override
    public void fillPosition() {
        double price1 = price;
        TradingOrder evt = hpdas.getTrade(transaction.getId());
        double fxprice = -1d;
        if (evt !=null ){
            price1 = evt.getPrice().getPrice();
            fxprice = evt.getPrice().getFxPrice();
        }
        if (price1 > 0) {
            price1 = price1 * fxprice;
        }
        /*********************************** INPUT DEAL DATA *********************/
        hp.setBasprx(price1 / 100);
        hp.setPrxref(price1);
        hp.setCodetyptkt(42);
        hp.setQuantity(String.valueOf(evt.getPrice().getQuantity()));
        /*********************************** INPUT EVENT DATA *********************/
        Date issueDate = transaction.getIssueDate();
        Date tradeDate = transaction.getTradeDate();
        if (DateTimeUtils.compareDate(issueDate, tradeDate)) {
            hp.setCreDate(issueDate);
            hp.setDaprx(tradeDate);
            hp.setDatefinthe(finalValueDate2);
        } else {
            hp.setCreDate(issueDate);
            hp.setDaprx(tradeDate);
            hp.setDatefinthe(tradeDate);
            hp.setCombck(combck);
        }
        hp.setValueDate(finalValueDate2);
    }
}
