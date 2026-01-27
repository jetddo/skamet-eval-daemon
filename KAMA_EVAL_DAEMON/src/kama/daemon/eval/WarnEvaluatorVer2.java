package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.metar.MetarParser;
import kama.daemon.eval.warn.WarnData;
import kama.daemon.util.DaemonUtil;
import kama.daemon.util.EvaluationUtils;

public class WarnEvaluatorVer2 extends WarnEvaluator {
	
	private MetarParser metarParser;
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	final int FIND_EFFCT_DELAY = 10;
	
	public WarnEvaluatorVer2(EvaluationDatabaseUtil evaluationDatabaseUtil) {
		
		this.evaluationDatabaseUtil = evaluationDatabaseUtil;
		
		this.metarParser = new MetarParser();
	}
	
	public Map<String, Object> getEvaluationResult(WarnEvaluationData warnEvaluationData, int scoreFixCount) {
		
		Map<String, Object> warnEvaluationResult = new HashMap<String, Object>();
		
		if(warnEvaluationData.isAvailable()) {
			
			WarnEvaluationData.Score score = warnEvaluationData.getScore();
			
			score.setTotal(
				DaemonUtil.setNumberFix(score.getEffct() + score.getPrev(), 
				scoreFixCount)
			);
			
			warnEvaluationResult.put("effctScore", DaemonUtil.setNumberFix(score.getEffct(), scoreFixCount));
			warnEvaluationResult.put("prevScore", DaemonUtil.setNumberFix(score.getPrev(), scoreFixCount));
			warnEvaluationResult.put("totalScore", score.getTotal());
			warnEvaluationResult.put("available", true);
			
		} else {
			warnEvaluationResult.put("available", false);
		}
		
		return warnEvaluationResult;
	}
	
	@Override
	public WarnEvaluationData evaluate(WarnData warnData) throws Exception {
		
		String stnCd = warnData.getStnCd();

		Date anncTm = warnData.getAnncTm();
		Date stEffctTm = warnData.getStEffctTm();
		Date edEffctTm = warnData.getEdEffctTm();
				
		Date _EdEffctTm = edEffctTm;
		
		Date stCnlTm = warnData.getStCnlTm();
		Date edCnlTm = warnData.getEdCnlTm();
		
		Date stExtTm = warnData.getStExtTm();
		Date edExtTm = warnData.getEdExtTm();
		
		// 경보 취소 정보가 있는 경우 경보 종료시점을 변경한다
		if(stCnlTm != null) {
			_EdEffctTm = stCnlTm;
		// 경보 취소 정보가 없고 연장정보가 있는 경우 경보 종료시점을 변경한다
		} else if(stCnlTm == null && edExtTm != null) {
			_EdEffctTm = edExtTm;
		}
		
		WarnEvaluationData warnEvaluationData = new WarnEvaluationData();
		warnEvaluationData.setWarnType(warnData.getWarnType());
		warnEvaluationData.setEvaluationTm(anncTm);	
		warnEvaluationData.setStEffctTm(stEffctTm);
		warnEvaluationData.setEdEffctTm(_EdEffctTm);
		
		switch(warnData.getWarnType()) {
		case CIG:
			
			if("RKPC".equals(stnCd)) {
			
				this.evaluateCig(stnCd, warnEvaluationData, 200d);
				
			} else if("RKPU".equals(stnCd)) {
				
				this.evaluateCig(stnCd, warnEvaluationData, 800d);
				
			} else {
				
				this.evaluateCig(stnCd, warnEvaluationData, warnData.getCig());
			}
			
			break;
			
		case HVY_RA:
			
			this.evaluateHvyRa(stnCd, warnEvaluationData);
			
			break;
			
		case HVY_SN:
			
			this.evaluateHvySn(stnCd, warnEvaluationData);
			
			break;		
			
		case SFC_VIS:
			
			switch(stnCd) {
			
			case "RKSI": this.evaluateSfcVis(stnCd, warnEvaluationData, 400d); break;
			case "RKSS": this.evaluateSfcVis(stnCd, warnEvaluationData, 600d); break;
			case "RKPU": this.evaluateSfcVis(stnCd, warnEvaluationData, 1600d); break;
			case "RKPC": this.evaluateSfcVis(stnCd, warnEvaluationData, 800d); break;
			case "RKJB": this.evaluateSfcVis(stnCd, warnEvaluationData, 800d); break;
			case "RKJY": this.evaluateSfcVis(stnCd, warnEvaluationData, 800d); break;
			case "RKNY": this.evaluateSfcVis(stnCd, warnEvaluationData, 800d); break;
			default: this.evaluateSfcVis(stnCd, warnEvaluationData, warnData.getVis());
			
			}
			
			break;
			
		case SFC_WSPD:
			
			this.evaluateSfcWspd(stnCd, warnEvaluationData);
			
			break;
			
		case TS:
			
			this.evaluateTs(stnCd, warnEvaluationData);
			
			break;
		default:
			break;
		
		}
		
		if(warnEvaluationData.isAvailable()) {
			this.evaluatePrev(warnEvaluationData);
		}
		
		return warnEvaluationData;
	}
	
