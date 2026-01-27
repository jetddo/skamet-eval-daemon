package kama.daemon.eval;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.df.DfData;
import kama.daemon.eval.df.DfElementSet;
import kama.daemon.eval.metar.MetarData;
import kama.daemon.eval.metar.MetarElement;

public class DfEvaluation {
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	public DfEvaluation(EvaluationDatabaseUtil evaluationDatabaseUtil) {
		this.evaluationDatabaseUtil = evaluationDatabaseUtil;
	}
	
	private List<DfElementSet> dfElementSetList = null;
	private List<MetarElement> metarElementList = null;
	private List<Map<String, Object>> metarOriginInfoList = null;
	
	public List<DfElementSet> getDfElementSetList() {
		return this.dfElementSetList;
	}
	
	public Map<String, Object> evaluateDf(String stnCd, DfData dfData, List<MetarData> metarDataList, List<Map<String, Object>> metarOriginInfoList, int scoreFixCount, int evalVer) throws Exception {
		
		this.dfElementSetList = new ArrayList<DfElementSet>();
		this.metarElementList = new ArrayList<MetarElement>();
		this.metarOriginInfoList = metarOriginInfoList;
			
		Collections.sort(metarDataList, new Comparator<MetarData>(){

			@Override
			public int compare(MetarData o1, MetarData o2) {
				return (int)(o1.getAnncTm().getTime() - o2.getAnncTm().getTime());
			}
		});
		
		this.dfElementSetList = dfData.getDfElementSetList();
		
		for(int i=0 ; i<metarDataList.size() ; i++) {
			
			if(metarDataList.get(i).isAvailable()) {
				this.metarElementList.add(metarDataList.get(i).getMetarElement());	
			}
		}
		
		Map<String, Object> dfEvaluationDataMap = new HashMap<String, Object>();
		
		DfEvaluator dfEvaluator = null;
			
		if(evalVer == 1) {
			dfEvaluator = new DfEvaluatorVer1(evaluationDatabaseUtil);
		}
		
		List<DfEvaluationData> dfEvaluationDataList = dfEvaluator.evaluate(stnCd, dfData.getStDfTm(), dfData.getEdDfTm(), dfElementSetList, metarElementList, metarOriginInfoList);
		Map<String, Object> dfEvaluationResult = dfEvaluator.getEvaluationResult(dfEvaluationDataList, scoreFixCount);
		
		dfEvaluationDataMap.put("dfEvaluationDataList", dfEvaluationDataList);
		dfEvaluationDataMap.put("dfEvaluationResult", dfEvaluationResult);
		
		return dfEvaluationDataMap;
	}
}