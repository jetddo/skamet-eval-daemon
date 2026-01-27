package kama.daemon.eval;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.eval.lf.LfData;
import kama.daemon.eval.lf.LfElement;
import kama.daemon.eval.lf.LfElementSet;
import kama.daemon.eval.metar.MetarElement;

abstract public class LfEvaluator {
	
	abstract public List<LfEvaluationData> evaluate(String stnCd, Date stLfTm, Date edLfTm, List<LfElementSet> lfElementSetList, List<MetarElement> metarElementList) throws Exception;
	
	abstract public Map<String, Object> getEvaluationResult(List<LfEvaluationData> lfEvaluationDataList, int scoreFixCount);

	protected List<MetarElement> findMetarElementListBetweenHours(Date lfTm, List<MetarElement> metarElementList, Map<String, Object> stateInfo, Integer hours) {
		
		Date stateStTm = (Date)stateInfo.get("stateStTm");
		Date stateEdTm = (Date)stateInfo.get("stateEdTm");		
		
		List<MetarElement> subMetarElementList = new ArrayList<MetarElement>();
				
		for(int i=0 ; i<metarElementList.size() ; i++) {
			
			Date metarTm = metarElementList.get(i).getMetarTm();
			
			if(metarTm.getTime() >= stateStTm.getTime() && metarTm.getTime() <= stateEdTm.getTime()) {
			
				if(metarTm.getTime() >= lfTm.getTime() - hours * (1000 * 60 * 60) && metarTm.getTime() <= lfTm.getTime() + hours * (1000 * 60 * 60)) {
					subMetarElementList.add(metarElementList.get(i));
				}
			}
		}
		
		return subMetarElementList;
	}
	
	protected LfElement adoptBecmgLfElement(LfElement fcstLfElement, LfElement _becmgLfElement) {
		
		LfElement lfElement = fcstLfElement.makeClone();
		LfElement becmgLfElement = _becmgLfElement.makeClone();
		
		Boolean vrb = becmgLfElement.isVrb();
		Double wdir = becmgLfElement.getWdir();
		Double wspd = becmgLfElement.getWspd();
		Double maxWspd = becmgLfElement.getMaxWspd();
		Double vis = becmgLfElement.getVis();
		String skyCondition = becmgLfElement.getSkyCondition();
		
		List<String> cloudAmountLayer1List = becmgLfElement.getCloudAmountLayer1List();
		List<Double> cloudHeightLayer1List = becmgLfElement.getCloudHeightLayer1List();
		List<Boolean> cbCloudLayer1List = becmgLfElement.getCbCloudLayer1List();
		List<String> cloudAmountLayer2List = becmgLfElement.getCloudAmountLayer2List();
		List<Double> cloudHeightLayer2List = becmgLfElement.getCloudHeightLayer2List();
		List<Boolean> cbCloudLayer2List = becmgLfElement.getCbCloudLayer2List();
		
		boolean cavok = becmgLfElement.isCavok();
		boolean nsw = becmgLfElement.isNsw();
		boolean skc = becmgLfElement.isSkc();
		boolean nsc = becmgLfElement.isNsc();
		
		if(vrb) {
			lfElement.setVrb(vrb);
		}
		
		if(wdir != null) {
			lfElement.setWdir(wdir);
		}
		
		if(wspd != null) {
			lfElement.setWspd(wspd);
		}
		
		if(maxWspd != null) {
			lfElement.setMaxWspd(maxWspd);
		}
		
		if(vis != null) {
			lfElement.setVis(vis);
		}
		
		if(!"".equals(skyCondition)) {
			lfElement.setSkyCondition(skyCondition);
		}
		
		if(cloudAmountLayer1List.size() > 0 || cloudAmountLayer2List.size() > 0) {
			
			lfElement.setCloudAmountLayer1List(cloudAmountLayer1List);
			lfElement.setCloudHeightLayer1List(cloudHeightLayer1List);
			lfElement.setCbCloudLayer1List(cbCloudLayer1List);						
			
			lfElement.setCloudAmountLayer2List(cloudAmountLayer2List);
			lfElement.setCloudHeightLayer2List(cloudHeightLayer2List);
			lfElement.setCbCloudLayer2List(cbCloudLayer2List);
		}
		
		if(cavok) {
			lfElement.setCavok(cavok);
			lfElement.getCloudAmountLayer1List().clear();
			lfElement.getCloudAmountLayer2List().clear();
			lfElement.getCloudHeightLayer1List().clear();
			lfElement.getCloudHeightLayer2List().clear();
			lfElement.getCbCloudLayer1List().clear();
			lfElement.getCbCloudLayer2List().clear();
			lfElement.setVis(9999.0);	
		}
		
		if(nsw) {
			lfElement.setNsw(nsw);
			lfElement.setSkyCondition("");	
		}
		
		if(skc) {
			lfElement.setSkc(skc);
			lfElement.getCloudAmountLayer1List().clear();
			lfElement.getCloudAmountLayer2List().clear();
			lfElement.getCloudHeightLayer1List().clear();
			lfElement.getCloudHeightLayer2List().clear();
			lfElement.getCbCloudLayer1List().clear();
			lfElement.getCbCloudLayer2List().clear();
		}
		
		if(nsc) {
			lfElement.setNsc(nsc);
			lfElement.getCloudAmountLayer1List().clear();
			lfElement.getCloudAmountLayer2List().clear();
			lfElement.getCloudHeightLayer1List().clear();
			lfElement.getCloudHeightLayer2List().clear();
			lfElement.getCbCloudLayer1List().clear();
			lfElement.getCbCloudLayer2List().clear();
		}
		
		return lfElement;
	}
	
