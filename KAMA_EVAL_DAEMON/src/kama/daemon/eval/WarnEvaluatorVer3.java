package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.warn.WarnData;
import kama.daemon.util.EvaluationUtils;

public class WarnEvaluatorVer3 extends WarnEvaluator {
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	private final double WSPD_CRITERIA = 25d;
	private final double GUST_CRITERIA = 35d;
	
	public WarnEvaluatorVer3(EvaluationDatabaseUtil evaluationDatabaseUtil) {
		
		this.evaluationDatabaseUtil = evaluationDatabaseUtil;
	}
	
	// Ver1, Ver2 전용 점수 평가 메서드
	@Override
	public WarnEvaluationData evaluate(WarnData warnData) throws Exception {
		
		throw new UnsupportedOperationException("Ver3 supports lead time evaluation only.");
	}
	
	@Override
	public WarnEvaluationData evaluateLeadTime(WarnData warnData) throws Exception {
		
		String stnCd = warnData.getStnCd();
		
		Date anncTm = warnData.getAnncTm();
		Date stEffctTm = warnData.getStEffctTm();
		Date edEffctTm = warnData.getEdEffctTm();
		
		Date _EdEffctTm = edEffctTm;
		
		Date stCnlTm = warnData.getStCnlTm();
		Date edExtTm = warnData.getEdExtTm();
		
		// 경보 취소 정보가 있는 경우 경보 종료시점을 변경한다
		if(stCnlTm != null) {
			_EdEffctTm = stCnlTm;
		// 경보 취소 정보가 없고 연장정보가 있는 경우 경보 종료시점을 변경한다
		} else if(edExtTm != null) {
			_EdEffctTm = edExtTm;
		}
		
		WarnEvaluationData evaluationData = new WarnEvaluationData();
		evaluationData.setWarnType(warnData.getWarnType());
		evaluationData.setEvaluationTm(anncTm);
		evaluationData.setStEffctTm(stEffctTm);
		evaluationData.setEdEffctTm(_EdEffctTm);
		evaluationData.setFirstArrMin(0);
		
		switch(warnData.getWarnType()) {
		case SFC_WSPD:
			
			this.evaluateSfcWspd(stnCd, evaluationData);
			
			break;
		default:
			
			evaluationData.setAvailable(false);
			
			break;
		}
		
		return evaluationData;
	}
	
	// 강풍경보 선행시간 평가
	public void evaluateSfcWspd(String stnCd, WarnEvaluationData evaluationData) throws Exception {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		Date anncTm = evaluationData.getEvaluationTm();
		Date stEffctTm = evaluationData.getStEffctTm();
		Date edEffctTm = evaluationData.getEdEffctTm();
		
		if(anncTm == null || stEffctTm == null || edEffctTm == null ||
		   edEffctTm.before(stEffctTm) || edEffctTm.before(anncTm)) {
			
			evaluationData.setAvailable(false);
			return;
		}
		
		// 발표시각의 초를 제거하고 해당 분의 관측자료를 사용한다
		Calendar cal = new GregorianCalendar();
		cal.setTime(anncTm);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date issueObsTm = cal.getTime();
		
		// Ver2와 동일한 AMOS 자료 조회 메서드를 사용한다 (조회결과 시각은 KST)
		List<Map<String, Object>> amosDataList = this.evaluationDatabaseUtil.getAmosDataListForWarnEvaluate(
			EvaluationUtils.getAirportId(stnCd), sdf.format(issueObsTm), sdf.format(edEffctTm));
		
		if(amosDataList == null || amosDataList.size() == 0) {
			
			evaluationData.setAvailable(false);
			return;
		}
		
		List<Map<String, Object>> splitAmosDataList = EvaluationUtils.splitAmosDatabyTm(amosDataList);
		
		boolean issueObsFound = false;
		boolean validIssue = false;
		Date firstArrTm = null;
		String firstArrVal = null;
		Date lastObsTm = null;
		String lastObsVal = null;
		
		for(int i=0 ; i<splitAmosDataList.size() ; i++) {
			
			Map<String, Object> splitAmosData = splitAmosDataList.get(i);
			Date obsTm = sdf.parse(splitAmosData.get("tm").toString());
			
			cal = new GregorianCalendar();
			cal.setTime(obsTm);
			cal.add(Calendar.HOUR_OF_DAY, -9);
			obsTm = cal.getTime();
			
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> subAmosDataList =
				(List<Map<String, Object>>)splitAmosData.get("list");
			
			Double wspd10MinAvg = EvaluationUtils.findAmosMaxValue(subAmosDataList, "wspd10minAvg");
			Double wspd1MinMax = EvaluationUtils.findAmosMaxValue(subAmosDataList, "wspd1minMax");
			
			if(wspd10MinAvg == null || wspd1MinMax == null ||
			   wspd10MinAvg.isNaN() || wspd1MinMax.isNaN()) {
				continue;
			}
			
			String obsVal = "WSPD " + wspd10MinAvg + "KT MAX " + wspd1MinMax + "KT";
			
			// 발표시점의 두 풍속이 모두 경보기준의 50% 미만이어야 한다 (AND)
			if(obsTm.equals(issueObsTm)) {
				
				issueObsFound = true;
				validIssue = wspd10MinAvg < WSPD_CRITERIA * 0.5d &&
				             wspd1MinMax < GUST_CRITERIA * 0.5d;
			}
			
			if(obsTm.before(stEffctTm) || obsTm.after(edEffctTm)) {
				continue;
			}
			
			// 유효구간 내 마지막 관측자료를 보관한다
			if(lastObsTm == null || obsTm.after(lastObsTm)) {
				lastObsTm = obsTm;
				lastObsVal = obsVal;
			}
			
			// 평균풍속 또는 최대순간풍속 중 하나라도 경보기준에 도달하면 인정 (OR)
			if(wspd10MinAvg >= WSPD_CRITERIA || wspd1MinMax >= GUST_CRITERIA) {
				
				if(firstArrTm == null || obsTm.before(firstArrTm)) {
					firstArrTm = obsTm;
					firstArrVal = obsVal;
				}
			}
		}
		
		evaluationData.setLastObsTm(lastObsTm);
		evaluationData.setLastObsVal(lastObsVal);
		
		if(!issueObsFound) {
			
			evaluationData.setAvailable(false);
			return;
		}
		
		// 발표 당시 50% 조건 미충족 또는 실제 경보기준 미도달이면 0분
		if(!validIssue || firstArrTm == null) {
			return;
		}
		
		long leadMillis = firstArrTm.getTime() - anncTm.getTime();
		
		if(leadMillis < 0) {
			
			evaluationData.setAvailable(false);
			return;
		}
		
		int leadMinutes = (int)(leadMillis / 60000L);
		
		evaluationData.setFirstArrTm(firstArrTm);
		evaluationData.setFirstArrVal(firstArrVal);
		evaluationData.setFirstArrMin(leadMinutes);
		evaluationData.setPrevMin(leadMinutes);
	}
	
	public Map<String, Object> getEvaluationResult(WarnEvaluationData warnEvaluationData, int scoreFixCount) {
		
		Map<String, Object> warnEvaluationResult = new HashMap<String, Object>();
		
		if(warnEvaluationData.isAvailable()) {
			warnEvaluationResult.put("available", true);
			
		} else {
			warnEvaluationResult.put("available", false);
		}
		
		return warnEvaluationResult;
	}
}
