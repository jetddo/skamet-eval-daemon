
package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.metar.MetarParser;
import kama.daemon.eval.warn.WarnData;
import kama.daemon.util.EvaluationUtils;

// MetarParser는 Ver2에서 사용 중인 import를 추가

public class WarnEvaluatorVer3 extends WarnEvaluator {

    private EvaluationDatabaseUtil evaluationDatabaseUtil;
    private MetarParser metarParser;

    private final int ISSUE_OBS_RANGE_MIN = 60;

    public WarnEvaluatorVer3(EvaluationDatabaseUtil evaluationDatabaseUtil) {

        this.evaluationDatabaseUtil = evaluationDatabaseUtil;
        this.metarParser = new MetarParser();
    }

    // Ver1, Ver2 전용 점수 평가 메서드
    @Override
    public WarnEvaluationData evaluate(WarnData warnData) throws Exception {

        throw new UnsupportedOperationException("Ver3 supports lead time evaluation only.");
    }

    @Override
    public Map<String, Object> getEvaluationResult(WarnEvaluationData warnEvaluationData, int scoreFixCount) {

        throw new UnsupportedOperationException("Ver3 does not calculate scores.");
    }

    @Override
    public WarnEvaluationLeadTimeData evaluateLeadTime(WarnData warnData) throws Exception {
    	
        String stnCd = warnData.getStnCd();

        Date anncTm = warnData.getAnncTm();
        Date stEffctTm = warnData.getStEffctTm();
        Date edEffctTm = warnData.getEdEffctTm();

        Date stCnlTm = warnData.getStCnlTm();
        Date edExtTm = warnData.getEdExtTm();

        // 취소시각 우선, 취소가 없으면 연장시각 적용
        if(stCnlTm != null) {

            edEffctTm = stCnlTm;

        } else if(edExtTm != null) {

            edEffctTm = edExtTm;
        }

        WarnEvaluationLeadTimeData evaluationData = new WarnEvaluationLeadTimeData();

        evaluationData.setWarnType(warnData.getWarnType());
        evaluationData.setEvaluationTm(anncTm);
        evaluationData.setStEffctTm(stEffctTm);
        evaluationData.setEdEffctTm(edEffctTm);

        // 기본값은 무효경보
        evaluationData.setAvailable(false);
        evaluationData.setFirstArrMin(0);

        switch(warnData.getWarnType()) {

        case SFC_WSPD:

            this.evaluateSfcWspd(stnCd, evaluationData);
            break;

        case SFC_VIS:

            this.evaluateSfcVis(stnCd, evaluationData);
            break;
            
        case HVY_RA:
            this.evaluateHvyRa(stnCd, evaluationData);
            break;    
            
        case HVY_SN:
            this.evaluateHvySn(stnCd, evaluationData);
            break;    

        default:

            break;
        }
        
        this.printEvaluationLog(stnCd, evaluationData);

        return evaluationData;
    }

