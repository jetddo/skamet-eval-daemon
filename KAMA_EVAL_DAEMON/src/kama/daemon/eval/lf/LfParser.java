package kama.daemon.eval.lf;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import kama.daemon.util.EvaluationUtils;

public class LfParser {	
	
	// LF 발표시각
	private Date anncTm;
	
	// LF 적용 시작 시각
	private Date stLfTm;
	
	// LF 적용 종료 시각
	private Date edLfTm;
	
	// 현재 변화군 상태
	private LfData.State state;
	
	// 변화군 적용 기간
	private Date stateStLfTm = null;
	private Date stateEdLfTm = null;
	
	private boolean enabledFm = false;
	
	private boolean enabledTl = false;
	
	private boolean enabledAt = false;
	
	boolean isSetStatePeriod = false;
	
	public LfData parse(String stnCd, String lfSource, Date stdTm) throws LfParseException {
	
		LfData lfData = new LfData();
			
		// LF 발표시각
		this.anncTm = null;
		
		// LF 적용 시작 시각
		this.stLfTm = null;
		
		// LF 적용 종료 시각
		this.edLfTm = null;
		
		// 현재 변화군 상태
		this.state = LfData.State.FCST;
		
		// 변화군 적용 기간
		this.stateStLfTm = null;
		this.stateEdLfTm = null;
		
		this.isSetStatePeriod = false;		
		
		this.enabledFm = false;
		
		this.enabledTl = false;
		
		this.enabledAt = false;
		
		// Token 위치
		int cursor = 0;
		
		boolean isLfEnd = false;
		
		lfData.setStnCd(stnCd);						
		lfData.setLfSource(lfSource);
		
		// LF 와 = 문자 제거
		lfSource = lfSource.replaceAll("=", "").trim();
		
		String[] lfTokens = lfSource.split("\\s+");
		
		// 현천이 존재하는지?
		boolean existSkyConditions = false;
		
		// 구름이 존재하는지?
		boolean existCloudConditions = false;
		
		// 현천이 초기화되어야 하는지?
		boolean clearSkyConditions = false;
		
		// 구름이 초기화되어야 하는지?
		boolean clearCloudConditions = false;
		
		while(cursor < lfTokens.length) {
			
			if(isLfEnd) {
				break;
			}
			
			int identifyCode = LfTokenParser.identifyLfToken(lfTokens[cursor]);
			
			switch(identifyCode) {
				
			case 0: // 발표시각 패턴
				
				anncTm = LfTokenParser.parsePartialDate(stdTm, lfTokens[cursor], 6);	
				
				lfData.setAnncTm(anncTm);
				
				// 착륙예보는 METAR 발표시각 부터 두시간까지.
				
				List<Date> lfTmList = EvaluationUtils.makeMinTmList(anncTm, 2);
				
				stLfTm = new Date(lfTmList.get(0).getTime());
				edLfTm = new Date(lfTmList.get(lfTmList.size()-1).getTime());
				
				if(LfData.State.FCST.equals(state)) {
					lfData.setLfTmList(lfTmList);
				}
				
				break;
				
			case 1: // 바람 패턴
				
				Map<String, Object> windMap = LfTokenParser.parseWind(lfTokens[cursor]);
				
				if(LfData.State.FCST.equals(state)) {
					
					lfData.updateLfElementBetween(stLfTm, edLfTm, windMap, LfData.Element.WIND, state);
						
				} else if(LfData.State.BECMG.equals(state) || LfData.State.TEMPO.equals(state)) {
					
					setStatePeriod(lfData);
					
					if(LfData.State.BECMG.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, windMap, LfData.Element.WIND, state);
						lfData.updateLfElementAfter(stateEdLfTm, windMap, LfData.Element.WIND, LfData.State.FCST);	
						
					} else {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, windMap, LfData.Element.WIND, state);
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, windMap, LfData.Element.WIND, LfData.State.FCST);
					}
				}
				
				break;
				
			case 2: // 시정 패턴
				
				Double vis = LfTokenParser.parseVis(lfTokens[cursor]);
				
