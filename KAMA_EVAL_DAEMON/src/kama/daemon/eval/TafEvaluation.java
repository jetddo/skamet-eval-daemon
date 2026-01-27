package kama.daemon.eval;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Test;

import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.metar.MetarData;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.taf.TafData;
import kama.daemon.eval.taf.TafElementSet;

public class TafEvaluation {
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	public TafEvaluation(EvaluationDatabaseUtil evaluationDatabaseUtil) {
		this.evaluationDatabaseUtil = evaluationDatabaseUtil;
	}
	
	private List<TafElementSet> tafElementSetList = null;
	private List<MetarElement> metarElementList = null;
	
	public List<TafElementSet> getTafElementSetList() {
		return this.tafElementSetList;
	}
	
	public Map<String, Object> evaluateTaf(String stnCd, TafData tafData, List<MetarData> metarDataList, int scoreFixCount, int version, int becmgType) throws Exception {
		
		this.tafElementSetList = new ArrayList<TafElementSet>();
		this.metarElementList = new ArrayList<MetarElement>();
			
		Collections.sort(metarDataList, new Comparator<MetarData>(){

			@Override
			public int compare(MetarData o1, MetarData o2) {
				return (int)(o1.getAnncTm().getTime() - o2.getAnncTm().getTime());
			}
		});
		
		this.tafElementSetList = tafData.getTafElementSetList();
		
		for(int i=0 ; i<metarDataList.size() ; i++) {
			
			if(metarDataList.get(i).isAvailable()) {
				this.metarElementList.add(metarDataList.get(i).getMetarElement());	
			}
		}
		
		Map<String, Object> tafEvaluationDataMap = new HashMap<String, Object>();
		
		TafEvaluator tafEvaluator = null;
			
		if(version == 1) {
			tafEvaluator = new TafEvaluatorVer1();
		} else if(version == 2) {
			tafEvaluator = new TafEvaluatorVer2(this.evaluationDatabaseUtil, becmgType);
		}
		
		List<TafEvaluationData> tafEvaluationDataList = tafEvaluator.evaluate(stnCd, tafData.getStTafTm(), tafData.getEdTafTm(), tafElementSetList, metarElementList);
		Map<String, Object> tafEvaluationResult = tafEvaluator.getEvaluationResult(tafEvaluationDataList, scoreFixCount);
		
		tafEvaluationDataMap.put("tafEvaluationDataList", tafEvaluationDataList);
		tafEvaluationDataMap.put("tafEvaluationResult", tafEvaluationResult);
		
		return tafEvaluationDataMap;
	}
}