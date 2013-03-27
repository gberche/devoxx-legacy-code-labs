import legacy.error.CheckResult;
import legacy.hedge.HedgingPosition;
import legacy.hedge.HedgingPositionManagementImpl;
import org.junit.Test;

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


        HedgingPosition hp = new HedgingPosition();
        CheckResult<HedgingPosition> result = hedgingPositionManagement.initAndSendHedgingPosition(hp);
        HedgingPosition hedgingPosition = result.getResult();
    }
}