				if(LfData.State.FCST.equals(state)) {
					
					lfData.updateLfElementBetween(stLfTm, edLfTm, vis, LfData.Element.VIS, state);
						
				}  else if(LfData.State.BECMG.equals(state) || LfData.State.TEMPO.equals(state)) {
					
					setStatePeriod(lfData);		
					
					if(LfData.State.BECMG.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, vis, LfData.Element.VIS, state);
						lfData.updateLfElementAfter(stateEdLfTm, vis, LfData.Element.VIS, LfData.State.FCST);
						
					} else {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, vis, LfData.Element.VIS, state);
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, vis, LfData.Element.VIS, LfData.State.FCST);	
					}
				}
				
				break;
				
			case 3: // 기온 패턴
				
				Map<String, Double> tempMap = LfTokenParser.parseTemperature(lfTokens[cursor]);
				
				Object tx = tempMap.get("tx");
				Object tn = tempMap.get("tn");
				
				if(LfData.State.FCST.equals(state)) {
					
					lfData.updateLfElementBetween(stLfTm, edLfTm, tx, LfData.Element.MAXTEMP, state);
					lfData.updateLfElementBetween(stLfTm, edLfTm, tn, LfData.Element.MINTEMP, state);
						
				} else if(LfData.State.BECMG.equals(state) || LfData.State.TEMPO.equals(state)) {
					
					setStatePeriod(lfData);		
					
					if(LfData.State.BECMG.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, tx, LfData.Element.MAXTEMP, state);
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, tn, LfData.Element.MINTEMP, state);
						
						lfData.updateLfElementAfter(stateEdLfTm, tx, LfData.Element.MAXTEMP, LfData.State.FCST);
						lfData.updateLfElementAfter(stateEdLfTm, tn, LfData.Element.MINTEMP, LfData.State.FCST);
						
					} else {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, tx, LfData.Element.MAXTEMP, state);
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, tn, LfData.Element.MINTEMP, state);
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, tx, LfData.Element.MAXTEMP, LfData.State.FCST);
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, tn, LfData.Element.MINTEMP, LfData.State.FCST);	
					}
				}
											
				break;		
				
			case 4: // 현천 패턴	
				
				existSkyConditions = true;
				
				if(LfData.State.FCST.equals(state)) {
					
					lfData.updateLfElementBetween(stLfTm, edLfTm, lfTokens[cursor], LfData.Element.SKYCONDITION, state);
						
				} else if(LfData.State.BECMG.equals(state) || LfData.State.TEMPO.equals(state)) {
					
					setStatePeriod(lfData);
					
					if(LfData.State.BECMG.equals(state)) {
						
						if(clearSkyConditions) {
							
							lfData.clearFcstLfElementAfter(stateEdLfTm, LfData.Element.SKYCONDITION);	
							clearSkyConditions = false;
						}
						
						// BECMG 영역을 먼저 업데이트한다
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, lfTokens[cursor], LfData.Element.SKYCONDITION, state);
						
						// FCST 영역을 업데이트 한다
						lfData.updateLfElementAfter(stateEdLfTm, lfTokens[cursor], LfData.Element.SKYCONDITION, LfData.State.FCST);
						
					} else if(LfData.State.TEMPO.equals(state)) {
						
						if(clearSkyConditions) {
							
							lfData.clearFcstLfElementBetween(stateStLfTm, stateEdLfTm, LfData.Element.SKYCONDITION);	
							clearSkyConditions = false;
						}
						
						// TEMPO 영역을 먼저 업데이트한다
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, lfTokens[cursor], LfData.Element.SKYCONDITION, state);
						
						// FCST 영역을 업데이트 한다
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, lfTokens[cursor], LfData.Element.SKYCONDITION, LfData.State.FCST);
						
					}
				}
				
				break;
				
			case 5: // 구름 패턴
				
				existCloudConditions = true;
				
				if(LfData.State.FCST.equals(state)) {
					
					lfData.updateLfElementBetween(stLfTm, edLfTm, lfTokens[cursor], LfData.Element.CLOUDCONDITION, state);
						
				} else if(LfData.State.BECMG.equals(state) || LfData.State.TEMPO.equals(state)) {
					
					setStatePeriod(lfData);
					
					if(LfData.State.BECMG.equals(state)) {
						
						if(clearCloudConditions) {
							
							lfData.clearFcstLfElementAfter(stateEdLfTm, LfData.Element.CLOUDCONDITION);	
							clearCloudConditions = false;
						}
						
						// BECMG 영역을 먼저 업데이트한다
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, lfTokens[cursor], LfData.Element.CLOUDCONDITION, state);
						
						// FCST 영역을 업데이트 한다
						lfData.updateLfElementAfter(stateEdLfTm, lfTokens[cursor], LfData.Element.CLOUDCONDITION, LfData.State.FCST);
						
					} else if(LfData.State.TEMPO.equals(state)) {
						
						if(clearCloudConditions) {
							
							lfData.clearFcstLfElementBetween(stateStLfTm, stateEdLfTm,LfData.Element.CLOUDCONDITION);	
							clearCloudConditions = false;
						}
						
						// TEMPO 영역을 먼저 업데이트한다
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, lfTokens[cursor], LfData.Element.CLOUDCONDITION, state);
						
						// FCST 영역을 업데이트 한다
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, lfTokens[cursor], LfData.Element.CLOUDCONDITION, LfData.State.FCST);
						
					}					
				} 
				
				break;	
				
			case 6: // BECMG 지시어 패턴
				
				stateStLfTm = null;
				stateEdLfTm = null;
				
				state = LfData.State.BECMG;
				
				// 기존에 현천값이 존재하면 이후 변화군에 따라 변경될수있으므로 초기화 될 수 있다
				if(existSkyConditions) {
					clearSkyConditions = true;
				}
				
				if(existCloudConditions) {
					clearCloudConditions = true;
				}
				
				break;
				
			case 7: // TEMPO 지시어 패턴
				
				stateStLfTm = null;
				stateEdLfTm = null;
				
				state = LfData.State.TEMPO;
				
				// 기존에 현천값이 존재하면 이후 변화군에 따라 변경될수있으므로 초기화 될 수 있다
				if(existSkyConditions) {
					clearSkyConditions = true;
				}
				
				if(existCloudConditions) {
					clearCloudConditions = true;
				}
				
				break;	
				
			case 8: // AT 패턴
				
				{
				
					Date tm = LfTokenParser.parsePartialDate(anncTm, "yyyyMMdd", lfTokens[cursor], 4);
					
					stateStLfTm = new Date(tm.getTime());
					stateEdLfTm = new Date(edLfTm.getTime());
					
					this.enabledAt = true;
				}
				
				break;
				
			case 9: // FM 패턴
				
				{
					
					Date tm = LfTokenParser.parsePartialDate(anncTm, "yyyyMMdd", lfTokens[cursor], 4);
					
					stateStLfTm = new Date(tm.getTime());
					
					this.enabledFm = true;
				}
				
				break;
				
			case 10: // TL 패턴
				
				{
					
					Date tm = LfTokenParser.parsePartialDate(anncTm, "yyyyMMdd", lfTokens[cursor], 4);
					
					stateEdLfTm = new Date(tm.getTime());
					
					this.enabledTl = true;
				}
	
				break;
	
			case 11: // CAVOK 패턴
				
				if(LfData.State.FCST.equals(state)) {
					
					lfData.updateLfElementBetween(stLfTm, edLfTm, true, LfData.Element.CAVOK, state);
						
				} else if(LfData.State.BECMG.equals(state) || LfData.State.TEMPO.equals(state)) {
					
					setStatePeriod(lfData);
					
					if(LfData.State.BECMG.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, true, LfData.Element.CAVOK, state);
						lfData.updateLfElementAfter(stateEdLfTm, true, LfData.Element.CAVOK, LfData.State.FCST);
						
					} else if(LfData.State.TEMPO.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, true, LfData.Element.CAVOK, state);
						lfData.updateLfElementBetween(stateEdLfTm, stateEdLfTm, true, LfData.Element.CAVOK, LfData.State.FCST);	
					} 		
				}
				
				break;
				
			case 12: // SKC 패턴
				
				if(LfData.State.FCST.equals(state)) {
					
					lfData.updateLfElementBetween(stLfTm, edLfTm, true, LfData.Element.SKC, state);
						
				} else if(LfData.State.BECMG.equals(state) || LfData.State.TEMPO.equals(state)) {
					
					setStatePeriod(lfData);
					
					if(LfData.State.BECMG.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, true, LfData.Element.SKC, state);
						lfData.updateLfElementAfter(stateEdLfTm, true, LfData.Element.SKC, LfData.State.FCST);
						
					} else if(LfData.State.TEMPO.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, true, LfData.Element.SKC, state);
						lfData.updateLfElementBetween(stateEdLfTm, stateEdLfTm, true, LfData.Element.SKC, LfData.State.FCST);
						
					}
				}
				
				break;	
				
			case 13: // NSW 패턴
				
				if(LfData.State.FCST.equals(state)) {
					
					lfData.updateLfElementBetween(stLfTm, edLfTm, true, LfData.Element.NSW, state);
						
				} else if(LfData.State.BECMG.equals(state) || LfData.State.TEMPO.equals(state)) {
					
					setStatePeriod(lfData);
					
					if(LfData.State.BECMG.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, true, LfData.Element.NSW, state);
						lfData.updateLfElementAfter(stateEdLfTm, true, LfData.Element.NSW, LfData.State.FCST);
						
					} else if(LfData.State.TEMPO.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, true, LfData.Element.NSW, state);
						lfData.updateLfElementBetween(stateEdLfTm, stateEdLfTm, true, LfData.Element.NSW, LfData.State.FCST);
						
					}
				}
				
				break;	
				
			case 14: // NSC 패턴
				
				if(LfData.State.FCST.equals(state)) {
					
					lfData.updateLfElementBetween(stLfTm, edLfTm, true, LfData.Element.NSC, state);
						
				} else if(LfData.State.BECMG.equals(state) || LfData.State.TEMPO.equals(state)) {
					
					setStatePeriod(lfData);
					
					if(LfData.State.BECMG.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, true, LfData.Element.NSC, state);
						lfData.updateLfElementAfter(stateEdLfTm, true, LfData.Element.NSC, LfData.State.FCST);
						
					} else if(LfData.State.TEMPO.equals(state)) {
						
						lfData.updateLfElementBetween(stateStLfTm, stateEdLfTm, true, LfData.Element.NSC, state);
						lfData.updateLfElementBetween(stateEdLfTm, stateEdLfTm, true, LfData.Element.NSC, LfData.State.FCST);						
					}
				}
				
				break;
				
			case 15: // END 패턴
				
				isLfEnd = true;
			}
			
			cursor++;
		}
		
		lfData.checkAvaliable();
		
		return lfData;
	}
	
	// 변화군 결정 메서드
	private void setStatePeriod(LfData lfData) {
		
		if(isSetStatePeriod) {
			return;
		}
		
		// 변화군 기간이 설정되어있지 않다면 전체 영역을 기준으로 설정한다					
		if(stateStLfTm == null && stateEdLfTm == null) {
			
			stateStLfTm = new Date(stLfTm.getTime());
			stateEdLfTm = new Date(edLfTm.getTime());
			
		// 변화 시작 시간이 설정되어있지 않다면 착륙예보 시작점부터 설정한다
		} else if(stateStLfTm == null && stateEdLfTm != null) {
			
			stateStLfTm = new Date(stLfTm.getTime());
		
		// 변화 종료 시간이 설정되어있지 않다면 착륙예보 종료점욿 설정한다
		} else if(stateStLfTm != null && stateEdLfTm == null) {
			stateEdLfTm = new Date(edLfTm.getTime());
		}
		
		// 변화군 시작 시각이 착륙예보 시작 시각보다 작은 경우는 날이 바뀌었을때뿐이다
		if(stateStLfTm.getTime() < stLfTm.getTime()) {
			stateStLfTm = new Date(stateStLfTm.getTime() + 1000 * 60 * 60 * 24);
		}
		
		// 변화군 종료 시각이 시작 시각보다 작은 경우는 날이 바뀌었을때뿐이다
		if(stateEdLfTm.getTime() < stateStLfTm.getTime()) {
			stateEdLfTm = new Date(stateEdLfTm.getTime() + 1000 * 60 * 60 * 24);
		}
		
		if(LfData.State.BECMG.equals(state)) {
			lfData.activateStateLf(stateStLfTm, stateEdLfTm, state, this.enabledFm, this.enabledTl, this.enabledAt, 0);
		} else {
			lfData.activateStateLf(stateStLfTm, stateEdLfTm, state, this.enabledFm, this.enabledTl, this.enabledAt, 0);	
		}		
		
		isSetStatePeriod = true;
	}
	
	public static void main(String[] args) throws Exception {
		
		String lfSource = 
				"METAR RKSI 210230Z 13006KT 100V160 9999 FEW019 BKN040 BKN070 07/M06 Q1023 BECMG -RA=";		
		
		LfParser lfParser = new LfParser();
		
		Date d = new SimpleDateFormat("yyyyMMddHH").parse("2021012100");
		
		LfData lfData = lfParser.parse("RKSI", lfSource, d);
		
		System.out.println(lfData);

	}
}
