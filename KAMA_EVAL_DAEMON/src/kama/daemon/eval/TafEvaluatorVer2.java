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
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.taf.TafData;
import kama.daemon.eval.taf.TafElement;
import kama.daemon.eval.taf.TafElementSet;
import kama.daemon.util.DaemonUtil;
import kama.daemon.util.EvaluationUtils;

public class TafEvaluatorVer2 extends TafEvaluator {
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	private int becmgType = 1;
	
	private String stnCd;
	
	public TafEvaluatorVer2(EvaluationDatabaseUtil evaluationDatabaseUtil, int becmgType) {
		this.evaluationDatabaseUtil = evaluationDatabaseUtil;
		this.becmgType = becmgType;
	}
	
	@Override
	public Map<String, Object> getEvaluationResult(List<TafEvaluationData> tafEvaluationDataList, int scoreFixCount) {
		
		Map<String, Object> tafEvaluationResult = new HashMap<String, Object>();
		
		Float windDirectionScoreSum = 0f;
		Float windSpeedScoreSum = 0f;
		Float visibilityScoreSum = 0f;
		Float rainOrClearScoreSum = 0f;
		Float cloudAmountScoreSum = 0f;
		Float cloudHeightScoreSum = 0f;
		Float temperatureScoreSum = 0f;
		Float temperatureVer2ScoreSum = 0f;
		
		int evalType1Cnt = 0; // METAR 만 있는 점수 그룹 숫자
		int evalType2Cnt = 0; // METAR 와 기온 둘다 있는 점수 그룹 숫자
		int evalType3Cnt = 0; // 기온만 있는 점수 그룹 숫자
		
		for(int i=0 ; i<tafEvaluationDataList.size() ; i++) {
			
			TafEvaluationData tafEvaluationData = tafEvaluationDataList.get(i);
			
			if(!tafEvaluationData.isAvailable()) {
				continue;
			}
			
			TafEvaluationData.Score score = tafEvaluationData.getScore();
			
			switch(tafEvaluationData.getEvalType()) {
			
			// METAR 점수만 있는 경우
			case 1:
					
				evalType1Cnt++;
				
				windDirectionScoreSum += score.getWindDirection();
				windSpeedScoreSum += score.getWindSpeed();
				visibilityScoreSum += score.getVisibility();
				rainOrClearScoreSum += score.getRainOrClear();
				cloudAmountScoreSum += score.getCloudAmount();
				cloudHeightScoreSum += score.getCloudHeight();
				
				break;
				
			// METAR 와 기온 점수 둘 다 있는 경우
			case 2:
					
				evalType2Cnt++;
				
				windDirectionScoreSum += score.getWindDirection();
				windSpeedScoreSum += score.getWindSpeed();
				visibilityScoreSum += score.getVisibility();
				rainOrClearScoreSum += score.getRainOrClear();
				cloudAmountScoreSum += score.getCloudAmount();
				cloudHeightScoreSum += score.getCloudHeight();				
				temperatureScoreSum += score.getTemperature();
				temperatureVer2ScoreSum += score.getTemperatureVer2();
				
				break;
				
			// 기온 점수만 있는 경우
			case 3:
				
				evalType3Cnt++;
				
				temperatureScoreSum += score.getTemperature();				
				temperatureVer2ScoreSum += score.getTemperatureVer2();
				
				break;
			}
			
			// 평가 방법 2은 각 요소를 다 더해서 그냥 나눔, 기온 점수가 있는 경우에만 함께 계산
			// 기온 점수가 있는 경우에는 기온점수만 사용한다
		}
		
		Float windDirectionScoreAvg = DaemonUtil.setNumberFix(windDirectionScoreSum / (evalType1Cnt + evalType2Cnt), scoreFixCount);
		Float windSpeedScoreAvg = DaemonUtil.setNumberFix(windSpeedScoreSum / (evalType1Cnt + evalType2Cnt), scoreFixCount);
		Float visibilityScoreAvg = DaemonUtil.setNumberFix(visibilityScoreSum / (evalType1Cnt + evalType2Cnt), scoreFixCount);
		Float rainOrClearScoreAvg = DaemonUtil.setNumberFix(rainOrClearScoreSum / (evalType1Cnt + evalType2Cnt), scoreFixCount);
		Float cloudAmountScoreAvg = DaemonUtil.setNumberFix(cloudAmountScoreSum / (evalType1Cnt + evalType2Cnt), scoreFixCount);
		Float cloudHeightScoreAvg = DaemonUtil.setNumberFix(cloudHeightScoreSum / (evalType1Cnt + evalType2Cnt), scoreFixCount);
		Float temperatureScoreAvg = -1f;
		Float temperatureVer2ScoreAvg = -1f;
		
		if(evalType2Cnt + evalType3Cnt > 0) {
			temperatureScoreAvg = DaemonUtil.setNumberFix(temperatureScoreSum / (evalType2Cnt + evalType3Cnt), scoreFixCount);
			temperatureVer2ScoreAvg = DaemonUtil.setNumberFix(temperatureVer2ScoreSum / (evalType2Cnt + evalType3Cnt), scoreFixCount);
		} 
		
		tafEvaluationResult.put("windDirectionScoreAvg", windDirectionScoreAvg);
		tafEvaluationResult.put("windSpeedScoreAvg", windSpeedScoreAvg);
		tafEvaluationResult.put("visibilityScoreAvg", visibilityScoreAvg);
		tafEvaluationResult.put("rainOrClearScoreAvg", rainOrClearScoreAvg);
		tafEvaluationResult.put("cloudAmountScoreAvg", cloudAmountScoreAvg);
		tafEvaluationResult.put("cloudHeightScoreAvg", cloudHeightScoreAvg);
		tafEvaluationResult.put("temperatureScoreAvg", temperatureScoreAvg);
		tafEvaluationResult.put("temperatureVer2ScoreAvg", temperatureVer2ScoreAvg);
				
		if(evalType1Cnt + evalType2Cnt + evalType3Cnt > 0) {
			tafEvaluationResult.put("available", true);
		} else {
			tafEvaluationResult.put("available", false);
		}
		
		return tafEvaluationResult;
	}
	
