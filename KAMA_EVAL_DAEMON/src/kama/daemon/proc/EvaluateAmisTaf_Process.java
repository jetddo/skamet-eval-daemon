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
import kama.daemon.eval.AmosElement;
import kama.daemon.eval.TafEvaluation;
import kama.daemon.eval.TafEvaluationData;
import kama.daemon.eval.metar.MetarData;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.metar.MetarParser;
import kama.daemon.eval.taf.TafData;
import kama.daemon.eval.taf.TafElement;
import kama.daemon.eval.taf.TafElementSet;
import kama.daemon.eval.taf.TafParser;
import kama.daemon.util.DatabaseUtil;
import kama.daemon.util.EvaluationUtils;

import org.apache.commons.configuration2.Configuration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class EvaluateAmisTaf_Process extends DaemonProcess {
	
	private SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private Configuration config;
	
	private TafEvaluation tafEvaluation;
	
	private TafParser tafParser = new TafParser();
	private MetarParser metarParser = new MetarParser();
	
	private DataBaseManager aamiDBManager;
	private DataBaseManager amisDBManager;
	
	private EvaluationDatabaseUtil evaluationDatabaseUtil;
	
	private Map<String, String> reqMap;
	
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
			
			this.tafEvaluation = new TafEvaluation(this.evaluationDatabaseUtil);
			
			this.aamiDBManager.setAutoCommit(false);
				
		} catch (Exception e ) {
			
			System.out.println("Error : EvaluateAmisTaf.initialize -> " + e);
			
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
	public void process(Configuration config, Map<String, String> reqMap) {
		
		this.config = config;
		this.reqMap = reqMap;
		
		System.out.println(":: Start Initialize");
		
		if(!this.initialize()) {
			
			System.out.println("Error : EvaluateAmisTaf.process -> initialize failed");
			return;
		}
		
		System.out.println(":: Initialize Complete");
		
		try {
			
			evaluateAmisTaf();
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		} finally {
			
			this.destroy();
		}		
	}
	
	public void evaluateAmisTaf() {
		
		List<String> stnCdList = EvaluationUtils.getAirportStnCdListForEvalTaf();
		
		for(int i=0 ; i<stnCdList.size() ; i++) {

			System.out.println("==============================================================================");	
			
			System.out.println(":: Start Airport [ " + stnCdList.get(i) + " ]");
			
			String stnCd = stnCdList.get(i);
			String msgType = "TAF";
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			
			Calendar cal = new GregorianCalendar();
			
			// 시작일은 3일을 뺀다, UTC 이므로 9를 더뺌
			cal.add(Calendar.HOUR_OF_DAY, -72-9);
			
			String startTmStr = sdf.format(cal.getTime());
			cal.add(Calendar.HOUR_OF_DAY, 24);
			// 종료일은 시작일에서 1일을 더한다
			String endTmStr = sdf.format(cal.getTime());
			
			if(this.reqMap != null) {
				
				String s = this.reqMap.get("-s");
				String e = this.reqMap.get("-e");
				
				if(s != null && e != null) {
					startTmStr = s;
					endTmStr = e;
				}
			}
			
			System.out.println(":: START DATE: " + startTmStr);
			System.out.println(":: END DATE: " + endTmStr);
			
			List<Map<String, Object>> tafInfoList = this.evaluationDatabaseUtil.getAmisTafInfoList(stnCd, msgType.replaceAll("_", " "), startTmStr, endTmStr);
			
			for(int j=0 ; j<tafInfoList.size() ; j++) {
				
				Map<String, Object> tafInfo = tafInfoList.get(j);
				
				try {
					
					// BECMG 패치 전 평가
					Map<String, Object> tafEvaluationInfo = getTafEvaluationInfo(tafInfo, stnCd, 2, 1);
				
					if(tafEvaluationInfo != null) {
						this.insertEvaluationResult(tafEvaluationInfo, 2, 1);	
					}
					
					// BECMG 패치 후 평가
					tafEvaluationInfo = getTafEvaluationInfo(tafInfo, stnCd, 2, 2);
					
					if(tafEvaluationInfo != null) {
						this.insertEvaluationResult(tafEvaluationInfo, 2, 2);	
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("-> Fatal Error On Taf Evaluation Insert");
				}
			}

			System.out.println("==============================================================================");	
		}
	}
	
	@SuppressWarnings("unchecked")
	private void insertEvaluationResult(Map<String, Object> tafEvaluationInfo, int evalVer, int becmgType) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		System.out.println("-> Start Insert Taf Evaluation Result");
		
		final String evaluationId = EvaluationUtils.createTafEvaluationId(this.evaluationDatabaseUtil.getEvaluationNextSeq("TAF"));
		
		Map<String, Object> tafInfo = (Map<String, Object>)tafEvaluationInfo.get("tafInfo");
		TafData tafData = (TafData)tafEvaluationInfo.get("tafData");
		List<Map<String, Object>> metarInfoList = (List<Map<String, Object>>)tafEvaluationInfo.get("metarInfoList");
		List<MetarData> metarDataList = (List<MetarData>)tafEvaluationInfo.get("metarDataList");
			
		Map<String, Object> tafEvaluationResult = (Map<String, Object>)tafEvaluationInfo.get("tafEvaluationResult");
		List<TafEvaluationData> tafEvaluationDataList = (List<TafEvaluationData>)tafEvaluationInfo.get("tafEvaluationDataList");
		
		String stnCd = (String)tafInfo.get("stnCd");
		String msgSts = (String)tafInfo.get("msgSts");
		String fcstKind = (String)tafInfo.get("msgType");
		String evalTmStr = (String)tafInfo.get("tm");
		String stTafTmStr = sdf.format(tafData.getStTafTm());
		String edTafTmStr = sdf.format(tafData.getEdTafTm());
		String inpNm = (String)tafInfo.get("inpNm");
		
		System.out.println("->\t Insert Taf Msg ...");
		
		System.out.println("->\t Check Duplicated Row ...");
		
		List<String> dupEvalUIDList = this.evaluationDatabaseUtil.getTafEvalResultCount(stnCd, fcstKind, msgSts, evalTmStr, evalVer, becmgType);
		
		if(dupEvalUIDList != null && dupEvalUIDList.size() >= 0) {
			
			System.out.println("->\t\t Find Duplicated Row (Count: " + dupEvalUIDList.size() + ")");
			System.out.println("->\t\t Delete Duplicated Row (Count: " + dupEvalUIDList.size() + ")");
			
			this.evaluationDatabaseUtil.removeTafEvalResultData(dupEvalUIDList);
		}		
		
		if(this.evaluationDatabaseUtil.insertEvalTafTafMsg(evaluationId, tafInfo, tafData) < 0) {
			this.aamiDBManager.rollback();
			return;
		}
		
		if(metarInfoList != null && metarDataList != null) {
			
			System.out.println("->\t Insert Metar Msg ... (MetarInfo Count: " + metarInfoList.size() + ", MetarData Count: " + metarDataList.size() + ")");
			
			for(int i=0 ; i<metarInfoList.size() ; i++) {
				
				Map<String, Object> metarInfo = metarInfoList.get(i);
				MetarData metarData = metarDataList.get(i);
				
				if(this.evaluationDatabaseUtil.insertEvalTafMetarMsg(evaluationId, metarInfo, metarData.isAvailable()) < 0) {
					this.aamiDBManager.rollback();
					return;
				}
			}
		}
		
		if(tafEvaluationDataList != null) {
			
			System.out.println("->\t Insert Taf Evaluation Result Detail ...");
			
			for(int i=0 ; i<tafEvaluationDataList.size() ; i++) {
				
				List<Object> paramList = new ArrayList<Object>();
				
				// 평가 고유 번호
				paramList.add(evaluationId);
							
				paramList.add(stnCd);
				
				TafEvaluationData tafEvaluationData = tafEvaluationDataList.get(i);
				
				// 평가 시간
				Date evaluationTm = tafEvaluationData.getEvaluationTm();
				paramList.add(sdf.format(evaluationTm));
				
				TafElementSet tafElementSet = tafEvaluationData.getTafElementSet();
				
				// FCST Taf Element			
				TafElement fcstTafElement = tafElementSet.getFcstTafElement();
				
				paramList.addAll(DatabaseUtil.getTafElementParamList(fcstTafElement));
				
				// BECMG Taf Element
				TafElement becmgTafElement = tafElementSet.getBecmgTafElement();			
							
				if(becmgTafElement == null) {
					paramList.addAll(DatabaseUtil.getEmptyParamList(17));
				} else {
					paramList.addAll(DatabaseUtil.getTafElementParamList(becmgTafElement));	
				}
				
				// TEMPO Taf Element
				TafElement tempoTafElement = tafElementSet.getTempoTafElement();
				
				if(tempoTafElement == null) {
					paramList.addAll(DatabaseUtil.getEmptyParamList(17));
				} else {
					paramList.addAll(DatabaseUtil.getTafElementParamList(tempoTafElement));	
				}
				
				// METAR Element
				MetarElement metarElement = tafEvaluationData.getMetarElement();
				
				if(metarElement == null) {
					paramList.addAll(DatabaseUtil.getEmptyParamList(15));
				} else {
					paramList.addAll(DatabaseUtil.getMetarElementParamList(metarElement));	
				}
				
				// AMOS Element
				AmosElement amosElement = tafEvaluationData.getAmosElement();
				
				if(amosElement == null) {
					paramList.addAll(DatabaseUtil.getEmptyParamList(2));
				} else {
					paramList.addAll(DatabaseUtil.getAmosElementParamList(amosElement));	
				}
				
				// Score
				TafEvaluationData.Score score = tafEvaluationData.getScore();
				paramList.addAll(DatabaseUtil.getScoreParamList(score));
				
				// Eval Type
				paramList.add(tafEvaluationData.getEvalType());
				
				if(this.evaluationDatabaseUtil.insertEvalTafResultDetail(paramList) < 0) {
					this.aamiDBManager.rollback();
					return;
				}
			}
		}
		
		System.out.println("->\t Insert Taf Evaluation Result  ...");
		
		List<Object> paramList = new ArrayList<Object>();
		
		paramList.add(evaluationId);
		paramList.add(evalVer);
		paramList.add(becmgType);
		paramList.add(stnCd);
		paramList.add(evalTmStr);
		paramList.add(stTafTmStr);
		paramList.add(edTafTmStr);
		paramList.add(msgSts);
		paramList.add(fcstKind);
		
		if(tafEvaluationResult != null && metarDataList != null && metarDataList.size() > 0) {
			
			paramList.add(tafEvaluationResult.get("windDirectionScoreAvg"));
			paramList.add(tafEvaluationResult.get("windSpeedScoreAvg"));
			paramList.add(tafEvaluationResult.get("visibilityScoreAvg"));
			paramList.add(tafEvaluationResult.get("rainOrClearScoreAvg"));
			paramList.add(tafEvaluationResult.get("cloudAmountScoreAvg"));
			paramList.add(tafEvaluationResult.get("cloudHeightScoreAvg"));
			paramList.add(tafEvaluationResult.get("temperatureScoreAvg"));
			paramList.add(tafEvaluationResult.get("temperatureVer2ScoreAvg"));
			paramList.add("Y");
			
		} else {
			
			paramList.addAll(DatabaseUtil.getEmptyParamList(8));
			paramList.add("N");
		}
		
		paramList.add(inpNm);
		
		if(this.evaluationDatabaseUtil.insertEvalTafResult(paramList) < 0) {
			this.aamiDBManager.rollback();
			return;
		}
		
		this.aamiDBManager.commit();
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getTafEvaluationInfo(Map<String, Object> tafInfo, String stnCd, int evalVer, int becmgType) {
			
		System.out.println("-> " + tafInfo.get("msgSrc"));
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		Map<String, Object> tafEvaluationInfo = new HashMap<String, Object>();
		
		tafEvaluationInfo.put("tafInfo", tafInfo);
			
		try {
			
			String tafString = (String)tafInfo.get("msgSrc");
			
			Date tafStdTm = sdf.parse((String)tafInfo.get("tm"));

			System.out.print("-> Start Taf Parse");
			
			TafData tafData = tafParser.parse(stnCd, tafString, tafStdTm);
			
			tafEvaluationInfo.put("tafData", tafData);
			
			if(!tafData.isAvailable()) {
				
				System.out.println(" ... Fail");
				
				tafData.printErrorMsgList();				
				return tafEvaluationInfo;
				
			} else {
				
				Date stTafTm = tafData.getStTafTm();
				Date edTafTm = tafData.getEdTafTm();
				
				System.out.println(" ... Success (Taf Period: " + sdf2.format(stTafTm) +" ~ " + sdf2.format(edTafTm) + ")");
				
				System.out.print("-> Get Metar List For Taf Period From Amis Database");
				
				List<MetarData> metarDataList = new ArrayList<MetarData>();
				
				List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(stnCd, sdf.format(stTafTm), sdf.format(edTafTm));
					
				// METAR 의 시작과 끝이 TAF 의 시작과 끝에 대응해야 평가를 시작한다, 이부분은 보류한다. 공군과 미군의 METAR 는 빼야하니까.
				
//				String stMetarTmStr = (String)metarInfoList.get(0).get("tm");
//				String edMetarTmStr = (String)metarInfoList.get(metarInfoList.size()-1).get("tm");
//				
//				if(!sdf.format(stTafTm).equals(stMetarTmStr) || !sdf.format(edTafTm).equals(edMetarTmStr)) {
//					System.out.println(" ... Fail: Not yet produced");
//					return null;
//				} else {
//					System.out.println(" ... Success");
//				}
				
				System.out.println(" ... Success");
				
				System.out.println("-> Start Filter Metar List");
				EvaluationUtils.filterAirportForcasts(metarInfoList, stnCd);
				
				System.out.print("-> Start Metar Parse");
			
				int metarParseFailCnt = 0;
				
				for(int j=0 ; j<metarInfoList.size() ; j++) {
					
					Map<String, Object> metarInfo = metarInfoList.get(j);
					
					String metarString = (String)metarInfo.get("msgSrc");
					
					Date metarStdTm = sdf.parse((String)metarInfo.get("tm"));
					
					MetarData metarData = metarParser.parse(stnCd, metarString, metarStdTm);
					
					if(!metarData.isAvailable()) {
						metarParseFailCnt++;
						metarData.printErrorMsgList();
					}
					
					metarDataList.add(metarData);
				}
				
				System.out.println(" ... End (Parse Fail Ratio: " + metarParseFailCnt + "/" + metarInfoList.size());
				
				tafEvaluationInfo.put("metarInfoList", metarInfoList);
				tafEvaluationInfo.put("metarDataList", metarDataList);
				
				System.out.print("-> Start Taf Evaluation (evalVer: " + evalVer + ", becmgType: " + becmgType + ")");
				
				Map<String, Object> tafEvaluationDataMap = tafEvaluation.evaluateTaf(stnCd, tafData, metarDataList, 2, evalVer, becmgType);
				
				List<TafEvaluationData> tafEvaluationDataList = (List<TafEvaluationData>)tafEvaluationDataMap.get("tafEvaluationDataList");
				Map<String, Object> tafEvaluationResult = (Map<String, Object>)tafEvaluationDataMap.get("tafEvaluationResult");
								
				boolean available = (boolean)tafEvaluationResult.get("available");
				
				if(available) {
					
					System.out.println(" ... Success (EvaluationDataList Size: " + tafEvaluationDataList.size() + ")");
					System.out.println("\t -> Evaluation Result: " + tafEvaluationResult);
					tafEvaluationInfo.putAll(tafEvaluationDataMap);
					
				} else {
					
					System.out.println(" ... Fail");
					return tafEvaluationInfo;
				}
			}				
			
		} catch (Exception e) {
			
			System.out.println("-> Fatal Error On Taf Evaluation");
			e.printStackTrace();
			return null;
		}
		
		return tafEvaluationInfo;
	}
}