package legacy.hedge;

import legacy.error.CheckResult;

/**
*
*/
public interface HedginPositionMgrInvoker {
    CheckResult<HedgingPosition> invoke(HedgingPosition hp);
}
