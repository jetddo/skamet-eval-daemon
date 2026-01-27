package kama.daemon.eval;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.taf.TafData;
import kama.daemon.eval.taf.TafElement;
import kama.daemon.eval.taf.TafElementSet;

abstract public class TafEvaluator {
	
	abstract public List<TafEvaluationData> evaluate(String stnCd, Date stTafTm, Date edTafTm, List<TafElementSet> tafElementSetList, List<MetarElement> metarElementList) throws Exception;
	
	abstract public Map<String, Object> getEvaluationResult(List<TafEvaluationData> tafEvaluationDataList, int scoreFixCount);

	protected List<MetarElement> findMetarElementListBetweenHours(Date tafTm, List<MetarElement> metarElementList, Map<String, Object> stateInfo, Integer hours) {
		
		Date stateStTm = (Date)stateInfo.get("stateStTm");
		Date stateEdTm = (Date)stateInfo.get("stateEdTm");		
		
		List<MetarElement> subMetarElementList = new ArrayList<MetarElement>();
				
		for(int i=0 ; i<metarElementList.size() ; i++) {
			
			Date metarTm = metarElementList.get(i).getMetarTm();
			
			if(metarTm.getTime() >= stateStTm.getTime() && metarTm.getTime() <= stateEdTm.getTime()) {
			
				if(metarTm.getTime() >= tafTm.getTime() - hours * (1000 * 60 * 60) && metarTm.getTime() <= tafTm.getTime() + hours * (1000 * 60 * 60)) {
					subMetarElementList.add(metarElementList.get(i));
				}
			}
		}
		
		return subMetarElementList;
	}
	
	protected TafElement adoptBecmgTafElement(TafElement fcstTafElement, TafElement _becmgTafElement) {
		
		TafElement tafElement = fcstTafElement.makeClone();
		TafElement becmgTafElement = _becmgTafElement.makeClone();
		
		Boolean vrb = becmgTafElement.isVrb();
		Double wdir = becmgTafElement.getWdir();
		Double wspd = becmgTafElement.getWspd();
		Double maxWspd = becmgTafElement.getMaxWspd();
		Double vis = becmgTafElement.getVis();
		String skyCondition = becmgTafElement.getSkyCondition();
		
		List<String> cloudAmountLayer1List = becmgTafElement.getCloudAmountLayer1List();
		List<Double> cloudHeightLayer1List = becmgTafElement.getCloudHeightLayer1List();
		List<Boolean> cbCloudLayer1List = becmgTafElement.getCbCloudLayer1List();
		List<String> cloudAmountLayer2List = becmgTafElement.getCloudAmountLayer2List();
		List<Double> cloudHeightLayer2List = becmgTafElement.getCloudHeightLayer2List();
		List<Boolean> cbCloudLayer2List = becmgTafElement.getCbCloudLayer2List();
		
		boolean cavok = becmgTafElement.isCavok();
		boolean nsw = becmgTafElement.isNsw();
		boolean skc = becmgTafElement.isSkc();
		boolean nsc = becmgTafElement.isNsc();
		
		if(vrb) {
			tafElement.setVrb(vrb);
		}
		
		if(wdir != null) {
			tafElement.setWdir(wdir);
		}
		
		if(wspd != null) {
			tafElement.setWspd(wspd);
		}
		
		if(maxWspd != null) {
			tafElement.setMaxWspd(maxWspd);
		}
		
		if(vis != null) {
			tafElement.setVis(vis);
		}
		
		if(!"".equals(skyCondition)) {
			tafElement.setSkyCondition(skyCondition);
		}
		
		if(cloudAmountLayer1List.size() > 0 || cloudAmountLayer2List.size() > 0) {
			
			tafElement.setCloudAmountLayer1List(cloudAmountLayer1List);
			tafElement.setCloudHeightLayer1List(cloudHeightLayer1List);
			tafElement.setCbCloudLayer1List(cbCloudLayer1List);						
			
			tafElement.setCloudAmountLayer2List(cloudAmountLayer2List);
			tafElement.setCloudHeightLayer2List(cloudHeightLayer2List);
			tafElement.setCbCloudLayer2List(cbCloudLayer2List);
		}
		
		if(cavok) {
			tafElement.setCavok(cavok);
			tafElement.getCloudAmountLayer1List().clear();
			tafElement.getCloudAmountLayer2List().clear();
			tafElement.getCloudHeightLayer1List().clear();
			tafElement.getCloudHeightLayer2List().clear();
			tafElement.getCbCloudLayer1List().clear();
			tafElement.getCbCloudLayer2List().clear();
			tafElement.setVis(9999.0);	
		}
		
		if(nsw) {
			tafElement.setNsw(nsw);
			tafElement.setSkyCondition("");	
		}
		
		if(skc) {
			tafElement.setSkc(skc);
			tafElement.getCloudAmountLayer1List().clear();
			tafElement.getCloudAmountLayer2List().clear();
			tafElement.getCloudHeightLayer1List().clear();
			tafElement.getCloudHeightLayer2List().clear();
			tafElement.getCbCloudLayer1List().clear();
			tafElement.getCbCloudLayer2List().clear();
		}
		
		if(nsc) {
			tafElement.setNsc(nsc);
			tafElement.getCloudAmountLayer1List().clear();
			tafElement.getCloudAmountLayer2List().clear();
			tafElement.getCloudHeightLayer1List().clear();
			tafElement.getCloudHeightLayer2List().clear();
			tafElement.getCbCloudLayer1List().clear();
			tafElement.getCbCloudLayer2List().clear();
		}
		
		return tafElement;
	}
	
