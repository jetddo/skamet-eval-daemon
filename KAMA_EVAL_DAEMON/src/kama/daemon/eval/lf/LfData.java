package kama.daemon.eval.lf;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LfData {
	
	// LF 예보 요소
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
	
	// LF 변화군
	public enum State {
		
		FCST,
		BECMG,
		TEMPO,
		FM
	}
	
	private List<String> errorMsgList = new ArrayList<String>();
	
	private boolean isAvailable = true;
	
	// LF 전문
	private String lfSource;
	
	// LF 발표 시각
	private Date anncTm;
	
	// LF 예보 시작 시각
	private Date stLfTm;
	
	// LF 예보 종료 시각
	private Date edLfTm;
	
	private String stnCd;
	
	private List<LfElementSet> lfElementSetList = new ArrayList<LfElementSet>();
	
	// lfTmList 를 계산하여 Default LfElementSet 을 구성한다
	public void setLfTmList(List<Date> lfTmList) {
		
		this.lfElementSetList.clear();
		
		for(int i=0 ; i<lfTmList.size() ; i++) {
			
			LfElementSet lfElementSet = new LfElementSet();
			
			lfElementSet.setLfTm(lfTmList.get(i));
			
			lfElementSetList.add(lfElementSet);
		}
		
		this.setStLfTm(lfTmList.get(0));
		this.setEdLfTm(lfTmList.get(lfTmList.size()-1));
	}
	
	// stTm 부터 edTm 까지 특정 요소를 업데이트 한다
	@SuppressWarnings("unchecked")
	public void updateLfElementBetween(Date stTm, Date edTm, Object elementValue, LfData.Element element, LfData.State state) {
	
		for(int i=0 ; i<this.lfElementSetList.size() ; i++) {
			
			Date lfTm = this.lfElementSetList.get(i).getLfTm();
			
			LfElement lfElement = null;
			
			if(LfData.State.FCST.equals(state)) {
				lfElement = this.lfElementSetList.get(i).getFcstLfElement();
			} else if(LfData.State.BECMG.equals(state)) {
				lfElement = this.lfElementSetList.get(i).getBecmgLfElement();
			} else if(LfData.State.TEMPO.equals(state)) {
				lfElement = this.lfElementSetList.get(i).getTempoLfElement();
			}
				
			// 현재 lfTm 이 업데이트 기간 안쪽에 있을때			
			if(lfTm.getTime() >= stTm.getTime() && lfTm.getTime() <= edTm.getTime()) {
				
				if(LfData.Element.WIND.equals(element)) {
					
					this.updateWind(lfElement, (Map<String, Object>)elementValue);
					
				} else if(LfData.Element.VIS.equals(element)) {
					
					this.updateVis(lfElement, (double)elementValue);
					
				} else if(LfData.Element.SKYCONDITION.equals(element)) {
					
					this.updateSkyCondition(lfElement, (String)elementValue);
					
				} else if(LfData.Element.CLOUDCONDITION.equals(element)) {
					
					this.updateCloudCondition(lfElement, (String)elementValue);
					
				} else if(LfData.Element.MAXTEMP.equals(element)) {
					
					this.updateMaxTemp(lfElement, (double)elementValue);
					
				} else if(LfData.Element.MINTEMP.equals(element)) {
					
					this.updateMinTemp(lfElement, (double)elementValue);
					
				} else if(LfData.Element.CAVOK.equals(element)) {
					
					this.updateCavok(lfElement, (boolean)elementValue);
					
				} else if(LfData.Element.NSW.equals(element)) {
					
					this.updateNsw(lfElement, (boolean)elementValue);
					
				} else if(LfData.Element.NSC.equals(element)) {
					
					this.updateNsc(lfElement, (boolean)elementValue);
					
				} else if(LfData.Element.SKC.equals(element)) {
					
					this.updateSkc(lfElement, (boolean)elementValue);
					
				}
			}
		}
	}
	
	// stTm 부터 edTm 까지 특정 요소를 업데이트 한다
	@SuppressWarnings("unchecked")
	public void updateLfElementAfter(Date tm, Object elementValue, LfData.Element element, LfData.State state) {
	
		for(int i=0 ; i<this.lfElementSetList.size() ; i++) {
			
			Date lfTm = this.lfElementSetList.get(i).getLfTm();
			
			LfElement lfElement = null;
			
			if(LfData.State.FCST.equals(state)) {
				lfElement = this.lfElementSetList.get(i).getFcstLfElement();
			} else if(LfData.State.BECMG.equals(state)) {
				lfElement = this.lfElementSetList.get(i).getBecmgLfElement();
			} else if(LfData.State.TEMPO.equals(state)) {
				lfElement = this.lfElementSetList.get(i).getTempoLfElement();
			}
				
			// 현재 lfTm 이 업데이트 기간 안쪽에 있을때			
			if(lfTm.getTime() >= tm.getTime()) {
				
				if(LfData.Element.WIND.equals(element)) {
					
					this.updateWind(lfElement, (Map<String, Object>)elementValue);
					
				} else if(LfData.Element.VIS.equals(element)) {
					
					this.updateVis(lfElement, (double)elementValue);
					
				} else if(LfData.Element.SKYCONDITION.equals(element)) {
					
					this.updateSkyCondition(lfElement, (String)elementValue);
					
				} else if(LfData.Element.CLOUDCONDITION.equals(element)) {
					
					this.updateCloudCondition(lfElement, (String)elementValue);
					
				} else if(LfData.Element.MAXTEMP.equals(element)) {
					
					this.updateMaxTemp(lfElement, (double)elementValue);
					
				} else if(LfData.Element.MINTEMP.equals(element)) {
					
					this.updateMinTemp(lfElement, (double)elementValue);
					
				} else if(LfData.Element.CAVOK.equals(element)) {
					
					this.updateCavok(lfElement, (boolean)elementValue);
					
				} else if(LfData.Element.NSW.equals(element)) {
					
					this.updateNsw(lfElement, (boolean)elementValue);
					
				} else if(LfData.Element.NSC.equals(element)) {
					
					this.updateNsc(lfElement, (boolean)elementValue);
					
				} else if(LfData.Element.SKC.equals(element)) {
					
					this.updateSkc(lfElement, (boolean)elementValue);
					
				}
			}
		}
	}
	
	// stTm 부터 edTm 까지 특정 요소를 초기화 한다
	public void clearFcstLfElementBetween(Date stTm, Date edTm, LfData.Element element) {
		
		for(int i=0 ; i<this.lfElementSetList.size() ; i++) {
			
			Date lfTm = this.lfElementSetList.get(i).getLfTm();
			
			LfElement fcstLfElement = this.lfElementSetList.get(i).getFcstLfElement();
					
			// 현재 lfTm 이 업데이트 기간 안쪽에 있을때
			
			if(lfTm.getTime() < stTm.getTime()) {
				
			} else if(lfTm.getTime() >= stTm.getTime() && lfTm.getTime() <= edTm.getTime()) {
					
				if(LfData.Element.SKYCONDITION.equals(element)) {
					
					this.clearSkyCondition(fcstLfElement);
					
				} else if(LfData.Element.CLOUDCONDITION.equals(element)) {
					
					this.clearCloudCondition(fcstLfElement);
				}
			}
		}
	}
	
	// fcstLfElement 에서 tm 이후의 시간대의 특정 요소를 초기화 한다
	public void clearFcstLfElementAfter(Date tm, LfData.Element element) {
		
		for(int i=0 ; i<this.lfElementSetList.size() ; i++) {
			
			Date lfTm = this.lfElementSetList.get(i).getLfTm();
			
			LfElement fcstLfElement = this.lfElementSetList.get(i).getFcstLfElement();
				
			// 현재 lfTm 이 업데이트 기간 안쪽에 있을때
			
			if(lfTm.getTime() >= tm.getTime()) {
				
				if(LfData.Element.SKYCONDITION.equals(element)) {
					
					this.clearSkyCondition(fcstLfElement);
					
				} else if(LfData.Element.CLOUDCONDITION.equals(element)) {
					
					this.clearCloudCondition(fcstLfElement);
				}
			} 
		}
	}
	
	private void updateWind(LfElement lfElement, Map<String, Object> windMap) {
		
		Boolean vrb = (boolean)windMap.get("vrb");
		Object wdir = windMap.get("wdir");
		Double wspd = (double)windMap.get("wspd");
		Object maxWspd = windMap.get("maxWspd");

		lfElement.setVrb(vrb);				
		lfElement.setWdir(wdir != null ? (double)wdir : null);				
		lfElement.setWspd(wspd != null ? wspd : null);				
		lfElement.setMaxWspd(maxWspd != null ? (double)maxWspd : null);	
	}
	
	private void updateVis(LfElement lfElement, Double vis) {	
		
		lfElement.setVis(vis != null ? vis : null);
		lfElement.setCavok(false);
	}
	
	private void updateSkyCondition(LfElement lfElement, String skyCondition) {	
		
		lfElement.setSkyCondition((lfElement.getSkyCondition() + " " + skyCondition).trim());
		lfElement.setNsw(false);
	}
	
	private void updateCloudCondition(LfElement lfElement, String cloudCondition) {	
		
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
			lfElement.getCloudAmountLayer1List().add(cloudAmount);
			lfElement.getCloudHeightLayer1List().add(cloudHeight);
			lfElement.getCbCloudLayer1List().add(isCbCloud);	
			
			// 1층과 2층 고도에 구름이 있을 경우에는 NSC 및 SKC 를 해제한다
			lfElement.setNsc(false);
			lfElement.setSkc(false);
			
			// 2층 운량은 BKN 과 OVC 만 평가한다
		} else if(cloudHeight > 1500 && cloudHeight <= 10000) {
			lfElement.getCloudAmountLayer2List().add(cloudAmount);
			lfElement.getCloudHeightLayer2List().add(cloudHeight);
			lfElement.getCbCloudLayer2List().add(isCbCloud);
			
			// 1층과 2층 고도에 구름이 있을 경우에는  NSC 및 SKC 를 해제한다
			lfElement.setNsc(false);
			lfElement.setSkc(false);
		}
		
		lfElement.setCavok(false);
	}
	
	private void clearSkyCondition(LfElement lfElement) {
		
		lfElement.setSkyCondition("");
	}
	
	private void clearCloudCondition(LfElement lfElement) {
			
		lfElement.getCloudAmountLayer1List().clear();
		lfElement.getCloudAmountLayer2List().clear();
		lfElement.getCloudHeightLayer1List().clear();
		lfElement.getCloudHeightLayer2List().clear();
		lfElement.getCbCloudLayer1List().clear();
		lfElement.getCbCloudLayer2List().clear();
	}
	
	private void updateCavok(LfElement lfElement, boolean cavok) {
		
		lfElement.setCavok(cavok);
		
		if(cavok) {
			
			lfElement.getCloudAmountLayer1List().clear();
			lfElement.getCloudAmountLayer2List().clear();
			lfElement.getCloudHeightLayer1List().clear();
			lfElement.getCloudHeightLayer2List().clear();
			lfElement.getCbCloudLayer1List().clear();
			lfElement.getCbCloudLayer2List().clear();
			lfElement.setVis(9999.0);	
			lfElement.setSkyCondition("");
		}
	}
	
	private void updateSkc(LfElement lfElement, boolean skc) {
		
		lfElement.setSkc(skc);
		lfElement.setNsc(skc);
		
		if(skc) {
			
			lfElement.getCloudAmountLayer1List().clear();
			lfElement.getCloudAmountLayer2List().clear();
			lfElement.getCloudHeightLayer1List().clear();
			lfElement.getCloudHeightLayer2List().clear();
			lfElement.getCbCloudLayer1List().clear();
			lfElement.getCbCloudLayer2List().clear();
		}
	}
	
	private void updateNsc(LfElement lfElement, boolean nsc) {
		
		lfElement.setSkc(nsc);
		lfElement.setNsc(nsc);
		
		if(nsc) {
			
			lfElement.getCloudAmountLayer1List().clear();
			lfElement.getCloudAmountLayer2List().clear();
			lfElement.getCloudHeightLayer1List().clear();
			lfElement.getCloudHeightLayer2List().clear();
			lfElement.getCbCloudLayer1List().clear();
			lfElement.getCbCloudLayer2List().clear();
		}
	}
	
	private void updateNsw(LfElement lfElement, boolean nsw) {
		
		lfElement.setNsw(nsw);	
		
		if(nsw) {
			lfElement.setSkyCondition("");	
		}
	}
	
	private void updateMaxTemp(LfElement lfElement, Double maxTemp) {
			
		lfElement.setTx(maxTemp);
	}
	
	private void updateMinTemp(LfElement lfElement, Double minTemp) {
		
		lfElement.setTn(minTemp);
	}
	
	public void activateStateLf(Date stateStLfTm, Date stateEdLfTm, LfData.State state, boolean enabledFm, boolean enabledTl, boolean enabledAt, Integer stateIdx) {
		
		// BECMG 의 경우 FM, TL, AT 의 등장에 따라 평가 방법을 지정해준다
			
		for(int i=0 ; i<this.lfElementSetList.size() ; i++) {
			
			LfElementSet lfElementSet = this.lfElementSetList.get(i);
			
			Date lfTm = lfElementSet.getLfTm();
			
			if(lfTm.getTime() >= stateStLfTm.getTime() && lfTm.getTime() <= stateEdLfTm.getTime()) {
				
				Integer stateStatus = null;
				
				if(lfTm.getTime() == stateStLfTm.getTime()) {
					stateStatus = 0;
				} else if(lfTm.getTime() == stateEdLfTm.getTime()) {
					stateStatus = 2;
				} else {
					stateStatus = 1;
				}
				
				if(LfData.State.BECMG.equals(state)) {
					
					lfElementSet.activateBecmgLf(stateIdx, stateStatus);
					
					// FM 만 있는 경우					
					if(enabledFm && !enabledTl && !enabledAt) {
						
						// BECMG 의 끝점이 아니라면 둘다 평가하도록 한다						
						if(stateStatus != 2) {
							lfElementSet.setBecmgEvaluateMode(3);
						} else {
							lfElementSet.setBecmgEvaluateMode(2);
						}
						
					// TL 만 있는 경우
					} else if(!enabledFm && enabledTl && !enabledAt) {
						
						// BECMG 의 끝점이 아니면 둘다 평가하도록 한다						
						if(stateStatus != 2) {
							lfElementSet.setBecmgEvaluateMode(3);
						} else {
							lfElementSet.setBecmgEvaluateMode(2);
						}
						
					// FM, TL 둘다 있는 경우
					} else if(enabledFm && enabledTl && !enabledAt) {
						
						// BECMG 의 끝점이 아니면 둘다 평가하도록 한다						
						if(stateStatus != 2) {
							lfElementSet.setBecmgEvaluateMode(3);
						} else {
							lfElementSet.setBecmgEvaluateMode(2);
						}
						
					// AT 만 있는 경우
					} else if(!enabledFm && !enabledTl && enabledAt) {
						
						lfElementSet.setBecmgEvaluateMode(2);
						
					// BECMG 시간 지정이 없는 경우
					} else if(!enabledFm && !enabledTl && !enabledAt) {
						
						// BECMG 의 끝점이 아니면 둘다 평가하도록 한다						
						if(stateStatus != 2) {
							lfElementSet.setBecmgEvaluateMode(3);
						} else {
							lfElementSet.setBecmgEvaluateMode(2);
						}
					}
					
				} else if(LfData.State.TEMPO.equals(state)) {
					
					lfElementSet.activateTempoLf(stateIdx, stateStatus);
					
				}	
			}	
		}
	}
	
	@Override
	public String toString() {
		
		String s = "";
		
		SimpleDateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		s += "====================================================\n";
		s += "\t\tLF HEADER\n";
		s += "====================================================\n";
		s += "\tanncTm: " + datePattern.format(this.anncTm) + "\n";
		s += "\tstLfTm: " + datePattern.format(this.stLfTm) + "\n";
		s += "\tedLfTm: " + datePattern.format(this.edLfTm) + "\n";		
		s += "====================================================\n";
		s += "\n";
		s += "====================================================\n";
		s += "\t\tLF CONTENTS\n";		
		s += "====================================================\n";
			
		for(int i=0 ; i<this.lfElementSetList.size() ; i++) {			
			s += this.lfElementSetList.get(i).toString();			
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
		
		if(this.lfElementSetList.size() == 0 || this.stLfTm == null || this.edLfTm == null || this.anncTm == null) {
			
			this.errorMsgList.add("this.lfElementSetList.size() == 0 || this.stLfTm == null || this.edLfTm == null || this.anncTm == null");
			this.isAvailable = false;
		}
		
		for(int i=0 ; i<this.lfElementSetList.size() ; i++) {
		
			LfElementSet lfElementSet = this.lfElementSetList.get(i);
			
			LfElement fcstLfElement = lfElementSet.getFcstLfElement();
			
			if(lfElementSet.getLfTm() == null) {
				this.errorMsgList.add("lfElementSet.getLfTm() == null");
				this.isAvailable = false;
			}
			
			// 전 구간에서 풍향과 풍속은 항상 있어야함
			if((fcstLfElement.getWdir() == null && !fcstLfElement.isVrb()) || fcstLfElement.getWspd() == null) {
				this.errorMsgList.add("(fcstLfElement.getWdir() == null && !fcstLfElement.isVrb()) || fcstLfElement.getWspd() == null");
				this.isAvailable = false;
			}
			
			// 시정이 없을 경우 CAVOK 상태가 아니라면 에러
			if(fcstLfElement.getVis() == null && !fcstLfElement.isCavok()) {
				this.errorMsgList.add("fcstLfElement.getVis() == null && !fcstLfElement.isCavok()");
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

	public String getLfSource() {
		return lfSource;
	}

	public void setLfSource(String lfSource) {
		this.lfSource = lfSource;
	}

	public Date getAnncTm() {
		return anncTm;
	}

	public void setAnncTm(Date anncTm) {
		this.anncTm = anncTm;
	}

	public Date getStLfTm() {
		return stLfTm;
	}

	public void setStLfTm(Date stLfTm) {
		this.stLfTm = stLfTm;
	}

	public Date getEdLfTm() {
		return edLfTm;
	}

	public void setEdLfTm(Date edLfTm) {
		this.edLfTm = edLfTm;
	}

	public List<LfElementSet> getLfElementSetList() {
		return lfElementSetList;
	}

	public void setLfElementSetList(List<LfElementSet> lfElementSetList) {
		this.lfElementSetList = lfElementSetList;
	}

	public String getStnCd() {
		return stnCd;
	}

	public void setStnCd(String stnCd) {
		this.stnCd = stnCd;
	}
	
	
}