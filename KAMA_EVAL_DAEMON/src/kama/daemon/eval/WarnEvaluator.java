package kama.daemon.eval;

import java.util.Map;

import kama.daemon.eval.warn.WarnData;

abstract public class WarnEvaluator {
	
	abstract public WarnEvaluationData evaluate(WarnData warnData) throws Exception;
	
    // 신규 선행시간 평가체계
    public WarnEvaluationLeadTimeData evaluateLeadTime(WarnData warnData) throws Exception {
        throw new UnsupportedOperationException("Lead time evaluation is not supported.");
    }
	
	abstract public Map<String, Object> getEvaluationResult(WarnEvaluationData warnEvaluationData, int scoreFixCount);
}
