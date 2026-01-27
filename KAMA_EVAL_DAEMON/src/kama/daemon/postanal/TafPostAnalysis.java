package kama.daemon.postanal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.eval.taf.TafData;
import kama.daemon.eval.taf.TafElement;
import kama.daemon.eval.taf.TafElementSet;

public class TafPostAnalysis {
	
	public List<Map<String, Object>> analysis(String stnCd, Date stTafTm, Date edTafTm, List<TafElementSet> tafElementSetList, List<Map<String, Object>> metarObsDataList) throws Exception {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		List<TafPostAnalData> tafPostAnalDataList = new ArrayList<TafPostAnalData>();
		
		// 평가는 Metar 를 기준으로 수행한다
		
		for(int i=0 ; i<metarObsDataList.size() ; i++) {
			
			Map<String, Object> metarObsData = metarObsDataList.get(i);
			
			Date metarTm = null;
			
			try {
				
				metarTm = sdf.parse(metarObsData.get("tm").toString());
				
			} catch (Exception e) {}
			
			if(metarTm == null) {
				continue;
			}
			
			Map<String, Object> tafElementSetInfo = this.getTafElementSetInfobyTm(tafElementSetList, metarTm);			
			TafElementSet tafElementSet  = (TafElementSet)tafElementSetInfo.get("tafElementSet");
			
			TafPostAnalData tafPostAnalData = new TafPostAnalData();
			
			tafPostAnalData.setPostAnalTm(metarTm);
			tafPostAnalData.setMetarTm(metarTm);
			
			if(tafElementSet == null) {
				tafPostAnalData.setAvailable(false);
				continue;
			}
			
			tafPostAnalData.setTafTm(tafElementSet.getTafTm());
			
			TafElement fcstTafElement = tafElementSet.getStateTafElement(TafData.State.FCST);
			TafElement tempoTafElement = tafElementSet.getStateTafElement(TafData.State.TEMPO);
			
			this.analysisFog(tafPostAnalData, fcstTafElement, tempoTafElement, metarObsData);
						
			tafPostAnalDataList.add(tafPostAnalData);
		}
		
		List<Map<String, Object>> tafPostAnalInfoList = new ArrayList<Map<String, Object>>();
		
		this.createFgTafPostAnalInfo(tafPostAnalInfoList, tafPostAnalDataList);
		//this.createRaTafPostAnalInfo(tafPostAnalInfoList, tafPostAnalDataList);
		
		
		return tafPostAnalInfoList;
	}
	
	private void analysisFog(TafPostAnalData tafPostAnalData, TafElement fcstTafElement, TafElement tempoTafElement, Map<String, Object> metarObsData) {
		
		String tafSkyCondition = fcstTafElement.getSkyCondition();		
		String metarSkyCondition = (String)metarObsData.get("skycondition");
		
		tafPostAnalData.setTafState("FCST");
		
		tafPostAnalData.setTafSkyCondition(tafSkyCondition);
		tafPostAnalData.setMetarSkyCondition(metarSkyCondition);
		
		// 안개를 예측했을때
		if(tafSkyCondition != null && tafSkyCondition.contains("FG")) {
			
			// 안개가 관측되었다면 OK
			if(metarSkyCondition != null && metarSkyCondition.contains("FG")) {
				
				tafPostAnalData.setFgAnalResult("1");
				
			// 안개가 관측되지 않았다면 FAIL
			} else {
				
				if(tempoTafElement != null && tempoTafElement.getSkyCondition() != null && tempoTafElement.getSkyCondition().contains("FG")) {
					tafPostAnalData.setTafState("TEMPO");
					tafPostAnalData.setFgAnalResult("1");	
				} else {
					tafPostAnalData.setFgAnalResult("0");	
				}		
			}			
			
		// 안걔를 예측하지 않았을때
		} else {
			
			// 안개가 관측되었다면 FAIL
			if(metarSkyCondition != null && metarSkyCondition.contains("FG")) {
				
				tafPostAnalData.setFgAnalResult("0");
				
			// 안개가 관측되지 않았다면 OK
			} else {
				tafPostAnalData.setFgAnalResult("1");
			}
		}	
	}
	
	private void createFgTafPostAnalInfo(List<Map<String, Object>> tafPostAnalInfoList, List<TafPostAnalData> tafPostAnalDataList) {
		
		Map<String, Object> postAnalInfo = new HashMap<String, Object>();
		
		postAnalInfo.put("tafPostAnalDataList", tafPostAnalDataList);
		
		String postAnalType = "2";
		String postAnalResult = "1";		
		
		for(int i=0 ; i<tafPostAnalDataList.size() ; i++) {
			
			TafPostAnalData tafPostAnalData = tafPostAnalDataList.get(i);
			
			if("0".equals(tafPostAnalData.getFgAnalResult())) {
				postAnalResult = "0";
			}
		}
		
		postAnalInfo.put("postAnalType", postAnalType);
		postAnalInfo.put("postAnalResult", postAnalResult);
		
		tafPostAnalInfoList.add(postAnalInfo);
	}
	
	private Map<String, Object> createRaTafPostAnalInfo(List<Map<String, Object>> tafPostAnalInfoList, List<TafPostAnalData> tafPostAnalDataList) {
		
		return null;
	}
	
	private Map<String, Object> getTafElementSetInfobyTm(List<TafElementSet> tafElementSetList, Date tm) {
		
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
}
