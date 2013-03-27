import legacy.dto.DateRange;
import legacy.dto.Transaction;
import legacy.error.CheckResult;
import legacy.hedge.HedgingPosition;
import legacy.hedge.HedgingPositionManagementImpl;
import legacy.service.ITradingDataAccessService;
import legacy.service.TransactionWay;
import org.junit.Test;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlaceHolderTest {

	@Test
	public void instanciateClass() {
        HedgingPositionManagementImpl hedgingPositionManagement = new HedgingPositionManagementImpl();
    }

	@Test
	public void sendHedgingPositionWithDefaultPositionReturned() {
        HedgingPositionManagementImpl hedgingPositionManagement = new HedgingPositionManagementImpl();
        hedgingPositionManagement.setHedginPositionMgrInvoker(new HedgingPositionManagementImpl.HedginPositionMgrInvoker() {
            @Override
            public CheckResult<HedgingPosition> invoke(HedgingPosition hp) {
                CheckResult<HedgingPosition> result = new CheckResult<HedgingPosition>();
                result.setResult(new HedgingPosition());
                return result;
            }
        });

        //getting transaction:0
        //returned transaction:Transaction{bookName='Golgoth Supra Book 2000', valueDate=null, outerEdge=0, way=SHORT, positionKey='POS_0_cheval', issueDate=Sun Oct 09 00:00:00 CEST 3910, tradeDate=null, dateRange=legacy.dto.DateRange@aa348fd3}
        ITradingDataAccessService dataAccessService = mock(ITradingDataAccessService.class);
        Transaction transaction = new Transaction();
        transaction.setBookName("Golgoth Supra Book 2000");
        transaction.setWay(TransactionWay.SHORT);
        transaction.setPositionKey("POS_0_cheval");
        transaction.setIssueDate(new Date(2010, 9, 9));
        DateRange dateRange = new DateRange();
        dateRange.setCreDate(new Date());
        dateRange.setUpdateVersion(93);
        dateRange.setUpdateDate(new Date(2013,9,9));
        transaction.setDateRange(dateRange);
        transaction.setOuterEdge(new Long(0));
        when(dataAccessService.getTransactionById(0)).thenReturn(transaction);

        hedgingPositionManagement.setTradingDataAccessService(dataAccessService);



        HedgingPosition hp = new HedgingPosition();
        CheckResult<HedgingPosition> result = hedgingPositionManagement.initAndSendHedgingPosition(hp);
        HedgingPosition hedgingPosition = result.getResult();
    }
}