	@Override	
	public List<TafEvaluationData> evaluate(String stnCd, Date stTafTm, Date edTafTm, List<TafElementSet> tafElementSetList, List<MetarElement> metarElementList) throws Exception {
			
		this.stnCd = stnCd;
		
		List<TafEvaluationData> tafEvaluationDataList = new ArrayList<TafEvaluationData>();
		
		// 평가는 Metar 를 기준으로 수행한다
		// 기온은 Metar 와 관련이 없으므로 별도로 수행한다
		
		for(int i=0 ; i<metarElementList.size() ; i++) {
			
			MetarElement evalMetarElement = metarElementList.get(i);
			
			// evalMetarElement 에 맞는 tafElementSet 을 찾는다
			Map<String, Object> tafElementSetInfo = this.getTafElementSetInfobyTm(tafElementSetList, evalMetarElement.getMetarTm());			
			TafElementSet evalTafElementSet  = (TafElementSet)tafElementSetInfo.get("tafElementSet");
			Integer evalTafElementSetIndex  = (Integer)tafElementSetInfo.get("tafElementSetIndex");
			
			TafEvaluationData tafEvaluationData = new TafEvaluationData();
			
			if(!this.checkAvailableEvaluation(evalTafElementSet, evalMetarElement)) {
				tafEvaluationData.setAvailable(false);
				continue;
			}
			
			tafEvaluationDataList.add(tafEvaluationData);
			
			TafElement evalFcstTafElement = evalTafElementSet.getStateTafElement(TafData.State.FCST);
			TafElement evalBecmgTafElement = evalTafElementSet.getStateTafElement(TafData.State.BECMG);
			TafElement evalTempoTafElement = evalTafElementSet.getStateTafElement(TafData.State.TEMPO);
			TafElement evalFmTafElement = evalTafElementSet.getStateTafElement(TafData.State.FM);
			
			tafEvaluationData.setEvaluationTm(evalMetarElement.getMetarTm());	
			tafEvaluationData.setTafTm(evalTafElementSet.getTafTm());
			tafEvaluationData.setMetarTm(evalMetarElement.getMetarTm());
			tafEvaluationData.setTafElementSet(evalTafElementSet);
			tafEvaluationData.setMetarElement(evalMetarElement);
			TafEvaluationData.Score tafEvaluationScore = tafEvaluationData.getScore();
			
			
			// 변화군이 적용되어있지 않으면 일반 평가 로직을 따른다
			if(evalBecmgTafElement == null && evalTempoTafElement == null) {
				
				Float windDirectionScore = this.evaluateWindDirection(evalFcstTafElement, evalMetarElement);
				Float windSpeedScore = this.evaluateWindSpeed(evalFcstTafElement, evalMetarElement);
				Float visibilityScore = this.evaluateVis(evalFcstTafElement, evalMetarElement);
				Float rainOrClearScore = this.evaluateRainOrClear(evalFcstTafElement, evalMetarElement);		
				Float[] cloudAmountScores = this.evaluateCloudAmount(evalFcstTafElement, evalMetarElement);
				Float cloudHeightScore = this.evaluateCloudHeight(evalFcstTafElement, evalMetarElement);
				
				tafEvaluationScore.setWindDirection(windDirectionScore);
				tafEvaluationScore.setWindSpeed(windSpeedScore);
				tafEvaluationScore.setVisibility(visibilityScore);
				tafEvaluationScore.setRainOrClear(rainOrClearScore);
				tafEvaluationScore.setCloudAmounts(cloudAmountScores);
				tafEvaluationScore.setCloudHeight(cloudHeightScore);
					
			// BECMG 변화군인 경우
			} else if (evalBecmgTafElement != null && evalTempoTafElement == null) {
				
				Map<String, Object> stateInfo = this.getStateInfo(tafElementSetList, evalTafElementSetIndex, evalBecmgTafElement.getState());
				
				if(stateInfo == null) {
					tafEvaluationData.setAvailable(false);
					continue;
				}
					
				// 변화전 예보
				TafElement beforeStateTafElement = null; 
						
				// 변화후 예보 (BECMG 이후의 예보를 사용하면 바로 TEMPO 변화군이 올 수 있는 위험성이 있다, TEMPO 변화군은 바로 예보에 영향을 미치기 때문.)
				TafElement afterStateTafElement = null; 
						
				// 현재 변화군이 마지막인 경우에는 이미 예보값에 반영이 되어있으므로 이전 TAF 에보를 가져온다
				if(evalBecmgTafElement.getStateStatus() == 2) {
					beforeStateTafElement = tafElementSetList.get(evalTafElementSetIndex > 0 ? evalTafElementSetIndex-1 : evalTafElementSetIndex).getFcstTafElement();
					afterStateTafElement = evalFcstTafElement;
				} else {
					beforeStateTafElement = evalFcstTafElement;
					afterStateTafElement = this.adoptBecmgTafElement(evalFcstTafElement, evalBecmgTafElement);
				}
				
				Date stateStTm = (Date)stateInfo.get("stateStTm");
				Date stateEdTm = (Date)stateInfo.get("stateEdTm");				
				Float stateHours = (Float)stateInfo.get("stateHours");
				
				// BECMG 변화군 패치 이전
				if(this.becmgType == 1) {
				
					if(stateHours <= 1) {
						
						// BECMG 종료 시각과 같은 경우
						if(stateEdTm.getTime() == evalMetarElement.getMetarTm().getTime()) {
							
							// 변화후 예보로 평가한다
							Float windDirectionScore = this.evaluateWindDirection(afterStateTafElement, evalMetarElement);
							Float windSpeedScore = this.evaluateWindSpeed(afterStateTafElement, evalMetarElement);
							Float visibilityScore = this.evaluateVis(afterStateTafElement, evalMetarElement);
							Float rainOrClearScore = this.evaluateRainOrClear(afterStateTafElement, evalMetarElement);
							Float[] cloudAmountScores = this.evaluateCloudAmount(afterStateTafElement, evalMetarElement);
							Float cloudHeightScore = this.evaluateCloudHeight(afterStateTafElement, evalMetarElement);
							
							tafEvaluationScore.setWindDirection(windDirectionScore);
							tafEvaluationScore.setWindSpeed(windSpeedScore);
							tafEvaluationScore.setVisibility(visibilityScore);
							tafEvaluationScore.setRainOrClear(rainOrClearScore);			
							tafEvaluationScore.setCloudAmounts(cloudAmountScores);
							tafEvaluationScore.setCloudHeight(cloudHeightScore);
							
						} else {
						
							// 변화전 예보로 평가한다
							Float windDirectionScore = this.evaluateWindDirection(beforeStateTafElement, evalMetarElement);
							Float windSpeedScore = this.evaluateWindSpeed(beforeStateTafElement, evalMetarElement);
							Float visibilityScore = this.evaluateVis(beforeStateTafElement, evalMetarElement);
							Float rainOrClearScore = this.evaluateRainOrClear(beforeStateTafElement, evalMetarElement);					
							Float[] cloudAmountScores = this.evaluateCloudAmount(beforeStateTafElement, evalMetarElement);	
							Float cloudHeightScore = this.evaluateCloudHeight(beforeStateTafElement, evalMetarElement);
								
							// 변화후 예보 점수와 비교하여 높은 것을 택한다 (변화전 예보와 변화후 예보 같은 요소는 중복되므로 문제되지 않음)
							windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(afterStateTafElement, evalMetarElement));
							windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(afterStateTafElement, evalMetarElement));
							visibilityScore = Math.max(visibilityScore, this.evaluateVis(afterStateTafElement, evalMetarElement));
							rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(afterStateTafElement, evalMetarElement));				
							cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(afterStateTafElement, evalMetarElement));
							cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(afterStateTafElement, evalMetarElement));
							
							tafEvaluationScore.setWindDirection(windDirectionScore);
							tafEvaluationScore.setWindSpeed(windSpeedScore);
							tafEvaluationScore.setVisibility(visibilityScore);
							tafEvaluationScore.setRainOrClear(rainOrClearScore);			
							tafEvaluationScore.setCloudAmounts(cloudAmountScores);
							tafEvaluationScore.setCloudHeight(cloudHeightScore);
						}
						
					} else {
						
						// BECMG 종료시각과 같은 경우
						if(stateEdTm.getTime() == evalMetarElement.getMetarTm().getTime()) {
							
							// 변화후 예보로 평가한다
							Float windDirectionScore = this.evaluateWindDirection(afterStateTafElement, evalMetarElement);
							Float windSpeedScore = this.evaluateWindSpeed(afterStateTafElement, evalMetarElement);
							Float visibilityScore = this.evaluateVis(afterStateTafElement, evalMetarElement);
							Float rainOrClearScore = this.evaluateRainOrClear(afterStateTafElement, evalMetarElement);
							Float[] cloudAmountScores = this.evaluateCloudAmount(afterStateTafElement, evalMetarElement);
							Float cloudHeightScore = this.evaluateCloudHeight(afterStateTafElement, evalMetarElement);
							
							tafEvaluationScore.setWindDirection(windDirectionScore);
							tafEvaluationScore.setWindSpeed(windSpeedScore);
							tafEvaluationScore.setVisibility(visibilityScore);
							tafEvaluationScore.setRainOrClear(rainOrClearScore);			
							tafEvaluationScore.setCloudAmounts(cloudAmountScores);
							tafEvaluationScore.setCloudHeight(cloudHeightScore);
							
						} else {

							// 변화전 예보로 평가한다
							Float windDirectionScore = this.evaluateWindDirection(beforeStateTafElement, evalMetarElement);
							Float windSpeedScore = this.evaluateWindSpeed(beforeStateTafElement, evalMetarElement);
							Float visibilityScore = this.evaluateVis(beforeStateTafElement, evalMetarElement);
							Float rainOrClearScore = this.evaluateRainOrClear(beforeStateTafElement, evalMetarElement);
							Float[] cloudAmountScores = this.evaluateCloudAmount(beforeStateTafElement, evalMetarElement);
							Float cloudHeightScore = this.evaluateCloudHeight(beforeStateTafElement, evalMetarElement);
							
							tafEvaluationScore.setWindDirection(windDirectionScore);
							tafEvaluationScore.setWindSpeed(windSpeedScore);
							tafEvaluationScore.setVisibility(visibilityScore);
							tafEvaluationScore.setRainOrClear(rainOrClearScore);			
							tafEvaluationScore.setCloudAmounts(cloudAmountScores);
							tafEvaluationScore.setCloudHeight(cloudHeightScore);
						}
					}
					
				} else if(this.becmgType == 2) {
					
					// 변화전 예보로 평가한다
					Float windDirectionScore = this.evaluateWindDirection(beforeStateTafElement, evalMetarElement);
					Float windSpeedScore = this.evaluateWindSpeed(beforeStateTafElement, evalMetarElement);
					Float visibilityScore = this.evaluateVis(beforeStateTafElement, evalMetarElement);
					Float rainOrClearScore = this.evaluateRainOrClear(beforeStateTafElement, evalMetarElement);					
					Float[] cloudAmountScores = this.evaluateCloudAmount(beforeStateTafElement, evalMetarElement);	
					Float cloudHeightScore = this.evaluateCloudHeight(beforeStateTafElement, evalMetarElement);
						
					// 변화후 예보 점수와 비교하여 높은 것을 택한다 (변화전 예보와 변화후 예보 같은 요소는 중복되므로 문제되지 않음)
					windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(afterStateTafElement, evalMetarElement));
					windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(afterStateTafElement, evalMetarElement));
					visibilityScore = Math.max(visibilityScore, this.evaluateVis(afterStateTafElement, evalMetarElement));
					rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(afterStateTafElement, evalMetarElement));				
					cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(afterStateTafElement, evalMetarElement));
					cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(afterStateTafElement, evalMetarElement));
					
					tafEvaluationScore.setWindDirection(windDirectionScore);
					tafEvaluationScore.setWindSpeed(windSpeedScore);
					tafEvaluationScore.setVisibility(visibilityScore);
					tafEvaluationScore.setRainOrClear(rainOrClearScore);			
					tafEvaluationScore.setCloudAmounts(cloudAmountScores);
					tafEvaluationScore.setCloudHeight(cloudHeightScore);
				}
				
			// TEMPO 변화군인 경우, 변화하는 요소에 대해서만 적용해야한다
			} else if (evalBecmgTafElement == null && evalTempoTafElement != null) {
				
				Map<String, Object> stateInfo = this.getStateInfo(tafElementSetList, evalTafElementSetIndex, evalTempoTafElement.getState());
				
				if(stateInfo == null) {
					tafEvaluationData.setAvailable(false);
					continue;
				}
				
				Map<String, Boolean> changedElementInfo = this.getChangedElementInfo(evalTempoTafElement);
				
				// Metar 정보에서 현재 TafElement 의 시각과 +1시간, -1시간에 해당하는 모든 Metar 리스트를 가져온다				
				List<MetarElement> subMetarElementList = this.findMetarElementListBetweenHours(evalFcstTafElement.getTafTm(), metarElementList, stateInfo, 1);
				
				// 현재 Metar 로 기본 점수 셋팅
				Float windDirectionScore = this.evaluateWindDirection(evalFcstTafElement, evalMetarElement);
				Float windSpeedScore = this.evaluateWindSpeed(evalFcstTafElement, evalMetarElement);
				Float visibilityScore = this.evaluateVis(evalFcstTafElement, evalMetarElement);
				Float rainOrClearScore = this.evaluateRainOrClear(evalFcstTafElement, evalMetarElement);
				Float[] cloudAmountScores = this.evaluateCloudAmount(evalFcstTafElement, evalMetarElement);
				Float cloudHeightScore = this.evaluateCloudHeight(evalFcstTafElement, evalMetarElement);				
					
				for(int j=0 ; j<subMetarElementList.size() ; j++) {
					
					// 변화한 요소에 대해서는 -1~+1 시간의 관측에 대해서 모두 평가하여 가장 높은 점수를 줌
					
					if(changedElementInfo.get("windDirection")) {
						windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(evalFcstTafElement, subMetarElementList.get(j)));	
					}
					
					if(changedElementInfo.get("windSpeed")) {
						windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(evalFcstTafElement, subMetarElementList.get(j)));
					}
					
					if(changedElementInfo.get("visibility")) {
						visibilityScore = Math.max(visibilityScore, this.evaluateVis(evalFcstTafElement, subMetarElementList.get(j)));
					}
					
					if(changedElementInfo.get("rainOrClear")) {
						rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(evalFcstTafElement, subMetarElementList.get(j)));
					}
					
					if(changedElementInfo.get("cloudAmount")) {
						cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(evalFcstTafElement, subMetarElementList.get(j)));
					}
					
					if(changedElementInfo.get("cloudHeight")) {
						cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(evalFcstTafElement, subMetarElementList.get(j)));	
					}		
				}
				
				tafEvaluationScore.setWindDirection(windDirectionScore);
				tafEvaluationScore.setWindSpeed(windSpeedScore);
				tafEvaluationScore.setVisibility(visibilityScore);
				tafEvaluationScore.setRainOrClear(rainOrClearScore);			
				tafEvaluationScore.setCloudAmounts(cloudAmountScores);
				tafEvaluationScore.setCloudHeight(cloudHeightScore);
				
			// BECMG 변화군과 TEMPO 변화군이 동시에 적용된 경우 ( 이부분은 잘 모르겠음.. 두가지 변화군을 다하고 높은 점수로 하자)
			} else if (evalBecmgTafElement != null && evalTempoTafElement != null) {
				
				Float windDirectionScore = 0f;
				Float windSpeedScore = 0f;
				Float visibilityScore = 0f;
				Float rainOrClearScore = 0f;
				Float[] cloudAmountScores = new Float[]{0f, 0f, 0f};
				Float cloudHeightScore = 0f;
				
				// BECMG 변화군 부분
				{
					Map<String, Object> stateInfo = this.getStateInfo(tafElementSetList, evalTafElementSetIndex, evalBecmgTafElement.getState());
					
					if(stateInfo == null) {
						tafEvaluationData.setAvailable(false);
						continue;
					}
						
					// 변화전 예보
					TafElement beforeStateTafElement = null; 
							
					// 변화후 예보 (BECMG 이후의 예보를 사용하면 바로 TEMPO 변화군이 올 수 있는 위험성이 있다, TEMPO 변화군은 바로 예보에 영향을 미치기 때문.)
					TafElement afterStateTafElement = null; 
							
					if(evalBecmgTafElement.getStateStatus() == 2) {
						beforeStateTafElement = tafElementSetList.get(evalTafElementSetIndex > 0 ? evalTafElementSetIndex-1 : evalTafElementSetIndex).getFcstTafElement();
						afterStateTafElement = evalFcstTafElement;
					} else {
						beforeStateTafElement = evalFcstTafElement;
						afterStateTafElement = this.adoptBecmgTafElement(evalFcstTafElement, evalBecmgTafElement);
					}
					
					Date stateStTm = (Date)stateInfo.get("stateStTm");
					Date stateEdTm = (Date)stateInfo.get("stateEdTm");				
					Float stateHours = (Float)stateInfo.get("stateHours");
					
					if(this.becmgType == 1) {
						
						if(stateHours <= 1) {
							
							// BECMG 종료 시각과 같은 경우
							if(stateEdTm.getTime() == evalMetarElement.getMetarTm().getTime()) {
								
								// 변화후 예보로 평가한다
								windDirectionScore = this.evaluateWindDirection(afterStateTafElement, evalMetarElement);
								windSpeedScore = this.evaluateWindSpeed(afterStateTafElement, evalMetarElement);
								visibilityScore = this.evaluateVis(afterStateTafElement, evalMetarElement);
								rainOrClearScore = this.evaluateRainOrClear(afterStateTafElement, evalMetarElement);
								cloudAmountScores = this.evaluateCloudAmount(afterStateTafElement, evalMetarElement);
								cloudHeightScore = this.evaluateCloudHeight(afterStateTafElement, evalMetarElement);
								
								tafEvaluationScore.setWindDirection(windDirectionScore);
								tafEvaluationScore.setWindSpeed(windSpeedScore);
								tafEvaluationScore.setVisibility(visibilityScore);
								tafEvaluationScore.setRainOrClear(rainOrClearScore);			
								tafEvaluationScore.setCloudAmounts(cloudAmountScores);
								tafEvaluationScore.setCloudHeight(cloudHeightScore);
								
							} else {
							
								// 변화전 예보로 평가한다
								windDirectionScore = this.evaluateWindDirection(beforeStateTafElement, evalMetarElement);
								windSpeedScore = this.evaluateWindSpeed(beforeStateTafElement, evalMetarElement);
								visibilityScore = this.evaluateVis(beforeStateTafElement, evalMetarElement);
								rainOrClearScore = this.evaluateRainOrClear(beforeStateTafElement, evalMetarElement);					
								cloudAmountScores = this.evaluateCloudAmount(beforeStateTafElement, evalMetarElement);	
								cloudHeightScore = this.evaluateCloudHeight(beforeStateTafElement, evalMetarElement);
									
								// 변화후 예보 점수와 비교하여 높은 것을 택한다 (변화전 예보와 변화후 예보 같은 요소는 중복되므로 문제되지 않음)
								windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(afterStateTafElement, evalMetarElement));
								windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(afterStateTafElement, evalMetarElement));
								visibilityScore = Math.max(visibilityScore, this.evaluateVis(afterStateTafElement, evalMetarElement));
								rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(afterStateTafElement, evalMetarElement));				
								cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(afterStateTafElement, evalMetarElement));
								cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(afterStateTafElement, evalMetarElement));
								
								tafEvaluationScore.setWindDirection(windDirectionScore);
								tafEvaluationScore.setWindSpeed(windSpeedScore);
								tafEvaluationScore.setVisibility(visibilityScore);
								tafEvaluationScore.setRainOrClear(rainOrClearScore);			
								tafEvaluationScore.setCloudAmounts(cloudAmountScores);
								tafEvaluationScore.setCloudHeight(cloudHeightScore);
							}
								
						} else {
							
							// BECMG 종료시각인 경우
							if(stateEdTm.getTime() == evalMetarElement.getMetarTm().getTime()) {
								
								// 변화전 예보로 평가한다
								windDirectionScore = this.evaluateWindDirection(beforeStateTafElement, evalMetarElement);
								windSpeedScore = this.evaluateWindSpeed(beforeStateTafElement, evalMetarElement);
								visibilityScore = this.evaluateVis(beforeStateTafElement, evalMetarElement);
								rainOrClearScore = this.evaluateRainOrClear(beforeStateTafElement, evalMetarElement);
								cloudAmountScores = this.evaluateCloudAmount(beforeStateTafElement, evalMetarElement);
								cloudHeightScore = this.evaluateCloudHeight(beforeStateTafElement, evalMetarElement);
										
							} else {
								
								// 변화후 예보로 평가한다
								windDirectionScore = this.evaluateWindDirection(afterStateTafElement, evalMetarElement);
								windSpeedScore = this.evaluateWindSpeed(afterStateTafElement, evalMetarElement);
								visibilityScore = this.evaluateVis(afterStateTafElement, evalMetarElement);
								rainOrClearScore = this.evaluateRainOrClear(afterStateTafElement, evalMetarElement);
								cloudAmountScores = this.evaluateCloudAmount(afterStateTafElement, evalMetarElement);
								cloudHeightScore = this.evaluateCloudHeight(afterStateTafElement, evalMetarElement);							
							}
						}
						
					} else if(this.becmgType == 2) {
						
						// 변화전 예보로 평가한다
						windDirectionScore = this.evaluateWindDirection(beforeStateTafElement, evalMetarElement);
						windSpeedScore = this.evaluateWindSpeed(beforeStateTafElement, evalMetarElement);
						visibilityScore = this.evaluateVis(beforeStateTafElement, evalMetarElement);
						rainOrClearScore = this.evaluateRainOrClear(beforeStateTafElement, evalMetarElement);					
						cloudAmountScores = this.evaluateCloudAmount(beforeStateTafElement, evalMetarElement);	
						cloudHeightScore = this.evaluateCloudHeight(beforeStateTafElement, evalMetarElement);
							
						// 변화후 예보 점수와 비교하여 높은 것을 택한다 (변화전 예보와 변화후 예보 같은 요소는 중복되므로 문제되지 않음)
						windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(afterStateTafElement, evalMetarElement));
						windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(afterStateTafElement, evalMetarElement));
						visibilityScore = Math.max(visibilityScore, this.evaluateVis(afterStateTafElement, evalMetarElement));
						rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(afterStateTafElement, evalMetarElement));				
						cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(afterStateTafElement, evalMetarElement));
						cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(afterStateTafElement, evalMetarElement));
						
						tafEvaluationScore.setWindDirection(windDirectionScore);
						tafEvaluationScore.setWindSpeed(windSpeedScore);
						tafEvaluationScore.setVisibility(visibilityScore);
						tafEvaluationScore.setRainOrClear(rainOrClearScore);			
						tafEvaluationScore.setCloudAmounts(cloudAmountScores);
						tafEvaluationScore.setCloudHeight(cloudHeightScore);
					}
				}
				
				// TEMPO 변화군 부분
				{
					
					Map<String, Object> stateInfo = this.getStateInfo(tafElementSetList, evalTafElementSetIndex, evalTempoTafElement.getState());
					
					if(stateInfo == null) {
						tafEvaluationData.setAvailable(false);
						continue;
					}
					
					Map<String, Boolean> changedElementInfo = this.getChangedElementInfo(evalTempoTafElement);
					
					// Metar 정보에서 현재 TafElement 의 시각과 +1시간, -1시간에 해당하는 모든 Metar 리스트를 가져온다				
					List<MetarElement> subMetarElementList = this.findMetarElementListBetweenHours(evalFcstTafElement.getTafTm(), metarElementList, stateInfo, 1);
					
					// 현재 Metar 로 기본 점수 셋팅
					windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(evalFcstTafElement, evalMetarElement));
					windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(evalFcstTafElement, evalMetarElement));
					visibilityScore = Math.max(visibilityScore, this.evaluateVis(evalFcstTafElement, evalMetarElement));
					rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(evalFcstTafElement, evalMetarElement));
					cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(evalFcstTafElement, evalMetarElement));
					cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(evalFcstTafElement, evalMetarElement));			
						
					for(int j=0 ; j<subMetarElementList.size() ; j++) {
						
						if(changedElementInfo.get("windDirection")) {
							windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(evalFcstTafElement, subMetarElementList.get(j)));	
						}
						
						if(changedElementInfo.get("windSpeed")) {
							windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(evalFcstTafElement, subMetarElementList.get(j)));
						}
						
						if(changedElementInfo.get("visibility")) {
							visibilityScore = Math.max(visibilityScore, this.evaluateVis(evalFcstTafElement, subMetarElementList.get(j)));
						}
						
						if(changedElementInfo.get("rainOrClear")) {
							rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(evalFcstTafElement, subMetarElementList.get(j)));
						}
						
						if(changedElementInfo.get("cloudAmount")) {
							cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(evalFcstTafElement, subMetarElementList.get(j)));
						}
						
						if(changedElementInfo.get("cloudHeight")) {
							cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(evalFcstTafElement, subMetarElementList.get(j)));	
						}					
					}					
				}
				
				tafEvaluationScore.setWindDirection(windDirectionScore);
				tafEvaluationScore.setWindSpeed(windSpeedScore);
				tafEvaluationScore.setVisibility(visibilityScore);
				tafEvaluationScore.setRainOrClear(rainOrClearScore);			
				tafEvaluationScore.setCloudAmounts(cloudAmountScores);
				tafEvaluationScore.setCloudHeight(cloudHeightScore);
			}
		}
		
		// 기온 평가를 시작한다
		// 기온 평가는 TAF 가 중심이다		
		// TAF 리스트에서 기온이 존재하는 행을 뽑아 평가를 수행하고 tafEvaluationDataList 에 집어넣는다
		
		SimpleDateFormat sdf = new SimpleDateFormat("mm");
		
		for(int i=0 ; i<tafElementSetList.size() ; i++) {
			
			TafElementSet tafElementSet = tafElementSetList.get(i);
			
			// 기온은 정시에만 존재하므로 이외의 것은 continue 처리한다.
			
			if(!"00".equals(sdf.format(tafElementSet.getTafTm()))) {
				continue;
			}
			
			TafElement tafElement = tafElementSet.getFcstTafElement();
			
			// 최저기온이나 최대기온이 없어도 continue 처리한다
			if(tafElement.getTn() == null && tafElement.getTx() == null) {
				continue;
			}
			
			// tafElement 시간대로 이미 평가가 되어있다면 꺼내본다
			
			TafEvaluationData tafEvaluationData = this.getTafEvaluationDatabyTm(tafEvaluationDataList, tafElement.getTafTm());
			
			TafEvaluationData.Score tafEvaluationScore = null;
			
			// AUTO METAR 에 걸려 생성되어있지 않은 METAR 라면 새로 만들어준다
			if(tafEvaluationData == null) {
				
				tafEvaluationData = new TafEvaluationData();
				
				tafEvaluationData.setEvaluationTm(tafElement.getTafTm());	
				tafEvaluationData.setTafTm(tafElement.getTafTm());
				tafEvaluationData.setTafElementSet(tafElementSet);
				tafEvaluationData.setEvalType(3);
				tafEvaluationDataList.add(tafEvaluationData);
				
				tafEvaluationScore = tafEvaluationData.getScore();
				
				// 점수가 비활성화이므로 -1f 처리를 해준다
				tafEvaluationScore.setWindDirection(-1f);
				tafEvaluationScore.setWindSpeed(-1f);
				tafEvaluationScore.setRainOrClear(-1f);
				tafEvaluationScore.setVisibility(-1f);
				tafEvaluationScore.setCloudAmounts(new Float[]{-1f, -1f, -1f});
				tafEvaluationScore.setCloudHeight(-1f);
				tafEvaluationScore.setCloudAmount(-1f);		
				
			} else {
				
				tafEvaluationData.setEvalType(2);
				tafEvaluationScore = tafEvaluationData.getScore();
			}			
				
			// TAF Element 의 기온 예보가 있으면 기온 평가를 진행한다
			if(tafElement.getTx() != null) {
				
				Float txScore = this.evaluateTx(tafEvaluationData, tafElement, stTafTm, edTafTm, 1);
				
				// 기온평가 버전2 는 무한정 늘릴수는 없다. 기본적으로 2가지를 가져가고 추후에 버전2를 없애는 방향으로 가야함
				Float txVer2Score = this.evaluateTx(tafEvaluationData, tafElement, stTafTm, edTafTm, 3);
				
				// 기온 평가가 오류라면 METAR 만 평가하는걸로 변경한다
				if(txScore == null || txVer2Score == null) {
					
					tafEvaluationData.setEvalType(1);
					
				} else {
				
					tafEvaluationScore.setTemperature(txScore);
					tafEvaluationScore.setTemperatureVer2(txVer2Score);
				}
				
				tafEvaluationScore.setMaxTemp(true);
			}
			
			if(tafElement.getTn() != null) {
				
				Float tnScore = this.evaluateTn(tafEvaluationData, tafElement, stTafTm, edTafTm, 1);
				
				// 기온평가 버전2 는 무한정 늘릴수는 없다. 기본적으로 2가지를 가져가고 추후에 버전2를 없애는 방향으로 가야함
				Float tnVer2Score = this.evaluateTn(tafEvaluationData, tafElement, stTafTm, edTafTm, 3);
				
				// 기온 평가가 오류라면 METAR 만 평가하는걸로 변경한다
				if(tnScore == null || tnVer2Score == null) {
					
					tafEvaluationData.setEvalType(1);
					
				} else {
				
					tafEvaluationScore.setTemperature(tnScore);
					tafEvaluationScore.setTemperatureVer2(tnVer2Score);
				}
				
				tafEvaluationScore.setMinTemp(true);
			}
		}
		
		// 재정렬해준다
		Collections.sort(tafEvaluationDataList, new Comparator<TafEvaluationData>(){

			@Override
			public int compare(TafEvaluationData arg0, TafEvaluationData arg1) {
				return (int)(arg0.getEvaluationTm().getTime() - arg1.getEvaluationTm().getTime());	
			}
		});
	
		return tafEvaluationDataList;
	}
	
	private Float evaluateTn(TafEvaluationData tafEvaluationData, TafElement tafElement, Date stTafTm, Date edTafTm, int version) {
		
		Double tn = tafElement.getTn();
		
		if(tn == null) {
			return null;
		}
		
		Date tafTm = tafElement.getTafTm();
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(tafTm);
		
		cal.add(Calendar.HOUR_OF_DAY, -2);
		cal.add(Calendar.MINUTE, 1);
		
		Date searchStTm = new Date(cal.getTime().getTime());
		
		cal.setTime(tafTm);
		cal.add(Calendar.HOUR_OF_DAY, 2);
		
		Date searchEdTm = new Date(cal.getTime().getTime());
				
		Double minTmp = this.evaluationDatabaseUtil.getAmosMinTemperature(EvaluationUtils.getAirportId(this.stnCd), searchStTm, searchEdTm);
		
		if(minTmp == null) {
			return null;
		}
		
		AmosElement amosElement = new AmosElement();
		amosElement.setTafTm(tafTm);
		amosElement.setTn(Math.round(minTmp) * 1.0);		
		tafEvaluationData.setAmosElement(amosElement);
		
		double gap = Math.abs(tn - Math.round(minTmp));
		
		switch(version) {
		
		case 1:
		
			if(gap <= 1) {
				return 100f;
			} else if(gap > 1 && gap <= 2) {
				return 50f;
			} else {
				return 0f;
			}
			
		case 2:
			
			if(gap <= 2) {
				return 100f;
			} else {
				return 0f;
			}
			
		case 3:
			
			if(gap <= 1) {
				return 100f;
			} else {
				return 0f;
			}
			
		default:
		
			if(gap <= 1) {
				return 100f;
			} else if(gap > 1 && gap <= 2) {
				return 50f;
			} else {
				return 0f;
			}
		}
	}
	
	private Float evaluateTx(TafEvaluationData tafEvaluationData, TafElement tafElement, Date stTafTm, Date edTafTm, int version) {
		
		Double tx = tafElement.getTx();
		
		if(tx == null) {
			return null;
		}
	
		Date tafTm = tafElement.getTafTm();
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(tafTm);
		
		cal.add(Calendar.HOUR_OF_DAY, -2);
		cal.add(Calendar.MINUTE, 1);
		
		Date searchStTm = new Date(cal.getTime().getTime());
		
		cal.setTime(tafTm);
		cal.add(Calendar.HOUR_OF_DAY, 2);
		
		Date searchEdTm = new Date(cal.getTime().getTime());
				
		Double maxTmp = this.evaluationDatabaseUtil.getAmosMaxTemperature(EvaluationUtils.getAirportId(this.stnCd), searchStTm, searchEdTm);
		
		if(maxTmp == null) {
			return null;
		}
		
		AmosElement amosElement = new AmosElement();
		amosElement.setTafTm(tafTm);
		amosElement.setTx(Math.round(maxTmp) * 1.0);		
		tafEvaluationData.setAmosElement(amosElement);
		
		double gap = Math.abs(tx - Math.round(maxTmp));
		
		switch(version) {
		
		case 1:
		
			if(gap <= 1) {
				return 100f;
			} else if(gap > 1 && gap <= 2) {
				return 50f;
			} else {
				return 0f;
			}
			
		case 2:
			
			if(gap <= 2) {
				return 100f;
			} else {
				return 0f;
			}
			
		case 3:
			
			if(gap <= 1) {
				return 100f;
			} else {
				return 0f;
			}
			
		default:
		
			if(gap <= 1) {
				return 100f;
			} else if(gap > 1 && gap <= 2) {
				return 50f;
			} else {
				return 0f;
			}
		}
	}
	
	private Float evaluateCloudHeight(TafElement tafElement, MetarElement metarElement) {
	
		// 일단 5000 이하의 BKN 이상의 구름을 찾는다, TAF 와 METAR 모두, 1층 2층 통합으로!!
		Map<String, Object> metarFirstBknOvcCloudInfo = null;
		Map<String, Object> tafFirstBknOvcCloudInfo = null;
		
		try {
		
			metarFirstBknOvcCloudInfo = this.getFirstBknOvcCloudInfo(metarElement);
			tafFirstBknOvcCloudInfo = this.getFirstBknOvcCloudInfo(tafElement);
			
		} catch (Exception e) {
			return 0f;
		}
		
		// METAR 와 TAF 모두 BKN 이상의 구름이 없다면 100점
		if(metarFirstBknOvcCloudInfo == null && tafFirstBknOvcCloudInfo == null) {
			
			return 100f;
			
		// METAR 에 BKN 이상의 구름이 없고 TAF 에 BKN 이상의 구름이 있다면 
		} else if(metarFirstBknOvcCloudInfo == null && tafFirstBknOvcCloudInfo != null) {
			
			Double firstTafCloudHeight = (double)tafFirstBknOvcCloudInfo.get("firstCloudHeight");
			
			// TAF 의 첫번째 BKN 운고가 5000또는 MSA 이상일때 CAVOK 처리한다
			if(firstTafCloudHeight >= Math.max(5000, EvaluationUtils.getAirportMsa(this.stnCd))) {
				return 100f;
			}
			
			return 0f;
			
		} else if(metarFirstBknOvcCloudInfo != null && tafFirstBknOvcCloudInfo == null) {
			
			Double firstMetarCloudHeight = (double)metarFirstBknOvcCloudInfo.get("firstCloudHeight");
			
			// METAR 의 첫번째 BKN 운고가 5000또는 MSA 이상일때 CAVOK 처리한다
			if(firstMetarCloudHeight >= Math.max(5000, EvaluationUtils.getAirportMsa(this.stnCd))) {
				return 100f;
			}
			
			return 0f;
			
		} else if(metarFirstBknOvcCloudInfo != null && tafFirstBknOvcCloudInfo != null) {
			
			Double firstMetarCloudHeight = (double)metarFirstBknOvcCloudInfo.get("firstCloudHeight");			
			Double firstTafCloudHeight = (double)tafFirstBknOvcCloudInfo.get("firstCloudHeight");
			
			// 첫번째 BKN 이상 구름이 모두 1000ft 이하에 있을 경우
			if(firstTafCloudHeight <= 1000) {
				
				return Math.abs(firstMetarCloudHeight - firstTafCloudHeight) <= 100 ? 100f : 0f;
				
			// 첫번째 BKN 이상 구름이 모두 1000ft 를 초과하고 5000ft 또는 MSA 중 높은 고도 미만일때
			} else if(firstTafCloudHeight > 1000 && firstTafCloudHeight < Math.max(5000, EvaluationUtils.getAirportMsa(this.stnCd))) {
				
				return Math.abs(firstMetarCloudHeight - firstTafCloudHeight) <= firstTafCloudHeight * 0.3 ? 100f : 0f;
				
			// 첫번째 BKN 이상 구름이 모두 5000ft 또는 MSA 중 높은 고도 이상이고 10000ft 이하일때
			} else if((firstMetarCloudHeight >= EvaluationUtils.getAirportMsa(this.stnCd) && firstTafCloudHeight >= EvaluationUtils.getAirportMsa(this.stnCd)) &&
					  (firstMetarCloudHeight <= 10000 && firstTafCloudHeight <= 10000)) {
				return 100f;
			}
		} 
				
		return 0f;
	}
	
	private Float[] evaluateCloudAmount(TafElement tafElement, MetarElement metarElement) {
		
		// 일단 5000 이하의 BKN 이상의 구름을 찾는다, TAF 와 METAR 모두, 1층 2층 통합으로!!
		Map<String, Object> metarFirstBknOvcCloudInfo = null;
		Map<String, Object> tafFirstBknOvcCloudInfo = null;
		
		Float cloudAmountLayer1Score = 0f;
		Float cloudAmountLayer2Score = 0f;
		
		try {
		
			metarFirstBknOvcCloudInfo = this.getFirstBknOvcCloudInfo(metarElement);
			tafFirstBknOvcCloudInfo = this.getFirstBknOvcCloudInfo(tafElement);
			
		} catch (Exception e) {
			return new Float[]{0f, 0f, EvaluationUtils.getCloudAmountScore(0f, 0f)};
		}
		
		int tafCloudLayer1Count = tafElement.getCloudAmountLayer1List().size();
		int tafCloudLayer2Count = tafElement.getCloudAmountLayer2List().size();
		boolean tafCloudClear = (tafElement.isCavok() || tafElement.isNsc() || tafElement.isSkc()) ? true : false;
		
		int metarCloudLayer1Count = metarElement.getCloudAmountLayer1List().size();
		int metarCloudLayer2Count = metarElement.getCloudAmountLayer2List().size();
		boolean metarCloudClear = (metarElement.isCavok() || metarElement.isNsc() || metarElement.isSkc()) ? true : false;
				
		// METAR 관측 운량이 없을때
		if((metarCloudLayer1Count == 0 && metarCloudLayer2Count == 0) || metarCloudClear) {
			
			// TAF 예보 운량이 1층만 있을때
			if((tafCloudLayer1Count > 0 && tafCloudLayer2Count == 0) && !tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 1층과 2층 둘다 있을때
			} else if((tafCloudLayer1Count > 0 && tafCloudLayer2Count > 0) && !tafCloudClear) {
			
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 2층만 있을때
			} else if((tafCloudLayer1Count == 0 && tafCloudLayer2Count > 0) && !tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 없을때
			} else if((tafCloudLayer1Count == 0 && tafCloudLayer2Count == 0) || tafCloudClear) {
				
				cloudAmountLayer1Score = 100f;
				cloudAmountLayer2Score = 100f;
			}
		
		// METAR 관측 운량이 1층만 있을때
		} else if((metarCloudLayer1Count > 0 && metarCloudLayer2Count == 0) && !metarCloudClear) {
			
			// TAF 예보 운량이 1층만 있을때
			if((tafCloudLayer1Count > 0 && tafCloudLayer2Count == 0) && !tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
				//cloudAmountLayer2Score = -1f;
				
			// TAF 예보 운량이 1층과 2층 둘다 있을때
			} else if((tafCloudLayer1Count > 0 && tafCloudLayer2Count > 0) && !tafCloudClear) {
			
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 2층만 있을때
			} else if((tafCloudLayer1Count == 0 && tafCloudLayer2Count > 0) && !tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 없을때
			} else if((tafCloudLayer1Count == 0 && tafCloudLayer2Count == 0) || tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
			}
			
		// METAR 관측 운량이 1층과 2층 둘다 있을때
		} else if((metarCloudLayer1Count > 0 && metarCloudLayer2Count > 0) && !metarCloudClear) {
			
			// TAF 예보 운량이 1층만 있을때
			if((tafCloudLayer1Count > 0 && tafCloudLayer2Count == 0) && !tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 1층과 2층 둘다 있을때
			} else if((tafCloudLayer1Count > 0 && tafCloudLayer2Count > 0) && !tafCloudClear) {
			
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 2층만 있을때
			} else if((tafCloudLayer1Count == 0 && tafCloudLayer2Count > 0) && !tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 없을때
			} else if((tafCloudLayer1Count == 0 && tafCloudLayer2Count == 0) || tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
			}
			
		// METAR 관측 운량이 2층만 있을때
		} else if((metarCloudLayer1Count == 0 && metarCloudLayer2Count > 0) && !metarCloudClear) {
			
			// TAF 예보 운량이 1층만 있을때
			if((tafCloudLayer1Count > 0 && tafCloudLayer2Count == 0) && !tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 1층과 2층 둘다 있을때
			} else if((tafCloudLayer1Count > 0 && tafCloudLayer2Count > 0) && !tafCloudClear) {
			
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 2층만 있을때
			} else if((tafCloudLayer1Count == 0 && tafCloudLayer2Count > 0) && !tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
				
			// TAF 예보 운량이 없을때
			} else if((tafCloudLayer1Count == 0 && tafCloudLayer2Count == 0) || tafCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(tafElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(tafElement, metarElement);
			}
		}
		
		return new Float[]{cloudAmountLayer1Score, cloudAmountLayer2Score, EvaluationUtils.getCloudAmountScore(cloudAmountLayer1Score, cloudAmountLayer2Score)};
	}
	
	private Float calculateCloudAmount2LayerScore(TafElement tafElement, MetarElement metarElement) {
		
		List<String> tafCloudAmount2LayerList = tafElement.getCloudAmountLayer2List();		
		List<String> metarCloudAmount2LayerList = metarElement.getCloudAmountLayer2List();
		
		boolean tafBknOvc = false;
		boolean metarBknOvc = false;
		
		for(int i=0 ; i<tafCloudAmount2LayerList.size() ; i++) {
			
			if("BKN".equals(tafCloudAmount2LayerList.get(i)) || "OVC".equals(tafCloudAmount2LayerList.get(i))) {
				tafBknOvc = true;
			}
		}
		
		for(int i=0 ; i<metarCloudAmount2LayerList.size() ; i++) {
			
			if("BKN".equals(metarCloudAmount2LayerList.get(i)) || "OVC".equals(metarCloudAmount2LayerList.get(i))) {
				metarBknOvc = true;
			}
		}
		
		if(tafBknOvc == true && metarBknOvc == true) {
			return 100f;
		} else if(tafBknOvc == false && metarBknOvc == false) {
			return 100f;
		} else {
			return 0f;
		}
	}
	
	private Float calculateCloudAmount1LayerScore(TafElement tafElement, MetarElement metarElement) {
			
		List<String> tafCloudAmountLayer1List = tafElement.getCloudAmountLayer1List();
		List<String> metarCloudAmountLayer1List = metarElement.getCloudAmountLayer1List();
		
		List<String> cloudAmountCategoryList = Arrays.asList(new String[]{
			"", "FEW", "SCT", "BKN", "OVC"
		});
		
		Integer maxMetarCloudAmount = 0;
		Integer maxTafCloudAmount = 0;
		
		for(int i=0 ; i<metarCloudAmountLayer1List.size() ; i++) {
			maxMetarCloudAmount = Math.max(maxMetarCloudAmount, cloudAmountCategoryList.indexOf(metarCloudAmountLayer1List.get(i)));
		}
		
		for(int i=0 ; i<tafCloudAmountLayer1List.size() ; i++) {
			maxTafCloudAmount = Math.max(maxTafCloudAmount, cloudAmountCategoryList.indexOf(tafCloudAmountLayer1List.get(i)));
		}
		
		if(Math.abs(maxMetarCloudAmount - maxTafCloudAmount) <= 1) {			
			return 100f;
		} else {			
			return 0f;
		}		
	}
	
	private Float evaluateRainOrClear(TafElement tafElement, MetarElement metarElement) {
		
		String tafSkyCondition = tafElement.getSkyCondition();		
		String metarSkyCondition = metarElement.getSkyCondition();
		
		String regex = "(.*)(DZ|RA|SN|SG|PL|GR|GS|UP)+(.*)";
		
		if(tafSkyCondition.matches(regex)) {
			
			if(metarSkyCondition.matches(regex)) {				
				return 100f;
			} else {
				return 0f;
			}
			
		} else {
			
			// 일기현상이 종료되는 경우
			if(tafElement.isNsw()) {
				
			}
			
			if(metarSkyCondition.matches(regex)) {				
				return 0f;
			} else {
				return 100f;
			}
		}
	}
	
	private Float evaluateVis(TafElement tafElement, MetarElement metarElement) {
		
		Double tafVis = tafElement.getVis();
		Double metarVis = metarElement.getVis();
		
		// CAVOK 인 경우에는 시정을 9999로 가정한다
		if(tafElement.isCavok()) {
			tafVis = 9999.0;
		}
		
		if(metarElement.isCavok()) {
			metarVis = 9999.0;
		}
		
		// 공항경보 기준치는 800m로 가정한다
		if(tafVis <= 800) {
			
			if(Math.abs(tafVis - metarVis) <= 200) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		if(tafVis > 800 && tafVis <= 1500) {
			
			if(Math.abs(tafVis - metarVis) <= 300) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		if(tafVis > 1500 && tafVis <= 3000) {
			
			if(Math.abs(tafVis - metarVis) <= 500) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		if(tafVis > 3000 && tafVis < 5000) {
			
			if(Math.abs(tafVis - metarVis) <= 1000) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		if(tafVis >= 5000) {
			
			if(metarVis >= 5000) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		return 0f;
	}
	
	private Float evaluateWindDirection(TafElement tafElement, MetarElement metarElement) {
		
		if(metarElement.getWspd() <= 5 || metarElement.isVrb()) {
			
			return 100f;
			
		} else if(metarElement.getWspd() > 5) {
			
			if(tafElement.isVrb()) {
				
				return 0f;
				
			} else {
				
				Double gap = Math.abs(metarElement.getWdir() - tafElement.getWdir());
				
				gap = Math.min(gap, 360 - gap);
				
				if(gap <= 20) {	
					return 100f;
				} else if(gap > 20 && gap <= 50) {					
					return 50f;
				} else {						
					return 0f;
				}
			}
		}
		
		return 0f;
	}
	
	private Float evaluateWindSpeed(TafElement tafElement, MetarElement metarElement) {
		
		Double gap = Math.abs(metarElement.getWspd() - tafElement.getWspd());
		
		if(gap <= 5) {
			return 100f;
		} else {
			return 0f;
		}
	}
}