	// 선행시간 계산
	public void evaluatePrev(WarnEvaluationData warnEvaluationData) {
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
			
			Float prevWeight = 0f;
			
			// 현상발생점수가 경보기준도달이라면 가중치 30점
			if(score.getEffct() >= 70f) {
				prevWeight = 30f;
			// 현상발생점수가 유효기준도달이라면 가중치 24점
			} else if(score.getEffct() > 0f && score.getEffct() < 70f) {
				prevWeight = 24f;
			}
			
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
			Date firstArrTm = warnEvaluationData.getFirstArrTm();
			 
			Integer prevMin = (int)(firstArrTm.getTime() - anncTm.getTime()) / 1000 / 60;
			
			Integer firstArrMin = (int)(firstArrTm.getTime() - anncTm.getTime()) / 1000 / 60;
			
			warnEvaluationData.setPrevMin(prevMin);
			warnEvaluationData.setFirstArrMin(firstArrMin);
			
			float minPrevMin = Math.min(prevMin, 120f);
			
			// 정상발표
			if(minPrevMin >= 0) {
				
				Float prevScore = (minPrevMin / 120f) * prevWeight;		
				score.setPrev(prevScore);
				
				System.out.println("=================== 정상임 ===================");
				
			} else if(minPrevMin < 0 && minPrevMin >= -3) {	
				
				score.setPrev(0f);
				
				System.out.println("=================== 지연임 ===================");
				
			} else if(minPrevMin < -3 && minPrevMin >= -10) {
							
				score.setPrev(0f);
				score.setEffct(score.getEffct() / 2);
				
				System.out.println("=================== 지연임 ===================");
				
			} else {
				
				score.setPrev(0f);
				score.setEffct(0f);
			}
			
		} catch (Exception e) {
			
		}
	}
	
	// 강풍 경보 평가
	public void evaluateSfcWspd(String stnCd, WarnEvaluationData warnEvaluationData) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
						
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(anncTm);
			cal.add(Calendar.MINUTE, -FIND_EFFCT_DELAY);
			
			Date _anncTm = cal.getTime();
			
			// 구간별 경보 도달 여부
			boolean[] arrYnList = new boolean[]{false, false};
			
			// 구간별 최초 도달 시각
			Date[] firstArrTmList = new Date[]{null, null};
			
			double[] firstArrValRatioList = new double[]{0d, 0d};
			
			// 구간별 최초 현상 값
			String[] firstArrValList = new String[]{null, null};
			
			String[] firstArrValTypeList = new String[] {null, null};
			
			// 구간별 최종 관측 시각
			Date[] lastObsTmList = new Date[]{null, null};
			
			// 구간별 최종 현상 값
			String[] lastObsValList = new String[]{null, null};
			
			List<Map<String, Object>> amosDataList = this.evaluationDatabaseUtil.getAmosDataListForWarnEvaluate(
					EvaluationUtils.getAirportId(stnCd), sdf.format(_anncTm), sdf.format(edEffctTm));
			
			List<Map<String, Object>> splitAmosDataList = EvaluationUtils.splitAmosDatabyTm(amosDataList);
			
			for(int i=0 ; i<splitAmosDataList.size() ; i++) {				
				
				Map<String, Object> splitAmosData = splitAmosDataList.get(i);
				
				Date tm = sdf.parse(splitAmosData.get("tm").toString());
				
				List<Map<String, Object>> subAmosDataList = (List<Map<String, Object>>)splitAmosData.get("list");
				
				cal = new GregorianCalendar();
				cal.setTime(tm);
				cal.add(Calendar.HOUR_OF_DAY, -9);
				
				tm = cal.getTime();
				
				// 같은 시간대에서 요소별로 최대값을 찾고
				// 서로 ratio 를 비교하여 ratio가 높은값을 찾는다

				Double wspd10MinAvg = EvaluationUtils.findAmosMaxValue(subAmosDataList, "wspd10minAvg");
				Double wspd1MinMax = EvaluationUtils.findAmosMaxValue(subAmosDataList, "wspd1minMax");
				
				double ratio1 = wspd10MinAvg / 25 * 100;
				double ratio2 = wspd1MinMax / 35 * 100;
				
				double ratio = 0d;
				Double value = 0d;
				String valueType = null;
				
				if(ratio1 > ratio2) {
					ratio = ratio1;
					value = wspd10MinAvg;
					valueType = "wspd";
				} else {
					ratio = ratio2;
					value = wspd1MinMax;
					valueType = "gust";
				}
				
				// 평균 풍속 또는 GUST 가 경보기준 이상일때
				if(wspd10MinAvg >= 25 || wspd1MinMax >= 35) {	
					
					// 경보기준값 만족
					arrYnList[0] = true;
					
					if(firstArrTmList[0] == null) {
						
						firstArrTmList[0] = tm;
						firstArrValList[0] = "wspd".equals(valueType) ? value.toString() : "G"+value.toString();
						firstArrValTypeList[0] = valueType;							
						firstArrValRatioList[0] = ratio;
					}
					
					lastObsTmList[0] = tm;
					lastObsValList[0] = "wspd".equals(valueType) ? value.toString() : "G"+value.toString();
					
				} else if((wspd10MinAvg < 25 && wspd10MinAvg >= 20) || 
						  (wspd1MinMax < 35 && wspd1MinMax >= 28)) {
					
					arrYnList[1] = true;
					
					if(ratio > firstArrValRatioList[1]) {
						firstArrTmList[1] = tm;
						firstArrValList[1] = "wspd".equals(valueType) ? value.toString() : "G"+value.toString();
						firstArrValTypeList[1] = valueType;						
						firstArrValRatioList[1] = ratio;
					}
					
					lastObsTmList[1] = tm;
					lastObsValList[1] = "wspd".equals(valueType) ? value.toString() : "G"+value.toString();			
				}
			}
			
			if(arrYnList[0]) {
				
				score.setEffct(70f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[0]);
				warnEvaluationData.setFirstArrVal(firstArrValList[0]);
				
				if(arrYnList[1]) {
					
					if(lastObsTmList[0].getTime() >= lastObsTmList[1].getTime()) {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[0]);
						warnEvaluationData.setLastObsVal(lastObsValList[0]);	
						
					} else {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[1]);
						warnEvaluationData.setLastObsVal(lastObsValList[1]);	
					}
					
				} else {
					
					warnEvaluationData.setLastObsTm(lastObsTmList[0]);
					warnEvaluationData.setLastObsVal(lastObsValList[0]);	
				}
				
			} else if(arrYnList[1]) {
				
				Float effctScore = 0f;
				
				if("gust".equals(firstArrValTypeList[1])) {
					effctScore = (float)(Float.valueOf(firstArrValList[1].replace("G", "")) / 35 * 70);
				} else {
					effctScore = (float)(Float.valueOf(firstArrValList[1]) / 25 * 70);
				}
				
				score.setEffct(effctScore);
				warnEvaluationData.setFirstArrTm(firstArrTmList[1]);
				warnEvaluationData.setFirstArrVal(firstArrValList[1]);
				warnEvaluationData.setLastObsTm(lastObsTmList[1]);
				warnEvaluationData.setLastObsVal(lastObsValList[1]);
			}
						
		} catch (Exception e) {
			e.printStackTrace();
			warnEvaluationData.setAvailable(false);
		}
	}
	
	// 천둥번개 경보 평가
	public void evaluateTs(String stnCd, WarnEvaluationData warnEvaluationData) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
			
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(anncTm);
			cal.add(Calendar.MINUTE, -FIND_EFFCT_DELAY);
			
			Date _anncTm = cal.getTime();
			
			// 구간별 경보 도달 여부
			boolean[] arrYnList = new boolean[]{false, false};
			
			// 구간별 최초 도달 시각
			Date[] firstArrTmList = new Date[]{null, null};
			
			// 구간별 최초 현상 값
			String[] firstArrValList = new String[]{null, null};
			
			// 구간별 최종 관측 시각
			Date[] lastObsTmList = new Date[]{null, null};
			
			// 구간별 최종 현상 값
			String[] lastObsValList = new String[]{null, null};
			
			List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(stnCd, sdf.format(_anncTm), sdf.format(edEffctTm));
			
			List<Map<String, Object>> localInfoList = this.evaluationDatabaseUtil.getAmisLocalInfoList(stnCd, sdf.format(_anncTm), sdf.format(edEffctTm));
				
			// METAR 와 LOCAL 를 합성한다
			
			List<Map<String, Object>> obsInfoList = EvaluationUtils.combineMetarLocalList(this.metarParser, stnCd, metarInfoList, localInfoList);
				
			for(int i=0 ; i<obsInfoList.size() ; i++) {
				
				Map<String, Object> obsInfo = obsInfoList.get(i);
				
				Date obsStdTm = sdf.parse((String)obsInfo.get("tm"));
				
				String skyCondition = (String)obsInfo.get("skyCondition");
				List<String> skyConditionList = (List<String>)obsInfo.get("skyConditionList");
				String cbString = (String)obsInfo.get("cbString");
				
				int arrIndex = -1;
				
				// METAR 에 TS 가 있다면				
				for(int j=0 ; j<skyConditionList.size() ; j++) {
				
					if(skyConditionList.get(j).contains("TS") && !skyConditionList.get(j).contains("VCTS")) {
						arrIndex = 0;			
					}
				}
				
				if(arrIndex < 0) {
				
					for(int j=0 ; j<skyConditionList.size() ; j++) {
						
						if(skyConditionList.get(j).contains("VCTS")) {
							arrIndex = 1;			
						}
					}
				}
				
				if(arrIndex < 0) {
					
					if(cbString.length() > 0) {
						arrIndex = 1;
						skyCondition = cbString;
					}
				}
				
				if(arrIndex < 0) {
					continue;
				}
				
				arrYnList[arrIndex] = true;
				
				if(firstArrTmList[arrIndex] == null) {
					firstArrTmList[arrIndex] = obsStdTm;
					firstArrValList[arrIndex] = skyCondition;					
					lastObsTmList[arrIndex] = obsStdTm;
					lastObsValList[arrIndex] = skyCondition;
				} else {
					lastObsTmList[arrIndex] = obsStdTm;
					lastObsValList[arrIndex] = skyCondition;
					
					if(arrIndex == 1) {
						lastObsTmList[0] = obsStdTm;
						lastObsValList[0] = skyCondition;
					}
				}	
			}
			
			if(arrYnList[0]) {
				score.setEffct(70f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[0]);
				warnEvaluationData.setFirstArrVal(firstArrValList[0]);
				
				if(arrYnList[1]) {
					
					if(lastObsTmList[0].getTime() >= lastObsTmList[1].getTime()) {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[0]);
						warnEvaluationData.setLastObsVal(lastObsValList[0]);	
						
					} else {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[1]);
						warnEvaluationData.setLastObsVal(lastObsValList[1]);	
					}
					
				} else {
					
					warnEvaluationData.setLastObsTm(lastObsTmList[0]);
					warnEvaluationData.setLastObsVal(lastObsValList[0]);	
				}
				
			} else if(arrYnList[1]) {
				
				score.setEffct(56f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[1]);
				warnEvaluationData.setFirstArrVal(firstArrValList[1]);
				warnEvaluationData.setLastObsTm(lastObsTmList[1]);
				warnEvaluationData.setLastObsVal(lastObsValList[1]);
			}
						
		} catch (Exception e) {
			e.printStackTrace();
			warnEvaluationData.setAvailable(false);
		}
	}
	
	// 운고 경보 평가
	public void evaluateCig(String stnCd, WarnEvaluationData warnEvaluationData, Double cig) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
			
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(anncTm);
			cal.add(Calendar.MINUTE, -FIND_EFFCT_DELAY);
			
			Date _anncTm = cal.getTime();
			
			// 구간별 경보 도달 여부
			boolean[] arrYnList = new boolean[]{false, false};
			
			// 구간별 최초 도달 시각
			Date[] firstArrTmList = new Date[]{null, null};
			
			// 구간별 최초 현상 값
			String[] firstArrValList = new String[]{null, null};
			
			// 구간별 최종 관측 시각
			Date[] lastObsTmList = new Date[]{null, null};
			
			// 구간별 최종 현상 값
			String[] lastObsValList = new String[]{null, null};
			
			List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(stnCd, sdf.format(_anncTm), sdf.format(edEffctTm));
			
			List<Map<String, Object>> localInfoList = this.evaluationDatabaseUtil.getAmisLocalInfoList(stnCd, sdf.format(_anncTm), sdf.format(edEffctTm));
			
			// METAR 와 LOCAL 를 합성한다
			
			List<Map<String, Object>> obsInfoList = EvaluationUtils.combineMetarLocalList(this.metarParser, stnCd, metarInfoList, localInfoList);
			
			for(int i=0 ; i<obsInfoList.size() ; i++) {
				
				Map<String, Object> obsInfo = obsInfoList.get(i);
				
				Date obsStdTm = sdf.parse((String)obsInfo.get("tm"));
					
				Double lowestBknOvcHeight = (Double)obsInfo.get("lowestBknOvcHeight"); 
				
				if(lowestBknOvcHeight == null) {
					continue;
				}
				
				int arrIndex = -1;
				
				if(lowestBknOvcHeight <= cig) {
					arrIndex = 0;
				}
				
				if(lowestBknOvcHeight > cig && lowestBknOvcHeight <= cig + 100) {
					arrIndex = 1;
				}
				
				if(arrIndex < 0) {
					continue;
				}
				
				arrYnList[arrIndex] = true;
				
				if(firstArrTmList[arrIndex] == null) {
					firstArrTmList[arrIndex] = obsStdTm;
					firstArrValList[arrIndex] = lowestBknOvcHeight.toString();
					lastObsTmList[arrIndex] = obsStdTm;
					lastObsValList[arrIndex] = lowestBknOvcHeight.toString();					
				} else {
					
					lastObsTmList[arrIndex] = obsStdTm;
					lastObsValList[arrIndex] = lowestBknOvcHeight.toString();
					
					if(arrIndex == 1) {
						lastObsTmList[0] = obsStdTm;
						lastObsValList[0] = lowestBknOvcHeight.toString();
					}
				}					
			}
			
			if(arrYnList[0]) {
				
				score.setEffct(70f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[0]);
				warnEvaluationData.setFirstArrVal(firstArrValList[0]);
				
				if(arrYnList[1]) {
					
					if(lastObsTmList[0].getTime() >= lastObsTmList[1].getTime()) {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[0]);
						warnEvaluationData.setLastObsVal(lastObsValList[0]);	
						
					} else {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[1]);
						warnEvaluationData.setLastObsVal(lastObsValList[1]);	
					}
					
				} else {
					
					warnEvaluationData.setLastObsTm(lastObsTmList[0]);
					warnEvaluationData.setLastObsVal(lastObsValList[0]);	
				}
				
			} else if(arrYnList[1]) {
				
				score.setEffct(56f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[1]);
				warnEvaluationData.setFirstArrVal(firstArrValList[1]);
				warnEvaluationData.setLastObsTm(lastObsTmList[1]);
				warnEvaluationData.setLastObsVal(lastObsValList[1]);
			}
						
		} catch (Exception e) {
			e.printStackTrace();
			warnEvaluationData.setAvailable(false);
		}
	}
	
	// 저시정 경보 평가
	public void evaluateSfcVis(String stnCd, WarnEvaluationData warnEvaluationData, Double vis) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
			
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(anncTm);
			cal.add(Calendar.MINUTE, -FIND_EFFCT_DELAY);
			
			Date _anncTm = cal.getTime();
		
			// 구간별 경보 도달 여부
			boolean[] arrYnList = new boolean[]{false, false};
			
			// 구간별 최초 도달 시각
			Date[] firstArrTmList = new Date[]{null, null};
			
			// 구간별 최초 현상 값
			String[] firstArrValList = new String[]{null, null};
			
			// 구간별 최종 관측 시각
			Date[] lastObsTmList = new Date[]{null, null};
			
			// 구간별 최종 현상 값
			String[] lastObsValList = new String[]{null, null};
			
			List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(stnCd, sdf.format(_anncTm), sdf.format(edEffctTm));
			
			List<Map<String, Object>> localInfoList = this.evaluationDatabaseUtil.getAmisLocalInfoList(stnCd, sdf.format(_anncTm), sdf.format(edEffctTm));
			
			// METAR 와 LOCAL 를 합성한다
			
			List<Map<String, Object>> obsInfoList = EvaluationUtils.combineMetarLocalList(this.metarParser, stnCd, metarInfoList, localInfoList);
			
			for(int i=0 ; i<obsInfoList.size() ; i++) {
				
				Map<String, Object> obsInfo = obsInfoList.get(i);
					
				Date obsStdTm = sdf.parse((String)obsInfo.get("tm"));
				
				Double obsVis = (Double)obsInfo.get("vis");
				
				if(obsVis == null) {
					continue;
				}
				
				int arrIndex = -1;
				
				if(obsVis <= vis) {
					arrIndex = 0;
				}
				
				if(obsVis > vis && obsVis <= vis + 200) {
					arrIndex = 1;
				}
				
				if(arrIndex < 0) {
					continue;
				}
				
				arrYnList[arrIndex] = true;
				
				if(firstArrTmList[arrIndex] == null) {
					firstArrTmList[arrIndex] = obsStdTm;
					firstArrValList[arrIndex] = obsVis.toString();
					lastObsTmList[arrIndex] = obsStdTm;
					lastObsValList[arrIndex] = obsVis.toString();					
				} else {
					lastObsTmList[arrIndex] = obsStdTm;
					lastObsValList[arrIndex] = obsVis.toString();
					
					if(arrIndex == 1) {
						lastObsTmList[0] = obsStdTm;
						lastObsValList[0] = obsVis.toString();
					}
				}					
			}
			
			if(arrYnList[0]) {
				
				score.setEffct(70f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[0]);
				warnEvaluationData.setFirstArrVal(firstArrValList[0]);
				
				if(arrYnList[1]) {
					
					if(lastObsTmList[0].getTime() >= lastObsTmList[1].getTime()) {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[0]);
						warnEvaluationData.setLastObsVal(lastObsValList[0]);	
						
					} else {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[1]);
						warnEvaluationData.setLastObsVal(lastObsValList[1]);	
					}
					
				} else {
					
					warnEvaluationData.setLastObsTm(lastObsTmList[0]);
					warnEvaluationData.setLastObsVal(lastObsValList[0]);	
				}
				
			} else if(arrYnList[1]) {
				
				score.setEffct(56f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[1]);
				warnEvaluationData.setFirstArrVal(firstArrValList[1]);
				warnEvaluationData.setLastObsTm(lastObsTmList[1]);
				warnEvaluationData.setLastObsVal(lastObsValList[1]);
			}
						
		} catch (Exception e) {
			e.printStackTrace();
			warnEvaluationData.setAvailable(false);
		}
	}
	
	// 대설 경보 평가
	public void evaluateHvySn(String stnCd, WarnEvaluationData warnEvaluationData) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
			
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(anncTm);
			cal.add(Calendar.MINUTE, -FIND_EFFCT_DELAY);
			
			Date _anncTm = cal.getTime();
		
			Double maxFrsc = 0d;
			
			// 구간별 경보 도달 여부
			boolean[] arrYnList = new boolean[]{false, false};
			
			// 구간별 최초 도달 시각
			Date[] firstArrTmList = new Date[]{null, null};
			
			// 구간별 최초 현상 값
			String[] firstArrValList = new String[]{null, null};
			
			// 구간별 최종 관측 시각
			Date[] lastObsTmList = new Date[]{null, null};
			
			// 구간별 최종 현상 값
			String[] lastObsValList = new String[]{null, null};
			
			List<Map<String, Object>> metarObsDataList = this.evaluationDatabaseUtil.getAmisMetarObsDataList(stnCd, sdf.format(_anncTm), sdf.format(edEffctTm));
			
			for(int i=0 ; i<metarObsDataList.size() ; i++) {
				
				Map<String, Object> metarObsData = metarObsDataList.get(i);
					
				Date metarTm = sdf.parse((String)metarObsData.get("tm"));
				
				Double frsc = (Double)metarObsData.get("frsc");
				
				if(frsc == null) {
					continue;
				}
				
				maxFrsc = Math.max(maxFrsc, frsc);
				
				int arrIndex = -1;
				
				if(frsc >= 3) {
					arrIndex = 0;
				}
				
				if(frsc < 3 && frsc >= 2.4) {
					arrIndex = 1;
				}
				
				if(arrIndex < 0) {
					continue;
				}
				
				arrYnList[arrIndex] = true;
				
				if(firstArrTmList[arrIndex] == null) {
					firstArrTmList[arrIndex] = metarTm;
					firstArrValList[arrIndex] = frsc.toString();
					lastObsTmList[arrIndex] = metarTm;
					lastObsValList[arrIndex] = frsc.toString();					
				} else {
					lastObsTmList[arrIndex] = metarTm;
					lastObsValList[arrIndex] = frsc.toString();
					
					if(arrIndex == 1) {
						lastObsTmList[0] = metarTm;
						lastObsValList[0] = frsc.toString();
					}
				}					
			}
			
			if(arrYnList[0]) {
				
				score.setEffct(70f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[0]);
				warnEvaluationData.setFirstArrVal(firstArrValList[0]);
				
				if(arrYnList[1]) {
					
					if(lastObsTmList[0].getTime() >= lastObsTmList[1].getTime()) {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[0]);
						warnEvaluationData.setLastObsVal(lastObsValList[0]);	
						
					} else {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[1]);
						warnEvaluationData.setLastObsVal(lastObsValList[1]);	
					}
					
				} else {
					
					warnEvaluationData.setLastObsTm(lastObsTmList[0]);
					warnEvaluationData.setLastObsVal(lastObsValList[0]);	
				}
				
			} else if(arrYnList[1]) {
				
				score.setEffct((float)(maxFrsc / 3f * 70f));
				warnEvaluationData.setFirstArrTm(firstArrTmList[1]);
				warnEvaluationData.setFirstArrVal(firstArrValList[1]);
				warnEvaluationData.setLastObsTm(lastObsTmList[1]);
				warnEvaluationData.setLastObsVal(lastObsValList[1]);
			}
						
		} catch (Exception e) {
			e.printStackTrace();
			warnEvaluationData.setAvailable(false);
		}
	}
	
	// 강풍 경보 평가
	public void evaluateHvyRa(String stnCd, WarnEvaluationData warnEvaluationData) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
						
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(anncTm);
			cal.add(Calendar.MINUTE, -FIND_EFFCT_DELAY);
			
			Date _anncTm = cal.getTime();
			
			// 구간별 경보 도달 여부
			boolean[] arrYnList = new boolean[]{false, false};
			
			// 구간별 최초 도달 시각
			Date[] firstArrTmList = new Date[]{null, null};
			
			// 구간별 최초 현상 값
			String[] firstArrValList = new String[]{null, null};
			
			double[] firstArrValRatioList = new double[]{0d, 0d};
			
			String[] firstArrValTypeList = new String[] {null, null};
			
			// 구간별 최종 관측 시각
			Date[] lastObsTmList = new Date[]{null, null};
			
			// 구간별 최종 현상 값
			String[] lastObsValList = new String[]{null, null};
			
			List<Map<String, Object>> amosDataList = this.evaluationDatabaseUtil.getAmosDataListForWarnEvaluate(
					EvaluationUtils.getAirportId(stnCd), sdf.format(_anncTm), sdf.format(edEffctTm));
			
			List<Map<String, Object>> splitAmosDataList = EvaluationUtils.splitAmosDatabyTm(amosDataList);
			
			for(int i=0 ; i<splitAmosDataList.size() ; i++) {				
				
				Map<String, Object> splitAmosData = splitAmosDataList.get(i);
				
				Date tm = sdf.parse(splitAmosData.get("tm").toString());
				
				List<Map<String, Object>> subAmosDataList = (List<Map<String, Object>>)splitAmosData.get("list");
				
				cal = new GregorianCalendar();
				cal.setTime(tm);
				cal.add(Calendar.HOUR_OF_DAY, -9);
				
				tm = cal.getTime();
				
				// 같은 시간대에서 요소별로 최대값을 찾고
				// 서로 ratio 를 비교하여 ratio가 높은값을 찾는다

				Double rn3Hr = EvaluationUtils.findAmosMaxValue(subAmosDataList, "rn3hr");
				Double rn12Hr = EvaluationUtils.findAmosMaxValue(subAmosDataList, "rn12hr");
				
				if(rn3Hr == null || rn12Hr == null) {
					continue;
				}
				
				double ratio1 = rn3Hr / 60 * 100;
				double ratio2 = rn12Hr / (60 + 50) * 100;
				
				double ratio = 0d;
				Double value = 0d;
				String valueType = null;
				
				if(ratio1 > ratio2) {
					ratio = ratio1;
					value = rn3Hr;
					valueType = "rn3Hr";
				} else {
					ratio = ratio2;
					value = rn12Hr;
					valueType = "rn12Hr";
				}
				
				if(rn3Hr >= 60 || rn12Hr >= 60 + 50) {
					
					// 경보기준값 만족
					arrYnList[0] = true;
					
					if(firstArrTmList[0] == null) {
						
						firstArrTmList[0] = tm;
						firstArrValList[0] = value.toString();
						firstArrValTypeList[0] = valueType;							
						firstArrValRatioList[0] = ratio;
					}
					
					lastObsTmList[0] = tm;
					lastObsValList[0] = value.toString();
					
				} else if((rn3Hr < 60 && rn3Hr >= 48) || 
						  (rn12Hr < 60 + 50 && rn12Hr >= 88)) {
					
					arrYnList[1] = true;
					
					if(ratio > firstArrValRatioList[1]) {
						firstArrTmList[1] = tm;
						firstArrValList[1] = value.toString();
						firstArrValTypeList[1] = valueType;						
						firstArrValRatioList[1] = ratio;
					}
					
					lastObsTmList[1] = tm;
					lastObsValList[1] = value.toString();
				}
			}
			
			if(arrYnList[0]) {
				
				score.setEffct(70f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[0]);
				warnEvaluationData.setFirstArrVal(firstArrValList[0]);
				
				if(arrYnList[1]) {
					
					if(lastObsTmList[0].getTime() >= lastObsTmList[1].getTime()) {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[0]);
						warnEvaluationData.setLastObsVal(lastObsValList[0]);	
						
					} else {
						
						warnEvaluationData.setLastObsTm(lastObsTmList[1]);
						warnEvaluationData.setLastObsVal(lastObsValList[1]);	
					}
					
				} else {
					
					warnEvaluationData.setLastObsTm(lastObsTmList[0]);
					warnEvaluationData.setLastObsVal(lastObsValList[0]);	
				}
				
			} else if(arrYnList[1]) {
				
				Float effctScore = 0f;
				
				if("rn12Hr".equals(firstArrValTypeList[1])) {
					effctScore = (float)(Float.valueOf(firstArrValList[1]) / (60 + 50) * 70);
				} else {
					effctScore = (float)(Float.valueOf(firstArrValList[1]) / 60 * 70);
				}
				
				score.setEffct(effctScore);
				warnEvaluationData.setFirstArrTm(firstArrTmList[1]);
				warnEvaluationData.setFirstArrVal(firstArrValList[1]);
				warnEvaluationData.setLastObsTm(lastObsTmList[1]);
				warnEvaluationData.setLastObsVal(lastObsValList[1]);
			}
						
		} catch (Exception e) {
			e.printStackTrace();
			warnEvaluationData.setAvailable(false);
		}
	}
}
