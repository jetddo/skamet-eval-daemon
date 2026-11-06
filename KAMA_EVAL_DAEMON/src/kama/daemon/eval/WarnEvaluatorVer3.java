
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
    
    // 기존 WarnEvaluator 추상 메서드 구현
    @Override
    public Map<String, Object> getEvaluationResult(WarnEvaluationData warnEvaluationData, int scoreFixCount) {
    	throw new UnsupportedOperationException("Ver3 supports lead time evaluation only.");
    }

    @Override
    public WarnEvaluationLeadTimeData evaluateLeadTime(WarnData warnData) throws Exception {

        String stnCd = warnData.getStnCd();

        Date anncTm = warnData.getAnncTm();
        Date stEffctTm = warnData.getStEffctTm();
        Date edEffctTm = warnData.getEdEffctTm();

        Date stCnlTm = warnData.getStCnlTm();
        Date edExtTm = warnData.getEdExtTm();

        // 경보 취소 정보가 있는 경우 종료시각 변경
        if(stCnlTm != null) {

            edEffctTm = stCnlTm;

        // 경보 취소 정보가 없고 연장정보가 있는 경우 종료시각 변경
        } else if(edExtTm != null) {

            edEffctTm = edExtTm;
        }

        WarnEvaluationLeadTimeData evaluationData = new WarnEvaluationLeadTimeData();

        evaluationData.setWarnType(warnData.getWarnType());
        evaluationData.setEvaluationTm(anncTm);
        evaluationData.setStEffctTm(stEffctTm);
        evaluationData.setEdEffctTm(edEffctTm);
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
    public void evaluateSfcWspd(String stnCd, WarnEvaluationLeadTimeData evaluationData) throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

        Date anncTm = evaluationData.getEvaluationTm();
        Date stEffctTm = evaluationData.getStEffctTm();
        Date edEffctTm = evaluationData.getEdEffctTm();

        if(anncTm == null || stEffctTm == null || edEffctTm == null || edEffctTm.before(stEffctTm) || edEffctTm.before(anncTm)) {

            evaluationData.setAvailable(false);
            return;
        }

        // 발표시각의 초와 밀리초 제거
        Calendar cal = new GregorianCalendar();

        cal.setTime(anncTm);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date issueObsTm = cal.getTime();

        // AMOS 자료 조회
        List<Map<String, Object>> amosDataList = this.evaluationDatabaseUtil.getAmosDataListForWarnEvaluate(
            EvaluationUtils.getAirportId(stnCd),
            sdf.format(issueObsTm),
            sdf.format(edEffctTm)
        );

        if(amosDataList == null || amosDataList.isEmpty()) {

            evaluationData.setAvailable(false);
            return;
        }

        List<Map<String, Object>> splitAmosDataList = EvaluationUtils.splitAmosDatabyTm(amosDataList);

        boolean issueObsFound = false;
        boolean validIssue = false;

        // 경보기준 100% 최초 도달
        Date firstArrTm100 = null;
        String firstArrVal100 = null;

        // 경보기준 90% 최초 도달
        Date firstArrTm90 = null;
        String firstArrVal90 = null;

        for(int i=0 ; i<splitAmosDataList.size() ; i++) {

            Map<String, Object> splitAmosData = splitAmosDataList.get(i);

            Date obsTm = sdf.parse(splitAmosData.get("tm").toString());

            // AMOS KST -> UTC
            cal = new GregorianCalendar();
            cal.setTime(obsTm);
            cal.add(Calendar.HOUR_OF_DAY, -9);

            obsTm = cal.getTime();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subAmosDataList = (List<Map<String, Object>>)splitAmosData.get("list");

            Double wspd10MinAvg = EvaluationUtils.findAmosMaxValue(subAmosDataList, "wspd10minAvg");
            Double wspd1MinMax = EvaluationUtils.findAmosMaxValue(subAmosDataList, "wspd1minMax");

            if(wspd10MinAvg == null || wspd1MinMax == null ||
               wspd10MinAvg.isNaN() || wspd1MinMax.isNaN()) {

                continue;
            }

            // 경보기준 대비 도달률
            double wspdRatio = wspd10MinAvg / WSPD_CRITERIA;
            double gustRatio = wspd1MinMax / GUST_CRITERIA;

            // 동일 시각에서 도달률이 높은 요소 선택
            String obsVal;

            if(wspdRatio >= gustRatio) {

                obsVal = wspd10MinAvg.toString();

            } else {

                obsVal = "G" + wspd1MinMax.toString();
            }

            // 경보 발표시점 관측자료
            if(obsTm.equals(issueObsTm)) {

                issueObsFound = true;

                // 발표시점 평균풍속과 GUST 모두 저장
                evaluationData.setIssueObsTm(obsTm);
                evaluationData.setIssueObsVal(wspd10MinAvg.toString() + ",G" + wspd1MinMax.toString());

                // 두 요소 모두 경보기준의 50% 미만
                validIssue = wspd10MinAvg < WSPD_CRITERIA * 0.5d && wspd1MinMax < GUST_CRITERIA * 0.5d;
            }

            // 경보 유효구간 외 관측자료 제외
            if(obsTm.before(stEffctTm) || obsTm.after(edEffctTm)) {

                continue;
            }

            // 경보기준 100% 도달 (OR)
            boolean reached100 = wspd10MinAvg >= WSPD_CRITERIA || wspd1MinMax >= GUST_CRITERIA;

            // 경보기준 90% 도달 (OR)
            boolean reached90 = wspd10MinAvg >= WSPD_CRITERIA * 0.9d || wspd1MinMax >= GUST_CRITERIA * 0.9d;

            // 100% 최초 도달시각
            if(reached100) {

                if(firstArrTm100 == null || obsTm.before(firstArrTm100)) {

                    firstArrTm100 = obsTm;
                    firstArrVal100 = obsVal;
                }
            }

            // 90% 최초 도달시각
            if(reached90) {

                if(firstArrTm90 == null || obsTm.before(firstArrTm90)) {

                    firstArrTm90 = obsTm;
                    firstArrVal90 = obsVal;
                }
            }
        }
        
        // 경보기준 100% 최초 도달 로그
        if(firstArrTm100 != null) {

            System.out.println("[WARN VER3] 유효기간 내 경보기준 도달 발견" + " - STN=" + stnCd + ", 발표=" + sdf.format(anncTm) + ", 도달=" + sdf.format(firstArrTm100) + ", 값=" + firstArrVal100);
        }

        // 경보기준 90% 최초 도달 로그
        if(firstArrTm90 != null) {

            System.out.println("[WARN VER3] 유효기간 내 경보기준 90% 도달 발견"+ " - STN=" + stnCd + ", 발표=" + sdf.format(anncTm) + ", 도달=" + sdf.format(firstArrTm90) + ", 값=" + firstArrVal90);
        }

        // 경보 발표시점 50% 미만 관측값 로그
        if(issueObsFound && validIssue) {

            System.out.println("[WARN VER3] 경보발표시점 50% 미만 관측값 발견" + " - STN=" + stnCd + ", 발표=" + sdf.format(anncTm) + ", 관측=" + sdf.format(evaluationData.getIssueObsTm()) + ", 값=" + evaluationData.getIssueObsVal());
        }

        Date firstArrTm = null;
        String firstArrVal = null;

        // 1. 정상 경보: 경보기준 100% 도달
        if(firstArrTm100 != null) {

            firstArrTm = firstArrTm100;
            firstArrVal = firstArrVal100;

        // 2. 선제적 경보: 90% 도달 + 발표시점 50% 미만
        } else if(firstArrTm90 != null && issueObsFound && validIssue) {

            firstArrTm = firstArrTm90;
            firstArrVal = firstArrVal90;

        // 3. 무효 경보
        } else {

            // 선제적 경보 판정에 필요한 발표시점 관측자료 없음
            if(firstArrTm90 != null && !issueObsFound) {

                evaluationData.setAvailable(false);
            }

            return;
        }

        // 선행시간 = 최초 도달시각 - 경보 발표시각
        long leadMillis = firstArrTm.getTime() - anncTm.getTime();

        if(leadMillis < 0) {

            evaluationData.setAvailable(false);
            return;
        }

        int leadMinutes = (int)(leadMillis / 60000L);

        evaluationData.setFirstArrTm(firstArrTm);
        evaluationData.setFirstArrVal(firstArrVal);
        evaluationData.setFirstArrMin(leadMinutes);
    }

    // Ver3 평가 결과
    public Map<String, Object> getEvaluationResult(WarnEvaluationLeadTimeData evaluationData) {

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("available", evaluationData.isAvailable());

        result.put("warnType", evaluationData.getWarnType());
        result.put("evaluationTm", evaluationData.getEvaluationTm());
        result.put("stEffctTm", evaluationData.getStEffctTm());
        result.put("edEffctTm", evaluationData.getEdEffctTm());

        result.put("issueObsTm", evaluationData.getIssueObsTm());
        result.put("issueObsVal", evaluationData.getIssueObsVal());

        result.put("firstArrTm", evaluationData.getFirstArrTm());
        result.put("firstArrVal", evaluationData.getFirstArrVal());
        result.put("firstArrMin", evaluationData.getFirstArrMin());

        return result;
    }

    // 기존 WarnEvaluator 추상 메서드 구현
    @Override
    public Map<String, Object> getEvaluationLeadTimeResult(WarnEvaluationLeadTimeData warnEvaluationData) {

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("available", warnEvaluationData.isAvailable());

        return result;
    }
}
