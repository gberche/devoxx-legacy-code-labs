import junit.framework.Assert;
import legacy.dto.DateRange;
import legacy.dto.Transaction;
import legacy.error.CheckResult;
import legacy.hedge.HedginPositionMgrInvoker;
import legacy.hedge.HedgingPosition;
import legacy.service.ITradingDataAccessService;
import legacy.service.TransactionWay;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HedgingPositionManagementImplTest {

	@Test
	public void instanciateClass() {
        legacy.hedge.HedgingPositionManagementImpl hedgingPositionManagement = new legacy.hedge.HedgingPositionManagementImpl();
    }

	@Test
	public void sendHedgingPositionWithDefaultPositionReturned() {
        legacy.hedge.HedgingPositionManagementImpl hedgingPositionManagement = new legacy.hedge.HedgingPositionManagementImpl();
        hedgingPositionManagement.setHedginPositionMgrInvoker(new HedginPositionMgrInvoker() {
            @Override
            public CheckResult<HedgingPosition> invoke(HedgingPosition hp) {
                Assert.assertEquals("HedgingPosition{basprx=0.0, transactionId=0, type=INI, status=null, valueDate=null, noticePeriodEndDate=null, combck='0 0 CONTROL: [0x0x0x01h]', codetyptkt=20, transactionWay='S', errorLevel=null, hedgeMsg='null', storageAction=null, prxref=0.0, daprx=null, quantity='0.0', datefinthe=null, codtyptra=23, msgdev='null', msgerr='null', niverr=null, msgusr='null', ikRtH='autobot', hedgingTransactionId='null'}", hp.toString());
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
        dateRange.setUpdateDate(new Date(2013, 9, 9));
        transaction.setDateRange(dateRange);
        transaction.setOuterEdge(new Long(0));
        when(dataAccessService.getTransactionById(0)).thenReturn(transaction);

        hedgingPositionManagement.setTradingDataAccessService(dataAccessService);



        HedgingPosition hp = new HedgingPosition();
        CheckResult<HedgingPosition> result = hedgingPositionManagement.initAndSendHedgingPosition(hp);
        HedgingPosition hedgingPosition = result.getResult();
        assertEquals("HedgingPosition{basprx=100.0, transactionId=0, type=INI, status=HEDGED, valueDate=null, noticePeriodEndDate=null, combck='null', codetyptkt=0, transactionWay='null', errorLevel=null, hedgeMsg='null', storageAction=CREATE, prxref=0.0, daprx=null, quantity='null', datefinthe=null, codtyptra=42, msgdev='null', msgerr='null', niverr=null, msgusr='null', ikRtH='null', hedgingTransactionId='null'}", hedgingPosition.toString());
    }
}
