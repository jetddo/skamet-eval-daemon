package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.eval.df.DfElementSet;
import kama.daemon.eval.metar.MetarElement;

abstract public class DfEvaluator {
	
	abstract public List<DfEvaluationData> evaluate(String stnCd, Date stDfTm, Date edDfTm, List<DfElementSet> dfElementSetList, List<MetarElement> metarElementList, List<Map<String, Object>> metarOriginInfoList) throws Exception;
	
	abstract public Map<String, Object> getEvaluationResult(List<DfEvaluationData> dfEvaluationDataList, int scoreFixCount);

	protected List<MetarElement> findMetarElementListBetweenHours(Date dfTm, List<MetarElement> metarElementList, Map<String, Object> stateInfo, Integer hours) {
		
		Date stateStTm = (Date)stateInfo.get("stateStTm");
		Date stateEdTm = (Date)stateInfo.get("stateEdTm");		
		
		List<MetarElement> subMetarElementList = new ArrayList<MetarElement>();
				
		for(int i=0 ; i<metarElementList.size() ; i++) {
			
			Date metarTm = metarElementList.get(i).getMetarTm();
			
			if(metarTm.getTime() >= stateStTm.getTime() && metarTm.getTime() <= stateEdTm.getTime()) {
			
				if(metarTm.getTime() >= dfTm.getTime() - hours * (1000 * 60 * 60) && metarTm.getTime() <= dfTm.getTime() + hours * (1000 * 60 * 60)) {
					subMetarElementList.add(metarElementList.get(i));
				}
			}
		}
		
		return subMetarElementList;
	}
	
	
	protected boolean checkAvailableEvaluation(DfElementSet dfElementSet, MetarElement metarElement) {
		// 여긴 어떻게 처리??
		// MetarTm 에 해당하는 DfElementSet 이 없는 경우
		if(dfElementSet == null) {
//			System.out.println("MetarTm 에 해당하는 DfElementSet 이 없음");
//			System.out.println(metarElement);
			return false;
		}
				
		return true;
	}
	
	protected Map<String, Object> getDfElementSetInfobyTm(List<DfElementSet> dfElementSetList, Date tm) {
		
		Map<String, Object> dfElementSetInfo = new HashMap<String, Object>();
		
		for(int i=0 ; i<dfElementSetList.size() ; i++) {
			
			DfElementSet dfElementSet = dfElementSetList.get(i);
			
			if(dfElementSet.getDfTm().getTime() == tm.getTime()) {		
				dfElementSetInfo.put("dfElementSet", dfElementSet);
				dfElementSetInfo.put("dfElementSetIndex", i);
				break;
			}
		}
		
		return dfElementSetInfo;
	}
	
	protected Map<String, Object> getMetarOriginInfobyTm(List<Map<String, Object>> metarOriginInfoList, Date tm) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		try {
			
			for(int i=0 ; i<metarOriginInfoList.size() ; i++) {
				
				Map<String, Object> metarOriginInfo = metarOriginInfoList.get(i);
				
				Date metarTm = sdf.parse(metarOriginInfo.get("tm").toString());
				
				Object qnh = metarOriginInfo.get("aqnh");
				
				if(tm.getTime() == metarTm.getTime() && qnh != null) {
					return metarOriginInfoList.get(i);
				}
			}
			
		} catch (Exception e) {
			return null;
		}
		
		return null;
	}
}
