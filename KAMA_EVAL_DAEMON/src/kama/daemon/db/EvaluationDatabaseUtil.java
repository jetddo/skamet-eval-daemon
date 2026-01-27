package kama.daemon.db;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import kama.daemon.eval.lf.LfData;
import kama.daemon.eval.taf.TafData;
import kama.daemon.util.EvaluationUtils;

public class EvaluationDatabaseUtil {
	
	// 테스트 테이블에 입력할것인지 여부
	private boolean testMode = false;
	
	// 테스트 테이블인 경우 TEST_ 접두어 붙음
	private String tablePrefix = "";
	
	private DataBaseManager amisDBManager;
	private DataBaseManager aamiDBManager;
	
	public EvaluationDatabaseUtil(DataBaseManager amisDBManager, DataBaseManager aamiDBManager) {
		
		this.amisDBManager = amisDBManager;
		this.aamiDBManager = aamiDBManager;		
	}
	
	public EvaluationDatabaseUtil(DataBaseManager amisDBManager, DataBaseManager aamiDBManager, boolean testMode) {
		
		this.amisDBManager = amisDBManager;
		this.aamiDBManager = aamiDBManager;		
		
		this.testMode = testMode;
		
		if(this.testMode) {
			this.tablePrefix = "TEST_";
		}
	}
	
