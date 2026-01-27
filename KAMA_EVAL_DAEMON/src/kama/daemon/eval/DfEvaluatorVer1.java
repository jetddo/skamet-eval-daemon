package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.df.DfElement;
import kama.daemon.eval.df.DfElementSet;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.util.DaemonUtil;

public class DfEvaluatorVer1 extends DfEvaluator {
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	private String stnCd;
	
	public DfEvaluatorVer1(EvaluationDatabaseUtil evaluationDatabaseUtil) {
		this.evaluationDatabaseUtil = evaluationDatabaseUtil;
	}
	
	@Override
	public Map<String, Object> getEvaluationResult(List<DfEvaluationData> dfEvaluationDataList, int scoreFixCount) {
		
		Map<String, Object> dfEvaluationResult = new HashMap<String, Object>();
		
		Float windDirectionScoreSum = 0f;
		Float windSpeedScoreSum = 0f;
		Float temperatureScoreSum = 0f;
		Float qnhScoreSum = 0f;
		
		int availableCnt = 0;
		
		for(int i=0 ; i<dfEvaluationDataList.size() ; i++) {
			
			if(!dfEvaluationDataList.get(i).isAvailable()) {
				continue;
			}
			
			availableCnt++;
			
			DfEvaluationData.Score score = dfEvaluationDataList.get(i).getScore();
			
			score.setTotal(
				DaemonUtil.setNumberFix(
					(score.getWindDirection() + score.getWindSpeed() + score.getTemperature() + score.getQnh())/4, 
				scoreFixCount)
			);
			
			windDirectionScoreSum += score.getWindDirection();
			windSpeedScoreSum += score.getWindSpeed();
			temperatureScoreSum += score.getTemperature();
			qnhScoreSum += score.getQnh();
		}
		
		Float windDirectionScoreAvg = DaemonUtil.setNumberFix(windDirectionScoreSum / availableCnt, scoreFixCount);
		Float windSpeedScoreAvg = DaemonUtil.setNumberFix(windSpeedScoreSum / availableCnt, scoreFixCount);
		Float temperatureScoreAvg = DaemonUtil.setNumberFix(temperatureScoreSum / availableCnt, scoreFixCount);
		Float qnhScoreAvg = DaemonUtil.setNumberFix(qnhScoreSum / availableCnt, scoreFixCount);
		
		Float totalScore = DaemonUtil.setNumberFix(
				(windDirectionScoreAvg + windSpeedScoreAvg + temperatureScoreAvg + qnhScoreAvg) / 4, 
		scoreFixCount);
		
		dfEvaluationResult.put("windDirectionScoreAvg", windDirectionScoreAvg);
		dfEvaluationResult.put("windSpeedScoreAvg", windSpeedScoreAvg);
		dfEvaluationResult.put("temperatureScoreAvg", temperatureScoreAvg);
		dfEvaluationResult.put("qnhScoreAvg", qnhScoreAvg);
		
		dfEvaluationResult.put("windDirectionScore", windDirectionScoreAvg);
		dfEvaluationResult.put("windSpeedScore", windSpeedScoreAvg);
		dfEvaluationResult.put("temperatureScore", temperatureScoreAvg);
		dfEvaluationResult.put("qnhScore", qnhScoreAvg);
		dfEvaluationResult.put("totalScore", totalScore);
		
		if(availableCnt > 0) {
			dfEvaluationResult.put("available", true);
		} else {
			dfEvaluationResult.put("available", false);
		}
		
		return dfEvaluationResult;
	}
	
