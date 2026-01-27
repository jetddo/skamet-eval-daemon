package kama.daemon.eval;
import java.util.HashMap;
import java.util.Map;

import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.warn.WarnData;

public class WarnEvaluation {
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	public WarnEvaluation(EvaluationDatabaseUtil evaluationDatabaseUtil) {
		this.evaluationDatabaseUtil = evaluationDatabaseUtil;
	}
		
	public Map<String, Object> evaluateWarn(WarnData warnData, int scoreFixCount, int evalVer) throws Exception {
		
		Map<String, Object> warnEvaluationDataMap = new HashMap<String, Object>();
		
		WarnEvaluator warnEvaluator = null;
			
		if(evalVer == 1) {
			warnEvaluator = new WarnEvaluatorVer1(this.evaluationDatabaseUtil);
		} else if(evalVer == 2) {
			warnEvaluator = new WarnEvaluatorVer2(this.evaluationDatabaseUtil);
		}
		
		WarnEvaluationData warnEvaluationData = warnEvaluator.evaluate(warnData);
		Map<String, Object> warnEvaluationResult = warnEvaluator.getEvaluationResult(warnEvaluationData, scoreFixCount);
		
		warnEvaluationDataMap.put("warnEvaluationData", warnEvaluationData);
		warnEvaluationDataMap.put("warnEvaluationResult", warnEvaluationResult);
		
		return warnEvaluationDataMap;
	}
}