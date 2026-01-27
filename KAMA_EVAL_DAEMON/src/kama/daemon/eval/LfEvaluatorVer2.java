package kama.daemon.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.eval.lf.LfData;
import kama.daemon.eval.lf.LfElement;
import kama.daemon.eval.lf.LfElementSet;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.util.DaemonUtil;
import kama.daemon.util.EvaluationUtils;

public class LfEvaluatorVer2 extends LfEvaluator {
	
	private String stnCd;
	
	@Override
	public Map<String, Object> getEvaluationResult(List<LfEvaluationData> lfEvaluationDataList, int scoreFixCount) {
		
		Map<String, Object> lfEvaluationResult = new HashMap<String, Object>();
		
		Float windDirectionScoreSum = 0f;
		Float windSpeedScoreSum = 0f;
		Float visibilityScoreSum = 0f;
		Float rainOrClearScoreSum = 0f;
		Float cloudAmountScoreSum = 0f;
		Float cloudHeightScoreSum = 0f;
		
		int availableCnt = 0;
		
		for(int i=0 ; i<lfEvaluationDataList.size() ; i++) {
			
			if(!lfEvaluationDataList.get(i).isAvailable()) {
				continue;
			}
			
			availableCnt++;
			
			LfEvaluationData.Score score = lfEvaluationDataList.get(i).getScore();
			
			// 평가 방법 1은 각 요소마다 가중치를 부여해 계산해야됨
			score.setTotal(
				DaemonUtil.setNumberFix((					
					score.getWindDirection() +
					score.getWindSpeed() +
					score.getVisibility() +
					score.getRainOrClear() +
					score.getCloudAmount() +
					score.getCloudHeight())/6,							 
				scoreFixCount)
			);
			
			windDirectionScoreSum += score.getWindDirection();
			windSpeedScoreSum += score.getWindSpeed();
			visibilityScoreSum += score.getVisibility();
			rainOrClearScoreSum += score.getRainOrClear();
			cloudAmountScoreSum += score.getCloudAmount();
			cloudHeightScoreSum += score.getCloudHeight();
		}
		
		Float windDirectionScoreAvg = DaemonUtil.setNumberFix(windDirectionScoreSum / availableCnt, scoreFixCount);
		Float windSpeedScoreAvg = DaemonUtil.setNumberFix(windSpeedScoreSum / availableCnt, scoreFixCount);
		Float visibilityScoreAvg = DaemonUtil.setNumberFix(visibilityScoreSum / availableCnt, scoreFixCount);
		Float rainOrClearScoreAvg = DaemonUtil.setNumberFix(rainOrClearScoreSum / availableCnt, scoreFixCount);
		Float cloudAmountScoreAvg = DaemonUtil.setNumberFix(cloudAmountScoreSum / availableCnt, scoreFixCount);
		Float cloudHeightScoreAvg = DaemonUtil.setNumberFix(cloudHeightScoreSum / availableCnt, scoreFixCount);
		
		Float totalScore = DaemonUtil.setNumberFix((
			windDirectionScoreAvg + 
			windSpeedScoreAvg + 
			visibilityScoreAvg + 
			rainOrClearScoreAvg + 
			cloudAmountScoreAvg + 
			cloudHeightScoreAvg)/6, 
		scoreFixCount);
		
		lfEvaluationResult.put("windDirectionScoreAvg", windDirectionScoreAvg);
		lfEvaluationResult.put("windSpeedScoreAvg", windSpeedScoreAvg);
		lfEvaluationResult.put("visibilityScoreAvg", visibilityScoreAvg);
		lfEvaluationResult.put("rainOrClearScoreAvg", rainOrClearScoreAvg);
		lfEvaluationResult.put("cloudAmountScoreAvg", cloudAmountScoreAvg);
		lfEvaluationResult.put("cloudHeightScoreAvg", cloudHeightScoreAvg);
		
		lfEvaluationResult.put("windDirectionScore", windDirectionScoreAvg);
		lfEvaluationResult.put("windSpeedScore", windSpeedScoreAvg);
		lfEvaluationResult.put("visibilityScore", visibilityScoreAvg);
		lfEvaluationResult.put("rainOrClearScore", rainOrClearScoreAvg);
		lfEvaluationResult.put("cloudAmountScore", cloudAmountScoreAvg);
		lfEvaluationResult.put("cloudHeightScore", cloudHeightScoreAvg);
		lfEvaluationResult.put("totalScore", totalScore);
		
		if(availableCnt > 0) {
			lfEvaluationResult.put("available", true);
		} else {
			lfEvaluationResult.put("available", false);
		}
		
		return lfEvaluationResult;
	}
	
