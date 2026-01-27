package kama.daemon.proc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.db.DataBaseManager;
import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.WarnEvaluation;
import kama.daemon.eval.WarnEvaluationData;
import kama.daemon.eval.warn.WarnData;
import kama.daemon.eval.warn.WarnParser;
import kama.daemon.util.DatabaseUtil;
import kama.daemon.util.EvaluationUtils;

import org.apache.commons.configuration2.Configuration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class EvaluateAmisWarn_Process extends DaemonProcess {
	
	private SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private Configuration config;
	
	private WarnEvaluation warnEvaluation;
	
	private WarnParser warnParser = new WarnParser();
	
	private DataBaseManager aamiDBManager;
	private DataBaseManager amisDBManager;
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	private boolean initialize() {
		
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setPassword("pwkey");
		
		try {
				
			this.aamiDBManager = new DataBaseManager(
						this.config.getString("db.oracle.aami.username"), 
						encryptor.decrypt(this.config.getString("db.oracle.aami.password")), 
						this.config.getString("db.oracle.aami.url"));
			
			this.amisDBManager = new DataBaseManager(
						this.config.getString("db.oracle.amis.username"), 
						encryptor.decrypt(this.config.getString("db.oracle.amis.password")), 
						this.config.getString("db.oracle.amis.url"));
			
			// true 면 TEST_ 테이블에 입력된다
			this.evaluationDatabaseUtil = new EvaluationDatabaseUtil(this.amisDBManager, this.aamiDBManager, false);
			
			this.warnEvaluation = new WarnEvaluation(evaluationDatabaseUtil);
			
			this.aamiDBManager.setAutoCommit(false);
				
		} catch (Exception e ) {
			
			System.out.println("Error : EvaluateAmisWarn.initialize -> " + e);
			
			this.aamiDBManager.safeClose();			
			this.amisDBManager.safeClose();
					
			return false;
		}
		
		return true;
	}
	
	private void destroy() {
		
		this.aamiDBManager.safeClose();
		this.amisDBManager.safeClose();	
	}

	@Override
	public void process(Configuration config) {
		
		this.config = config;
		
		System.out.println(":: Start Initialize");
		
		if(!this.initialize()) {
			
			System.out.println("Error : EvaluateAmisWarn.process -> initialize failed");
			return;
		}
		
		System.out.println(":: Initialize Complete");
		
		try {
			
			evaluateAmisWarn();
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		} finally {
			
			this.destroy();
		}		
	}
	
	public void evaluateAmisWarn() {
		
		List<String> stnCdList = EvaluationUtils.getAirportStnCdListForEvalWarn();
		
		for(int i=0 ; i<stnCdList.size() ; i++) {

			System.out.println("==============================================================================");	
			
			System.out.println(":: Start Airport [ " + stnCdList.get(i) + " ]");
			
			String stnCd = stnCdList.get(i);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			
			Calendar cal = new GregorianCalendar();
			
			// 시작일은 3일을 뺀다, UTC 이므로 9를 더뺌
			cal.add(Calendar.HOUR_OF_DAY, -72-9);
			
			String startTmStr = "202411250000";//sdf.format(cal.getTime());
			cal.add(Calendar.HOUR_OF_DAY, 24);
			// 종료일은 시작일에서 1일을 더한다
			String endTmStr = "202411270000";//sdf.format(cal.getTime());
			
			System.out.println(":: START DATE: " + startTmStr);
			System.out.println(":: END DATE: " + endTmStr);
			
			List<Map<String, Object>> warnInfoList = this.evaluationDatabaseUtil.getAmisWarnInfoList(stnCd, startTmStr, endTmStr);
			
			try {
				
				Integer[] evalVerList = new Integer[]{1/*, 2*/};
				
				for(Integer evalVer : evalVerList) {
					
					//System.out.println(":: START WARN EVALUATION VERSION: " + evalVer);
				
					List<Map<String, Object>> warnEvaluationInfoList = this.getWarnEvaluationInfoList(warnInfoList, stnCd, evalVer);
					
					if(warnEvaluationInfoList != null) {
						
						for(int j=0 ; j<warnEvaluationInfoList.size() ; j++) {
							//this.insertEvaluationResult(warnEvaluationInfoList.get(j), evalVer);	
						}
					}
				}
			
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("-> Fatal Error On Warn Evaluation Insert");
			}		

			System.out.println("==============================================================================");	
		}
	}
	
	@SuppressWarnings("unchecked")
	private void insertEvaluationResult(Map<String, Object> warnEvaluationInfo, int evalVer) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		System.out.println("-> Start Insert Warn Evaluation Result");
		
		final String evaluationId = EvaluationUtils.createWarnEvaluationId(this.evaluationDatabaseUtil.getEvaluationNextSeq("WARN"));
		
		WarnData warnData = (WarnData)warnEvaluationInfo.get("warnData");
		WarnEvaluationData warnEvaluationData = (WarnEvaluationData)warnEvaluationInfo.get("warnEvaluationData");
		Map<String, Object> warnEvaluationResult = (Map<String, Object>)warnEvaluationInfo.get("warnEvaluationResult");
		
		String stnCd = warnData.getStnCd();
		String inpNm = warnData.getInpNm();
		Integer warnTypeCode = warnData.getWarnTypeCode();
		Integer warnNum = warnData.getWarnNum();
		
		String evalTmStr = sdf.format(warnData.getAnncTm());
		String stEffctTmStr = sdf.format(warnData.getStEffctTm());
		String edEffctTmStr = sdf.format(warnData.getEdEffctTm());		
		String stCnlTmStr = warnData.getStCnlTm() == null ? null : sdf.format(warnData.getStCnlTm());
		String edCnlTmStr = warnData.getEdCnlTm() == null ? null : sdf.format(warnData.getEdCnlTm());
		
		System.out.println("->\t Insert Warn Msg ...");
		
		System.out.println("->\t Check Duplicated Row ...");
		
		List<String> dupEvalUIDList = this.evaluationDatabaseUtil.getWarnResultCount(stnCd, evalTmStr, evalVer, warnTypeCode);
		
		if(dupEvalUIDList != null && dupEvalUIDList.size() >= 0) {
			
			System.out.println("->\t\t Find Duplicated Row (Count: " + dupEvalUIDList.size() + ")");
			System.out.println("->\t\t Delete Duplicated Row (Count: " + dupEvalUIDList.size() + ")");
			
			this.evaluationDatabaseUtil.removeWarnResultData(dupEvalUIDList);
		}	
		
		if(warnEvaluationData != null) {
			
			System.out.println("->\t Insert Warn Evaluation Result Detail ...");
			
			List<Object> paramList = new ArrayList<Object>();
			
			// 평가 고유 번호
			paramList.add(evaluationId);
						
			paramList.add(stnCd);
			
			// 평가 시간
			Date evaluationTm = warnEvaluationData.getEvaluationTm();
			paramList.add(sdf.format(evaluationTm));
			
			// 최초 도달 시각
			Date firstArrTm = warnEvaluationData.getFirstArrTm();
			paramList.add(firstArrTm == null ? null : sdf.format(firstArrTm));
			
			// 최초 도달 - 발표
			Integer firstArrMin = warnEvaluationData.getFirstArrMin();
			paramList.add(firstArrMin);
			
			// 유효 시각 - 발표
			Integer prevMin = warnEvaluationData.getPrevMin();
			paramList.add(prevMin);
			
			// 최초 도달 현상
			String firstArrVal = warnEvaluationData.getFirstArrVal();
			paramList.add(firstArrVal);
			
			// 최종 관측 시각
			Date lastObsTm = warnEvaluationData.getLastObsTm();
			paramList.add(lastObsTm == null ? null : sdf.format(lastObsTm));
			
			// 최종 관측 현상
			String lastObsVal = warnEvaluationData.getLastObsVal();
			paramList.add(lastObsVal);
						
			if(this.evaluationDatabaseUtil.insertEvalWarnResultDetail(paramList) < 0) {
				this.aamiDBManager.rollback();
				return;
			}
		}
		
		System.out.println("->\t Insert Warn Evaluation Result  ...");
		
		List<Object> paramList = new ArrayList<Object>();
		
		paramList.add(evaluationId);
		paramList.add(evalVer);
		paramList.add(stnCd);
		paramList.add(warnTypeCode);
		paramList.add(evalTmStr);
		paramList.add(stEffctTmStr);
		paramList.add(edEffctTmStr);
		paramList.add(stCnlTmStr);
		paramList.add(edCnlTmStr);
		
		if(warnEvaluationData != null && warnEvaluationData.isAvailable()) {
			
			paramList.add(warnEvaluationResult.get("effctScore"));
			paramList.add(warnEvaluationResult.get("prevScore"));
			paramList.add(warnEvaluationResult.get("totalScore"));
			paramList.add(inpNm);
			paramList.add("Y");
			
		} else {
			
			paramList.addAll(DatabaseUtil.getEmptyParamList(3));			
			paramList.add(inpNm);
			paramList.add("N");
		}
		
		paramList.add(warnNum);
		
		paramList.add(warnData.isAutoCancel() ? "Y" : "N");
		paramList.add(warnData.isPrevCancel() ? "Y" : "N");
		
		if(this.evaluationDatabaseUtil.insertEvalWarnResult(paramList) < 0) {
			this.aamiDBManager.rollback();
			return;
		}
		
		this.aamiDBManager.commit();
	}
	
	private List<Map<String, Object>> getWarnEvaluationInfoList(List<Map<String, Object>> warnInfoList, String stnCd, int evalVer) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		List<WarnData> warnDataList = new ArrayList<WarnData>();
		
		List<Map<String, Object>> warnEvaluationInfoList = new ArrayList<Map<String, Object>>();
		
		// 공항경보는 TAF,DF,LF 와 다르게 발표경보와 평가경보 갯수가 다를수 있기에 아래와 같이 한다
		for(int i=0 ; i<warnInfoList.size() ; i++) {
			
			try {
				
				Map<String, Object> warnInfo = warnInfoList.get(i);
			
				Date stdTm = sdf.parse((String)warnInfo.get("tm"));
				String warnSource = (String)warnInfo.get("msgText");
				String inpNm = (String)warnInfo.get("inpNm");

				
				WarnData warnData = this.warnParser.parse(stnCd, warnSource, stdTm);
				warnData.setInpNm(inpNm);
				
				warnDataList.add(warnData);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		this.warnParser.filterWarnDataList(warnDataList);
		
		for(int i=0 ; i<warnDataList.size() ; i++) {
			
			try {
				
				Map<String, Object> warnEvaluationInfo = new HashMap<String, Object>();
				
				WarnData warnData = warnDataList.get(i);
				
				warnEvaluationInfo.put("warnData", warnData);
				
				if(warnData.isAvailable()) {
					
					Map<String, Object> warnEvaluationDataMap = this.warnEvaluation.evaluateWarn(warnData, 2, evalVer);
					
					if(warnEvaluationDataMap != null) {
						warnEvaluationInfo.putAll(warnEvaluationDataMap);	
					}							
				}	
				
				warnEvaluationInfoList.add(warnEvaluationInfo);
				
			} catch (Exception e) {
				
			}
		}
		
		return warnEvaluationInfoList;
	}
}