    // 강풍경보 선행시간 평가
    public void evaluateSfcWspd(String stnCd, WarnEvaluationLeadTimeData evaluationData) throws Exception {

        double WSPD_CRITERIA = 25d;
        double GUST_CRITERIA = 35d;
    	
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

        Date anncTm = evaluationData.getEvaluationTm();
        Date stEffctTm = evaluationData.getStEffctTm();
        Date edEffctTm = evaluationData.getEdEffctTm();

        if(!this.isValidPeriod(anncTm, stEffctTm, edEffctTm)) {
            return;
        }

        // 발표시각의 초와 밀리초 제거
        Calendar cal = new GregorianCalendar();

        cal.setTime(anncTm);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date issueObsTm = cal.getTime();
        
        cal.add(Calendar.MINUTE, -5);
        Date searchStTm = cal.getTime();

        // Ver2 AMOS 조회 메서드 재사용 (조회자료 시각 KST)
        List<Map<String, Object>> amosDataList = this.evaluationDatabaseUtil.getAmosDataListForWarnEvaluate(
                EvaluationUtils.getAirportId(stnCd),
                sdf.format(searchStTm),
                sdf.format(edEffctTm));

        if(amosDataList == null || amosDataList.isEmpty()) {
            return;
        }

        List<Map<String, Object>> splitAmosDataList = EvaluationUtils.splitAmosDatabyTm(amosDataList);

        if(splitAmosDataList == null || splitAmosDataList.isEmpty()) {
            return;
        }

        boolean issueObsFound = false;
        boolean validIssue = false;

        Date firstArrTm100 = null;
        String firstArrVal100 = null;

        Date firstArrTm90 = null;
        String firstArrVal90 = null;
        
        long minIssueTimeDiff = Long.MAX_VALUE;

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

            Double wspd10MinAvg =
                EvaluationUtils.findAmosMaxValue(
                    subAmosDataList, "wspd10minAvg");

            Double wspd1MinMax = EvaluationUtils.findAmosMaxValue(subAmosDataList, "wspd1minMax");

            if(!this.isValidValue(wspd10MinAvg) ||
               !this.isValidValue(wspd1MinMax)) {
                continue;
            }

            String obsVal = wspd10MinAvg + ",G" + wspd1MinMax;

            // 발표시점 관측자료
            long timeDiff = Math.abs(obsTm.getTime() - issueObsTm.getTime());

            if(timeDiff <= 5 * 60000L &&
               (timeDiff < minIssueTimeDiff ||
               (timeDiff == minIssueTimeDiff &&
               (evaluationData.getIssueObsTm() == null ||
                obsTm.before(evaluationData.getIssueObsTm()))))) {

                minIssueTimeDiff = timeDiff;
                issueObsFound = true;

                evaluationData.setIssueObsTm(obsTm);
                evaluationData.setIssueObsVal(obsVal);

                validIssue =
                    wspd10MinAvg < WSPD_CRITERIA * 0.5d &&
                    wspd1MinMax < GUST_CRITERIA * 0.5d;
            }

            // 유효기간 외 자료 제외
            if(obsTm.before(stEffctTm) || obsTm.after(edEffctTm)) {
                continue;
            }

            // 100% 기준 도달 (OR)
            boolean reached100 = wspd10MinAvg >= WSPD_CRITERIA || wspd1MinMax >= GUST_CRITERIA;

            // 90% 기준 도달 (OR)
            boolean reached90 = wspd10MinAvg >= WSPD_CRITERIA * 0.9d || wspd1MinMax >= GUST_CRITERIA * 0.9d;

            if(reached100) {

                if(firstArrTm100 == null || obsTm.before(firstArrTm100)) {

                    firstArrTm100 = obsTm;
                    firstArrVal100 = obsVal;
                }
            }

            if(reached90) {

                if(firstArrTm90 == null || obsTm.before(firstArrTm90)) {

                    firstArrTm90 = obsTm;
                    firstArrVal90 = obsVal;
                }
            }
        }

        Date firstArrTm = null;
        String firstArrVal = null;

        // 100% 도달 우선
        if(firstArrTm100 != null) {

            firstArrTm = firstArrTm100;
            firstArrVal = firstArrVal100;

        // 90% 도달은 발표시점 50% 조건 필요
        } else if(firstArrTm90 != null && issueObsFound && validIssue) {

            firstArrTm = firstArrTm90;
            firstArrVal = firstArrVal90;
        }

        // 근거자료가 없으면 무효경보
        if(firstArrTm == null) {
            return;
        }

