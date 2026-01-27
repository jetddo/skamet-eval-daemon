package kama.daemon.eval.taf;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TafData {
	
	// TAF 예보 요소
	public enum Element {
		
		WIND,
		VIS,
		SKYCONDITION,
		CLOUDCONDITION,
		MAXTEMP,
		MINTEMP,
		CAVOK,
		NSW,
		NSC,
		SKC
	}
	
	// TAF 변화군
	public enum State {
		
		FCST,
		BECMG,
		TEMPO,
		FM
	}
	
	private List<String> errorMsgList = new ArrayList<String>();
	
	private boolean isAvailable = true;
	
	// TAF 전문
	private String tafSource;
	
	// TAF 발표 시각
	private Date anncTm;
	
	// TAF 예보 시작 시각
	private Date stTafTm;
	
	// TAF 예보 종료 시각
	private Date edTafTm;
	
	private String stnCd;
	
	private List<TafElementSet> tafElementSetList = new ArrayList<TafElementSet>();
	
	// tafTmList 를 계산하여 Default TafElementSet 을 구성한다
	public void setTafTmList(List<Date> tafTmList) {
		
		this.tafElementSetList.clear();
		
		for(int i=0 ; i<tafTmList.size() ; i++) {
			
			TafElementSet tafElementSet = new TafElementSet();
			
			tafElementSet.setTafTm(tafTmList.get(i));
			
			tafElementSetList.add(tafElementSet);
		}
		
		this.setStTafTm(tafTmList.get(0));
		this.setEdTafTm(tafTmList.get(tafTmList.size()-1));
	}
	
	// stTm 부터 edTm 까지 특정 요소를 업데이트 한다
	@SuppressWarnings("unchecked")
	public void updateTafElementBetween(Date stTm, Date edTm, Object elementValue, TafData.Element element, TafData.State state) {
		
		for(int i=0 ; i<this.tafElementSetList.size() ; i++) {
			
			Date tafTm = this.tafElementSetList.get(i).getTafTm();
			
			TafElement tafElement = null;
			
			if(TafData.State.FCST.equals(state)) {
				tafElement = this.tafElementSetList.get(i).getFcstTafElement();
			} else if(TafData.State.BECMG.equals(state)) {
				tafElement = this.tafElementSetList.get(i).getBecmgTafElement();
			} else if(TafData.State.TEMPO.equals(state)) {
				tafElement = this.tafElementSetList.get(i).getTempoTafElement();
			} else if(TafData.State.FM.equals(state)) {
				tafElement = this.tafElementSetList.get(i).getFmTafElement();
			}
				
			// 현재 tafTm 이 업데이트 기간 안쪽에 있을때			
			if(stTm != null && edTm != null && tafTm.getTime() >= stTm.getTime() && tafTm.getTime() <= edTm.getTime()) {
				
				if(TafData.Element.WIND.equals(element)) {
					
					this.updateWind(tafElement, (Map<String, Object>)elementValue);
					
				} else if(TafData.Element.VIS.equals(element)) {
					
					this.updateVis(tafElement, (double)elementValue);
					
				} else if(TafData.Element.SKYCONDITION.equals(element)) {
					
					this.updateSkyCondition(tafElement, (String)elementValue);
					
				} else if(TafData.Element.CLOUDCONDITION.equals(element)) {
					
					this.updateCloudCondition(tafElement, (String)elementValue);
					
				} else if(TafData.Element.MAXTEMP.equals(element)) {
					
					this.updateMaxTemp(tafElement, (double)elementValue);
					
				} else if(TafData.Element.MINTEMP.equals(element)) {
					
					this.updateMinTemp(tafElement, (double)elementValue);
					
				} else if(TafData.Element.CAVOK.equals(element)) {
					
					this.updateCavok(tafElement, (boolean)elementValue);
					
				} else if(TafData.Element.NSW.equals(element)) {
					
					this.updateNsw(tafElement, (boolean)elementValue);
					
				} else if(TafData.Element.NSC.equals(element)) {
					
					this.updateNsc(tafElement, (boolean)elementValue);
					
				} else if(TafData.Element.SKC.equals(element)) {
					
					this.updateSkc(tafElement, (boolean)elementValue);
					
				}
			}
		}
	}
	
	// stTm 부터 edTm 까지 특정 요소를 업데이트 한다
	@SuppressWarnings("unchecked")
	public void updateTafElementAfter(Date tm, Object elementValue, TafData.Element element, TafData.State state) {
	
		for(int i=0 ; i<this.tafElementSetList.size() ; i++) {
			
			Date tafTm = this.tafElementSetList.get(i).getTafTm();
			
			TafElement tafElement = null;
			
			if(TafData.State.FCST.equals(state)) {
				tafElement = this.tafElementSetList.get(i).getFcstTafElement();
			} else if(TafData.State.BECMG.equals(state)) {
				tafElement = this.tafElementSetList.get(i).getBecmgTafElement();
			} else if(TafData.State.TEMPO.equals(state)) {
				tafElement = this.tafElementSetList.get(i).getTempoTafElement();
			} else if(TafData.State.FM.equals(state)) {
				tafElement = this.tafElementSetList.get(i).getFmTafElement();
			}
				
			// 현재 tafTm 이 업데이트 기간 안쪽에 있을때			
			if(tm != null && tafTm.getTime() >= tm.getTime()) {
				
				if(TafData.Element.WIND.equals(element)) {
					
					this.updateWind(tafElement, (Map<String, Object>)elementValue);
					
				} else if(TafData.Element.VIS.equals(element)) {
					
					this.updateVis(tafElement, (double)elementValue);
					
				} else if(TafData.Element.SKYCONDITION.equals(element)) {
					
					this.updateSkyCondition(tafElement, (String)elementValue);
					
				} else if(TafData.Element.CLOUDCONDITION.equals(element)) {
					
					this.updateCloudCondition(tafElement, (String)elementValue);
					
				} else if(TafData.Element.MAXTEMP.equals(element)) {
					
					this.updateMaxTemp(tafElement, (double)elementValue);
					
				} else if(TafData.Element.MINTEMP.equals(element)) {
					
					this.updateMinTemp(tafElement, (double)elementValue);
					
				} else if(TafData.Element.CAVOK.equals(element)) {
					
					this.updateCavok(tafElement, (boolean)elementValue);
					
				} else if(TafData.Element.NSW.equals(element)) {
					
					this.updateNsw(tafElement, (boolean)elementValue);
					
				} else if(TafData.Element.NSC.equals(element)) {
					
					this.updateNsc(tafElement, (boolean)elementValue);
					
				} else if(TafData.Element.SKC.equals(element)) {
					
					this.updateSkc(tafElement, (boolean)elementValue);
					
				}
			}
		}
	}
	
	// stTm 부터 edTm 까지 특정 요소를 초기화 한다
	public void clearFcstTafElementBetween(Date stTm, Date edTm, TafData.Element element) {
		
		for(int i=0 ; i<this.tafElementSetList.size() ; i++) {
			
			Date tafTm = this.tafElementSetList.get(i).getTafTm();
			
			TafElement fcstTafElement = this.tafElementSetList.get(i).getFcstTafElement();
					
			// 현재 tafTm 이 업데이트 기간 안쪽에 있을때
			
			if(stTm != null && edTm != null) {
				if(tafTm.getTime() < stTm.getTime()) {
					
				} else if(tafTm.getTime() >= stTm.getTime() && tafTm.getTime() <= edTm.getTime()) {
						
					if(TafData.Element.SKYCONDITION.equals(element)) {
						
						this.clearSkyCondition(fcstTafElement);
						
					} else if(TafData.Element.CLOUDCONDITION.equals(element)) {
						
						this.clearCloudCondition(fcstTafElement);
					}
				}
			}
		}
	}
	
	// fcstTafElement 에서 tm 이후의 시간대의 특정 요소를 초기화 한다
	public void clearFcstTafElementAfter(Date tm, TafData.Element element) {
		
		for(int i=0 ; i<this.tafElementSetList.size() ; i++) {
			
			Date tafTm = this.tafElementSetList.get(i).getTafTm();
			
			TafElement fcstTafElement = this.tafElementSetList.get(i).getFcstTafElement();
				
			// 현재 tafTm 이 업데이트 기간 안쪽에 있을때
			
			if(tm != null && tafTm.getTime() >= tm.getTime()) {
				
				if(TafData.Element.SKYCONDITION.equals(element)) {
					
					this.clearSkyCondition(fcstTafElement);
					
				} else if(TafData.Element.CLOUDCONDITION.equals(element)) {
					
					this.clearCloudCondition(fcstTafElement);
				}
			} 
		}
	}
	
	private void updateWind(TafElement tafElement, Map<String, Object> windMap) {
		
		Boolean vrb = (boolean)windMap.get("vrb");
		Object wdir = windMap.get("wdir");
		Double wspd = (double)windMap.get("wspd");
		Object maxWspd = windMap.get("maxWspd");

		tafElement.setVrb(vrb);				
		tafElement.setWdir(wdir != null ? (double)wdir : null);				
		tafElement.setWspd(wspd != null ? wspd : null);				
		tafElement.setMaxWspd(maxWspd != null ? (double)maxWspd : null);	
	}
	
	private void updateVis(TafElement tafElement, Double vis) {	
		
		tafElement.setVis(vis != null ? vis : null);
		tafElement.setCavok(false);
	}
	
	private void updateSkyCondition(TafElement tafElement, String skyCondition) {	
		
		tafElement.setSkyCondition((tafElement.getSkyCondition() + " " + skyCondition).trim());
		tafElement.setNsw(false);
	}
	
	private void updateCloudCondition(TafElement tafElement, String cloudCondition) {	
		
		String cloudAmount = null;
		Double cloudHeight = null;
		Boolean isCbCloud = false; 
		
		if(cloudCondition.startsWith("VV")) {
			
			cloudAmount = "OVC";
			
			if(cloudCondition.substring(2, 5).equals("///")) {
				cloudHeight = 0.0;
			} else {
				cloudHeight = Double.valueOf(cloudCondition.substring(2, 5)) * 100;
			}
			
		} else {
			
			cloudAmount = cloudCondition.substring(0, 3);
			cloudHeight = Double.valueOf(cloudCondition.substring(3, 6)) * 100;
			isCbCloud = cloudCondition.contains("CB") ? true : false;
		}
		
		if(cloudHeight <= 1500) {
			tafElement.getCloudAmountLayer1List().add(cloudAmount);
			tafElement.getCloudHeightLayer1List().add(cloudHeight);
			tafElement.getCbCloudLayer1List().add(isCbCloud);	
			
			// 1층과 2층 고도에 구름이 있을 경우에는 NSC 및 SKC 를 해제한다
			tafElement.setNsc(false);
			tafElement.setSkc(false);
			
			// 2층 운량은 BKN 과 OVC 만 평가한다
		} else if(cloudHeight > 1500 && cloudHeight <= 10000) {
			tafElement.getCloudAmountLayer2List().add(cloudAmount);
			tafElement.getCloudHeightLayer2List().add(cloudHeight);
			tafElement.getCbCloudLayer2List().add(isCbCloud);
			
			// 1층과 2층 고도에 구름이 있을 경우에는  NSC 및 SKC 를 해제한다
			tafElement.setNsc(false);
			tafElement.setSkc(false);
		}
		
		tafElement.setCavok(false);
	}
	
	private void clearSkyCondition(TafElement tafElement) {
		
		tafElement.setSkyCondition("");
	}
	
	private void clearCloudCondition(TafElement tafElement) {
			
		tafElement.getCloudAmountLayer1List().clear();
		tafElement.getCloudAmountLayer2List().clear();
		tafElement.getCloudHeightLayer1List().clear();
		tafElement.getCloudHeightLayer2List().clear();
		tafElement.getCbCloudLayer1List().clear();
		tafElement.getCbCloudLayer2List().clear();
	}
	
	private void updateCavok(TafElement tafElement, boolean cavok) {
		
		tafElement.setCavok(cavok);
		
		if(cavok) {
			
			tafElement.getCloudAmountLayer1List().clear();
			tafElement.getCloudAmountLayer2List().clear();
			tafElement.getCloudHeightLayer1List().clear();
			tafElement.getCloudHeightLayer2List().clear();
			tafElement.getCbCloudLayer1List().clear();
			tafElement.getCbCloudLayer2List().clear();
			tafElement.setVis(9999.0);	
			tafElement.setSkyCondition("");
		}
	}
	
	private void updateSkc(TafElement tafElement, boolean skc) {
		
		tafElement.setSkc(skc);
		tafElement.setNsc(skc);
		
		if(skc) {
			
			tafElement.getCloudAmountLayer1List().clear();
			tafElement.getCloudAmountLayer2List().clear();
			tafElement.getCloudHeightLayer1List().clear();
			tafElement.getCloudHeightLayer2List().clear();
			tafElement.getCbCloudLayer1List().clear();
			tafElement.getCbCloudLayer2List().clear();
		}
	}
	
	private void updateNsc(TafElement tafElement, boolean nsc) {
		
		tafElement.setSkc(nsc);
		tafElement.setNsc(nsc);
		
		if(nsc) {
			
			tafElement.getCloudAmountLayer1List().clear();
			tafElement.getCloudAmountLayer2List().clear();
			tafElement.getCloudHeightLayer1List().clear();
			tafElement.getCloudHeightLayer2List().clear();
			tafElement.getCbCloudLayer1List().clear();
			tafElement.getCbCloudLayer2List().clear();
		}
	}
	
	private void updateNsw(TafElement tafElement, boolean nsw) {
		
		tafElement.setNsw(nsw);	
		
		if(nsw) {
			tafElement.setSkyCondition("");	
		}
	}
	
	private void updateMaxTemp(TafElement tafElement, Double maxTemp) {
			
		tafElement.setTx(maxTemp);
	}
	
	private void updateMinTemp(TafElement tafElement, Double minTemp) {
		
		tafElement.setTn(minTemp);
	}
	
	public void activateStateTaf(Date stateStTafTm, Date stateEdTafTm, TafData.State state, Integer stateIdx) {
		
		for(int i=0 ; i<this.tafElementSetList.size() ; i++) {
			
			TafElementSet tafElementSet = this.tafElementSetList.get(i);
			
			Date tafTm = tafElementSet.getTafTm();
			
			if(tafTm.getTime() >= stateStTafTm.getTime() && tafTm.getTime() <= stateEdTafTm.getTime()) {
				
				Integer stateStatus = null;
				
				if(tafTm.getTime() == stateStTafTm.getTime()) {
					stateStatus = 0;
				} else if(tafTm.getTime() == stateEdTafTm.getTime()) {
					stateStatus = 2;
				} else {
					stateStatus = 1;
				}
				
				if(TafData.State.BECMG.equals(state)) {
					
					tafElementSet.activateBecmgTaf(stateIdx, stateStatus);
					
				} else if(TafData.State.TEMPO.equals(state)) {
					
					tafElementSet.activateTempoTaf(stateIdx, stateStatus);
					
				} else if(TafData.State.FM.equals(state)) {
					
					tafElementSet.activateFmTaf(stateIdx, stateStatus);				
				}		
			}	
		}
	}
	
	@Override
	public String toString() {
		
		String s = "";
		
		SimpleDateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		s += "====================================================\n";
		s += "\t\tTAF HEADER\n";
		s += "====================================================\n";
		s += "\tanncTm: " + datePattern.format(this.anncTm) + "\n";
		s += "\tstTafTm: " + datePattern.format(this.stTafTm) + "\n";
		s += "\tedTafTm: " + datePattern.format(this.edTafTm) + "\n";		
		s += "====================================================\n";
		s += "\n";
		s += "====================================================\n";
		s += "\t\tTAF CONTENTS\n";		
		s += "====================================================\n";
			
		for(int i=0 ; i<this.tafElementSetList.size() ; i++) {			
			s += this.tafElementSetList.get(i).toString();			
		}
		
		s += "====================================================\n";
		
		return s;
	}
	
	public void printErrorMsgList() {
		
		for(int i=0 ; i<this.errorMsgList.size() ; i++) {
			System.out.println(this.errorMsgList.get(i));
		}
	}
	
	public void checkAvaliable() {
		
		if(this.tafElementSetList.size() == 0 || this.stTafTm == null || this.edTafTm == null || this.anncTm == null) {
			
			this.errorMsgList.add("this.tafElementSetList.size() == 0 || this.stTafTm == null || this.edTafTm == null || this.anncTm == null");
			this.isAvailable = false;
		}
		
		for(int i=0 ; i<this.tafElementSetList.size() ; i++) {
		
			TafElementSet tafElementSet = this.tafElementSetList.get(i);
			
			TafElement fcstTafElement = tafElementSet.getFcstTafElement();
			
			if(tafElementSet.getTafTm() == null) {
				this.errorMsgList.add("tafElementSet.getTafTm() == null");
				this.isAvailable = false;
			}
			
			// 전 구간에서 풍향과 풍속은 항상 있어야함
			if((fcstTafElement.getWdir() == null && !fcstTafElement.isVrb()) || fcstTafElement.getWspd() == null) {
				this.errorMsgList.add("(fcstTafElement.getWdir() == null && !fcstTafElement.isVrb()) || fcstTafElement.getWspd() == null");
				this.isAvailable = false;
			}
			
			// 시정이 없을 경우 CAVOK 상태가 아니라면 에러
			if(fcstTafElement.getVis() == null && !fcstTafElement.isCavok()) {
				this.errorMsgList.add("fcstTafElement.getVis() == null && !fcstTafElement.isCavok()");
				this.isAvailable = false;
			}
		}
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	public String getTafSource() {
		return tafSource;
	}

	public void setTafSource(String tafSource) {
		this.tafSource = tafSource;
	}

	public Date getAnncTm() {
		return anncTm;
	}

	public void setAnncTm(Date anncTm) {
		this.anncTm = anncTm;
	}

	public Date getStTafTm() {
		return stTafTm;
	}

	public void setStTafTm(Date stTafTm) {
		this.stTafTm = stTafTm;
	}

	public Date getEdTafTm() {
		return edTafTm;
	}

	public void setEdTafTm(Date edTafTm) {
		this.edTafTm = edTafTm;
	}

	public List<TafElementSet> getTafElementSetList() {
		return tafElementSetList;
	}

	public void setTafElementSetList(List<TafElementSet> tafElementSetList) {
		this.tafElementSetList = tafElementSetList;
	}

	public String getStnCd() {
		return stnCd;
	}

	public void setStnCd(String stnCd) {
		this.stnCd = stnCd;
	}
	
	
}