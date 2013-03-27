import legacy.error.CheckResult;
import legacy.hedge.HedgingPosition;
import legacy.hedge.HedgingPositionManagementImpl;
import org.junit.Test;

public class PlaceHolderTest {

	@Test
	public void instanciateClass() {
        HedgingPositionManagementImpl hedgingPositionManagement = new HedgingPositionManagementImpl();

        HedgingPosition hp = new HedgingPosition();
        hedgingPositionManagement.initAndSendHedgingPosition(hp);
    }

	@Test
	public void sendHedgingPositionDoesNotReturn() {
        HedgingPositionManagementImpl hedgingPositionManagement = new HedgingPositionManagementImpl();

        HedgingPosition hp = new HedgingPosition();
        CheckResult<HedgingPosition> result = hedgingPositionManagement.initAndSendHedgingPosition(hp);

    }
}