	protected Map<String, Object> getStateInfo(List<TafElementSet> tafElementSetList, int tafElementSetIndex, TafData.State state) {
		
		TafElementSet tafElementSet = tafElementSetList.get(tafElementSetIndex);
		
		TafElement stateTafElement = tafElementSet.getStateTafElement(state);
		
		Integer stateIdx = stateTafElement.getStateIdx();
		
		List<TafElement> tafElementList = new ArrayList<TafElement>();
		
		for(int i=0 ; i<tafElementSetList.size() ; i++) {
			
			TafElement _stateTafElement = tafElementSetList.get(i).getStateTafElement(state);
						
			if(_stateTafElement != null && _stateTafElement.getStateIdx() == stateIdx) {
				tafElementList.add(_stateTafElement);
			}
		}
		
		if(tafElementList.size() > 0) {
			
			Map<String, Object> stateInfo = new HashMap<String, Object>();
		
			Date stateStTm = tafElementList.get(0).getTafTm();
			Date stateEdTm = tafElementList.get(tafElementList.size()-1).getTafTm();
			
			Float stateHours = (stateEdTm.getTime() - stateStTm.getTime()) / 1000f / 60f / 60f;
			
			stateInfo.put("stateStTm", stateStTm);	
			stateInfo.put("stateEdTm", stateEdTm);
			stateInfo.put("stateHours", stateHours);
			
			return stateInfo;
		}
		
		return null;
	}
	
	protected boolean checkAvailableEvaluation(TafElementSet tafElementSet, MetarElement metarElement) {
		
		// MetarTm 에 해당하는 TafElementSet 이 없는 경우
		if(tafElementSet == null) {
			return false;
		}
				
		return true;
	}
	
	protected Map<String, Object> getTafElementSetInfobyTm(List<TafElementSet> tafElementSetList, Date tm) {
		
		Map<String, Object> tafElementSetInfo = new HashMap<String, Object>();
		
		for(int i=0 ; i<tafElementSetList.size() ; i++) {
			
			TafElementSet tafElementSet = tafElementSetList.get(i);
			
			if(tafElementSet.getTafTm().getTime() == tm.getTime()) {		
				tafElementSetInfo.put("tafElementSet", tafElementSet);
				tafElementSetInfo.put("tafElementSetIndex", i);
				break;
			}
		}
		
		return tafElementSetInfo;
	}
	
