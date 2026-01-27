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

public class WarnEvaluatorVer2 extends WarnEvaluator {
	
	private MetarParser metarParser;
	
	private final int DELAY_INTERVAL = 3;
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
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
		
		// 경보 취소 정보가 있는 경우 경보 종료시점을 변경한다
		if(stCnlTm != null) {
			_EdEffctTm = stCnlTm;
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
			
			this.evaluateHvyRa(stnCd, warnEvaluationData, warnData.getRa());
			
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
			
			if(score.getEffct() >= 70f) {
				prevWeight = 30f;
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
			
			if(minPrevMin < 0 && minPrevMin + DELAY_INTERVAL >= 0) {
				minPrevMin = 0;
			}
			
			Float prevScore = (minPrevMin / 120f) * prevWeight;
						
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
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(anncTm);
			cal.add(Calendar.MINUTE, -DELAY_INTERVAL);
			
			Date _anncTm = cal.getTime();
			
			Double maxWspd10MinAvg = 0d;
			Double maxWspd1MinMax = 0d;
			
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
			
			List<Map<String, Object>> amosDataList = this.evaluationDatabaseUtil.getAmosDataListForWarnEvaluate(
					EvaluationUtils.getAirportId(stnCd), sdf.format(_anncTm), sdf.format(edEffctTm));
			
			for(int i=0 ; i<amosDataList.size() ; i++) {
				
				Map<String, Object> amosData = amosDataList.get(i);
				
				Date tm = sdf.parse(amosData.get("tm").toString());
				
				cal = new GregorianCalendar();
				cal.setTime(tm);
				cal.add(Calendar.HOUR_OF_DAY, -9);
				
				tm = cal.getTime();
				
				Double wspd10MinAvg = Double.valueOf(amosData.get("wspd10minAvg").toString());
				Double wspd1MinMax = Double.valueOf(amosData.get("wspd1minMax").toString());
				
				maxWspd10MinAvg = Math.max(maxWspd10MinAvg, wspd10MinAvg);
				maxWspd1MinMax = Math.max(maxWspd1MinMax, wspd1MinMax);
				
				if(wspd10MinAvg >= wspd || wspd1MinMax >= maxWspd) {
					
					arrYnList[0] = true;
					
					if(firstArrTmList[0] == null) {
						
						firstArrTmList[0] = tm;
						lastObsTmList[0] = tm;
						
						if(wspd10MinAvg >= wspd) {						
							
							firstArrValList[0] = wspd10MinAvg.toString();
							lastObsValList[0] = wspd10MinAvg.toString();
							
						} else {
							
							firstArrValList[0] = "G" + wspd1MinMax.toString();
							lastObsValList[0] = "G" + wspd1MinMax.toString();
						}
						
					} else {
						
						lastObsTmList[0] = tm;
						
						if(wspd10MinAvg >= wspd) {						
							
							lastObsValList[0] = wspd10MinAvg.toString();
							
						} else {
							
							lastObsValList[0] = "G" + wspd1MinMax.toString();
						}
					}
					
				} else if((wspd10MinAvg < wspd && wspd10MinAvg >= wspd-3) || 
						  (wspd1MinMax < maxWspd && wspd1MinMax >= maxWspd-4)) {
					
					arrYnList[1] = true;
					
					if(firstArrTmList[1] == null) {
						
						firstArrTmList[1] = tm;
						lastObsTmList[0] = tm;
						lastObsTmList[1] = tm;
						
						if(wspd10MinAvg < wspd && wspd10MinAvg >= wspd-3) {
							
							firstArrValList[1] = wspd10MinAvg.toString();
							lastObsValList[0] = wspd10MinAvg.toString();
							lastObsValList[1] = wspd10MinAvg.toString();
							
						} else {
							
							firstArrValList[1] = "G" + wspd1MinMax.toString();
							lastObsValList[0] = "G" + wspd1MinMax.toString();
							lastObsValList[1] = "G" + wspd1MinMax.toString();
						}
						
					} else {
						
						lastObsTmList[0] = tm;
						lastObsTmList[1] = tm;
						
						if(wspd10MinAvg < wspd && wspd10MinAvg >= wspd-3) {
							
							lastObsValList[0] = wspd10MinAvg.toString();
							lastObsValList[1] = wspd10MinAvg.toString();
							
						} else {
							
							lastObsValList[0] = "G" + wspd1MinMax.toString();
							lastObsValList[1] = "G" + wspd1MinMax.toString();
						}
					}
				}
			}
			
			if(arrYnList[0]) {
				
				score.setEffct(70f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[0]);
				warnEvaluationData.setFirstArrVal(firstArrValList[0]);
				warnEvaluationData.setLastObsTm(lastObsTmList[0]);
				warnEvaluationData.setLastObsVal(lastObsValList[0]);
				
			} else if(arrYnList[1]) {
				
				Float score1 = (float)(maxWspd10MinAvg / wspd * 70);
				Float score2 = (float)(maxWspd1MinMax / maxWspd * 70);
				
				score.setEffct(Math.max(score1, score2));
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
			
			List<Map<String, Object>> obsInfoList = this.combineMetarLocalList(stnCd, metarInfoList, localInfoList);
				
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
				warnEvaluationData.setLastObsTm(lastObsTmList[0]);
				warnEvaluationData.setLastObsVal(lastObsValList[0]);
				
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
			
			List<Map<String, Object>> obsInfoList = this.combineMetarLocalList(stnCd, metarInfoList, localInfoList);
			
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
				warnEvaluationData.setLastObsTm(lastObsTmList[0]);
				warnEvaluationData.setLastObsVal(lastObsValList[0]);
				
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
			
			List<Map<String, Object>> obsInfoList = this.combineMetarLocalList(stnCd, metarInfoList, localInfoList);
			
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
				warnEvaluationData.setLastObsTm(lastObsTmList[0]);
				warnEvaluationData.setLastObsVal(lastObsValList[0]);
				
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
			
			List<Map<String, Object>> metarFrscDataList = this.evaluationDatabaseUtil.getAmisMetarFrscListForWarnEvaluate(stnCd, sdf.format(anncTm), sdf.format(edEffctTm));
			
			for(int i=0 ; i<metarFrscDataList.size() ; i++) {
				
				Map<String, Object> metarFrscData = metarFrscDataList.get(i);
					
				Date metarTm = sdf.parse((String)metarFrscData.get("tm"));
				
				Double frsc = (Double)metarFrscData.get("frsc");
				
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
				
				score.setEffct(70f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[0]);
				warnEvaluationData.setFirstArrVal(firstArrValList[0]);
				warnEvaluationData.setLastObsTm(lastObsTmList[0]);
				warnEvaluationData.setLastObsVal(lastObsValList[0]);
				
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
	public void evaluateHvyRa(String stnCd, WarnEvaluationData warnEvaluationData, Double ra) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnEvaluationData.Score score = warnEvaluationData.getScore();
		
		try {
						
			Date anncTm = warnEvaluationData.getEvaluationTm();
			Date stEffctTm = warnEvaluationData.getStEffctTm();
			Date edEffctTm = warnEvaluationData.getEdEffctTm();
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(anncTm);
			cal.add(Calendar.MINUTE, -DELAY_INTERVAL);
			
			Date _anncTm = cal.getTime();
				
			Double maxRn1Hr = 0d;
			Double maxRn3Hr = 0d;
			
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
			
			List<Map<String, Object>> amosDataList = this.evaluationDatabaseUtil.getAmosDataListForWarnEvaluate(
					EvaluationUtils.getAirportId(stnCd), sdf.format(_anncTm), sdf.format(edEffctTm));
			
			for(int i=0 ; i<amosDataList.size() ; i++) {
				
				Map<String, Object> amosData = amosDataList.get(i);
				
				Date tm = sdf.parse(amosData.get("tm").toString());
				
				cal = new GregorianCalendar();
				cal.setTime(tm);
				cal.add(Calendar.HOUR_OF_DAY, -9);
				
				tm = cal.getTime();
				
				Double rn1Hr = null;
				Double rn3Hr = null;
				
				if(amosData.get("rn1hr") != null && amosData.get("rn3hr") != null) {
					
					rn1Hr = Double.valueOf(amosData.get("rn1hr").toString());
					rn3Hr = Double.valueOf(amosData.get("rn3hr").toString());					
				}
				
				if(rn1Hr == null || rn3Hr == null) {
					continue;
				}
				
				maxRn1Hr = Math.max(maxRn1Hr, rn1Hr);
				maxRn3Hr = Math.max(maxRn3Hr, rn3Hr);
				
				if(rn1Hr >= ra || rn3Hr >= ra + 20) {
					
					arrYnList[0] = true;
					
					if(firstArrTmList[0] == null) {
						
						firstArrTmList[0] = tm;
						lastObsTmList[0] = tm;
						
						if(maxRn1Hr >= ra) {						
							
							firstArrValList[0] = rn1Hr.toString();
							lastObsValList[0] = rn3Hr.toString();
							
						} else {
							
							firstArrValList[0] = rn1Hr.toString();
							lastObsValList[0] = rn3Hr.toString();
						}
						
					} else {
						
						lastObsTmList[0] = tm;
						
						if(rn1Hr >= ra) {					
							
							lastObsValList[0] = rn1Hr.toString();
							
						} else {
							
							lastObsValList[0] = rn3Hr.toString();
						}
					}
					
				} else if((rn1Hr < ra && rn1Hr >= ra-3) || 
						  (rn3Hr < ra + 20 && rn3Hr >= ra + 20 -5)) {
					
					arrYnList[1] = true;
					
					if(firstArrTmList[1] == null) {
						
						firstArrTmList[1] = tm;
						lastObsTmList[0] = tm;
						lastObsTmList[1] = tm;
						
						if(rn1Hr < ra && rn1Hr >= ra-3) {
							
							firstArrValList[1] = rn1Hr.toString();
							lastObsValList[0] = rn1Hr.toString();
							lastObsValList[1] = rn1Hr.toString();
							
						} else {
							
							firstArrValList[1] = rn3Hr.toString();
							lastObsValList[0] = rn3Hr.toString();
							lastObsValList[1] = rn3Hr.toString();
						}
						
					} else {
						
						lastObsTmList[0] = tm;
						lastObsTmList[1] = tm;
						
						if(rn1Hr < ra && rn1Hr >= ra-3) {
							
							lastObsValList[0] = rn1Hr.toString();
							lastObsValList[1] = rn1Hr.toString();
							
						} else {
							
							lastObsValList[0] = rn3Hr.toString();
							lastObsValList[1] = rn3Hr.toString();
						}
					}
				}
			}
			
			if(arrYnList[0]) {
				
				score.setEffct(70f);
				warnEvaluationData.setFirstArrTm(firstArrTmList[0]);
				warnEvaluationData.setFirstArrVal(firstArrValList[0]);
				warnEvaluationData.setLastObsTm(lastObsTmList[0]);
				warnEvaluationData.setLastObsVal(lastObsValList[0]);
				
			} else if(arrYnList[1]) {
				
				Float score1 = (float)(maxRn3Hr / (ra + 20) * 70);
				Float score2 = (float)(maxRn1Hr / ra * 70);
				
				score.setEffct(Math.max(score1, score2));
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
	
	private List<Map<String, Object>> combineMetarLocalList(String stnCd, List<Map<String, Object>> metarInfoList, List<Map<String, Object>> localInfoList) throws Exception {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		List<Map<String, Object>> obsInfoList = new ArrayList<Map<String, Object>>();
		
		for(int i=0 ; i<metarInfoList.size() ; i++) {
			
			Map<String, Object> obsInfo = new HashMap<String, Object>();
			
			Map<String, Object> metarInfo = metarInfoList.get(i);
			
			String metarString = (String)metarInfo.get("msgSrc");
			
			Date metarStdTm = sdf.parse((String)metarInfo.get("tm"));
			
			MetarData metarData = metarParser.parse(stnCd, metarString, metarStdTm);
			
			if(!metarData.isAvailable()) {
				continue;
			}
			
			MetarElement metarElement = metarData.getMetarElement();
			
			obsInfo.put("type", "METAR");
			
			obsInfo.put("tm", sdf.format(metarElement.getMetarTm()));
			
			List<String> skyConditionList = Arrays.asList(metarElement.getSkyCondition().split("\\s+"));
			
			obsInfo.put("skyCondition", metarElement.getSkyCondition());
			obsInfo.put("skyConditionList", skyConditionList);
			
			Double metarVis = metarElement.isCavok() ? 9999d : metarElement.getVis();
			
			obsInfo.put("vis", metarVis);
			
			String cbString = "";
			
			for(String token : metarString.split("\\s+")) {
				
				if(token.matches("(SCT|FEW|BKN|OVC)([0-9]{3})(CB)")) {
					cbString += token + " ";
				}
			}
			
			obsInfo.put("cbString", cbString.trim());
			
			Double lowestBknOvcHeight = null; 
			
			try {
				
				lowestBknOvcHeight = Double.valueOf(EvaluationUtils.getLowestBknOvcCloudHeight(metarElement));
				
			} catch (Exception e) {}
					
			if(lowestBknOvcHeight == null) {
				continue;
			}
			
			if(lowestBknOvcHeight != null) {
				obsInfo.put("lowestBknOvcHeight", lowestBknOvcHeight);	
			}
			
			obsInfoList.add(obsInfo);
		}
		
		for(int i=0 ; i<localInfoList.size() ; i++) {
			
			Map<String, Object> obsInfo = new HashMap<String, Object>();
			
			Map<String, Object> localInfo = localInfoList.get(i);
			
			String localString = (String)localInfo.get("msgText");
			
			Date localStdTm = sdf.parse((String)localInfo.get("tm"));
			
			obsInfo.put("type", "LOCAL");
			
			obsInfo.put("tm", sdf.format(localStdTm));
			
			String mtph = (String)localInfo.get("mtph");
			
			obsInfo.put("skyCondition", mtph == null ? "" : mtph);
			
			if(mtph == null) {
				obsInfo.put("skyConditionList", new ArrayList<String>());
			} else {
				obsInfo.put("skyConditionList", Arrays.asList((mtph).split("\\s+")));
			}
			
			if(localInfo.get("vis") != null) {
				
				Double localVis = Double.valueOf((String)localInfo.get("vis"));
				
				obsInfo.put("vis", localVis);
			}
			
			String cbString = "";
			
			for(String token : localString.split("\\s+")) {
				
				if(token.matches("(SCT|FEW|BKN|OVC)([0-9]{3})(CB)")) {
					cbString += token + " ";
				}
			}
			
			obsInfo.put("cbString", cbString.trim());
			
			Double lowestBknOvcHeight = this.getLocalLowestBknOvcCloudHeight(localString);
			
			if(lowestBknOvcHeight != null) {
				obsInfo.put("lowestBknOvcHeight", lowestBknOvcHeight);	
			}
			
			obsInfoList.add(obsInfo);
		}
		
		Collections.sort(obsInfoList, new Comparator<Map<String, Object>>(){

			@Override
			public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
				
				return ((String)arg0.get("tm")).compareTo((String)arg1.get("tm"));
			}
		});
		
		return obsInfoList;
	}
	
	private Double getLocalLowestBknOvcCloudHeight(String msgText) {
		
		List<String> tokenList = Arrays.asList(msgText.split("\\s+"));
		
		int cldIndex = tokenList.indexOf("CLD");
		
		List<String> bknOvcList = new ArrayList<String>();
		List<Double> heightList = new ArrayList<Double>();
		
		if(cldIndex >= 0) {

			for(int i=cldIndex ; i<tokenList.size() ; i++) {
			
				try {
					
					String cloudKind = tokenList.get(i);
					
					if("BKN".equals(cloudKind) || "OVC".equals(cloudKind)) {
					
						bknOvcList.add(cloudKind);
						heightList.add(Double.valueOf(tokenList.get(i+1).replaceAll("FT", "")));
					}
					
				} catch (Exception e) {
					
				}
			}
		}
		
		if(heightList.size() > 0) {
			return heightList.get(0);
		} else {
			return null;
		}
	}
}
