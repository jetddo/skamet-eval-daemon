package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.metar.MetarData;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.metar.MetarParser;
import kama.daemon.eval.warn.WarnData;
import kama.daemon.util.DaemonUtil;
import kama.daemon.util.EvaluationUtils;

public class WarnEvaluatorVer1 extends WarnEvaluator {
	
	private MetarParser metarParser;
	
	private final int DELAY_INTERVAL = 3;
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	public WarnEvaluatorVer1(EvaluationDatabaseUtil evaluationDatabaseUtil) {
		
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
			
			this.evaluateHvyRa(stnCd, warnEvaluationData, 60d);
			
			break;
			
		case HVY_SN:
			
			this.evaluateHvySn(stnCd, warnEvaluationData, warnData.getSn());
			
			break;		
			
		case SFC_VIS:
			
			if("RKSI".equals(stnCd)) {
				
				this.evaluateSfcVis(stnCd, warnEvaluationData, 400d);
				
			} else if("RKSS".equals(stnCd)) {
				
				this.evaluateSfcVis(stnCd, warnEvaluationData, 600d);
				
			} else if("RKPU".equals(stnCd)) {
				
				this.evaluateSfcVis(stnCd, warnEvaluationData, 1600d);
				
			} else {
				
				this.evaluateSfcVis(stnCd, warnEvaluationData, warnData.getVis());
			}
			
			break;
			
		case SFC_WSPD:
			
			this.evaluateSfcWspd(stnCd, warnEvaluationData, warnData.getWspd(), warnData.getMaxWspd());
			
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
			
			if(score.getEffct() >= 50f) {
				prevWeight = 50f;
			} else if(score.getEffct() >= 40f && score.getEffct() < 50f) {
				prevWeight = 40f;
			}
			
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
			Date firstArrTm = warnEvaluationData.getFirstArrTm();
			
			Integer prevMin = (int)(stEffctTm.getTime() - anncTm.getTime()) / 1000 / 60;
			
			Integer firstArrMin = (int)(firstArrTm.getTime() - anncTm.getTime()) / 1000 / 60;
			
			warnEvaluationData.setPrevMin(prevMin);
			warnEvaluationData.setFirstArrMin(firstArrMin);
			
			Float prevScore = 0f;
			
			float minFirstArrMin = Math.min(firstArrMin, 120f);
			
			if(minFirstArrMin < 0 && minFirstArrMin + DELAY_INTERVAL >= 0) {
				minFirstArrMin = 0;
			}
			
			// 현상이 유효시간 내에 도달한 경우
			if(firstArrTm.getTime() >= stEffctTm.getTime() && firstArrTm.getTime() <= edEffctTm.getTime()) {
				
				prevScore = (Math.min(prevMin, 120f) / 120f) * prevWeight;
				
			// 현상이 유효시작시간 전에 도달한 경우
			} else if(firstArrTm.getTime() < stEffctTm.getTime()) {
				
				Float f1 = Math.min(minFirstArrMin, 120f) / 120f;
				
				Float f2 = firstArrMin / (prevMin * 1f);
				
				prevScore = f1 * f2 * prevWeight;			
			}
			
			if(prevScore < 0) {
				score.setEffct(score.getEffct() / 2);
				score.setPrev(0f);
			} else {
				score.setPrev(prevScore);
			}
			
		} catch (Exception e) {
			
		}
	}
	
	// 강풍 경보 평가
	public void evaluateSfcWspd(String stnCd, WarnEvaluationData warnEvaluationData, Double wspd, Double maxWspd) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
						
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
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
					EvaluationUtils.getAirportId(stnCd), sdf.format(anncTm), sdf.format(edEffctTm));
			
			for(int i=0 ; i<amosDataList.size() ; i++) {
				
				Map<String, Object> amosData = amosDataList.get(i);
				
				Date tm = sdf.parse(amosData.get("tm").toString());
				
				Calendar cal = new GregorianCalendar();
				cal.setTime(tm);
				cal.add(Calendar.HOUR_OF_DAY, -9);
				
				tm = cal.getTime();
				
				Double wspd10MinAvg = Double.valueOf(amosData.get("wspd10minAvg").toString());
				Double wspd1MinMax = Double.valueOf(amosData.get("wspd1minMax").toString());
				
				// 평균 풍속 또는 GUST 가 경보기준 이상일때
				if(wspd10MinAvg >= wspd || wspd1MinMax >= maxWspd) {
					
					// 경보기준값 만족
					arrYnList[0] = true;
										
					if(wspd10MinAvg >= wspd) {		
						
						double ratio = wspd10MinAvg / wspd * 100;
						
						if(firstArrTmList[0] == null) {
							
							firstArrTmList[0] = tm;
							firstArrValList[0] = wspd10MinAvg.toString();
							firstArrValTypeList[0] = "wspd";
							
							firstArrValRatioList[0] = ratio;
						}
						
						if(lastObsTmList[0] != null && lastObsValList[0] != null) {
							
							// 활주로 다른 방향인 경우 대비
							if(lastObsTmList[0].getTime() == tm.getTime()) {
								
								if(EvaluationUtils.compareSfcWspdRatio(lastObsValList[0], wspd10MinAvg.toString(), wspd, maxWspd) == 2) {
									lastObsValList[0] = wspd10MinAvg.toString();
								}
								
							} else {
								lastObsTmList[0] = tm;
								lastObsValList[0] = wspd10MinAvg.toString();			
							}
							
						} else {
							
							lastObsTmList[0] = tm;
							lastObsValList[0] = wspd10MinAvg.toString();			
						}					
					} 
					
					if (wspd1MinMax >= maxWspd){
						
						double ratio = wspd1MinMax / maxWspd * 100;
						
						if(firstArrTmList[0] == null) {
							
							firstArrTmList[0] = tm;
							firstArrValList[0] = "G" + wspd1MinMax.toString();
							firstArrValTypeList[0] = "gust";
							
							firstArrValRatioList[0] = ratio;
						}
						
						if(lastObsTmList[0] != null && lastObsValList[0] != null) {
							
							// 활주로 다른 방향인 경우 대비
							if(lastObsTmList[0].getTime() == tm.getTime()) {
								
								if(EvaluationUtils.compareSfcWspdRatio(lastObsValList[0], "G" + wspd1MinMax.toString(), wspd, maxWspd) == 2) {
									lastObsValList[0] = "G" + wspd1MinMax.toString();			
								}
								
							} else {
								lastObsTmList[0] = tm;
								lastObsValList[0] = "G" + wspd1MinMax.toString();			
							}
							
						} else {
							
							lastObsTmList[0] = tm;
							lastObsValList[0] = "G" + wspd1MinMax.toString();	
						}
					}
					
				} else if((wspd10MinAvg < wspd && wspd10MinAvg >= wspd-5) || 
						  (wspd1MinMax < maxWspd && wspd1MinMax >= maxWspd-7)) {
					
					arrYnList[1] = true;
					
					if(wspd10MinAvg < wspd && wspd10MinAvg >= wspd-5) {
						
						double ratio = wspd10MinAvg / wspd * 100;
						
						if(ratio > firstArrValRatioList[1]) {
							firstArrTmList[1] = tm;
							firstArrValList[1] = wspd10MinAvg.toString();
							firstArrValTypeList[1] = "wspd";
							
							firstArrValRatioList[1] = ratio;
						}

						if(lastObsTmList[1] != null && lastObsValList[1] != null) {
							
							// 활주로 다른 방향인 경우 대비
							if(lastObsTmList[1].getTime() == tm.getTime()) {
								
								if(EvaluationUtils.compareSfcWspdRatio(lastObsValList[1], wspd10MinAvg.toString(), wspd, maxWspd) == 2) {
									lastObsValList[1] = wspd10MinAvg.toString();
								}
								
							} else {
								lastObsTmList[1] = tm;
								lastObsValList[1] = wspd10MinAvg.toString();			
							}
							
						} else {
							
							lastObsTmList[1] = tm;
							lastObsValList[1] = wspd10MinAvg.toString();			
						}						
					} 
					
					if(wspd1MinMax < maxWspd && wspd1MinMax >= maxWspd-7) {
						
						double ratio = wspd1MinMax / maxWspd * 100;							
						
						if(ratio > firstArrValRatioList[1]) {
							firstArrTmList[1] = tm;
							firstArrValList[1] = "G" + wspd1MinMax.toString();
							firstArrValTypeList[1] = "gust";
							
							firstArrValRatioList[1] = ratio;
						}
						
						if(lastObsTmList[1] != null && lastObsValList[1] != null) {
							
							// 활주로 다른 방향인 경우 대비
							if(lastObsTmList[1].getTime() == tm.getTime()) {
								
								if(EvaluationUtils.compareSfcWspdRatio(lastObsValList[1], "G" + wspd1MinMax.toString(), wspd, maxWspd) == 2) {
									lastObsValList[1] = "G" + wspd1MinMax.toString();			
								}
								
							} else {
								lastObsTmList[1] = tm;
								lastObsValList[1] = "G" + wspd1MinMax.toString();			
							}
							
						} else {
							
							lastObsTmList[1] = tm;
							lastObsValList[1] = "G" + wspd1MinMax.toString();	
						}
					}					
				}
			}
			
			if(arrYnList[0]) {
				
				score.setEffct(50f);
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
					effctScore = (float)(Float.valueOf(firstArrValList[1].replace("G", "")) / maxWspd * 50);
				} else {
					effctScore = (float)(Float.valueOf(firstArrValList[1]) / wspd * 50);
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
			
			List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(stnCd, sdf.format(anncTm), sdf.format(edEffctTm));
			
			List<Map<String, Object>> localInfoList = this.evaluationDatabaseUtil.getAmisLocalInfoList(stnCd, sdf.format(anncTm), sdf.format(edEffctTm));
			
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
				
				score.setEffct(50f);
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
				
				score.setEffct(40f);
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
			
			List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(stnCd, sdf.format(anncTm), sdf.format(edEffctTm));
			
			List<Map<String, Object>> localInfoList = this.evaluationDatabaseUtil.getAmisLocalInfoList(stnCd, sdf.format(anncTm), sdf.format(edEffctTm));
			
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
				
				score.setEffct(50f);
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
				
				score.setEffct(40f);
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
			
			List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(stnCd, sdf.format(anncTm), sdf.format(edEffctTm));
			
			List<Map<String, Object>> localInfoList = this.evaluationDatabaseUtil.getAmisLocalInfoList(stnCd, sdf.format(anncTm), sdf.format(edEffctTm));
			
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
				
				score.setEffct(50f);
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
				
				score.setEffct(40f);
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
	public void evaluateHvySn(String stnCd, WarnEvaluationData warnEvaluationData, Double sn) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
			
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
		
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
			
			List<Map<String, Object>> metarObsDataList = this.evaluationDatabaseUtil.getAmisMetarObsDataList(stnCd, sdf.format(anncTm), sdf.format(edEffctTm));
			
			for(int i=0 ; i<metarObsDataList.size() ; i++) {
				
				Map<String, Object> metarObsData = metarObsDataList.get(i);
					
				Date metarTm = sdf.parse((String)metarObsData.get("tm"));
				
				Double frsc = (Double)metarObsData.get("frsc");
				
				if(frsc == null) {
					continue;
				}
				
				maxFrsc = Math.max(maxFrsc, frsc);
				
				int arrIndex = -1;
				
				if(frsc >= sn) {
					arrIndex = 0;
				}
				
				if(frsc < sn && frsc >= sn - 0.3d) {
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
				
				score.setEffct(50f);
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
				
				score.setEffct((float)(maxFrsc / 3f * 50f));
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
	public void evaluateHvyRa(String stnCd, WarnEvaluationData warnEvaluationData, Double ra) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
						
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
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
					EvaluationUtils.getAirportId(stnCd), sdf.format(anncTm), sdf.format(edEffctTm));
			
			for(int i=0 ; i<amosDataList.size() ; i++) {
				
				Map<String, Object> amosData = amosDataList.get(i);
				
				Date tm = sdf.parse(amosData.get("tm").toString());
				
				Calendar cal = new GregorianCalendar();
				cal.setTime(tm);
				cal.add(Calendar.HOUR_OF_DAY, -9);
				
				tm = cal.getTime();
				
				Double rn3Hr = null;
				Double rn12Hr = null;
				
				if(amosData.get("rn3hr") != null && amosData.get("rn12hr") != null) {
					
					rn3Hr = Double.valueOf(amosData.get("rn3hr").toString());
					rn12Hr = Double.valueOf(amosData.get("rn12hr").toString());					
				}
				
				if(rn3Hr == null || rn12Hr == null) {
					continue;
				}
				
				if(rn3Hr >= ra || rn12Hr >= ra + 50) {
					
					arrYnList[0] = true;
					
					if(rn3Hr >= ra) {			
						
						double ratio = rn3Hr / ra * 100;
						
						if(firstArrTmList[0] == null) {
							firstArrTmList[0] = tm;
							firstArrValList[0] = rn3Hr.toString();
							firstArrValTypeList[0] = "3hr";
							
							firstArrValRatioList[0] = ratio;
						}							
						
						lastObsTmList[0] = tm;
						lastObsValList[0] = rn3Hr.toString();						
					} 
					
					if(rn12Hr >= ra + 50) {
						
						double ratio = rn12Hr / (ra+50) * 100;
						
						if(firstArrTmList[0] == null) {
							firstArrTmList[0] = tm;
							firstArrValList[0] = rn12Hr.toString();
							firstArrValTypeList[0] = "12hr";
							
							firstArrValRatioList[0] = ratio;
						}
						
						lastObsTmList[0] = tm;
						lastObsValList[0] = rn3Hr.toString();
					}
					
				} else if((rn3Hr < ra && rn3Hr >= 48) || 
						  (rn12Hr < ra + 50 && rn12Hr >= 88)) {
					
					arrYnList[1] = true;
					
					if(rn3Hr < ra && rn3Hr >= 48) {
						
						double ratio = rn3Hr / ra * 100;
						
						if(ratio > firstArrValRatioList[1]) {
							firstArrTmList[1] = tm;
							firstArrValList[1] = rn3Hr.toString();
							firstArrValTypeList[1] = "3hr";
							
							firstArrValRatioList[1] = ratio;
						}
						
						lastObsTmList[1] = tm;
						lastObsValList[1] = rn3Hr.toString();						
					} 

					if(rn12Hr < ra + 50 && rn12Hr >= 88) {
						
						double ratio = rn12Hr / (ra+50) * 100;
						
						if(ratio > firstArrValRatioList[1]) {
							firstArrTmList[1] = tm;
							firstArrValList[1] = rn12Hr.toString();
							firstArrValTypeList[1] = "12hr";
							
							firstArrValRatioList[1] = ratio;
						}
						
						lastObsTmList[1] = tm;
						lastObsValList[1] = rn12Hr.toString();
					}
				}
			}
			
			if(arrYnList[0]) {
				
				score.setEffct(50f);
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
				
				if("12hr".equals(firstArrValTypeList[1])) {
					effctScore = (float)(Float.valueOf(firstArrValList[1]) / (ra + 50) * 50);
				} else {
					effctScore = (float)(Float.valueOf(firstArrValList[1]) / (ra) * 50);
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