	protected Map<String, Object> getStateInfo(List<LfElementSet> lfElementSetList, int lfElementSetIndex, LfData.State state) {
		
		LfElementSet lfElementSet = lfElementSetList.get(lfElementSetIndex);
		
		LfElement stateLfElement = lfElementSet.getStateLfElement(state);
		
		Integer stateIdx = stateLfElement.getStateIdx();
		
		List<LfElement> lfElementList = new ArrayList<LfElement>();
		
		for(int i=0 ; i<lfElementSetList.size() ; i++) {
			
			LfElement _stateLfElement = lfElementSetList.get(i).getStateLfElement(state);
						
			if(_stateLfElement != null && _stateLfElement.getStateIdx() == stateIdx) {
				lfElementList.add(_stateLfElement);
			}
		}
		
		if(lfElementList.size() > 0) {
			
			Map<String, Object> stateInfo = new HashMap<String, Object>();
		
			Date stateStTm = lfElementList.get(0).getLfTm();
			Date stateEdTm = lfElementList.get(lfElementList.size()-1).getLfTm();
			
			Float stateHours = (stateEdTm.getTime() - stateStTm.getTime()) / 1000f / 60f / 60f;
			
			stateInfo.put("stateStTm", stateStTm);	
			stateInfo.put("stateEdTm", stateEdTm);
			stateInfo.put("stateHours", stateHours);
			
			return stateInfo;
		}
		
		return null;
	}
	
	protected boolean checkAvailableEvaluation(LfElementSet lfElementSet, MetarElement metarElement) {
		
		// MetarTm 에 해당하는 LfElementSet 이 없는 경우
		if(lfElementSet == null) {
			System.out.println("MetarTm 에 해당하는 LfElementSet 이 없음");
			System.out.println(metarElement);
			return false;
		}
				
		return true;
	}
	
	protected Map<String, Object> getLfElementSetInfobyTm(List<LfElementSet> lfElementSetList, Date tm) {
		
		Map<String, Object> lfElementSetInfo = new HashMap<String, Object>();
		
		for(int i=0 ; i<lfElementSetList.size() ; i++) {
			
			LfElementSet lfElementSet = lfElementSetList.get(i);
			
			if(lfElementSet.getLfTm().getTime() == tm.getTime()) {		
				lfElementSetInfo.put("lfElementSet", lfElementSet);
				lfElementSetInfo.put("lfElementSetIndex", i);
				break;
			}
		}
		
		return lfElementSetInfo;
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
	
	protected Map<String, Boolean> getChangedElementInfo(LfElement lfElement) {
		
		Map<String, Boolean> changedElementInfo = new HashMap<String, Boolean>();
		changedElementInfo.put("windDirection", false);
		changedElementInfo.put("windSpeed", false);
		changedElementInfo.put("visibility", false);
		changedElementInfo.put("rainOrClear", false);
		changedElementInfo.put("cloudAmount", false);
		changedElementInfo.put("cloudHeight", false);

		if(lfElement.getWdir() != null || lfElement.isVrb()) {
			changedElementInfo.put("windDirection", true);
		}
		
		if(lfElement.getWspd() != null) {
			changedElementInfo.put("windSpeed", true);
		}
		
		if(lfElement.getVis() != null || lfElement.isCavok()) {
			changedElementInfo.put("visibility", true);
		}
		
		if(!"".equals(lfElement.getSkyCondition()) || lfElement.getSkyCondition() != null || lfElement.isNsw()) {
			changedElementInfo.put("rainOrClear", true);
		}
		
		if(lfElement.getCloudAmountLayer1List().size() > 0 || lfElement.getCloudAmountLayer2List().size() > 0 || lfElement.isCavok() || lfElement.isNsc() || lfElement.isSkc()) {
			changedElementInfo.put("cloudAmount", true);
		}
		
		if(lfElement.getCloudHeightLayer1List().size() > 0 || lfElement.getCloudHeightLayer2List().size() > 0 || lfElement.isCavok() || lfElement.isNsc() || lfElement.isSkc()) {
			changedElementInfo.put("cloudHeight", true);
		}
		
		return changedElementInfo;
	}
}