	@Override
	public List<LfEvaluationData> evaluate(String stnCd, Date stLfTm, Date edLfTm, List<LfElementSet> lfElementSetList, List<MetarElement> metarElementList) throws Exception {
		
		this.stnCd = stnCd;
		
		List<LfEvaluationData> lfEvaluationDataList = new ArrayList<LfEvaluationData>();
		
		// 평가는 Metar 를 기준으로 수행한다
		
		for(int i=0 ; i<metarElementList.size() ; i++) {
			
			MetarElement evalMetarElement = metarElementList.get(i);
			
			// evalMetarElement 에 맞는 lfElementSet 을 찾는다
			Map<String, Object> lfElementSetInfo = this.getLfElementSetInfobyTm(lfElementSetList, evalMetarElement.getMetarTm());			
			LfElementSet evalLfElementSet  = (LfElementSet)lfElementSetInfo.get("lfElementSet");
			Integer evalLfElementSetIndex  = (Integer)lfElementSetInfo.get("lfElementSetIndex");
			
			LfEvaluationData lfEvaluationData = new LfEvaluationData();
			
			if(!this.checkAvailableEvaluation(evalLfElementSet, evalMetarElement)) {
				lfEvaluationData.setAvailable(false);
				continue;
			}
			
			lfEvaluationDataList.add(lfEvaluationData);
			
			LfElement evalFcstLfElement = evalLfElementSet.getStateLfElement(LfData.State.FCST);
			LfElement evalBecmgLfElement = evalLfElementSet.getStateLfElement(LfData.State.BECMG);
			LfElement evalTempoLfElement = evalLfElementSet.getStateLfElement(LfData.State.TEMPO);
			LfElement evalFmLfElement = evalLfElementSet.getStateLfElement(LfData.State.FM);
			
			lfEvaluationData.setEvaluationTm(evalMetarElement.getMetarTm());	
			lfEvaluationData.setLfTm(evalLfElementSet.getLfTm());
			lfEvaluationData.setMetarTm(evalMetarElement.getMetarTm());
			lfEvaluationData.setLfElementSet(evalLfElementSet);
			lfEvaluationData.setMetarElement(evalMetarElement);
			LfEvaluationData.Score lfEvaluationScore = lfEvaluationData.getScore();
			
			
			// 변화군이 적용되어있지 않으면 일반 평가 로직을 따른다
			if(evalBecmgLfElement == null && evalTempoLfElement == null) {
				
				Float windDirectionScore = this.evaluateWindDirection(evalFcstLfElement, evalMetarElement);
				Float windSpeedScore = this.evaluateWindSpeed(evalFcstLfElement, evalMetarElement);
				Float visibilityScore = this.evaluateVis(evalFcstLfElement, evalMetarElement);
				Float rainOrClearScore = this.evaluateRainOrClear(evalFcstLfElement, evalMetarElement);		
				Float[] cloudAmountScores = this.evaluateCloudAmount(evalFcstLfElement, evalMetarElement);
				Float cloudHeightScore = this.evaluateCloudHeight(evalFcstLfElement, evalMetarElement);
				
				lfEvaluationScore.setWindDirection(windDirectionScore);
				lfEvaluationScore.setWindSpeed(windSpeedScore);
				lfEvaluationScore.setVisibility(visibilityScore);
				lfEvaluationScore.setRainOrClear(rainOrClearScore);
				lfEvaluationScore.setCloudAmounts(cloudAmountScores);
				lfEvaluationScore.setCloudHeight(cloudHeightScore);
					
			// BECMG 변화군인 경우
			} else if (evalBecmgLfElement != null && evalTempoLfElement == null) {
				
//				System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(evalLfElementSet.getLfTm()) + "," + evalLfElementSet.getBecmgEvaluateMode());
					
				// 변화전 예보
				LfElement beforeStateLfElement = null; 
						
				// 변화후 예보 (BECMG 이후의 예보를 사용하면 바로 TEMPO 변화군이 올 수 있는 위험성이 있다, TEMPO 변화군은 바로 예보에 영향을 미치기 때문.)
				LfElement afterStateLfElement = null; 
						
				// 현재 변화군이 마지막인 경우에는 이미 예보값에 반영이 되어있으므로 이전 TAF 에보를 가져온다
				if(evalBecmgLfElement.getStateStatus() == 2) {
					beforeStateLfElement = lfElementSetList.get(evalLfElementSetIndex > 0 ? evalLfElementSetIndex-1 : evalLfElementSetIndex).getFcstLfElement();
					afterStateLfElement = evalFcstLfElement;
				} else {
					beforeStateLfElement = evalFcstLfElement;
					afterStateLfElement = this.adoptBecmgLfElement(evalFcstLfElement, evalBecmgLfElement);
				}
				
				if(evalLfElementSet.getBecmgEvaluateMode() == 1) {
					
					// 변화전 예보로 평가한다
					Float windDirectionScore = this.evaluateWindDirection(beforeStateLfElement, evalMetarElement);
					Float windSpeedScore = this.evaluateWindSpeed(beforeStateLfElement, evalMetarElement);
					Float visibilityScore = this.evaluateVis(beforeStateLfElement, evalMetarElement);
					Float rainOrClearScore = this.evaluateRainOrClear(beforeStateLfElement, evalMetarElement);					
					Float[] cloudAmountScores = this.evaluateCloudAmount(beforeStateLfElement, evalMetarElement);	
					Float cloudHeightScore = this.evaluateCloudHeight(beforeStateLfElement, evalMetarElement);
						
					lfEvaluationScore.setWindDirection(windDirectionScore);
					lfEvaluationScore.setWindSpeed(windSpeedScore);
					lfEvaluationScore.setVisibility(visibilityScore);
					lfEvaluationScore.setRainOrClear(rainOrClearScore);			
					lfEvaluationScore.setCloudAmounts(cloudAmountScores);
					lfEvaluationScore.setCloudHeight(cloudHeightScore);
					
				} else if(evalLfElementSet.getBecmgEvaluateMode() == 2) {
					
					// 변화후 예보로 평가한다
					Float windDirectionScore = this.evaluateWindDirection(afterStateLfElement, evalMetarElement);
					Float windSpeedScore = this.evaluateWindSpeed(afterStateLfElement, evalMetarElement);
					Float visibilityScore = this.evaluateVis(afterStateLfElement, evalMetarElement);
					Float rainOrClearScore = this.evaluateRainOrClear(afterStateLfElement, evalMetarElement);					
					Float[] cloudAmountScores = this.evaluateCloudAmount(afterStateLfElement, evalMetarElement);	
					Float cloudHeightScore = this.evaluateCloudHeight(afterStateLfElement, evalMetarElement);
						
					lfEvaluationScore.setWindDirection(windDirectionScore);
					lfEvaluationScore.setWindSpeed(windSpeedScore);
					lfEvaluationScore.setVisibility(visibilityScore);
					lfEvaluationScore.setRainOrClear(rainOrClearScore);			
					lfEvaluationScore.setCloudAmounts(cloudAmountScores);
					lfEvaluationScore.setCloudHeight(cloudHeightScore);
					
				} else if(evalLfElementSet.getBecmgEvaluateMode() == 3) {
					
					// 변화전 예보로 평가한다
					Float windDirectionScore = this.evaluateWindDirection(beforeStateLfElement, evalMetarElement);
					Float windSpeedScore = this.evaluateWindSpeed(beforeStateLfElement, evalMetarElement);
					Float visibilityScore = this.evaluateVis(beforeStateLfElement, evalMetarElement);
					Float rainOrClearScore = this.evaluateRainOrClear(beforeStateLfElement, evalMetarElement);					
					Float[] cloudAmountScores = this.evaluateCloudAmount(beforeStateLfElement, evalMetarElement);	
					Float cloudHeightScore = this.evaluateCloudHeight(beforeStateLfElement, evalMetarElement);
						
					// 변화후 예보 점수와 비교하여 높은 것을 택한다 (변화전 예보와 변화후 예보 같은 요소는 중복되므로 문제되지 않음)
					windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(afterStateLfElement, evalMetarElement));
					windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(afterStateLfElement, evalMetarElement));
					visibilityScore = Math.max(visibilityScore, this.evaluateVis(afterStateLfElement, evalMetarElement));
					rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(afterStateLfElement, evalMetarElement));				
					cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(afterStateLfElement, evalMetarElement));
					cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(afterStateLfElement, evalMetarElement));
					
					lfEvaluationScore.setWindDirection(windDirectionScore);
					lfEvaluationScore.setWindSpeed(windSpeedScore);
					lfEvaluationScore.setVisibility(visibilityScore);
					lfEvaluationScore.setRainOrClear(rainOrClearScore);			
					lfEvaluationScore.setCloudAmounts(cloudAmountScores);
					lfEvaluationScore.setCloudHeight(cloudHeightScore);
				}
				
			// TEMPO 변화군인 경우, 변화하는 요소에 대해서만 적용해야한다
			} else if (evalBecmgLfElement == null && evalTempoLfElement != null) {
				
				Map<String, Object> stateInfo = this.getStateInfo(lfElementSetList, evalLfElementSetIndex, evalTempoLfElement.getState());
				
				if(stateInfo == null) {
					lfEvaluationData.setAvailable(false);
					continue;
				}
				
				Map<String, Boolean> changedElementInfo = this.getChangedElementInfo(evalTempoLfElement);
				
				// Metar 정보에서 현재 LfElement 의 시각과 +1시간, -1시간에 해당하는 모든 Metar 리스트를 가져온다				
				List<MetarElement> subMetarElementList = this.findMetarElementListBetweenHours(evalFcstLfElement.getLfTm(), metarElementList, stateInfo, 1);
				
				// 현재 Metar 로 기본 점수 셋팅
				Float windDirectionScore = this.evaluateWindDirection(evalFcstLfElement, evalMetarElement);
				Float windSpeedScore = this.evaluateWindSpeed(evalFcstLfElement, evalMetarElement);
				Float visibilityScore = this.evaluateVis(evalFcstLfElement, evalMetarElement);
				Float rainOrClearScore = this.evaluateRainOrClear(evalFcstLfElement, evalMetarElement);
				Float[] cloudAmountScores = this.evaluateCloudAmount(evalFcstLfElement, evalMetarElement);
				Float cloudHeightScore = this.evaluateCloudHeight(evalFcstLfElement, evalMetarElement);				
					
				for(int j=0 ; j<subMetarElementList.size() ; j++) {
					
					// 변화한 요소에 대해서는 -1~+1 시간의 관측에 대해서 모두 평가하여 가장 높은 점수를 줌
					
					if(changedElementInfo.get("windDirection")) {
						windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(evalFcstLfElement, subMetarElementList.get(j)));	
					}
					
					if(changedElementInfo.get("windSpeed")) {
						windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(evalFcstLfElement, subMetarElementList.get(j)));
					}
					
					if(changedElementInfo.get("visibility")) {
						visibilityScore = Math.max(visibilityScore, this.evaluateVis(evalFcstLfElement, subMetarElementList.get(j)));
					}
					
					if(changedElementInfo.get("rainOrClear")) {
						rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(evalFcstLfElement, subMetarElementList.get(j)));
					}
					
					if(changedElementInfo.get("cloudAmount")) {
						cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(evalFcstLfElement, subMetarElementList.get(j)));
					}
					
					if(changedElementInfo.get("cloudHeight")) {
						cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(evalFcstLfElement, subMetarElementList.get(j)));	
					}		
				}
				
				lfEvaluationScore.setWindDirection(windDirectionScore);
				lfEvaluationScore.setWindSpeed(windSpeedScore);
				lfEvaluationScore.setVisibility(visibilityScore);
				lfEvaluationScore.setRainOrClear(rainOrClearScore);			
				lfEvaluationScore.setCloudAmounts(cloudAmountScores);
				lfEvaluationScore.setCloudHeight(cloudHeightScore);
				
			// BECMG 변화군과 TEMPO 변화군이 동시에 적용된 경우 ( 이부분은 잘 모르겠음.. 두가지 변화군을 다하고 높은 점수로 하자)
			} else if (evalBecmgLfElement != null && evalTempoLfElement != null) {
				
				Float windDirectionScore = 0f;
				Float windSpeedScore = 0f;
				Float visibilityScore = 0f;
				Float rainOrClearScore = 0f;
				Float[] cloudAmountScores = new Float[]{0f, 0f, 0f};
				Float cloudHeightScore = 0f;
				
				// BECMG 변화군 부분
				{
						
					// 변화전 예보
					LfElement beforeStateLfElement = null; 
							
					// 변화후 예보 (BECMG 이후의 예보를 사용하면 바로 TEMPO 변화군이 올 수 있는 위험성이 있다, TEMPO 변화군은 바로 예보에 영향을 미치기 때문.)
					LfElement afterStateLfElement = null; 
							
					if(evalBecmgLfElement.getStateStatus() == 2) {
						beforeStateLfElement = lfElementSetList.get(evalLfElementSetIndex > 0 ? evalLfElementSetIndex-1 : evalLfElementSetIndex).getFcstLfElement();
						afterStateLfElement = evalFcstLfElement;
					} else {
						beforeStateLfElement = evalFcstLfElement;
						afterStateLfElement = this.adoptBecmgLfElement(evalFcstLfElement, evalBecmgLfElement);
					}
					
					if(evalLfElementSet.getBecmgEvaluateMode() == 1) {
						
						// 변화전 예보로 평가한다
						windDirectionScore = this.evaluateWindDirection(beforeStateLfElement, evalMetarElement);
						windSpeedScore = this.evaluateWindSpeed(beforeStateLfElement, evalMetarElement);
						visibilityScore = this.evaluateVis(beforeStateLfElement, evalMetarElement);
						rainOrClearScore = this.evaluateRainOrClear(beforeStateLfElement, evalMetarElement);					
						cloudAmountScores = this.evaluateCloudAmount(beforeStateLfElement, evalMetarElement);	
						cloudHeightScore = this.evaluateCloudHeight(beforeStateLfElement, evalMetarElement);
						
					} else if(evalLfElementSet.getBecmgEvaluateMode() == 2) {
						
						// 변화후 예보로 평가한다
						windDirectionScore = this.evaluateWindDirection(afterStateLfElement, evalMetarElement);
						windSpeedScore = this.evaluateWindSpeed(afterStateLfElement, evalMetarElement);
						visibilityScore = this.evaluateVis(afterStateLfElement, evalMetarElement);
						rainOrClearScore = this.evaluateRainOrClear(afterStateLfElement, evalMetarElement);					
						cloudAmountScores = this.evaluateCloudAmount(afterStateLfElement, evalMetarElement);	
						cloudHeightScore = this.evaluateCloudHeight(afterStateLfElement, evalMetarElement);
							
						lfEvaluationScore.setWindDirection(windDirectionScore);
						lfEvaluationScore.setWindSpeed(windSpeedScore);
						lfEvaluationScore.setVisibility(visibilityScore);
						lfEvaluationScore.setRainOrClear(rainOrClearScore);			
						lfEvaluationScore.setCloudAmounts(cloudAmountScores);
						lfEvaluationScore.setCloudHeight(cloudHeightScore);
						
					} else if(evalLfElementSet.getBecmgEvaluateMode() == 3) {
						
						// 변화전 예보로 평가한다
						windDirectionScore = this.evaluateWindDirection(beforeStateLfElement, evalMetarElement);
						windSpeedScore = this.evaluateWindSpeed(beforeStateLfElement, evalMetarElement);
						visibilityScore = this.evaluateVis(beforeStateLfElement, evalMetarElement);
						rainOrClearScore = this.evaluateRainOrClear(beforeStateLfElement, evalMetarElement);					
						cloudAmountScores = this.evaluateCloudAmount(beforeStateLfElement, evalMetarElement);	
						cloudHeightScore = this.evaluateCloudHeight(beforeStateLfElement, evalMetarElement);
							
						// 변화후 예보 점수와 비교하여 높은 것을 택한다 (변화전 예보와 변화후 예보 같은 요소는 중복되므로 문제되지 않음)
						windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(afterStateLfElement, evalMetarElement));
						windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(afterStateLfElement, evalMetarElement));
						visibilityScore = Math.max(visibilityScore, this.evaluateVis(afterStateLfElement, evalMetarElement));
						rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(afterStateLfElement, evalMetarElement));				
						cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(afterStateLfElement, evalMetarElement));
						cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(afterStateLfElement, evalMetarElement));						
					}
				}
				
				// TEMPO 변화군 부분
				{
					
					Map<String, Object> stateInfo = this.getStateInfo(lfElementSetList, evalLfElementSetIndex, evalTempoLfElement.getState());
					
					if(stateInfo == null) {
						lfEvaluationData.setAvailable(false);
						continue;
					}
					
					Map<String, Boolean> changedElementInfo = this.getChangedElementInfo(evalTempoLfElement);
					
					// Metar 정보에서 현재 LfElement 의 시각과 +1시간, -1시간에 해당하는 모든 Metar 리스트를 가져온다				
					List<MetarElement> subMetarElementList = this.findMetarElementListBetweenHours(evalFcstLfElement.getLfTm(), metarElementList, stateInfo, 1);
					
					// 현재 Metar 로 기본 점수 셋팅
					windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(evalFcstLfElement, evalMetarElement));
					windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(evalFcstLfElement, evalMetarElement));
					visibilityScore = Math.max(visibilityScore, this.evaluateVis(evalFcstLfElement, evalMetarElement));
					rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(evalFcstLfElement, evalMetarElement));
					cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(evalFcstLfElement, evalMetarElement));
					cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(evalFcstLfElement, evalMetarElement));			
						
					for(int j=0 ; j<subMetarElementList.size() ; j++) {
						
						if(changedElementInfo.get("windDirection")) {
							windDirectionScore = Math.max(windDirectionScore, this.evaluateWindDirection(evalFcstLfElement, subMetarElementList.get(j)));	
						}
						
						if(changedElementInfo.get("windSpeed")) {
							windSpeedScore = Math.max(windSpeedScore, this.evaluateWindSpeed(evalFcstLfElement, subMetarElementList.get(j)));
						}
						
						if(changedElementInfo.get("visibility")) {
							visibilityScore = Math.max(visibilityScore, this.evaluateVis(evalFcstLfElement, subMetarElementList.get(j)));
						}
						
						if(changedElementInfo.get("rainOrClear")) {
							rainOrClearScore = Math.max(rainOrClearScore, this.evaluateRainOrClear(evalFcstLfElement, subMetarElementList.get(j)));
						}
						
						if(changedElementInfo.get("cloudAmount")) {
							cloudAmountScores = EvaluationUtils.maxCloudAmountScores(cloudAmountScores, this.evaluateCloudAmount(evalFcstLfElement, subMetarElementList.get(j)));
						}
						
						if(changedElementInfo.get("cloudHeight")) {
							cloudHeightScore = Math.max(cloudHeightScore, this.evaluateCloudHeight(evalFcstLfElement, subMetarElementList.get(j)));	
						}					
					}					
				}
				
				lfEvaluationScore.setWindDirection(windDirectionScore);
				lfEvaluationScore.setWindSpeed(windSpeedScore);
				lfEvaluationScore.setVisibility(visibilityScore);
				lfEvaluationScore.setRainOrClear(rainOrClearScore);			
				lfEvaluationScore.setCloudAmounts(cloudAmountScores);
				lfEvaluationScore.setCloudHeight(cloudHeightScore);
			}
		}
		
		return lfEvaluationDataList;
	}
	
	private Float evaluateCloudHeight(LfElement lfElement, MetarElement metarElement) {
	
		// 일단 5000 이하의 BKN 이상의 구름을 찾는다, TAF 와 METAR 모두, 1층 2층 통합으로!!
		Map<String, Object> metarFirstBknOvcCloudInfo = null;
		Map<String, Object> lfFirstBknOvcCloudInfo = null;
		
		try {
		
			metarFirstBknOvcCloudInfo = this.getFirstBknOvcCloudInfo(metarElement);
			lfFirstBknOvcCloudInfo = this.getFirstBknOvcCloudInfo(lfElement);
			
		} catch (Exception e) {
			return 0f;
		}
		
		// METAR 와 TAF 모두 BKN 이상의 구름이 없다면 100점
		if(metarFirstBknOvcCloudInfo == null && lfFirstBknOvcCloudInfo == null) {
			
			return 100f;
			
		// METAR 와 TAF 중에 하나만 BKN 이상의 구름이 있다면 0점
		} else if((metarFirstBknOvcCloudInfo == null && lfFirstBknOvcCloudInfo != null) || 
				   metarFirstBknOvcCloudInfo != null && lfFirstBknOvcCloudInfo == null) {
			
			return 0f;
			
		} else if(metarFirstBknOvcCloudInfo != null && lfFirstBknOvcCloudInfo != null) {
			
			Double firstMetarCloudHeight = (double)metarFirstBknOvcCloudInfo.get("firstCloudHeight");			
			Double firstLfCloudHeight = (double)lfFirstBknOvcCloudInfo.get("firstCloudHeight");
			
			// 첫번째 BKN 이상 구름이 모두 1000ft 이하에 있을 경우
			if(firstLfCloudHeight <= 1000) {
				
				return Math.abs(firstMetarCloudHeight - firstLfCloudHeight) <= 100 ? 100f : 0f;
				
			// 첫번째 BKN 이상 구름이 모두 1000ft 를 초과하고 5000ft 또는 MSA 중 높은 고도 미만일때
			} else if(firstLfCloudHeight > 1000 && firstLfCloudHeight < Math.max(5000, EvaluationUtils.getAirportMsa(this.stnCd))) {
				
				return Math.abs(firstMetarCloudHeight - firstLfCloudHeight) <= firstLfCloudHeight * 0.3 ? 100f : 0f;
				
			// 첫번째 BKN 이상 구름이 모두 5000ft 또는 MSA 중 높은 고도 이상이고 10000ft 미만일때
			} else if((firstMetarCloudHeight >= EvaluationUtils.getAirportMsa(this.stnCd) && firstLfCloudHeight >= EvaluationUtils.getAirportMsa(this.stnCd)) &&
					  (firstMetarCloudHeight < 10000 && firstLfCloudHeight < 10000)) {
				return 100f;
			}
		} 
				
		return 0f;
	}
	
	private Float[] evaluateCloudAmount(LfElement lfElement, MetarElement metarElement) {
		
		// 일단 5000 이하의 BKN 이상의 구름을 찾는다, TAF 와 METAR 모두, 1층 2층 통합으로!!
		Map<String, Object> metarFirstBknOvcCloudInfo = null;
		Map<String, Object> lfFirstBknOvcCloudInfo = null;
		
		Float cloudAmountLayer1Score = 0f;
		Float cloudAmountLayer2Score = 0f;
		
		try {
		
			metarFirstBknOvcCloudInfo = this.getFirstBknOvcCloudInfo(metarElement);
			lfFirstBknOvcCloudInfo = this.getFirstBknOvcCloudInfo(lfElement);
			
		} catch (Exception e) {
			return new Float[]{0f, 0f, EvaluationUtils.getCloudAmountScore(0f, 0f)};
		}
		
		int lfCloudLayer1Count = lfElement.getCloudAmountLayer1List().size();
		int lfCloudLayer2Count = lfElement.getCloudAmountLayer2List().size();
		boolean lfCloudClear = (lfElement.isCavok() || lfElement.isNsc() || lfElement.isSkc()) ? true : false;
		
		int metarCloudLayer1Count = metarElement.getCloudAmountLayer1List().size();
		int metarCloudLayer2Count = metarElement.getCloudAmountLayer2List().size();
		boolean metarCloudClear = (metarElement.isCavok() || metarElement.isNsc() || metarElement.isSkc()) ? true : false;
				
		// METAR 관측 운량이 없을때
		if((metarCloudLayer1Count == 0 && metarCloudLayer2Count == 0) || metarCloudClear) {
			
			// TAF 예보 운량이 1층만 있을때
			if((lfCloudLayer1Count > 0 && lfCloudLayer2Count == 0) && !lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 1층과 2층 둘다 있을때
			} else if((lfCloudLayer1Count > 0 && lfCloudLayer2Count > 0) && !lfCloudClear) {
			
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 2층만 있을때
			} else if((lfCloudLayer1Count == 0 && lfCloudLayer2Count > 0) && !lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 없을때
			} else if((lfCloudLayer1Count == 0 && lfCloudLayer2Count == 0) || lfCloudClear) {
				
				cloudAmountLayer1Score = 100f;
				cloudAmountLayer2Score = 100f;
			}
		
		// METAR 관측 운량이 1층만 있을때
		} else if((metarCloudLayer1Count > 0 && metarCloudLayer2Count == 0) && !metarCloudClear) {
			
			// TAF 예보 운량이 1층만 있을때
			if((lfCloudLayer1Count > 0 && lfCloudLayer2Count == 0) && !lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
				//cloudAmountLayer2Score = -1f;
				
			// TAF 예보 운량이 1층과 2층 둘다 있을때
			} else if((lfCloudLayer1Count > 0 && lfCloudLayer2Count > 0) && !lfCloudClear) {
			
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 2층만 있을때
			} else if((lfCloudLayer1Count == 0 && lfCloudLayer2Count > 0) && !lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 없을때
			} else if((lfCloudLayer1Count == 0 && lfCloudLayer2Count == 0) || lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
			}
			
		// METAR 관측 운량이 1층과 2층 둘다 있을때
		} else if((metarCloudLayer1Count > 0 && metarCloudLayer2Count > 0) && !metarCloudClear) {
			
			// TAF 예보 운량이 1층만 있을때
			if((lfCloudLayer1Count > 0 && lfCloudLayer2Count == 0) && !lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 1층과 2층 둘다 있을때
			} else if((lfCloudLayer1Count > 0 && lfCloudLayer2Count > 0) && !lfCloudClear) {
			
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 2층만 있을때
			} else if((lfCloudLayer1Count == 0 && lfCloudLayer2Count > 0) && !lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 없을때
			} else if((lfCloudLayer1Count == 0 && lfCloudLayer2Count == 0) || lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
			}
			
		// METAR 관측 운량이 2층만 있을때
		} else if((metarCloudLayer1Count == 0 && metarCloudLayer2Count > 0) && !metarCloudClear) {
			
			// TAF 예보 운량이 1층만 있을때
			if((lfCloudLayer1Count > 0 && lfCloudLayer2Count == 0) && !lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 1층과 2층 둘다 있을때
			} else if((lfCloudLayer1Count > 0 && lfCloudLayer2Count > 0) && !lfCloudClear) {
			
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 2층만 있을때
			} else if((lfCloudLayer1Count == 0 && lfCloudLayer2Count > 0) && !lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
				
			// TAF 예보 운량이 없을때
			} else if((lfCloudLayer1Count == 0 && lfCloudLayer2Count == 0) || lfCloudClear) {
				
				cloudAmountLayer1Score = this.calculateCloudAmount1LayerScore(lfElement, metarElement);
				cloudAmountLayer2Score = this.calculateCloudAmount2LayerScore(lfElement, metarElement);
			}
		}
		
		return new Float[]{cloudAmountLayer1Score, cloudAmountLayer2Score, EvaluationUtils.getCloudAmountScore(cloudAmountLayer1Score, cloudAmountLayer2Score)};
	}
	
	private Float calculateCloudAmount2LayerScore(LfElement lfElement, MetarElement metarElement) {
		
		List<String> lfCloudAmount2LayerList = lfElement.getCloudAmountLayer2List();		
		List<String> metarCloudAmount2LayerList = metarElement.getCloudAmountLayer2List();
		
		boolean lfBknOvc = false;
		boolean metarBknOvc = false;
		
		for(int i=0 ; i<lfCloudAmount2LayerList.size() ; i++) {
			
			if("BKN".equals(lfCloudAmount2LayerList.get(i)) || "OVC".equals(lfCloudAmount2LayerList.get(i))) {
				lfBknOvc = true;
			}
		}
		
		for(int i=0 ; i<metarCloudAmount2LayerList.size() ; i++) {
			
			if("BKN".equals(metarCloudAmount2LayerList.get(i)) || "OVC".equals(metarCloudAmount2LayerList.get(i))) {
				metarBknOvc = true;
			}
		}
		
		if(lfBknOvc == true && metarBknOvc == true) {
			return 100f;
		} else if(lfBknOvc == false && metarBknOvc == false) {
			return 100f;
		} else {
			return 0f;
		}
	}
	
	private Float calculateCloudAmount1LayerScore(LfElement lfElement, MetarElement metarElement) {
		
		List<String> lfCloudAmountLayer1List = lfElement.getCloudAmountLayer1List();
		List<String> metarCloudAmountLayer1List = metarElement.getCloudAmountLayer1List();
		
		List<String> cloudAmountCategoryList = Arrays.asList(new String[]{
			"", "FEW", "SCT", "BKN", "OVC"
		});
		
		Integer maxMetarCloudAmount = 0;
		Integer maxLfCloudAmount = 0;
		
		for(int i=0 ; i<metarCloudAmountLayer1List.size() ; i++) {
			maxMetarCloudAmount = Math.max(maxMetarCloudAmount, cloudAmountCategoryList.indexOf(metarCloudAmountLayer1List.get(i)));
		}
		
		for(int i=0 ; i<lfCloudAmountLayer1List.size() ; i++) {
			maxLfCloudAmount = Math.max(maxLfCloudAmount, cloudAmountCategoryList.indexOf(lfCloudAmountLayer1List.get(i)));
		}
		
		if(Math.abs(maxMetarCloudAmount - maxLfCloudAmount) <= 1) {			
			return 100f;
		} else {			
			return 0f;
		}		
	}
	
	private Float evaluateRainOrClear(LfElement lfElement, MetarElement metarElement) {
		
		String lfSkyCondition = lfElement.getSkyCondition();		
		String metarSkyCondition = metarElement.getSkyCondition();
		
		String regex = "(.*)(DZ|RA|SN|SG|PL|GR|GS|UP)+(.*)";
		
		if(lfSkyCondition.matches(regex)) {
			
			if(metarSkyCondition.matches(regex)) {				
				return 100f;
			} else {
				return 0f;
			}
			
		} else {
			
			// 일기현상이 종료되는 경우
			if(lfElement.isNsw()) {
				
			}
			
			if(metarSkyCondition.matches(regex)) {				
				return 0f;
			} else {
				return 100f;
			}
		}
	}
	
	private Float evaluateVis(LfElement lfElement, MetarElement metarElement) {
		
		Double lfVis = lfElement.getVis();
		Double metarVis = metarElement.getVis();
		
		// CAVOK 인 경우에는 시정을 9999로 가정한다
		if(lfElement.isCavok()) {
			lfVis = 9999.0;
		}
		
		if(metarElement.isCavok()) {
			metarVis = 9999.0;
		}
		
		// 공항경보 기준치는 800m로 가정한다
		if(lfVis <= 800) {
			
			if(Math.abs(lfVis - metarVis) <= 200) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		if(lfVis > 800 && lfVis <= 1500) {
			
			if(Math.abs(lfVis - metarVis) <= 300) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		if(lfVis > 1500 && lfVis <= 3000) {
			
			if(Math.abs(lfVis - metarVis) <= 500) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		if(lfVis > 3000 && lfVis <= 5000) {
			
			if(Math.abs(lfVis - metarVis) <= 1000) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		if(lfVis >= 5000) {
			
			if(metarVis >= 5000) {
				return 100f;
			} else {
				return 0f;
			}
		}
		
		return 0f;
	}
	
	private Float evaluateWindDirection(LfElement lfElement, MetarElement metarElement) {
		
		if(metarElement.getWspd() <= 5 || metarElement.isVrb()) {
			
			return 100f;
			
		} else if(metarElement.getWspd() > 5) {
			
			if(lfElement.isVrb()) {
				
				return 0f;
				
			} else {
				
				Double gap = Math.abs(metarElement.getWdir() - lfElement.getWdir());
				
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
	
	private Float evaluateWindSpeed(LfElement lfElement, MetarElement metarElement) {
		
		Double gap = Math.abs(metarElement.getWspd() - lfElement.getWspd());
		
		if(gap <= 5) {
			return 100f;
		} else {
			return 0f;
		}
	}	
}