	public int insertEvalTafResult(List<Object> paramList) {
				
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_TAF_RESULT VALUES (''{0}'',''{1}'',''{2}'',''{3}'',TO_DATE(''{4}'', ''YYYYMMDDHH24MI''),"+ 
				 "TO_DATE(''{5}'', ''YYYYMMDDHH24MI''),TO_DATE(''{6}'', ''YYYYMMDDHH24MI''),''{7}'', ''{8}'', sysdate, ''{9}'',''{10}'',"+ 
				 "''{11}'',''{12}'',''{13}'',''{14}'',''{15}'',''{16}'',''{17}'',''{18}'')"; 
			
		return this.aamiDBManager.insert(MessageFormat.format(query, paramList.toArray()).replace("'null'", "null"));
	}
	
	public int insertEvalTafResultDetail(List<Object> paramList) {
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_TAF_RESULT_DETAIL VALUES (''{0}'',''{1}'',TO_DATE(''{2}'', ''YYYYMMDDHH24MI''),''{3}'',''{4}'',''{5}'',''{6}'',"+ 
				 "''{7}'',''{8}'',''{9}'',''{10}'',''{11}'',''{12}'',''{13}'',''{14}'',''{15}'',''{16}'',''{17}'',''{18}'',''{19}'',''{20}'',''{21}'',"+ 
				 "''{22}'',''{23}'',''{24}'',''{25}'',''{26}'',''{27}'',''{28}'',''{29}'',''{30}'',''{31}'',''{32}'',''{33}'',''{34}'',''{35}'',''{36}'',"+ 
				 "''{37}'',''{38}'',''{39}'',''{40}'',''{41}'',''{42}'',''{43}'',''{44}'',''{45}'',''{46}'',''{47}'',''{48}'',''{49}'',''{50}'',''{51}'',''{52}'',"+ 
				 "''{53}'',''{54}'',''{55}'',''{56}'',''{57}'',''{58}'',''{59}'',''{60}'',''{61}'',''{62}'',''{63}'',''{64}'',''{65}'',''{66}'',''{67}'',''{68}'',"+ 
				 "''{69}'',''{70}'',''{71}'',''{72}'',''{73}'',''{74}'',''{75}'',''{76}'',''{77}'',''{78}'',''{79}'',''{80}'',''{81}'',''{82}'')"; 
		
		return this.aamiDBManager.insert(MessageFormat.format(query, paramList.toArray()).replace("'null'", "null"));
	}
	
	public int insertEvalTafTafMsg(String evaluationId, Map<String, Object> tafInfo, TafData tafData) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		String stnCd = (String)tafInfo.get("stnCd");
		String msgTm = (String)tafInfo.get("tm");
		String stTafTmStr = sdf.format(tafData.getStTafTm());
		String edTafTmStr = sdf.format(tafData.getEdTafTm());
		String msgType = (String)tafInfo.get("msgType");
		String msgSts = (String)tafInfo.get("msgSts");
		String msgSrc = (String)tafInfo.get("msgSrc");
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_TAF_TAF_MSG(EVAL_UID, STN_CD, TAF_TM, ST_TAF_TM, ED_TAF_TM, MSG_STS, FCST_KIND, MSG_SRC, DECODE_YN) VALUES "+ 
				"(''{0}'', ''{1}'', TO_DATE(''{2}'', ''YYYYMMDDHH24MI''), TO_DATE(''{3}'', ''YYYYMMDDHH24MI''), TO_DATE(''{4}'', ''YYYYMMDDHH24MI''), ''{5}'', ''{6}'', ''{7}'', ''{8}'')"; 
						
		return this.aamiDBManager.insert(MessageFormat.format(query, new Object[]{
			evaluationId, 
			stnCd,
			msgTm, 
			stTafTmStr,
			edTafTmStr,
			msgSts, 
			msgType,
			msgSrc,
			tafData.isAvailable() ? "Y" : "N"
		}).replace("'null'", "null"));
	}
	
	public List<String> getTafResultCount(String stnCd, String fcstKind, String msgSts, String anncTmStr, int evalVer, int becmgType) {
		
		final String query = 
				"SELECT EVAL_UID FROM AAMI." + this.tablePrefix + "EVAL_TAF_RESULT WHERE STN_CD = ''{0}'' AND FCST_KIND = ''{1}'' AND MSG_STS = ''{2}'' AND EVAL_TM = TO_DATE(''{3}'', ''YYYYMMDDHH24MI'') AND EVAL_VER = {4} AND BECMG_TYPE = {5}";
		
		List<Map<String, Object>> resultList = this.aamiDBManager.selectWithCamelcase(MessageFormat.format(query, new Object[]{
			stnCd, fcstKind, msgSts, anncTmStr, evalVer, becmgType	
		}));
		
		List<String> evalUIDList = new ArrayList<String>();
		
		if(resultList != null) {
			
			for(int i=0 ; i<resultList.size() ; i++) {
				evalUIDList.add((String)resultList.get(i).get("evalUid"));
			}
			
			return evalUIDList;
		} else {
			return null;
		}
	}
	
	public void removeTafResultData(List<String> evalUIDList) {
		
		final String queryTafMsg = "DELETE " + this.tablePrefix + "EVAL_TAF_TAF_MSG WHERE EVAL_UID = ''{0}''";
		final String queryMetarMsg = "DELETE " + this.tablePrefix + "EVAL_TAF_METAR_MSG WHERE EVAL_UID = ''{0}''";
		final String queryEvalResultDetail = "DELETE " + this.tablePrefix + "EVAL_TAF_RESULT_DETAIL WHERE EVAL_UID = ''{0}''";
		final String queryEvalResult = "DELETE " + this.tablePrefix + "EVAL_TAF_RESULT WHERE EVAL_UID = ''{0}''";
		
		for(int i=0 ; i<evalUIDList.size() ; i++) {
		
			this.aamiDBManager.delete(MessageFormat.format(queryTafMsg, new Object[]{
				evalUIDList.get(i)	
			}));
			
			this.aamiDBManager.delete(MessageFormat.format(queryMetarMsg, new Object[]{
				evalUIDList.get(i)	
			}));
			
			this.aamiDBManager.delete(MessageFormat.format(queryEvalResultDetail, new Object[]{
				evalUIDList.get(i)	
			}));
			
			this.aamiDBManager.delete(MessageFormat.format(queryEvalResult, new Object[]{
				evalUIDList.get(i)	
			}));
		}	
	}
	
	public int insertEvalTafMetarMsg(String evaluationId, Map<String, Object> metarInfo, boolean decodeYn) {
		
		String stnCd = (String)metarInfo.get("stnCd");
		String msgTm = (String)metarInfo.get("tm");
		String msgType = (String)metarInfo.get("msgType");
		String msgSts = (String)metarInfo.get("msgSts");
		String msgSrc = (String)metarInfo.get("msgSrc");
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_TAF_METAR_MSG(EVAL_UID, STN_CD, METAR_TM, MSG_STS, MSG_TYPE, MSG_SRC, DECODE_YN) VALUES "+ 
				"(''{0}'', ''{1}'', TO_DATE(''{2}'', ''YYYYMMDDHH24MI''), ''{3}'', ''{4}'', ''{5}'', ''{6}'')";
								
		return this.aamiDBManager.insert(MessageFormat.format(query, new Object[]{
			evaluationId, 
			stnCd,
			msgTm, 
			msgSts, 
			msgType,
			msgSrc,
			decodeYn ? "Y" : "N"
		}).replace("'null'", "null"));
	}
	
	public String getEvaluationNextSeq(String type) {
		
		final String query = "SELECT AAMI." + this.tablePrefix + "EVAL_"+type+"_SEQ.NEXTVAL AS NEXT_SEQ FROM DUAL";
		
		Map<String, Object> result = this.aamiDBManager.selectOneWithCamelcase(query);
			
		String nextSeq = result.get("nextSeq").toString();
		
		return nextSeq;
	}
	
	public Double getAmosMinTemperature(String stnId, Date time1, Date time2) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		final String query = " SELECT MIN(TMP) AS TMP FROM AMISUSER.AMOS "+ 
				 " WHERE TM >= TO_DATE('" + sdf.format(time1) + "', 'YYYYMMDDHH24MI') + 9/24 AND TM <= TO_DATE('" + sdf.format(time2) + "', 'YYYYMMDDHH24MI') + 9/24 "+
				 " AND TMP > -9999 "+
				 " AND STN_ID = '" + stnId + "'"+
				 " AND RWY_DIR IN ( " + EvaluationUtils.getAirportAmosRwyDir(stnId) + ")";
		
		Map<String, Object> result = this.amisDBManager.selectOneWithCamelcase(query);
		
		try {
		
			Double minTmp = Double.valueOf(result.get("tmp").toString());
			
			if(minTmp != null) {
				return minTmp / 10;
			} else {
				return null;
			}
			
		} catch (Exception e) {
			return null;
		}
	}
	
	public Double getAmosMaxTemperature(String stnId, Date time1, Date time2) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		final String query = " SELECT MAX(TMP) AS TMP FROM AMISUSER.AMOS "+ 
				 " WHERE TM >= TO_DATE('" + sdf.format(time1) + "', 'YYYYMMDDHH24MI') + 9/24 AND TM <= TO_DATE('" + sdf.format(time2) + "', 'YYYYMMDDHH24MI') + 9/24 "+
				 " AND TMP > -9999 "+
				 " AND STN_ID = '" + stnId + "'"+
				 " AND RWY_DIR IN ( " + EvaluationUtils.getAirportAmosRwyDir(stnId) + ")";
			
