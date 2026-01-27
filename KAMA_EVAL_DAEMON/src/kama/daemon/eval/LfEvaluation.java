package kama.daemon.eval;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.lf.LfData;
import kama.daemon.eval.lf.LfElementSet;
import kama.daemon.eval.metar.MetarData;
import kama.daemon.eval.metar.MetarElement;

public class LfEvaluation {
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	public LfEvaluation(EvaluationDatabaseUtil evaluationDatabaseUtil) {
		this.evaluationDatabaseUtil = evaluationDatabaseUtil;
	}
	
	private List<LfElementSet> lfElementSetList = null;
	private List<MetarElement> metarElementList = null;
	
	public List<LfElementSet> getLfElementSetList() {
		return this.lfElementSetList;
	}
	
	public Map<String, Object> evaluateLf(String stnCd, LfData lfData, List<MetarData> metarDataList, int scoreFixCount, int evalVer) throws Exception {
		
		this.lfElementSetList = new ArrayList<LfElementSet>();
		this.metarElementList = new ArrayList<MetarElement>();
			
		Collections.sort(metarDataList, new Comparator<MetarData>(){

			@Override
			public int compare(MetarData o1, MetarData o2) {
				return (int)(o1.getAnncTm().getTime() - o2.getAnncTm().getTime());
			}
		});
		
		this.lfElementSetList = lfData.getLfElementSetList();
		
		for(int i=0 ; i<metarDataList.size() ; i++) {
			
			if(metarDataList.get(i).isAvailable()) {
				this.metarElementList.add(metarDataList.get(i).getMetarElement());	
			}
		}
		
		Map<String, Object> lfEvaluationDataMap = new HashMap<String, Object>();
		
		LfEvaluator lfEvaluator = null;
			
		if(evalVer == 1) {
			lfEvaluator = new LfEvaluatorVer1();
		} else if(evalVer == 2) {
			lfEvaluator = new LfEvaluatorVer2();
		}
		
		List<LfEvaluationData> lfEvaluationDataList = lfEvaluator.evaluate(stnCd, lfData.getStLfTm(), lfData.getEdLfTm(), lfElementSetList, metarElementList);
		Map<String, Object> lfEvaluationResult = lfEvaluator.getEvaluationResult(lfEvaluationDataList, scoreFixCount);
		
		lfEvaluationDataMap.put("lfEvaluationDataList", lfEvaluationDataList);
		lfEvaluationDataMap.put("lfEvaluationResult", lfEvaluationResult);
		
		return lfEvaluationDataMap;
	}
}