        this.setLeadTimeResult(evaluationData, anncTm, firstArrTm, firstArrVal);
    }

    // 저시정경보 선행시간 평가
    public void evaluateSfcVis(String stnCd, WarnEvaluationLeadTimeData evaluationData) throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

        Date anncTm = evaluationData.getEvaluationTm();
        Date stEffctTm = evaluationData.getStEffctTm();
        Date edEffctTm = evaluationData.getEdEffctTm();

        if(!this.isValidPeriod(anncTm, stEffctTm, edEffctTm)) {
            return;
        }

        // 공항별 공식 저시정경보 평가 기준
        double visCriteria100;
        double visCriteria90;
        double visCriteria50;

        switch(stnCd) {

        case "RKSI":

            visCriteria100 = 400d;
            visCriteria90 = 450d;
            visCriteria50 = 600d;
            break;

        case "RKSS":

            visCriteria100 = 600d;
            visCriteria90 = 650d;
            visCriteria50 = 900d;
            break;

        case "RKPC":
        case "RKJB":
        case "RKJY":
        case "RKNY":

            visCriteria100 = 800d;
            visCriteria90 = 900d;
            visCriteria50 = 1200d;
            break;

        case "RKPU":

            visCriteria100 = 1600d;
            visCriteria90 = 1800d;
            visCriteria50 = 2400d;
            break;

        default:

            return;
        }

        // 발표시각 전후 10분 탐색
        Calendar cal = new GregorianCalendar();

        cal.setTime(anncTm);
        cal.add(Calendar.MINUTE, -ISSUE_OBS_RANGE_MIN);

        Date searchStTm = cal.getTime();

        // 조회 종료시각은 유효종료시각과 발표+10분 중 늦은 시각
        cal.setTime(anncTm);
        cal.add(Calendar.MINUTE, ISSUE_OBS_RANGE_MIN);

        Date searchEdTm = cal.getTime();

        if(edEffctTm.after(searchEdTm)) {
            searchEdTm = edEffctTm;
        }

        // Ver2 METAR / LOCAL 관측자료 조회
        List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(
                stnCd,
                sdf.format(searchStTm),
                sdf.format(searchEdTm));

        List<Map<String, Object>> localInfoList = this.evaluationDatabaseUtil.getAmisLocalInfoList(
                stnCd,
                sdf.format(searchStTm),
                sdf.format(searchEdTm));

        List<Map<String, Object>> obsInfoList = EvaluationUtils.combineMetarLocalList(this.metarParser, stnCd, metarInfoList, localInfoList);

        if(obsInfoList == null || obsInfoList.isEmpty()) {
            return;
        }

        boolean issueObsFound = false;
        boolean validIssue = false;

        long minIssueTimeDiff = Long.MAX_VALUE;

        Date firstArrTm100 = null;
        String firstArrVal100 = null;

        Date firstArrTm90 = null;
        String firstArrVal90 = null;

        for(int i=0 ; i<obsInfoList.size() ; i++) {

            Map<String, Object> obsInfo = obsInfoList.get(i);

            if(obsInfo.get("tm") == null) {
                continue;
            }

            Date obsTm = sdf.parse(obsInfo.get("tm").toString());

            Object visObj = obsInfo.get("vis");

            if(!(visObj instanceof Number)) {
                continue;
            }

            Double obsVis = ((Number)visObj).doubleValue();

            if(!this.isValidValue(obsVis)) {
                continue;
            }

            String obsVal = obsVis.toString();

            // 발표시각 전후 가장 가까운 관측자료 선택
            long timeDiff = Math.abs(obsTm.getTime() - anncTm.getTime());

            if(timeDiff <= ISSUE_OBS_RANGE_MIN * 60000L) {

                if(timeDiff < minIssueTimeDiff || (timeDiff == minIssueTimeDiff && 
            		(evaluationData.getIssueObsTm() == null || obsTm.before(evaluationData.getIssueObsTm())))) {

                    minIssueTimeDiff = timeDiff;
                    issueObsFound = true;

                    evaluationData.setIssueObsTm(obsTm);
                    evaluationData.setIssueObsVal(obsVal);

                    // 시정은 낮을수록 위험하므로 50% 기준 초과
                    validIssue = obsVis > visCriteria50;
                }
            }

            // 경보기준 도달은 유효기간 내에서만 판정
            if(obsTm.before(stEffctTm) || obsTm.after(edEffctTm)) {
                continue;
            }

            // 저시정은 값이 작을수록 위험
            boolean reached100 = obsVis <= visCriteria100;
            boolean reached90 = obsVis <= visCriteria90;

            if(reached100) {

                if(firstArrTm100 == null || obsTm.before(firstArrTm100)) {

                    firstArrTm100 = obsTm;
                    firstArrVal100 = obsVal;
                }
            }

            if(reached90) {

                if(firstArrTm90 == null || obsTm.before(firstArrTm90)) {

                    firstArrTm90 = obsTm;
                    firstArrVal90 = obsVal;
                }
            }
        }

        Date firstArrTm = null;
        String firstArrVal = null;

        // 100% 기준 도달 우선
        if(firstArrTm100 != null) {

            firstArrTm = firstArrTm100;
            firstArrVal = firstArrVal100;

        // 90% 기준은 발표시점 50% 조건 충족 시 인정
        } else if(firstArrTm90 != null && issueObsFound && validIssue) {

            firstArrTm = firstArrTm90;
            firstArrVal = firstArrVal90;
        }

        // 근거자료가 없으면 무효경보
        if(firstArrTm == null) {
            return;
        }

        this.setLeadTimeResult(evaluationData, anncTm, firstArrTm, firstArrVal);
    }
    
    @SuppressWarnings("unchecked")
    public void evaluateHvyRa(String stnCd, WarnEvaluationLeadTimeData evaluationData) throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

        Date anncTm = evaluationData.getEvaluationTm();
        Date stEffctTm = evaluationData.getStEffctTm();
        Date edEffctTm = evaluationData.getEdEffctTm();

        if(!this.isValidPeriod(anncTm, stEffctTm, edEffctTm)) {
            return;
        }

        final double RN3HR_CRITERIA = 60d;
        final double RN12HR_CRITERIA = 110d;
        final double MIN_RAIN_INCREASE = 30d;
        final int ISSUE_OBS_RANGE_MIN = 10;

        Calendar cal = new GregorianCalendar();

        // 발표시점 관측자료 탐색을 위해 10분 이전부터 조회
        cal.setTime(anncTm);
        cal.add(Calendar.MINUTE, -ISSUE_OBS_RANGE_MIN);
        Date searchStTm = cal.getTime();

        // 유효율 계산을 위한 발표 후 6시간
        cal.setTime(anncTm);
        cal.add(Calendar.HOUR_OF_DAY, 6);
        Date ratioEdTm = cal.getTime();

        // 경보기준 도달은 유효기간 전체를 확인
        Date searchEdTm = edEffctTm.after(ratioEdTm) ? edEffctTm : ratioEdTm;

        // Ver2 AMOS 관측자료 조회
        List<Map<String, Object>> amosDataList =this.evaluationDatabaseUtil.getAmosDataListForWarnEvaluate(
	        EvaluationUtils.getAirportId(stnCd),
	        sdf.format(searchStTm),
	        sdf.format(searchEdTm)
	    );

        if(amosDataList == null || amosDataList.isEmpty()) {
            return;
        }

        List<Map<String, Object>> splitAmosDataList = EvaluationUtils.splitAmosDatabyTm(amosDataList);

        if(splitAmosDataList == null || splitAmosDataList.isEmpty()) {
            return;
        }

        // 발표시점 3시간 누적강수량
        Double issueRn3Hr = null;
        long minIssueTimeDiff = Long.MAX_VALUE;

        // 경보기준 최초 도달
        Date firstArrTm = null;
        String firstArrVal = null;

        // 발표 후 6시간 이내 최대 3시간 누적강수량
        Double maxRn3Hr = null;
        Date maxRn3HrTm = null;

        for(int i=0 ; i<splitAmosDataList.size() ; i++) {

            Map<String, Object> splitAmosData = splitAmosDataList.get(i);

            if(splitAmosData.get("tm") == null) {
                continue;
            }

            Date obsTm = sdf.parse(splitAmosData.get("tm").toString());

            // AMOS 관측시각 KST -> UTC
            cal.setTime(obsTm);
            cal.add(Calendar.HOUR_OF_DAY, -9);
            obsTm = cal.getTime();

            List<Map<String, Object>> subAmosDataList = (List<Map<String, Object>>)splitAmosData.get("list");

            if(subAmosDataList == null || subAmosDataList.isEmpty()) {
                continue;
            }

            Double rn3Hr = EvaluationUtils.findAmosMaxValue(subAmosDataList, "rn3hr");
            Double rn12Hr = EvaluationUtils.findAmosMaxValue(subAmosDataList, "rn12hr");

            boolean validRn3Hr = this.isValidValue(rn3Hr) && rn3Hr >= 0d;
            boolean validRn12Hr = this.isValidValue(rn12Hr) && rn12Hr >= 0d;

            if(!validRn3Hr && !validRn12Hr) {
                continue;
            }

            // 1. 발표시점에 가장 가까운 3시간 누적강수량
            if(validRn3Hr) {

                long timeDiff = Math.abs(obsTm.getTime() - anncTm.getTime());

                if(timeDiff <= ISSUE_OBS_RANGE_MIN * 60000L) {

                    if(timeDiff < minIssueTimeDiff || (timeDiff == minIssueTimeDiff && 
                    		(evaluationData.getIssueObsTm() == null || obsTm.before(evaluationData.getIssueObsTm())))) {

                        minIssueTimeDiff = timeDiff;
                        issueRn3Hr = rn3Hr;

                        evaluationData.setIssueObsTm(obsTm);
                        evaluationData.setIssueObsVal("RN3HR:" +rn3Hr.toString());
                    }
                }
            }

            // 2. 경보 유효구간 내 실제 경보기준 도달 여부
            if(!obsTm.before(stEffctTm) && !obsTm.after(edEffctTm) &&
               !obsTm.before(anncTm)) {

                boolean reached3Hr = validRn3Hr && rn3Hr >= RN3HR_CRITERIA;
                boolean reached12Hr = validRn12Hr && rn12Hr >= RN12HR_CRITERIA;

                if(reached3Hr || reached12Hr) {

                    if(firstArrTm == null || obsTm.before(firstArrTm)) {

                        firstArrTm = obsTm;

                        if(reached3Hr && reached12Hr) {

                            double ratio3Hr = rn3Hr / RN3HR_CRITERIA;
                            double ratio12Hr = rn12Hr / RN12HR_CRITERIA;

                            firstArrVal = ratio3Hr >= ratio12Hr ? "RN3HR:" + rn3Hr : ", RN12HR:" + rn12Hr;

                        } else if(reached3Hr) {

                            firstArrVal = "RN3HR:" + rn3Hr;

                        } else {

                            firstArrVal = "RN12HR:" + rn12Hr;
                        }
                    }
                }
            }

            // 3. 발표 후 6시간 이내 최대 3시간 누적강수량
            if(validRn3Hr && !obsTm.before(anncTm) && !obsTm.after(ratioEdTm) && !obsTm.after(edEffctTm)) {

                if(maxRn3Hr == null || rn3Hr > maxRn3Hr || (rn3Hr.equals(maxRn3Hr) && obsTm.before(maxRn3HrTm))) {

                    maxRn3Hr = rn3Hr;
                    maxRn3HrTm = obsTm;
                }
            }
        }

        // 4. 실제 경보기준 도달 시 최초 도달시각으로 선행시간 계산
        if(firstArrTm != null) {

            this.setLeadTimeResult(
                evaluationData,
                anncTm,
                firstArrTm,
                firstArrVal
            );

            return;
        }

        // 5. 경보기준 미도달 시 호우 유효율 적용
        if(issueRn3Hr == null || maxRn3Hr == null || maxRn3HrTm == null) {
            return;
        }

        double rainIncrease = maxRn3Hr - issueRn3Hr;

        // 발표시점 대비 30mm 이상 증가해야 유효율 적용
        if(rainIncrease < MIN_RAIN_INCREASE) {
            return;
        }

        long elapsedMillis = maxRn3HrTm.getTime() - anncTm.getTime();

        if(elapsedMillis <= 0L) {
            return;
        }

        // 유효율 선행시간 = (최대강수량 - 발표시점강수량) / 60 * 경과시간
        double ratio = rainIncrease / RN3HR_CRITERIA;

        int leadMinutes = (int)Math.round(
            ratio * elapsedMillis / 60000d
        );

        evaluationData.setFirstArrTm(maxRn3HrTm);
        evaluationData.setFirstArrVal("RN3HR:" + maxRn3Hr);
        evaluationData.setFirstArrMin(leadMinutes);
        evaluationData.setAvailable(true);
    }
    
    // 대설경보 선행시간 평가
    public void evaluateHvySn(String stnCd, WarnEvaluationLeadTimeData evaluationData) throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

        Date anncTm = evaluationData.getEvaluationTm();
        Date stEffctTm = evaluationData.getStEffctTm();
        Date edEffctTm = evaluationData.getEdEffctTm();

        if(!this.isValidPeriod(anncTm, stEffctTm, edEffctTm)) {
            return;
        }

        final double FRSC_CRITERIA_50 = 2.5d;
        final double FRSC_CRITERIA_90 = 4.5d;
        final double FRSC_CRITERIA_100 = 5.0d;

        final int ISSUE_OBS_RANGE_MIN = 60;

        Calendar cal = new GregorianCalendar();

        cal.setTime(anncTm);
        cal.add(Calendar.MINUTE, -ISSUE_OBS_RANGE_MIN);
        Date searchStTm = cal.getTime();

        cal.setTime(anncTm);
        cal.add(Calendar.MINUTE, ISSUE_OBS_RANGE_MIN);
        Date issueSearchEdTm = cal.getTime();

        Date searchEdTm = edEffctTm.after(issueSearchEdTm)
            ? edEffctTm : issueSearchEdTm;

        // METAR 신적설 관측자료 조회
        List<Map<String, Object>> metarObsDataList =
            this.evaluationDatabaseUtil.getAmisMetarObsDataList(
                stnCd,
                sdf.format(searchStTm),
                sdf.format(searchEdTm)
            );

        if(metarObsDataList == null || metarObsDataList.isEmpty()) {
            return;
        }

        boolean issueObsFound = false;
        boolean validIssue = false;

        long minIssueTimeDiff = Long.MAX_VALUE;

        Date firstArrTm100 = null;
        String firstArrVal100 = null;

        Date firstArrTm90 = null;
        String firstArrVal90 = null;

        for(int i=0 ; i<metarObsDataList.size() ; i++) {

            Map<String, Object> metarObsData = metarObsDataList.get(i);

            if(metarObsData.get("tm") == null ||
               metarObsData.get("frsc") == null) {
                continue;
            }

            Date obsTm = sdf.parse(metarObsData.get("tm").toString());

            Object frscObj = metarObsData.get("frsc");

            if(!(frscObj instanceof Number)) {
                continue;
            }

            Double frsc = ((Number)frscObj).doubleValue();

            if(!this.isValidValue(frsc) || frsc < 0d) {
                continue;
            }

            // 발표시점에 가장 가까운 관측자료
            long timeDiff = Math.abs(obsTm.getTime() - anncTm.getTime());

            if(timeDiff <= ISSUE_OBS_RANGE_MIN * 60000L) {

                if(timeDiff < minIssueTimeDiff ||
                   (timeDiff == minIssueTimeDiff &&
                    (evaluationData.getIssueObsTm() == null ||
                     obsTm.before(evaluationData.getIssueObsTm())))) {

                    minIssueTimeDiff = timeDiff;
                    issueObsFound = true;

                    evaluationData.setIssueObsTm(obsTm);
                    evaluationData.setIssueObsVal(frsc.toString());

                    validIssue = frsc < FRSC_CRITERIA_50;
                }
            }

            // 발표 이전 또는 경보 유효기간 밖 관측자료 제외
            if(obsTm.before(anncTm) ||
               obsTm.before(stEffctTm) ||
               obsTm.after(edEffctTm)) {
                continue;
            }

            // 100% 기준 최초 도달
            if(frsc >= FRSC_CRITERIA_100) {

                if(firstArrTm100 == null || obsTm.before(firstArrTm100)) {

                    firstArrTm100 = obsTm;
                    firstArrVal100 = frsc.toString();
                }
            }

            // 90% 기준 최초 도달
            if(frsc >= FRSC_CRITERIA_90) {

                if(firstArrTm90 == null || obsTm.before(firstArrTm90)) {

                    firstArrTm90 = obsTm;
                    firstArrVal90 = frsc.toString();
                }
            }
        }

        Date firstArrTm = null;
        String firstArrVal = null;

        // 실제 경보기준 도달 우선
        if(firstArrTm100 != null) {

            firstArrTm = firstArrTm100;
            firstArrVal = firstArrVal100;

        } else if(firstArrTm90 != null && issueObsFound && validIssue) {

            // 발표시점 50% 미만 + 90% 기준 도달
            firstArrTm = firstArrTm90;
            firstArrVal = firstArrVal90;
        }

        if(firstArrTm == null) {
            return;
        }

        this.setLeadTimeResult(
            evaluationData,
            anncTm,
            firstArrTm,
            firstArrVal
        );
    }

    // 유효기간 검증
    private boolean isValidPeriod(Date anncTm, Date stEffctTm, Date edEffctTm) {

        if(anncTm == null || stEffctTm == null || edEffctTm == null) {
            return false;
        }

        return !edEffctTm.before(stEffctTm) && !edEffctTm.before(anncTm);
    }

    // 관측값 유효성 검증
    private boolean isValidValue(Double value) {

        return value != null && !value.isNaN() && !value.isInfinite();
    }

    // 선행시간 및 유효경보 결과 설정
    private void setLeadTimeResult(WarnEvaluationLeadTimeData evaluationData, Date anncTm, Date firstArrTm, String firstArrVal) {

        long leadMillis = firstArrTm.getTime() - anncTm.getTime();

        if(leadMillis < 0) {
            return;
        }

        int leadMinutes = (int)(leadMillis / 60000L);

        evaluationData.setFirstArrTm(firstArrTm);
        evaluationData.setFirstArrVal(firstArrVal);
        evaluationData.setFirstArrMin(leadMinutes);

        // 근거자료가 존재하는 경우에만 유효
        evaluationData.setAvailable(true);
    }
    
    // 경보 선행시간 평가 결과 로그
	private void printEvaluationLog(String stnCd, WarnEvaluationLeadTimeData data) {
	
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	    StringBuilder sb = new StringBuilder();
	
	    sb.append("[WARN EVAL Ver3] ");
	    sb.append("STN=").append(stnCd);
	    sb.append(", TYPE=").append(data.getWarnType());
	    sb.append(", TM=").append(formatLogDate(sdf, data.getEvaluationTm()));
	    sb.append(", AVAILABLE=").append(data.isAvailable() ? "Y" : "N");
	
	    sb.append(", ISSUE_OBS_TM=").append(formatLogDate(sdf, data.getIssueObsTm()));
	    sb.append(", ISSUE_OBS_VAL=").append(data.getIssueObsVal());
	    sb.append(", ST_EFFCT_TM=").append(formatLogDate(sdf, data.getStEffctTm()));
	    sb.append(", ED_EFFCT_TM=").append(formatLogDate(sdf, data.getEdEffctTm()));
	
	    sb.append(", FIRST_ARR_TM=").append(formatLogDate(sdf, data.getFirstArrTm()));
	    sb.append(", FIRST_ARR_VAL=").append(data.getFirstArrVal());
	    sb.append(", FIRST_ARR_MIN=").append(data.getFirstArrMin());
	
	    System.out.println(sb.toString());
	}
	
	// 로그 날짜 변환
	private String formatLogDate(SimpleDateFormat sdf, Date date) {
	
	    return date != null ? sdf.format(date) : "-";
	}
}
