package kama.daemon.eval.taf;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TafParser {

	// TAF 발표시각
	private Date anncTm;
	
	// TAF 적용 시작 시각
	private Date stTafTm;
	
	// TAF 적용 종료 시각
	private Date edTafTm;

	// 현재 변화군 상태
	private TafData.State state;
	
	// 변화군 적용 기간
	private Date stateStTafTm;
	private Date stateEdTafTm;
		
	public TafData parse(String stnCd, String tafSource, Date stdTm) throws TafParseException {
		
		int becmgIdx = 0;
		int tempoIdx = 0;
		int fmIdx = 0;

		TafData tafData = new TafData();

		// TAF 발표시각
		this.anncTm = null;
		
		// TAF 적용 시작 시각
		this.stTafTm = null;
		
		// TAF 적용 종료 시각
		this.edTafTm = null;

		// 현재 변화군 상태
		this.state = TafData.State.FCST;
		
		// 변화군 적용 기간
		this.stateStTafTm = null;
		this.stateEdTafTm = null;
		
		// Token 위치
		int cursor = 0;
		
		boolean isTafEnd = false;
		
		tafData.setStnCd(stnCd);						
		tafData.setTafSource(tafSource);
		
		// TAF 와 = 문자 제거
		tafSource = tafSource.replaceAll("=", "").trim();
		
		String[] tafTokens = tafSource.split("\\s+");
		
		// 현천이 존재하는지?
		boolean existSkyConditions = false;
		
		// 구름이 존재하는지?
		boolean existCloudConditions = false;
		
		// 현천이 초기화되어야 하는지?
		boolean clearSkyConditions = false;
		
		// 구름이 초기화되어야 하는지?
		boolean clearCloudConditions = false;
		
		while(cursor < tafTokens.length) {
			
			if(isTafEnd) {
				break;
			}
			
			int identifyCode = TafTokenParser.identifyTafToken(tafTokens[cursor]);
			
			switch(identifyCode) {
				
			case 0: // 발표시각 패턴
				
				anncTm = TafTokenParser.parsePartialDate(stdTm, tafTokens[cursor], 6);	
				
				tafData.setAnncTm(anncTm);
				break;
				
			case 1: // 유효기간 패턴
					
				List<Date> tafTmList = TafTokenParser.parseTmList(stdTm, tafTokens[cursor]);
					
				if(TafData.State.FCST.equals(state)) {
					
					// 변화군과 확률 상태가 아닐때에는 전체 예보 기간이므로 TAF DATA 에 전체 기간을 설정해준다
					tafData.setTafTmList(tafTmList);
				
					stTafTm = new Date(tafTmList.get(0).getTime());
					edTafTm = new Date(tafTmList.get(tafTmList.size()-1).getTime());
						
				} else if(TafData.State.BECMG.equals(state) || TafData.State.TEMPO.equals(state)) {
					
					// BECMG 또는 TEMPO 상태일때는 유효기간을 업데이트해준다
					stateStTafTm = new Date(tafTmList.get(0).getTime());
					stateEdTafTm = new Date(tafTmList.get(tafTmList.size()-1).getTime());
					
					if(TafData.State.BECMG.equals(state)) {
						tafData.activateStateTaf(stateStTafTm, stateEdTafTm, state, becmgIdx++);
					} else {
						tafData.activateStateTaf(stateStTafTm, stateEdTafTm, state, tempoIdx++);	
					}				
					
					// 기존에 현천값이 존재하면 이후 변화군에 따라 변경될수있으므로 초기화 될 수 있다
					if(existSkyConditions) {
						clearSkyConditions = true;
					}
					
					if(existCloudConditions) {
						clearCloudConditions = true;
					}
				}
				
				break;
				
			case 2: // 바람 패턴
				
				Map<String, Object> windMap = TafTokenParser.parseWind(tafTokens[cursor]);
				
				if(TafData.State.FCST.equals(state)) {
					
					tafData.updateTafElementBetween(stTafTm, edTafTm, windMap, TafData.Element.WIND, state);
						
				} else if(TafData.State.BECMG.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, windMap, TafData.Element.WIND, state);
					tafData.updateTafElementAfter(stateEdTafTm, windMap, TafData.Element.WIND, TafData.State.FCST);
					
				} else if(TafData.State.TEMPO.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, windMap, TafData.Element.WIND, state);
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, windMap, TafData.Element.WIND, TafData.State.FCST);
					
				} else if(TafData.State.FM.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, windMap, TafData.Element.WIND, state);	
					tafData.updateTafElementBetween(stateStTafTm, edTafTm, windMap, TafData.Element.WIND, TafData.State.FCST);
				}
				
				break;
				
			case 3: // 시정 패턴
				
				Double vis = TafTokenParser.parseVis(tafTokens[cursor]);
				
				if(TafData.State.FCST.equals(state)) {
					
					tafData.updateTafElementBetween(stTafTm, edTafTm, vis, TafData.Element.VIS, state);
						
				} else if(TafData.State.BECMG.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, vis, TafData.Element.VIS, state);
					tafData.updateTafElementAfter(stateEdTafTm, vis, TafData.Element.VIS, TafData.State.FCST);
					
				} else if(TafData.State.TEMPO.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, vis, TafData.Element.VIS, state);
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, vis, TafData.Element.VIS, TafData.State.FCST);
					
				} else if(TafData.State.FM.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, vis, TafData.Element.VIS, state);	
					tafData.updateTafElementBetween(stateStTafTm, edTafTm, vis, TafData.Element.VIS, TafData.State.FCST);
				}
				
				break;
				
			case 4: // 최대 기온 패턴
			
				{
					
					Date tm = TafTokenParser.parsePartialDate(stdTm, tafTokens[cursor], 4);
					Double temperature = TafTokenParser.parseTemperature(tafTokens[cursor]);
					
					tafData.updateTafElementBetween(tm, tm, temperature, TafData.Element.MAXTEMP, TafData.State.FCST);
				}
							
				break;	
				
			case 5: // 최소 기온 패턴
				
				{
					
					Date tm = TafTokenParser.parsePartialDate(stdTm, tafTokens[cursor], 4);
					Double temperature = TafTokenParser.parseTemperature(tafTokens[cursor]);
					
					tafData.updateTafElementBetween(tm, tm, temperature, TafData.Element.MINTEMP, TafData.State.FCST);
				}
			
				break;		
				
			case 6: // 현천 패턴	
				
				existSkyConditions = true;
				
				if(TafData.State.FCST.equals(state)) {
					
					tafData.updateTafElementBetween(stTafTm, edTafTm, tafTokens[cursor], TafData.Element.SKYCONDITION, state);
						
				} else if(TafData.State.BECMG.equals(state)) {
					
					if(clearSkyConditions) {
						
						tafData.clearFcstTafElementAfter(stateEdTafTm, TafData.Element.SKYCONDITION);	
						clearSkyConditions = false;
					}
					
					// BECMG 영역을 먼저 업데이트한다
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, tafTokens[cursor], TafData.Element.SKYCONDITION, state);
					
					// FCST 영역을 업데이트 한다
					tafData.updateTafElementAfter(stateEdTafTm, tafTokens[cursor], TafData.Element.SKYCONDITION, TafData.State.FCST);
					
				} else if(TafData.State.TEMPO.equals(state)) {
					
					if(clearSkyConditions) {
						
						tafData.clearFcstTafElementBetween(stateStTafTm, stateEdTafTm, TafData.Element.SKYCONDITION);	
						clearSkyConditions = false;
					}
					
					// TEMPO 영역을 먼저 업데이트한다
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, tafTokens[cursor], TafData.Element.SKYCONDITION, state);
					
					// FCST 영역을 업데이트 한다
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, tafTokens[cursor], TafData.Element.SKYCONDITION, TafData.State.FCST);
					
				} else if(TafData.State.FM.equals(state)) {
					
					if(clearSkyConditions) {
						
						tafData.clearFcstTafElementAfter(stateEdTafTm, TafData.Element.SKYCONDITION);
						clearSkyConditions = false;
					}
					
					// FM 영역을 먼저 업데이트한다
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, tafTokens[cursor], TafData.Element.SKYCONDITION, state);
					
					// FCST 영역을 업데이트 한다
					tafData.updateTafElementAfter(stateEdTafTm, tafTokens[cursor], TafData.Element.SKYCONDITION, TafData.State.FCST);
				}
				
				break;
				
			case 7: // 구름 패턴
				
				existCloudConditions = true;
				
				if(TafData.State.FCST.equals(state)) {
					
					tafData.updateTafElementBetween(stTafTm, edTafTm, tafTokens[cursor], TafData.Element.CLOUDCONDITION, state);
						
				} else if(TafData.State.BECMG.equals(state)) {
					
					if(clearCloudConditions) {
						
						tafData.clearFcstTafElementAfter(stateEdTafTm, TafData.Element.CLOUDCONDITION);	
						clearCloudConditions = false;
					}
					
					// BECMG 영역을 먼저 업데이트한다
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, tafTokens[cursor], TafData.Element.CLOUDCONDITION, state);
					
					// FCST 영역을 업데이트 한다
					tafData.updateTafElementAfter(stateEdTafTm, tafTokens[cursor], TafData.Element.CLOUDCONDITION, TafData.State.FCST);
					
				} else if(TafData.State.TEMPO.equals(state)) {
					
					if(clearCloudConditions) {
						
						tafData.clearFcstTafElementBetween(stateStTafTm, stateEdTafTm,TafData.Element.CLOUDCONDITION);	
						clearCloudConditions = false;
					}
					
					// TEMPO 영역을 먼저 업데이트한다
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, tafTokens[cursor], TafData.Element.CLOUDCONDITION, state);
					
					// FCST 영역을 업데이트 한다
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, tafTokens[cursor], TafData.Element.CLOUDCONDITION, TafData.State.FCST);
					
				} else if(TafData.State.FM.equals(state)) {
					
					if(clearCloudConditions) {
						
						tafData.clearFcstTafElementAfter(stateEdTafTm,TafData.Element.CLOUDCONDITION);
						clearCloudConditions = false;
					}
					
					// FM 영역을 먼저 업데이트한다
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, tafTokens[cursor], TafData.Element.CLOUDCONDITION, state);
					
					// FCST 영역을 업데이트 한다
					tafData.updateTafElementAfter(stateEdTafTm, tafTokens[cursor], TafData.Element.CLOUDCONDITION, TafData.State.FCST);
				}
				
				break;		
				
			case 8: // BECMG 지시어 패턴
				
				stateStTafTm = null;
				stateEdTafTm = null;
				
				state = TafData.State.BECMG;
				
				break;
				
			case 9: // TEMPO 지시어 패턴
				
				stateStTafTm = null;
				stateEdTafTm = null;
				
				state = TafData.State.TEMPO;
				
				break;	
				
			case 10: // FM 지시어 패턴
				
				String fmString = tafTokens[cursor];			
				
				stateStTafTm = TafTokenParser.parsePartialDate(stdTm, fmString, 6);						
				stateEdTafTm = TafTokenParser.parsePartialDate(stdTm, fmString, 6);
				
				state = TafData.State.FM;
				
				tafData.activateStateTaf(stateStTafTm, stateEdTafTm, state, fmIdx++);
				
				if(existSkyConditions) {
					clearSkyConditions = true;	
				}
				
				if(existCloudConditions) {
					clearCloudConditions = true;
				}
				
				break;		
				
			case 11: // 확률지시어 패턴
				
				
				break;
				
			case 12: // CAVOK 패턴
				
				if(TafData.State.FCST.equals(state)) {
					
					tafData.updateTafElementBetween(stTafTm, edTafTm, true, TafData.Element.CAVOK, state);
						
				} else if(TafData.State.BECMG.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.CAVOK, state);
					tafData.updateTafElementAfter(stateEdTafTm, true, TafData.Element.CAVOK, TafData.State.FCST);
					
				} else if(TafData.State.TEMPO.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.CAVOK, state);
					
				} else if(TafData.State.FM.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.CAVOK, state);	
					tafData.updateTafElementBetween(stateStTafTm, edTafTm, true, TafData.Element.CAVOK, TafData.State.FCST);
				}				
				
				break;
				
			case 13: // SKC 패턴
				
				if(TafData.State.FCST.equals(state)) {
					
					tafData.updateTafElementBetween(stTafTm, edTafTm, true, TafData.Element.SKC, state);
						
				} else if(TafData.State.BECMG.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.SKC, state);
					tafData.updateTafElementAfter(stateEdTafTm, true, TafData.Element.SKC, TafData.State.FCST);
					
				} else if(TafData.State.TEMPO.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.SKC, state);
					tafData.updateTafElementBetween(stateEdTafTm, stateEdTafTm, true, TafData.Element.SKC, TafData.State.FCST);
					
				} else if(TafData.State.FM.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.SKC, state);	
					tafData.updateTafElementBetween(stateStTafTm, edTafTm, true, TafData.Element.SKC, TafData.State.FCST);
				}
				
				break;	
				
			case 14: // NSW 패턴
				
				if(TafData.State.FCST.equals(state)) {
					
					tafData.updateTafElementBetween(stTafTm, edTafTm, true, TafData.Element.NSW, state);
						
				} else if(TafData.State.BECMG.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.NSW, state);
					tafData.updateTafElementAfter(stateEdTafTm, true, TafData.Element.NSW, TafData.State.FCST);
					
				} else if(TafData.State.TEMPO.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.NSW, state);
					tafData.updateTafElementBetween(stateEdTafTm, stateEdTafTm, true, TafData.Element.NSW, TafData.State.FCST);
					
				} else if(TafData.State.FM.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.NSW, state);	
					tafData.updateTafElementBetween(stateStTafTm, edTafTm, true, TafData.Element.NSW, TafData.State.FCST);
				}
				
				break;	
				
			case 15: // NSC 패턴
				
				if(TafData.State.FCST.equals(state)) {
					
					tafData.updateTafElementBetween(stTafTm, edTafTm, true, TafData.Element.NSC, state);
						
				} else if(TafData.State.BECMG.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.NSC, state);
					tafData.updateTafElementAfter(stateEdTafTm, true, TafData.Element.NSC, TafData.State.FCST);
					
				} else if(TafData.State.TEMPO.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.NSC, state);
					tafData.updateTafElementBetween(stateEdTafTm, stateEdTafTm, true, TafData.Element.NSC, TafData.State.FCST);
					
				} else if(TafData.State.FM.equals(state)) {
					
					tafData.updateTafElementBetween(stateStTafTm, stateEdTafTm, true, TafData.Element.NSC, state);	
					tafData.updateTafElementBetween(stateStTafTm, edTafTm, true, TafData.Element.NSC, TafData.State.FCST);
				}
				
				break;
				
			case 16: // END 패턴
				
				isTafEnd = true;
			}
			
			cursor++;
		}
		
		tafData.checkAvaliable();
		
		return tafData;
	}
	
	public static void main(String[] args) throws Exception {
		
		String tafSource = 
				"TAF RKJY 311100Z 3112/0118 18005KT 6000 BKN035 BKN080 "+
          "TN06/3116Z TX11/0105Z "+
          "BECMG 3115/3116 22006KT 4000 -RA SCT010 BKN025 OVC070 "+
          "BECMG 3118/3119 2000 RA "+
          "BECMG 3121/3122 25008KT 6000 -RA "+
          "BECMG 0108/0109 28010KT NSW BKN050 "+
          " BECMG 0111/0112 33015G25KT SCT050=";		
		
		TafParser tafParser = new TafParser();
		
		Date d = new SimpleDateFormat("yyyyMMddHHmm").parse("202101312300");
		
		TafData tafData = tafParser.parse("RKSI", tafSource, d);
		
		System.out.println(tafData);

	}
}
