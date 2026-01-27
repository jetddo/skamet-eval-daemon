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
import kama.daemon.eval.DfEvaluation;
import kama.daemon.eval.DfEvaluationData;
import kama.daemon.eval.df.DfData;
import kama.daemon.eval.df.DfElement;
import kama.daemon.eval.df.DfElementSet;
import kama.daemon.eval.metar.MetarData;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.metar.MetarParser;
import kama.daemon.util.DatabaseUtil;
import kama.daemon.util.EvaluationUtils;

import org.apache.commons.configuration2.Configuration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class EvaluateAmisDf_Process extends DaemonProcess {
	
	private SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private Configuration config;
	
	private DfEvaluation dfEvaluation;
	
	private MetarParser metarParser = new MetarParser();
	
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
			
			this.dfEvaluation = new DfEvaluation(this.evaluationDatabaseUtil);
			
			this.aamiDBManager.setAutoCommit(false);
				
		} catch (Exception e ) {
			
			System.out.println("Error : EvaluateAmisDf.initialize -> " + e);
			
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
			
			System.out.println("Error : EvaluateAmisDf.process -> initialize failed");
			return;
		}
		
		System.out.println(":: Initialize Complete");
		
		try {
			
			evaluateAmisDf();
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		} finally {
			
			this.destroy();
		}		
	}
	
	public void evaluateAmisDf() {
		
		List<String> stnCdList = EvaluationUtils.getAirportStnCdListForEvalDf();
		
		for(int i=0 ; i<stnCdList.size() ; i++) {

			System.out.println("==============================================================================");	
			
			System.out.println(":: Start Airport [ " + stnCdList.get(i) + " ]");
			
			String stnCd = stnCdList.get(i);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			
			Calendar cal = new GregorianCalendar();
			
			// 시작일은 3일을 뺀다, UTC 이므로 9를 더뺌
			cal.add(Calendar.HOUR_OF_DAY, -72-9);
			
			String startTmStr = sdf.format(cal.getTime());
			cal.add(Calendar.HOUR_OF_DAY, 24);
			// 종료일은 시작일에서 1일을 더한다
			String endTmStr = sdf.format(cal.getTime());
			
			System.out.println(":: START DATE: " + startTmStr);
			System.out.println(":: END DATE: " + endTmStr);
			
			List<String> dfIssuedTmList = this.evaluationDatabaseUtil.getDfIssuedTmList(stnCd, startTmStr, endTmStr);
			
			for(int j=0 ; j<dfIssuedTmList.size() ; j++) {
				
				String issuedTmStr = dfIssuedTmList.get(j);
				
				List<Map<String, Object>> dfInfoList = this.evaluationDatabaseUtil.getDfInfoList(stnCd, issuedTmStr);
				
				EvaluationUtils.filterDepartureForcasts(dfInfoList, stnCd);
										
				try {
				
					Map<String, Object> dfEvaluationInfo = this.getDfEvaluationInfo(dfInfoList, stnCd, issuedTmStr, 1);
					
					if(dfEvaluationInfo != null) {
						this.insertEvaluationResult(dfEvaluationInfo, 1);	
					}
				
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("-> Fatal Error On Df Evaluation Insert");
				}
			}			

			System.out.println("==============================================================================");	
		}
	}
	
	@SuppressWarnings("unchecked")
	private void insertEvaluationResult(Map<String, Object> dfEvaluationInfo, int evalVer) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		System.out.println("-> Start Insert Df Evaluation Result");
		
		final String evaluationId = EvaluationUtils.createDfEvaluationId(this.evaluationDatabaseUtil.getEvaluationNextSeq("DF"));
		
		List<Map<String, Object>> dfInfoList = (List<Map<String, Object>>)dfEvaluationInfo.get("dfInfoList");
		
		DfData dfData = (DfData)dfEvaluationInfo.get("dfData");
		List<Map<String, Object>> metarInfoList = (List<Map<String, Object>>)dfEvaluationInfo.get("metarInfoList");
		List<MetarData> metarDataList = (List<MetarData>)dfEvaluationInfo.get("metarDataList");
			
		Map<String, Object> dfEvaluationResult = (Map<String, Object>)dfEvaluationInfo.get("dfEvaluationResult");
		List<DfEvaluationData> dfEvaluationDataList = (List<DfEvaluationData>)dfEvaluationInfo.get("dfEvaluationDataList");
		
		String stnCd = dfData.getStnCd();
		// 대표 입력자
		String inpNm = dfInfoList.get(0).get("inpNm") == null ? "" : dfInfoList.get(0).get("inpNm").toString();
		
		String evalTmStr = sdf.format(dfData.getAnncTm());
		String stDfTmStr = sdf.format(dfData.getStDfTm());
		String edDfTmStr = sdf.format(dfData.getEdDfTm());
		
		System.out.println("->\t Insert Df Msg ...");
		
		System.out.println("->\t Check Duplicated Row ...");
		
		List<String> dupEvalUIDList = this.evaluationDatabaseUtil.getDfResultCount(stnCd, evalTmStr, evalVer);
		
		if(dupEvalUIDList != null && dupEvalUIDList.size() >= 0) {
			
			System.out.println("->\t\t Find Duplicated Row (Count: " + dupEvalUIDList.size() + ")");
			System.out.println("->\t\t Delete Duplicated Row (Count: " + dupEvalUIDList.size() + ")");
			
			this.evaluationDatabaseUtil.removeDfResultData(dupEvalUIDList);
		}	
		
		if(metarInfoList != null && metarDataList != null) {
			
			System.out.println("->\t Insert Metar Msg ... (MetarInfo Count: " + metarInfoList.size() + ", MetarData Count: " + metarDataList.size() + ")");
			
			for(int i=0 ; i<metarInfoList.size() ; i++) {
				
				Map<String, Object> metarInfo = metarInfoList.get(i);
				MetarData metarData = metarDataList.get(i);
				
				if(this.evaluationDatabaseUtil.insertEvalDfMetarMsg(evaluationId, metarInfo, metarData.isAvailable()) < 0) {
					this.aamiDBManager.rollback();
					return;
				}
			}
		}
		
		if(dfEvaluationDataList != null) {
			
			System.out.println("->\t Insert Df Evaluation Result Detail ...");
			
			for(int i=0 ; i<dfEvaluationDataList.size() ; i++) {
				
				List<Object> paramList = new ArrayList<Object>();
				
				// 평가 고유 번호
				paramList.add(evaluationId);
							
				paramList.add(stnCd);
				
				DfEvaluationData dfEvaluationData = dfEvaluationDataList.get(i);
				
				// 상세 입력자
				String detailInpNm = dfInfoList.get(i).get("inpNm") == null ? "" : dfInfoList.get(i).get("inpNm").toString();
				
				// 평가 시간
				Date evaluationTm = dfEvaluationData.getEvaluationTm();
				paramList.add(sdf.format(evaluationTm));
				
				DfElementSet dfElementSet = dfEvaluationData.getDfElementSet();
				
				// FCST Df Element			
				DfElement fcstDfElement = dfElementSet.getDfElement();
				
				paramList.addAll(DatabaseUtil.getDfElementParamList(fcstDfElement));
								
				// METAR Element
				MetarElement metarElement = dfEvaluationData.getMetarElement();
				
				// METAR Origin Info
				Map<String, Object> metarOriginInfo = dfEvaluationData.getMetarOriginInfo();
				
				if(metarElement == null) {
					paramList.addAll(DatabaseUtil.getEmptyParamList(5));
				} else {
					paramList.addAll(DatabaseUtil.getMetarElementParamListForDfEvaluate(metarElement, metarOriginInfo));	
				}
				
				// Score
				DfEvaluationData.Score score = dfEvaluationData.getScore();
				paramList.addAll(DatabaseUtil.getScoreParamList(score));
				
				paramList.add(detailInpNm);
				
				if(this.evaluationDatabaseUtil.insertEvalDfResultDetail(paramList) < 0) {
					this.aamiDBManager.rollback();
					return;
				}
			}
		}
		
		System.out.println("->\t Insert Df Evaluation Result  ...");
		
		List<Object> paramList = new ArrayList<Object>();
		
		paramList.add(evaluationId);
		paramList.add(evalVer);
		paramList.add(stnCd);
		paramList.add(evalTmStr);
		paramList.add(stDfTmStr);
		paramList.add(edDfTmStr);
		
		if(dfEvaluationResult != null && metarDataList != null && metarDataList.size() > 0) {
			
			paramList.add(dfEvaluationResult.get("windDirectionScoreAvg"));
			paramList.add(dfEvaluationResult.get("windSpeedScoreAvg"));
			paramList.add(dfEvaluationResult.get("temperatureScoreAvg"));
			paramList.add(dfEvaluationResult.get("qnhScoreAvg"));
			paramList.add("Y");
			paramList.add(inpNm);
			
		} else {
			
			paramList.addAll(DatabaseUtil.getEmptyParamList(4));
			paramList.add("N");
			paramList.add(inpNm);
		}
		
		if(this.evaluationDatabaseUtil.insertEvalDfResult(paramList) < 0) {
			this.aamiDBManager.rollback();
			return;
		}
		
		this.aamiDBManager.commit();
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getDfEvaluationInfo(List<Map<String, Object>> dfInfoList, String stnCd, String issuedTmStr, int evalVer) {
			
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		Map<String, Object> dfEvaluationInfo = new HashMap<String, Object>();
		
		dfEvaluationInfo.put("dfInfoList", dfInfoList);
			
		try {
		
			DfData dfData = new DfData();
			
			System.out.println("-> Try to Import Df Data");
			
			dfData.importDfInfoList(dfInfoList);
			
			dfEvaluationInfo.put("dfData", dfData);
			
			if(!dfData.isAvailable()) {
				
				System.out.println(" ... Fail");
				
				dfData.printErrorMsgList();				
				return dfEvaluationInfo;
				
			} else {
				
				Date stDfTm = dfData.getStDfTm();
				Date edDfTm = dfData.getEdDfTm();
				
				System.out.println(" ... Success (Df Period: " + sdf2.format(stDfTm) +" ~ " + sdf2.format(edDfTm) + ")");
				
				System.out.print("-> Get Metar List For Df Period From Amis Database");
				
				List<MetarData> metarDataList = new ArrayList<MetarData>();
				
				List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoListForDfEvaluate(stnCd, sdf.format(stDfTm), sdf.format(edDfTm));
				List<Map<String, Object>> metarOriginInfoList = this.evaluationDatabaseUtil.getAmisMetarOriginInfoListForDfEvaluate(stnCd, sdf.format(stDfTm), sdf.format(edDfTm));
							
				System.out.println(" ... Success");
				
				System.out.print("-> Start Metar Parse");
			
				int metarParseFailCnt = 0;
				
				// 	중복제거를 합시다
				
				String _tm = null;
				
				for(int j=0 ; j<metarInfoList.size() ; j++) {
					
					Map<String, Object> metarInfo = metarInfoList.get(j);
					
					String metarString = (String)metarInfo.get("msgSrc");
					
					Date metarStdTm = sdf.parse((String)metarInfo.get("tm"));
					
					if(_tm == null || !_tm.equals((String)metarInfo.get("tm"))) {
						
						_tm = (String)metarInfo.get("tm");
						
						MetarData metarData = metarParser.parse(stnCd, metarString, metarStdTm);
						
						if(!metarData.isAvailable()) {
							metarParseFailCnt++;
							metarData.printErrorMsgList();
						}
						
						metarDataList.add(metarData);
						
					} else if(_tm != null && _tm.equals((String)metarInfo.get("tm"))) {
						
						// 이놈은 제거하자
						
						metarInfoList.remove(j--);
						
						continue;
					}
				}
				
				System.out.println(" ... End (Parse Fail Ratio: " + metarParseFailCnt + "/" + metarInfoList.size());
				
				dfEvaluationInfo.put("metarInfoList", metarInfoList);
				dfEvaluationInfo.put("metarDataList", metarDataList);
				
				System.out.print("-> Start Df Evaluation (evalVer: " + evalVer + ")");
				
				Map<String, Object> dfEvaluationDataMap = dfEvaluation.evaluateDf(stnCd, dfData, metarDataList, metarOriginInfoList, 2, evalVer);
				
				List<DfEvaluationData> dfEvaluationDataList = (List<DfEvaluationData>)dfEvaluationDataMap.get("dfEvaluationDataList");
				Map<String, Object> dfEvaluationResult = (Map<String, Object>)dfEvaluationDataMap.get("dfEvaluationResult");
								
				boolean available = (boolean)dfEvaluationResult.get("available");
				
				if(available) {
					
					System.out.println(" ... Success (EvaluationDataList Size: " + dfEvaluationDataList.size() + ")");
					System.out.println("\t -> Evaluation Result: " + dfEvaluationResult);
					dfEvaluationInfo.putAll(dfEvaluationDataMap);
					
				} else {
					
					System.out.println(" ... Fail");
					return dfEvaluationInfo;
				}
			}				
			
		} catch (Exception e) {
			
			System.out.println("-> Fatal Error On Df Evaluation");
			e.printStackTrace();
			return null;
		}
		
		return dfEvaluationInfo;
	}
}