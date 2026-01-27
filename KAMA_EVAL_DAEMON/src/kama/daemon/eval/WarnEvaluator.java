package kama.daemon.eval;

import java.util.Map;

import kama.daemon.eval.warn.WarnData;

abstract public class WarnEvaluator {
	
	abstract public WarnEvaluationData evaluate(WarnData warnData) throws Exception;
	
	abstract public Map<String, Object> getEvaluationResult(WarnEvaluationData warnEvaluationDataList, int scoreFixCount);
}