	protected TafEvaluationData getTafEvaluationDatabyTm(List<TafEvaluationData> tafEvaluationDataList, Date tm) {
		
		Map<String, Object> tafElementSetInfo = new HashMap<String, Object>();
		
		for(int i=0 ; i<tafEvaluationDataList.size() ; i++) {
			
			TafEvaluationData tafEvaluationData = tafEvaluationDataList.get(i);
			
			if(tafEvaluationData.getEvaluationTm().getTime() == tm.getTime()) {		
				return tafEvaluationData;
			}
		}
		
		return null;
	}
	
	protected Map<String, Object> getFirstBknOvcCloudInfo(Object element) throws Exception {
		
		String firstCloudAmount = null;
		Double firstCloudHeight = null;
				
		Method getCloudAmountLayer1List = element.getClass().getMethod("getCloudAmountLayer1List");
		Method getCloudAmountLayer2List = element.getClass().getMethod("getCloudAmountLayer2List");
		
		Method getCloudHeightLayer1List = element.getClass().getMethod("getCloudHeightLayer1List");
		Method getCloudHeightLayer2List = element.getClass().getMethod("getCloudHeightLayer2List");
		
		List<String> cloudAmountList = new ArrayList<String>();		
		cloudAmountList.addAll((List<String>)getCloudAmountLayer1List.invoke(element, new Object[]{}));
		cloudAmountList.addAll((List<String>)getCloudAmountLayer2List.invoke(element, new Object[]{}));
		
		List<Double> cloudHeightList = new ArrayList<Double>();
		cloudHeightList.addAll((List<Double>)getCloudHeightLayer1List.invoke(element, new Object[]{}));
		cloudHeightList.addAll((List<Double>)getCloudHeightLayer2List.invoke(element, new Object[]{}));
		
		for(int i=0 ; i<cloudAmountList.size() ; i++) {
			
			if("BKN".equals(cloudAmountList.get(i)) || "OVC".equals(cloudAmountList.get(i))) {
				
				firstCloudAmount = cloudAmountList.get(i);
				firstCloudHeight = cloudHeightList.get(i);					
				
				break;
			}
		}
		
		if(firstCloudAmount != null && firstCloudHeight != null) {
		
			Map<String, Object> firstBknOvcCloudInfo = new HashMap<String, Object>();
			
			firstBknOvcCloudInfo.put("firstCloudAmount", firstCloudAmount);
			firstBknOvcCloudInfo.put("firstCloudHeight", firstCloudHeight);
			
			return firstBknOvcCloudInfo;
			
		} else {
			return null;
		}
	}
	
	protected Map<String, Boolean> getChangedElementInfo(TafElement tafElement) {
		
		Map<String, Boolean> changedElementInfo = new HashMap<String, Boolean>();
		changedElementInfo.put("windDirection", false);
		changedElementInfo.put("windSpeed", false);
		changedElementInfo.put("visibility", false);
		changedElementInfo.put("rainOrClear", false);
		changedElementInfo.put("cloudAmount", false);
		changedElementInfo.put("cloudHeight", false);

		if(tafElement.getWdir() != null || tafElement.isVrb()) {
			changedElementInfo.put("windDirection", true);
		}
		
		if(tafElement.getWspd() != null) {
			changedElementInfo.put("windSpeed", true);
		}
		
		if(tafElement.getVis() != null || tafElement.isCavok()) {
			changedElementInfo.put("visibility", true);
		}
		
		if(!"".equals(tafElement.getSkyCondition()) || tafElement.getSkyCondition() != null || tafElement.isNsw()) {
			changedElementInfo.put("rainOrClear", true);
		}
		
		if(tafElement.getCloudAmountLayer1List().size() > 0 || tafElement.getCloudAmountLayer2List().size() > 0 || tafElement.isCavok() || tafElement.isNsc() || tafElement.isSkc()) {
			changedElementInfo.put("cloudAmount", true);
		}
		
		if(tafElement.getCloudHeightLayer1List().size() > 0 || tafElement.getCloudHeightLayer2List().size() > 0 || tafElement.isCavok() || tafElement.isNsc() || tafElement.isSkc()) {
			changedElementInfo.put("cloudHeight", true);
		}
		
		return changedElementInfo;
	}
}
