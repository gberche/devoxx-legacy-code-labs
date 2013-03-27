package legacy.hedge;

import legacy.dto.Book;
import legacy.dto.Transaction;
import legacy.service.DataAccessService;
import legacy.service.IHedgingPositionDataAccessService;
import legacy.service.TradingOrder;

import java.math.BigInteger;
import java.util.Date;

/**
*
*/
class IniPositionFiller implements HedgingPositionManagementImpl.PositionFiller {
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

    @Override
    public void fillPosition() {
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
}
