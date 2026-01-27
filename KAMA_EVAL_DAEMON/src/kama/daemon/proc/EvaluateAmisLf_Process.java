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
import kama.daemon.eval.LfEvaluation;
import kama.daemon.eval.LfEvaluationData;
import kama.daemon.eval.lf.LfData;
import kama.daemon.eval.lf.LfElement;
import kama.daemon.eval.lf.LfElementSet;
import kama.daemon.eval.lf.LfParser;
import kama.daemon.eval.metar.MetarData;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.metar.MetarParser;
import kama.daemon.util.DatabaseUtil;
import kama.daemon.util.EvaluationUtils;

import org.apache.commons.configuration2.Configuration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class EvaluateAmisLf_Process extends DaemonProcess {
	
	private SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private Configuration config;
	
	private LfEvaluation lfEvaluation;
	
	private LfParser lfParser = new LfParser();
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
			
			this.lfEvaluation = new LfEvaluation(this.evaluationDatabaseUtil);
			
			this.aamiDBManager.setAutoCommit(false);
				
		} catch (Exception e ) {
			
			System.out.println("Error : EvaluateAmisLf.initialize -> " + e);
			
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
			
			System.out.println("Error : EvaluateAmisLf.process -> initialize failed");
			return;
		}
		
		System.out.println(":: Initialize Complete");
		
		try {
			
			evaluateAmisLf();
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		} finally {
			
			this.destroy();
		}		
	}
	
	public void evaluateAmisLf() {
		
		List<String> stnCdList = EvaluationUtils.getAirportStnCdListForEvalLf();
		
		for(int i=0 ; i<stnCdList.size() ; i++) {

			System.out.println("==============================================================================");	
			
			System.out.println(":: Start Airport [ " + stnCdList.get(i) + " ]");
			
			String stnCd = stnCdList.get(i);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			
			Calendar cal = new GregorianCalendar();
			
			// 시작일은 1일을 뺀다, UTC 이므로 9를 더뺌
			cal.add(Calendar.HOUR_OF_DAY, -24-9);
			
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
			
			List<Map<String, Object>> lfInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(stnCd, startTmStr, endTmStr);
			
			EvaluationUtils.filterLandingForcasts(lfInfoList, stnCd);
			
			for(int j=0 ; j<lfInfoList.size() ; j++) {
						
				Map<String, Object> lfInfo = lfInfoList.get(j);
				
				try {
					
					Map<String, Object> lfEvaluationInfo = getLfEvaluationInfo(lfInfo, stnCd, 1);
					
					if(lfEvaluationInfo != null) {
						this.insertEvaluationResult(lfEvaluationInfo, 1);	
					}
					
				} catch (Exception e) {
					System.out.println("-> Fatal Error On Lf Evaluation Insert");
				}
			}

			System.out.println("==============================================================================");	
		}
	}
	
	@SuppressWarnings("unchecked")
	private void insertEvaluationResult(Map<String, Object> lfEvaluationInfo, int evalVer) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		System.out.println("-> Start Insert Lf Evaluation Result");
		
		final String evaluationId = EvaluationUtils.createLfEvaluationId(this.evaluationDatabaseUtil.getEvaluationNextSeq("LF"));
		
		Map<String, Object> lfInfo = (Map<String, Object>)lfEvaluationInfo.get("lfInfo");
		LfData lfData = (LfData)lfEvaluationInfo.get("lfData");
		List<Map<String, Object>> metarInfoList = (List<Map<String, Object>>)lfEvaluationInfo.get("metarInfoList");
		List<MetarData> metarDataList = (List<MetarData>)lfEvaluationInfo.get("metarDataList");
			
		Map<String, Object> lfEvaluationResult = (Map<String, Object>)lfEvaluationInfo.get("lfEvaluationResult");
		List<LfEvaluationData> lfEvaluationDataList = (List<LfEvaluationData>)lfEvaluationInfo.get("lfEvaluationDataList");
		
		String stnCd = (String)lfInfo.get("stnCd");
		String msgSts = (String)lfInfo.get("msgSts");
		String msgType = (String)lfInfo.get("msgType");
		String evalTmStr = (String)lfInfo.get("tm");
		String inpNm = (String)lfInfo.get("inpNm");
		String stLfTmStr = sdf.format(lfData.getStLfTm());
		String edLfTmStr = sdf.format(lfData.getEdLfTm());
		
		System.out.println("->\t Insert Lf Msg ...");
		
		System.out.println("->\t Check Duplicated Row ...");
		
		List<String> dupEvalUIDList = this.evaluationDatabaseUtil.getLfEvalResultCount(stnCd, msgType, msgSts, evalTmStr, evalVer);
		
		if(dupEvalUIDList != null && dupEvalUIDList.size() >= 0) {
			
			System.out.println("->\t\t Find Duplicated Row (Count: " + dupEvalUIDList.size() + ")");
			System.out.println("->\t\t Delete Duplicated Row (Count: " + dupEvalUIDList.size() + ")");
			
			this.evaluationDatabaseUtil.removeLfEvalResultData(dupEvalUIDList);
		}		
		
		if(this.evaluationDatabaseUtil.insertEvalLfLfMsg(evaluationId, lfInfo, lfData) < 0) {
			this.aamiDBManager.rollback();
			return;
		}
		
		if(metarInfoList != null && metarDataList != null) {
			
			System.out.println("->\t Insert Metar Msg ... (MetarInfo Count: " + metarInfoList.size() + ", MetarData Count: " + metarDataList.size() + ")");
			
			for(int i=0 ; i<metarInfoList.size() ; i++) {
				
				Map<String, Object> metarInfo = metarInfoList.get(i);
				MetarData metarData = metarDataList.get(i);
				
				if(this.evaluationDatabaseUtil.insertEvalLfMetarMsg(evaluationId, metarInfo, metarData.isAvailable()) < 0) {
					this.aamiDBManager.rollback();
					return;
				}
			}
		}
		
		if(lfEvaluationDataList != null) {
			
			System.out.println("->\t Insert Lf Evaluation Result Detail ...");
			
			for(int i=0 ; i<lfEvaluationDataList.size() ; i++) {
				
				List<Object> paramList = new ArrayList<Object>();
				
				// 평가 고유 번호
				paramList.add(evaluationId);
							
				paramList.add(stnCd);
				
				LfEvaluationData lfEvaluationData = lfEvaluationDataList.get(i);
				
				// 평가 시간
				Date evaluationTm = lfEvaluationData.getEvaluationTm();
				paramList.add(sdf.format(evaluationTm));
				
				LfElementSet lfElementSet = lfEvaluationData.getLfElementSet();
				
				// FCST Lf Element			
				LfElement fcstLfElement = lfElementSet.getFcstLfElement();
				
				paramList.addAll(DatabaseUtil.getLfElementParamList(fcstLfElement));
				
				// BECMG Lf Element
				LfElement becmgLfElement = lfElementSet.getBecmgLfElement();			
							
				if(becmgLfElement == null) {
					paramList.addAll(DatabaseUtil.getEmptyParamList(17));
				} else {
					paramList.addAll(DatabaseUtil.getLfElementParamList(becmgLfElement));	
				}
				
				// TEMPO Lf Element
				LfElement tempoLfElement = lfElementSet.getTempoLfElement();
				
				if(tempoLfElement == null) {
					paramList.addAll(DatabaseUtil.getEmptyParamList(17));
				} else {
					paramList.addAll(DatabaseUtil.getLfElementParamList(tempoLfElement));	
				}
				
				// METAR Element
				MetarElement metarElement = lfEvaluationData.getMetarElement();
				
				if(metarElement == null) {
					paramList.addAll(DatabaseUtil.getEmptyParamList(15));
				} else {
					paramList.addAll(DatabaseUtil.getMetarElementParamList(metarElement));	
				}
				
				// Score
				LfEvaluationData.Score score = lfEvaluationData.getScore();
				paramList.addAll(DatabaseUtil.getScoreParamList(score));
				
				if(this.evaluationDatabaseUtil.insertEvalLfResultDetail(paramList) < 0) {
					this.aamiDBManager.rollback();
					return;
				}
			}
		}
		
		System.out.println("->\t Insert Lf Evaluation Result  ...");
		
		List<Object> paramList = new ArrayList<Object>();
		
		paramList.add(evaluationId);
		paramList.add(evalVer);
		paramList.add(stnCd);
		paramList.add(evalTmStr);
		paramList.add(stLfTmStr);
		paramList.add(edLfTmStr);
		paramList.add(msgSts);
		paramList.add(msgType);
		
		if(lfEvaluationResult != null && metarDataList != null && metarDataList.size() > 0) {
			
			paramList.add(lfEvaluationResult.get("windDirectionScoreAvg"));
			paramList.add(lfEvaluationResult.get("windSpeedScoreAvg"));
			paramList.add(lfEvaluationResult.get("visibilityScoreAvg"));
			paramList.add(lfEvaluationResult.get("rainOrClearScoreAvg"));
			paramList.add(lfEvaluationResult.get("cloudAmountScoreAvg"));
			paramList.add(lfEvaluationResult.get("cloudHeightScoreAvg"));
			paramList.add("Y");
			
		} else {
			
			paramList.addAll(DatabaseUtil.getEmptyParamList(6));
			paramList.add("N");
		}
		
		paramList.add(inpNm);
		
		if(this.evaluationDatabaseUtil.insertEvalLfResult(paramList) < 0) {
			this.aamiDBManager.rollback();
			return;
		}
		
		this.aamiDBManager.commit();
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getLfEvaluationInfo(Map<String, Object> lfInfo, String stnCd, int evalVer) {
			
		System.out.println("-> " + lfInfo.get("msgSrc"));
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		Map<String, Object> lfEvaluationInfo = new HashMap<String, Object>();
		
		lfEvaluationInfo.put("lfInfo", lfInfo);
			
		try {
			
			String lfString = (String)lfInfo.get("msgSrc");
			
			Date lfStdTm = sdf.parse((String)lfInfo.get("tm"));
			
			System.out.print("-> Start Lf Parse");
			
			LfData lfData = lfParser.parse(stnCd, lfString, lfStdTm);
		
			lfEvaluationInfo.put("lfData", lfData);
			
			if(!lfData.isAvailable()) {
				
				System.out.println(" ... Fail");
				
				lfData.printErrorMsgList();				
				return lfEvaluationInfo;
				
			} else {
				
				Date stLfTm = lfData.getStLfTm();
				Date edLfTm = lfData.getEdLfTm();
				
				// 1분을 더해서 같은 METAR 가 나오지 않도록 한다
				Date _stLfTm = new Date(stLfTm.getTime() + 1000 * 60);
				
				System.out.println(" ... Success (Lf Period: " + sdf2.format(_stLfTm) +" ~ " + sdf2.format(edLfTm) + ")");
				
				System.out.print("-> Get Metar List For Lf Period From Amis Database");
				
				List<MetarData> metarDataList = new ArrayList<MetarData>();
				
				List<Map<String, Object>> metarInfoList = this.evaluationDatabaseUtil.getAmisMetarInfoList(stnCd, sdf.format(_stLfTm), sdf.format(edLfTm));
							
				System.out.println(" ... Success");
				
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
				
				lfEvaluationInfo.put("metarInfoList", metarInfoList);
				lfEvaluationInfo.put("metarDataList", metarDataList);
				
				System.out.print("-> Start Lf Evaluation (evalVer: " + evalVer + ")");
				
				Map<String, Object> lfEvaluationDataMap = lfEvaluation.evaluateLf(stnCd, lfData, metarDataList, 2, evalVer);
				
				List<LfEvaluationData> lfEvaluationDataList = (List<LfEvaluationData>)lfEvaluationDataMap.get("lfEvaluationDataList");
				Map<String, Object> lfEvaluationResult = (Map<String, Object>)lfEvaluationDataMap.get("lfEvaluationResult");
								
				boolean available = (boolean)lfEvaluationResult.get("available");
				
				if(available) {
					
					System.out.println(" ... Success (EvaluationDataList Size: " + lfEvaluationDataList.size() + ")");
					System.out.println("\t -> Evaluation Result: " + lfEvaluationResult);
					lfEvaluationInfo.putAll(lfEvaluationDataMap);
					
				} else {
					
					System.out.println(" ... Fail");
					return lfEvaluationInfo;
				}
			}				
			
		} catch (Exception e) {
			
			System.out.println("-> Fatal Error On Lf Evaluation");
			e.printStackTrace();
			return null;
		}
		
		return lfEvaluationInfo;
	}
}