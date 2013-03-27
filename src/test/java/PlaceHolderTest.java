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
	public void sendHedgingPositionWithoutResultReturned() {
        HedgingPositionManagementImpl hedgingPositionManagement = new HedgingPositionManagementImpl();
        hedgingPositionManagement.setHedginPositionMgrInvoker(new HedgingPositionManagementImpl.HedginPositionMgrInvoker() {
            @Override
            public CheckResult<HedgingPosition> invoke(HedgingPosition hp) {
                return new CheckResult<HedgingPosition>();
            }
        });


        HedgingPosition hp = new HedgingPosition();
        CheckResult<HedgingPosition> result = hedgingPositionManagement.initAndSendHedgingPosition(hp);

    }
}