	@Override
	public List<DfEvaluationData> evaluate(String stnCd, Date stDfTm, Date edDfTm, List<DfElementSet> dfElementSetList, List<MetarElement> metarElementList, List<Map<String, Object>> metarOriginInfoList) throws Exception {
		
		this.stnCd = stnCd;
		
		List<DfEvaluationData> dfEvaluationDataList = new ArrayList<DfEvaluationData>();
		
		// 평가는 Metar 를 기준으로 수행한다
		
		for(int i=0 ; i<metarElementList.size() ; i++) {
			
			MetarElement evalMetarElement = metarElementList.get(i);
			
			// evalMetarElement 에 맞는 dfElementSet 을 찾는다
			Map<String, Object> dfElementSetInfo = this.getDfElementSetInfobyTm(dfElementSetList, evalMetarElement.getMetarTm());			
			DfElementSet evalDfElementSet  = (DfElementSet)dfElementSetInfo.get("dfElementSet");
			
			// Metar Origin 정보
			Map<String, Object> metarOriginInfo = this.getMetarOriginInfobyTm(metarOriginInfoList, evalMetarElement.getMetarTm());
			
			DfEvaluationData dfEvaluationData = new DfEvaluationData();
			
			if(!this.checkAvailableEvaluation(evalDfElementSet, evalMetarElement)) {
				dfEvaluationData.setAvailable(false);
				continue;
			}
			
			dfEvaluationDataList.add(dfEvaluationData);
			
			DfElement evalDfElement = evalDfElementSet.getDfElement();
			
			dfEvaluationData.setEvaluationTm(evalMetarElement.getMetarTm());	
			dfEvaluationData.setDfTm(evalDfElementSet.getDfTm());
			dfEvaluationData.setMetarTm(evalMetarElement.getMetarTm());
			dfEvaluationData.setDfElementSet(evalDfElementSet);
			dfEvaluationData.setMetarElement(evalMetarElement);
			dfEvaluationData.setMetarOriginInfo(metarOriginInfo);
			DfEvaluationData.Score dfEvaluationScore = dfEvaluationData.getScore();
			
			Float windDirectionScore = this.evaluateWindDirection(evalDfElement, evalMetarElement);
			Float windSpeedScore = this.evaluateWindSpeed(evalDfElement, evalMetarElement);
			Float temperatureScore = this.evaluateTemperature(evalDfElement, evalMetarElement);
			Float qnhScore = this.evaluateQnh(evalDfElement, metarOriginInfo);		
			
			dfEvaluationScore.setWindDirection(windDirectionScore);
			dfEvaluationScore.setWindSpeed(windSpeedScore);
			dfEvaluationScore.setTemperature(temperatureScore);
			dfEvaluationScore.setQnh(qnhScore);
		}
		
		// 동일한 시간중에 나중의것을 제거한다
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
		
		String prevDfTm = null;
		
		for(int i=0 ; i<dfEvaluationDataList.size() ; i++) {
			
			DfEvaluationData dfEvaluationData = dfEvaluationDataList.get(i);

			if(i == 0) {
				prevDfTm = sdf.format(dfEvaluationData.getDfTm());
			} else {
				
				if(prevDfTm.equals(sdf.format(dfEvaluationData.getDfTm()))) {
					dfEvaluationDataList.remove(i--);
				} else {
					prevDfTm = sdf.format(dfEvaluationData.getDfTm());
				}
			}
		}
		
		return dfEvaluationDataList;
	}
	
	private Float evaluateWindDirection(DfElement dfElement, MetarElement metarElement) {
		
		if(metarElement.getWspd() <= 5 || metarElement.isVrb()) {
			
			return 100f;
			
		} else if(metarElement.getWspd() > 5) {
			
			Double gap = Math.abs(metarElement.getWdir() - dfElement.getWdir());
			
			gap = Math.min(gap, 360 - gap);
			
			if(gap <= 20) {	
				return 100f;
			} else if(gap > 20 && gap <= 50) {					
				return 50f;
			} else {						
				return 0f;
			}
		}
		
		return 0f;
	}
	
	private Float evaluateWindSpeed(DfElement dfElement, MetarElement metarElement) {
		
		Double gap = Math.abs(metarElement.getWspd() - dfElement.getWspd());
		
		if(gap <= 5) {
			return 100f;
		} else {
			return 0f;
		}
	}	
	
	private Float evaluateQnh(DfElement dfElement, Map<String, Object> metarOriginInfo) {
		
		// 기압 관측이 없을 경우 일단 100을 준다
		if(metarOriginInfo == null) {
			return 100f;
		}
		
		Double metarQnh = Double.valueOf(metarOriginInfo.get("aqnh").toString());
		
		double gap = Math.abs(metarQnh - dfElement.getQnh());
		  
		gap = (gap / 0.029525 / 100);
		
		if(gap < 1.02) {
			return 100f;
		} else {
			return 0f;
		}
	}	
	
	private Float evaluateTemperature(DfElement dfElement, MetarElement metarElement) {
		
		Double dfTemp = dfElement.getTemp();
		Double metarTemp = metarElement.getTx();
		
		if(dfTemp == null || metarTemp == null) {
			return null;
		}
		
		String evalTempMethod = "1";
		
		Date dfTm = dfElement.getDfTm();
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(dfTm);
		
		Date searchStTm = new Date(cal.getTime().getTime());
		
		cal.setTime(dfTm);
		
		Date searchEdTm = new Date(cal.getTime().getTime());
		
		double gap = Math.abs(dfTemp - metarTemp);
		
		switch(evalTempMethod) {
		
		case "1":
		
			if(gap <= 1) {
				return 100f;
			} else {
				return 0f;
			}
			
		default:
		
			if(gap <= 1) {
				return 100f;
			} else {
				return 0f;
			}
		}
	}
}