		Map<String, Object> result = this.amisDBManager.selectOneWithCamelcase(query);
		
		try {
			
			Double maxTmp = Double.valueOf(result.get("tmp").toString());
			
			if(maxTmp != null) {
				return maxTmp / 10;
			} else {
				return null;
			}
			
		} catch (Exception e) {
			return null;
		}
	}
	
	public List<Map<String, Object>> getAmisTafInfoList(String stnCd, String msgType, String startTmStr, String endTmStr) {
		
		final String query = " SELECT "+ 
							 " 	TO_CHAR(TM, 'YYYYMMDDHH24MI') AS TM, "+ 
							 " 	STN_CD AS STN_CD, "+
							 " 	FCST_KND AS MSG_TYPE, "+
							 "  MSG_STS AS MSG_STS, "+
							 "  '00/00' AS VALID_TM, "+
							 " 	MSG_TEXT AS MSG_SRC, "+
							 "  STN_CD AS SENDER, "+
							 "  TO_CHAR(INP_TM, 'YYYYMMDDHH24MI') AS TM_IN, " +
							 "  INP_NM AS INP_NM" +
							 " FROM AMISUSER.TAF "+
							 " WHERE 1=1 "+
							 " AND STN_CD = '" + stnCd + "'"+
							 " AND FCST_KND = '" + msgType + "'"+
							 " AND TM >= TO_DATE('" + startTmStr + "', 'YYYYMMDDHH24MI') "+ 
							 " AND TM <= TO_DATE('" + endTmStr + "', 'YYYYMMDDHH24MI') "+
							 " AND MSG_TEXT NOT LIKE '%DUE%'"+
							 " AND MSG_TEXT NOT LIKE '%CNL%'"+							 
							 " AND ANNNC_DVSN NOT IN (3,4) "+
							 " AND (TO_CHAR(TM, 'HH24MI') LIKE '%'||'0500' OR TO_CHAR(TM, 'HH24MI') LIKE '%'||'1100' OR TO_CHAR(TM, 'HH24MI') LIKE '%'||'1700' OR TO_CHAR(TM, 'HH24MI') LIKE '%'||'2300' "+ 
							   " OR TO_CHAR(TM, 'HH24MI') LIKE '%'||'0600' OR TO_CHAR(TM, 'HH24MI') LIKE '%'||'1200' OR TO_CHAR(TM, 'HH24MI') LIKE '%'||'1800' OR TO_CHAR(TM, 'HH24MI') LIKE '%'||'0000') "+
							 " ORDER BY TM ASC ";
		
		return this.amisDBManager.selectWithCamelcase(query);
	}
	
	public Map<String, Object> getAmisTafInfo(String stnCd, String stdTmStr, String msgType) {
		
		final String query = " SELECT "+ 
							 " 	TO_CHAR(TM, 'YYYYMMDDHH24MI') AS TM, "+ 
							 " 	STN_CD AS STN_CD, "+
							 " 	FCST_KND AS MSG_TYPE, "+
							 "  MSG_STS AS MSG_STS, "+
							 "  '00/00' AS VALID_TM, "+
							 " 	MSG_TEXT AS MSG_SRC, "+
							 "  STN_CD AS SENDER, "+
							 "  TO_CHAR(INP_TM, 'YYYYMMDDHH24MI') AS TM_IN" +
							 " FROM AMISUSER.TAF "+
							 " WHERE 1=1 "+
							 " AND STN_CD = '" + stnCd + "'"+
							 " AND FCST_KND = '" + msgType + "'"+
							 " AND TM = TO_DATE('" + stdTmStr + "', 'YYYYMMDDHH24MI') "+
							 " AND MSG_TEXT NOT LIKE '%DUE%'"+
							 " AND MSG_TEXT NOT LIKE '%CNL%'"+
							 " AND ANNNC_DVSN NOT IN (3,4) "+
							 " ORDER BY TM ASC ";
		
		return this.amisDBManager.selectOneWithCamelcase(query);
	}
		
	public List<Map<String, Object>> getAmisMetarInfoList(String stnCd, String startTmStr, String endTmStr) {
		
		final String query = " SELECT                                                                "+
							" 	TO_CHAR(A.TM, 'YYYYMMDDHH24MI') AS TM,                              "+
							" 	A.STN_CD AS STN_CD,                                                 "+
							" 	A.MSG_TYPE AS MSG_TYPE,                                             "+
							" 	A.MSG_STS AS MSG_STS,                                               "+
							" 	A.MSG_TEXT AS MSG_SRC,                                              "+
							" 	A.STN_CD AS SENDER,                                                 "+
							" 	TO_CHAR(A.INP_TM, 'YYYYMMDDHH24MI') AS TM_IN,                       "+
							"   B.INP_NM                                                            "+
							" FROM AMISUSER.METAR_MSG A                                             "+
							" INNER JOIN AMISUSER.METAR B                                           "+
							" ON A.TM = B.TM AND A.STN_CD = B.STN_CD AND A.MSG_TYPE = B.MSG_TYPE    "+
							" WHERE A.MSG_TYPE IN ('METARSCIAL', 'SPECI', 'METAR', 'METARSPECI')    "+
							" AND A.STN_CD = '" + stnCd + "'                                        "+
							" AND A.TM >= TO_DATE('" + startTmStr + "', 'YYYYMMDDHH24MI')           "+
							" AND A.TM <= TO_DATE('" + endTmStr + "', 'YYYYMMDDHH24MI')             "+
							" AND A.MSG_TEXT NOT LIKE '%NIL%'                                       "+
							" AND A.MSG_TEXT NOT LIKE '%AUTO%'                                      "+
							" AND A.INP_TYPE NOT IN (3,4)                                           "+
							" ORDER BY A.TM ASC                                                     ";
		
		List<Map<String, Object>> resultList = this.amisDBManager.selectWithCamelcase(query);
		
		if(resultList != null) {
			
			return resultList;
			
		} else {
			return null;
		}
	}
	
	public Map<String, Object> getAmisMetarInfo(String stnCd, String stdTmStr) {
		
		final String query = " SELECT "+ 
							 " 	TO_CHAR(TM, 'YYYYMMDDHH24MI') AS TM, "+ 
							 " 	STN_CD AS STN_CD, "+
							 "  MSG_TYPE AS MSG_TYPE, "+
							 "  MSG_STS AS MSG_STS, "+
							 " 	MSG_TEXT AS MSG_SRC, "+
							 "  STN_CD AS SENDER, "+
							 "  TO_CHAR(INP_TM, 'YYYYMMDDHH24MI') AS TM_IN"+
							 " FROM AMISUSER.METAR_MSG "+
							 " WHERE MSG_TYPE IN ('METARSCIAL', 'SPECI', 'METAR', 'METARSPECI') "+
							 " AND STN_CD = '" + stnCd + "'"+
							 " AND TM = TO_DATE('" + stdTmStr + "', 'YYYYMMDDHH24MI') "+ 
							 " AND MSG_TEXT NOT LIKE '%NIL%'"+
							 " AND MSG_TEXT NOT LIKE '%AUTO%'"+
							 " AND INP_TYPE NOT IN (3,4) "+
							 " ORDER BY TM ASC ";
		
		Map<String, Object> result = this.amisDBManager.selectOneWithCamelcase(query);
		
		if(result != null) {
			
			return result;
			
		} else {
			return null;
		}
	}
	
	//////// LF ///////
	
	public List<String> getLfResultCount(String stnCd, String msgType, String msgSts, String anncTmStr, int evalVer) {
		
		final String query = 
				"SELECT EVAL_UID FROM AAMI." + this.tablePrefix + "EVAL_LF_RESULT WHERE STN_CD = ''{0}'' AND MSG_TYPE = ''{1}'' AND MSG_STS = ''{2}'' AND EVAL_TM = TO_DATE(''{3}'', ''YYYYMMDDHH24MI'') AND EVAL_VER = {4}";
		
		List<Map<String, Object>> resultList = this.aamiDBManager.selectWithCamelcase(MessageFormat.format(query, new Object[]{
			stnCd, msgType, msgSts, anncTmStr, evalVer	
		}));
		
		List<String> evalUIDList = new ArrayList<String>();
		
		if(resultList != null) {
			
			for(int i=0 ; i<resultList.size() ; i++) {
				evalUIDList.add((String)resultList.get(i).get("evalUid"));
			}
			
			return evalUIDList;
		} else {
			return null;
		}
	}
	
	public void removeLfResultData(List<String> evalUIDList) {
		
		final String queryLfMsg = "DELETE " + this.tablePrefix + "EVAL_LF_LF_MSG WHERE EVAL_UID = ''{0}''";
		final String queryMetarMsg = "DELETE " + this.tablePrefix + "EVAL_LF_METAR_MSG WHERE EVAL_UID = ''{0}''";
		final String queryEvalResultDetail = "DELETE " + this.tablePrefix + "EVAL_LF_RESULT_DETAIL WHERE EVAL_UID = ''{0}''";
		final String queryEvalResult = "DELETE " + this.tablePrefix + "EVAL_LF_RESULT WHERE EVAL_UID = ''{0}''";
		
		for(int i=0 ; i<evalUIDList.size() ; i++) {
		
			this.aamiDBManager.delete(MessageFormat.format(queryLfMsg, new Object[]{
				evalUIDList.get(i)	
			}));
			
			this.aamiDBManager.delete(MessageFormat.format(queryMetarMsg, new Object[]{
				evalUIDList.get(i)	
			}));
			
			this.aamiDBManager.delete(MessageFormat.format(queryEvalResultDetail, new Object[]{
				evalUIDList.get(i)	
			}));
			
			this.aamiDBManager.delete(MessageFormat.format(queryEvalResult, new Object[]{
				evalUIDList.get(i)	
			}));
		}	
	}
	
	public int insertEvalLfLfMsg(String evaluationId, Map<String, Object> lfInfo, LfData lfData) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		String stnCd = (String)lfInfo.get("stnCd");
		String msgTm = (String)lfInfo.get("tm");
		String stLfTmStr = sdf.format(lfData.getStLfTm());
		String edLfTmStr = sdf.format(lfData.getEdLfTm());
		String msgType = (String)lfInfo.get("msgType");
		String msgSts = (String)lfInfo.get("msgSts");
		String msgSrc = (String)lfInfo.get("msgSrc");
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_LF_LF_MSG(EVAL_UID, STN_CD, LF_TM, ST_LF_TM, ED_LF_TM, MSG_STS, MSG_TYPE, MSG_SRC, DECODE_YN) VALUES "+ 
				"(''{0}'', ''{1}'', TO_DATE(''{2}'', ''YYYYMMDDHH24MI''), TO_DATE(''{3}'', ''YYYYMMDDHH24MI''), TO_DATE(''{4}'', ''YYYYMMDDHH24MI''), ''{5}'', ''{6}'', ''{7}'', ''{8}'')"; 
						
		return this.aamiDBManager.insert(MessageFormat.format(query, new Object[]{
			evaluationId, 
			stnCd,
			msgTm, 
			stLfTmStr,
			edLfTmStr,
			msgSts, 
			msgType,
			msgSrc,
			lfData.isAvailable() ? "Y" : "N"
		}).replace("'null'", "null"));
	}
	
	public int insertEvalLfMetarMsg(String evaluationId, Map<String, Object> metarInfo, boolean decodeYn) {
		
		String stnCd = (String)metarInfo.get("stnCd");
		String msgTm = (String)metarInfo.get("tm");
		String msgType = (String)metarInfo.get("msgType");
		String msgSts = (String)metarInfo.get("msgSts");
		String msgSrc = (String)metarInfo.get("msgSrc");
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_LF_METAR_MSG(EVAL_UID, STN_CD, METAR_TM, MSG_STS, MSG_TYPE, MSG_SRC, DECODE_YN) VALUES "+ 
				"(''{0}'', ''{1}'', TO_DATE(''{2}'', ''YYYYMMDDHH24MI''), ''{3}'', ''{4}'', ''{5}'', ''{6}'')";
								
		return this.aamiDBManager.insert(MessageFormat.format(query, new Object[]{
			evaluationId, 
			stnCd,
			msgTm, 
			msgSts, 
			msgType,
			msgSrc,
			decodeYn ? "Y" : "N"
		}).replace("'null'", "null"));
	}
	
	public int insertEvalLfResultDetail(List<Object> paramList) {
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_LF_RESULT_DETAIL VALUES (''{0}'',''{1}'',TO_DATE(''{2}'', ''YYYYMMDDHH24MI''),''{3}'',''{4}'',''{5}'',''{6}'',"+ 
				 "''{7}'',''{8}'',''{9}'',''{10}'',''{11}'',''{12}'',''{13}'',''{14}'',''{15}'',''{16}'',''{17}'',''{18}'',''{19}'',''{20}'',''{21}'',"+ 
				 "''{22}'',''{23}'',''{24}'',''{25}'',''{26}'',''{27}'',''{28}'',''{29}'',''{30}'',''{31}'',''{32}'',''{33}'',''{34}'',''{35}'',''{36}'',"+ 
				 "''{37}'',''{38}'',''{39}'',''{40}'',''{41}'',''{42}'',''{43}'',''{44}'',''{45}'',''{46}'',''{47}'',''{48}'',''{49}'',''{50}'',''{51}'',''{52}'',"+ 
				 "''{53}'',''{54}'',''{55}'',''{56}'',''{57}'',''{58}'',''{59}'',''{60}'',''{61}'',''{62}'',''{63}'',''{64}'',''{65}'',''{66}'',''{67}'',''{68}'',"+ 
				 "''{69}'',''{70}'',''{71}'',''{72}'',''{73}'',''{74}'')"; 
		
		return this.aamiDBManager.insert(MessageFormat.format(query, paramList.toArray()).replace("'null'", "null"));
	}
	
	public int insertEvalLfResult(List<Object> paramList) {
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_LF_RESULT VALUES (''{0}'',''{1}'',''{2}'',TO_DATE(''{3}'', ''YYYYMMDDHH24MI''),"+ 
				 "TO_DATE(''{4}'', ''YYYYMMDDHH24MI''),TO_DATE(''{5}'', ''YYYYMMDDHH24MI''),''{6}'', ''{7}'', sysdate, ''{8}'',''{9}'',"+ 
				 "''{10}'',''{11}'',''{12}'',''{13}'',''{14}'',''{15}'')"; 
	
		return this.aamiDBManager.insert(MessageFormat.format(query, paramList.toArray()).replace("'null'", "null"));
	}
	
	//// DF ////
	
	public List<String> getDfIssuedTmList(String stnCd, String startTmStr, String endTmStr) {
		
		final String query = " SELECT A.ISSUED_TM FROM (SELECT DISTINCT TO_CHAR(TM, ''YYYYMMDDHH24MI'') AS ISSUED_TM FROM AMISUSER.TKOF_FCST "+ 
				 " WHERE STN_CD = ''{0}'' AND "+
				 " (TM >= TO_DATE(''{1}'',''YYYYMMDDHH24MI'') AND TM <= TO_DATE(''{2}'',''YYYYMMDDHH24MI''))) A ORDER BY A.ISSUED_TM ";
		
		List<Map<String, Object>> resultList = this.amisDBManager.selectWithCamelcase(MessageFormat.format(query, new Object[]{
			stnCd, startTmStr, endTmStr	
		}));
		
		List<String> dfIssuedTmList = new ArrayList<String>();
		
		if(resultList != null) {
			
			for(int i=0 ; i<resultList.size() ; i++) {
				dfIssuedTmList.add((String)resultList.get(i).get("issuedTm"));
			}
			
			return dfIssuedTmList;
		} else {
			return null;
		}
	}
	
	public List<Map<String, Object>> getDfInfoList(String stnCd, String issuedTmStr) {
		
		final String query = " SELECT "+ 
							 " 	TO_CHAR(A.TM, ''YYYYMMDDHH24MI'') AS TM, "+ 
							 " 	A.STN_CD AS STN_CD, "+
							 " 	TO_CHAR(A.TM_FC, ''YYYYMMDDHH24MI'') AS TM_FC, "+ 
							 " 	A.WD AS WD, "+
							 "  A.WSPD AS WSPD, "+
							 "  A.TEMP AS TEMP, "+
							 " 	A.QNH AS QNH, "+
							 "  B.USER_NAME AS INP_NM "+
							 " FROM AMISUSER.TKOF_FCST A "+
							 " LEFT OUTER JOIN AMISUSER.SYS_USER_INFO B "+
							 " ON A.USER_ID = B.USER_ID "+							  
							 " WHERE 1=1 "+
							 " AND A.STN_CD = ''{0}''"+
							 " AND A.TM = TO_DATE(''{1}'', ''YYYYMMDDHH24MI'') "+
							 " ORDER BY A.TM ASC, A.TM_FC ASC ";
		
		List<Map<String, Object>> resultList = this.amisDBManager.selectWithCamelcase(MessageFormat.format(query, new Object[]{
			stnCd, issuedTmStr
		}));
		
		return resultList;
	}
	
	// 자동관측 포함 버전 (추후에 위 메소드랑 합쳐야함)
	public List<Map<String, Object>> getAmisMetarInfoListForDfEvaluate(String stnCd, String startTmStr, String endTmStr) {
		
		final String query = " SELECT "+ 
							 " 	TO_CHAR(TM, 'YYYYMMDDHH24MI') AS TM, "+ 
							 " 	STN_CD AS STN_CD, "+
							 "  MSG_TYPE AS MSG_TYPE, "+
							 "  MSG_STS AS MSG_STS, "+
							 " 	MSG_TEXT AS MSG_SRC, "+
							 "  STN_CD AS SENDER, "+
							 "  TO_CHAR(INP_TM, 'YYYYMMDDHH24MI') AS TM_IN"+
							 " FROM AMISUSER.METAR_MSG "+
							 " WHERE MSG_TYPE IN ('METARSCIAL', 'SPECI', 'METAR', 'METARSPECI') "+
							 " AND STN_CD = '" + stnCd + "'"+
							 " AND TM >= TO_DATE('" + startTmStr + "', 'YYYYMMDDHH24MI') "+ 
							 " AND TM <= TO_DATE('" + endTmStr + "', 'YYYYMMDDHH24MI') "+
							 " AND MSG_TEXT NOT LIKE '%NIL%'"+
							 " AND INP_TYPE NOT IN (3,4) "+
							 " ORDER BY TM ASC ";
		
		List<Map<String, Object>> resultList = this.amisDBManager.selectWithCamelcase(query);
		
		if(resultList != null) {
			
			return resultList;
			
		} else {
			return null;
		}
	}
	
	// Metar AQNH 값 가져오기
	public List<Map<String, Object>> getAmisMetarOriginInfoListForDfEvaluate(String stnCd, String startTmStr, String endTmStr) {
		
		final String query = " SELECT "+ 
							 " 	TO_CHAR(TM, 'YYYYMMDDHH24MI') AS TM, "+ 
							 "  AQNH"+
							 " FROM AMISUSER.METAR "+
							 " WHERE 1=1 "+
							 " AND STN_CD = '" + stnCd + "'"+
							 " AND TM >= TO_DATE('" + startTmStr + "', 'YYYYMMDDHH24MI') "+ 
							 " AND TM <= TO_DATE('" + endTmStr + "', 'YYYYMMDDHH24MI') "+
							 " AND INP_TYPE NOT IN (3,4) "+
							 " ORDER BY TM ASC ";
		
		List<Map<String, Object>> resultList = this.amisDBManager.selectWithCamelcase(query);
		
		if(resultList != null) {
			
			return resultList;
			
		} else {
			return null;
		}
	}
	
	public List<String> getDfResultCount(String stnCd, String anncTmStr, int evalVer) {
		
		final String query = 
				"SELECT EVAL_UID FROM AAMI." + this.tablePrefix + "EVAL_DF_RESULT WHERE STN_CD = ''{0}'' AND EVAL_TM = TO_DATE(''{1}'', ''YYYYMMDDHH24MI'') AND EVAL_VER = {2}";
		
		List<Map<String, Object>> resultList = this.aamiDBManager.selectWithCamelcase(MessageFormat.format(query, new Object[]{
			stnCd, anncTmStr, evalVer	
		}));
		
		List<String> evalUIDList = new ArrayList<String>();
		
		if(resultList != null) {
			
			for(int i=0 ; i<resultList.size() ; i++) {
				evalUIDList.add((String)resultList.get(i).get("evalUid"));
			}
			
			return evalUIDList;
		} else {
			return null;
		}
	}
	
	public void removeDfResultData(List<String> evalUIDList) {
		
		final String queryMetarMsg = "DELETE " + this.tablePrefix + "EVAL_DF_METAR_MSG WHERE EVAL_UID = ''{0}''";
		final String queryEvalResultDetail = "DELETE " + this.tablePrefix + "EVAL_DF_RESULT_DETAIL WHERE EVAL_UID = ''{0}''";
		final String queryEvalResult = "DELETE " + this.tablePrefix + "EVAL_DF_RESULT WHERE EVAL_UID = ''{0}''";
		
		for(int i=0 ; i<evalUIDList.size() ; i++) {
			
			this.aamiDBManager.delete(MessageFormat.format(queryMetarMsg, new Object[]{
				evalUIDList.get(i)	
			}));
			
			this.aamiDBManager.delete(MessageFormat.format(queryEvalResultDetail, new Object[]{
				evalUIDList.get(i)	
			}));
			
			this.aamiDBManager.delete(MessageFormat.format(queryEvalResult, new Object[]{
				evalUIDList.get(i)	
			}));
		}	
	}
	
	
	public int insertEvalDfMetarMsg(String evaluationId, Map<String, Object> metarInfo, boolean decodeYn) {
		
		String stnCd = (String)metarInfo.get("stnCd");
		String msgTm = (String)metarInfo.get("tm");
		String msgType = (String)metarInfo.get("msgType");
		String msgSts = (String)metarInfo.get("msgSts");
		String msgSrc = (String)metarInfo.get("msgSrc");
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_DF_METAR_MSG(EVAL_UID, STN_CD, METAR_TM, MSG_STS, MSG_TYPE, MSG_SRC, DECODE_YN) VALUES "+ 
				"(''{0}'', ''{1}'', TO_DATE(''{2}'', ''YYYYMMDDHH24MI''), ''{3}'', ''{4}'', ''{5}'', ''{6}'')";
								
		return this.aamiDBManager.insert(MessageFormat.format(query, new Object[]{
			evaluationId, 
			stnCd,
			msgTm, 
			msgSts, 
			msgType,
			msgSrc,
			decodeYn ? "Y" : "N"
		}).replace("'null'", "null"));
	}
	
	public int insertEvalDfResultDetail(List<Object> paramList) {
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_DF_RESULT_DETAIL VALUES (''{0}'',''{1}'',TO_DATE(''{2}'', ''YYYYMMDDHH24MI''),''{3}'',''{4}'',''{5}'',''{6}'',"+ 
				 "''{7}'',''{8}'',''{9}'',''{10}'',''{11}'',''{12}'',''{13}'',''{14}'',''{15}'',''{16}'')"; 
		
		return this.aamiDBManager.insert(MessageFormat.format(query, paramList.toArray()).replace("'null'", "null"));
	}
	
	public int insertEvalDfResult(List<Object> paramList) {
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_DF_RESULT VALUES (''{0}'',''{1}'',''{2}'',TO_DATE(''{3}'', ''YYYYMMDDHH24MI''),"+ 
				 "TO_DATE(''{4}'', ''YYYYMMDDHH24MI''),TO_DATE(''{5}'', ''YYYYMMDDHH24MI''), sysdate, ''{6}'',''{7}'',"+ 
				 "''{8}'',''{9}'',''{10}'',''{11}'')"; 
	
		return this.aamiDBManager.insert(MessageFormat.format(query, paramList.toArray()).replace("'null'", "null"));
	}
	
	//// WARN ////
	
	public List<Map<String, Object>> getAmosDataListForWarnEvaluate(String stnId, String startTmStr, String endTmStr) {
		
		final String query = " SELECT TO_CHAR(TM, 'YYYYMMDDHH24MI') AS TM, "+ 
							 " WSPD_10MIN_AVG/10 AS WSPD_10MIN_AVG, "+
							 " WSPD_1MIN_MAX/10 AS WSPD_1MIN_MAX, "+
							 " RN_1HR/10 AS RN_1HR, "+
							 " RN_3HR/10 AS RN_3HR "+
							 " FROM AMISUSER.AMOS "+ 
							 " WHERE 1=1 "+
							 " AND TM >= TO_DATE('" + startTmStr + "', 'YYYYMMDDHH24MI') + 9/24 "+
							 " AND TM <= TO_DATE('" + endTmStr + "', 'YYYYMMDDHH24MI') + 9/24 "+				 
							 " AND STN_ID = '" + stnId + "'";
		
		List<Map<String, Object>> resultList = this.amisDBManager.selectWithCamelcase(query);
					
		return resultList;
	}
	
	public List<Map<String, Object>> getAmisLocalInfoList(String stnCd, String startTmStr, String endTmStr) {
		
		final String query = " SELECT "+ 
							 " 	TO_CHAR(TM, 'YYYYMMDDHH24MI') AS TM, "+ 
							 " 	STN_CD AS STN_CD, "+
							 "  VIS AS VIS, "+
							 "  MTPH1 AS MTPH1, "+
							 " 	MTPH2 AS MTPH2, "+
							 "  MTPH3 AS MTPH3, "+
							 "  MSG_TEXT AS MSG_TEXT "+
							 " FROM AMISUSER.LOCAL "+
							 " WHERE 1=1 "+
							 " AND STN_CD = '" + stnCd + "'"+
							 " AND TM >= TO_DATE('" + startTmStr + "', 'YYYYMMDDHH24MI') "+ 
							 " AND TM <= TO_DATE('" + endTmStr + "', 'YYYYMMDDHH24MI') "+
							 " AND MSG_TEXT IS NOT NULL "+
							 " ORDER BY TM ASC ";
		
		List<Map<String, Object>> resultList = this.amisDBManager.selectWithCamelcase(query);
		
		for(int i=0 ; i<resultList.size() ; i++) {
			
			Map<String, Object> map = resultList.get(i);
			
			String mtph1 = (String)map.get("mtph1");
			String mtph2 = (String)map.get("mtph2");
			String mtph3 = (String)map.get("mtph3");
			
			String mtph = mtph1;
			
			if(mtph2 != null) {
				mtph += " " + mtph2;
			}
			
			if(mtph3 != null) {
				mtph += " " + mtph3;
			}
			
			map.put("mtph", mtph);
		}
		
		return resultList;
	}
	
	public List<Map<String, Object>> getAmisMetarFrscListForWarnEvaluate(String stnCd, String startTmStr, String endTmStr) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		final String query = " SELECT "+ 
				 " 	TO_CHAR(TM, 'YYYYMMDDHH24MI') AS TM, "+ 
				 " 	FRSC AS FRSC "+
				 " FROM AMISUSER.METAR "+
				 " WHERE MSG_TYPE IN ('METARSCIAL', 'SPECI', 'METAR', 'METARSPECI') "+
				 " AND STN_CD = '" + stnCd + "'"+
				 " AND TM >= TO_DATE('" + startTmStr + "', 'YYYYMMDDHH24MI') "+ 
				 " AND TM <= TO_DATE('" + endTmStr + "', 'YYYYMMDDHH24MI') "+
				 " AND INP_TYPE NOT IN (3,4) "+
				 " ORDER BY TM ASC ";
		
		List<Map<String, Object>> resultList = this.amisDBManager.selectWithCamelcase(query);
			
		for(int i=0 ; i<resultList.size() ; i++) {
			
			Map<String, Object> map = resultList.get(i);
			
			String frsc = (String)map.get("frsc");
			
			if(frsc != null) {				
				map.put("frsc", Double.valueOf(frsc)/10);
			}
		}
			
		return resultList;
	}
	
	public List<Map<String, Object>> getAmisWarnInfoList(String stnCd, String startTmStr, String endTmStr) {
		
		final String query = " SELECT TO_CHAR(TM, 'YYYYMMDDHH24MI') AS TM, STN_CD, MSG_TEXT, INP_NM "+
							 " FROM AMISUSER.WRNG "+
							 " WHERE 1=1 "+
							 " AND STN_CD = '" + stnCd + "'"+
							 " AND TM >= TO_DATE('"+startTmStr+"','YYYYMMDDHH24MI') "+
							 " AND TM <= TO_DATE('"+endTmStr+"','YYYYMMDDHH24MI') "+
							 " AND WRNG_TYPE IN ('1', '2', '3', '4', '5', '8') "+
							// " AND MSG_TEXT NOT LIKE '%EXTENDED%'"+
							 " ORDER BY TM DESC ";

		List<Map<String, Object>> resultList = this.amisDBManager.selectWithCamelcase(query);
		
		return resultList;
	}
	
	public List<String> getWarnResultCount(String stnCd, String anncTmStr, int evalVer, int warnTypeCode) {
		
		final String query = 
				"SELECT EVAL_UID FROM AAMI." + this.tablePrefix + "EVAL_WARN_RESULT WHERE STN_CD = ''{0}'' AND EVAL_TM = TO_DATE(''{1}'', ''YYYYMMDDHH24MI'') AND EVAL_VER = {2} AND WARN_TYPE_CODE = {3}";
		
		List<Map<String, Object>> resultList = this.aamiDBManager.selectWithCamelcase(MessageFormat.format(query, new Object[]{
			stnCd, anncTmStr, evalVer, warnTypeCode	
		}));
		
		List<String> evalUIDList = new ArrayList<String>();
		
		if(resultList != null) {
			
			for(int i=0 ; i<resultList.size() ; i++) {
				evalUIDList.add((String)resultList.get(i).get("evalUid"));
			}
			
			return evalUIDList;
		} else {
			return null;
		}
	}
	
	public void removeWarnResultData(List<String> evalUIDList) {
		
		final String queryEvalResultDetail = "DELETE " + this.tablePrefix + "EVAL_WARN_RESULT_DETAIL WHERE EVAL_UID = ''{0}''";
		final String queryEvalResult = "DELETE " + this.tablePrefix + "EVAL_WARN_RESULT WHERE EVAL_UID = ''{0}''";
		
		for(int i=0 ; i<evalUIDList.size() ; i++) {
				
			this.aamiDBManager.delete(MessageFormat.format(queryEvalResultDetail, new Object[]{
				evalUIDList.get(i)	
			}));
			
			this.aamiDBManager.delete(MessageFormat.format(queryEvalResult, new Object[]{
				evalUIDList.get(i)	
			}));
		}	
	}
	
	public int insertEvalWarnResultDetail(List<Object> paramList) {
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_WARN_RESULT_DETAIL VALUES (''{0}'',''{1}'',TO_DATE(''{2}'', ''YYYYMMDDHH24MI''),TO_DATE(''{3}'', ''YYYYMMDDHH24MI''),''{4}'',''{5}'',''{6}'',"+ 
				 "TO_DATE(''{7}'', ''YYYYMMDDHH24MI''),''{8}'')"; 
		
		return this.aamiDBManager.insert(MessageFormat.format(query, paramList.toArray()).replace("'null'", "null"));
	}
	
	public int insertEvalWarnResult(List<Object> paramList) {
		
		final String query = 
				"INSERT INTO AAMI." + this.tablePrefix + "EVAL_WARN_RESULT VALUES (''{0}'',''{1}'',''{2}'',''{3}'',TO_DATE(''{4}'', ''YYYYMMDDHH24MI''),TO_DATE(''{5}'', ''YYYYMMDDHH24MI''),TO_DATE(''{6}'', ''YYYYMMDDHH24MI''),TO_DATE(''{7}'', ''YYYYMMDDHH24MI''),TO_DATE(''{8}'', ''YYYYMMDDHH24MI''),sysdate,"+ 
				 "''{9}'',''{10}'',''{11}'',''{12}'',''{13}'',''{14}'',''{15}'',''{16}'')"; 
		
		return this.aamiDBManager.insert(MessageFormat.format(query, paramList.toArray()).replace("'null'", "null"));
	}
}

