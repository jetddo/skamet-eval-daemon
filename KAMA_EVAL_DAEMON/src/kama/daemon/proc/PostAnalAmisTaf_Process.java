package kama.daemon.proc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import kama.daemon.db.DataBaseManager;
import kama.daemon.db.EvaluationDatabaseUtil;
import kama.daemon.eval.metar.MetarParser;
import kama.daemon.eval.taf.TafData;
import kama.daemon.eval.taf.TafElementSet;
import kama.daemon.eval.taf.TafParser;
import kama.daemon.postanal.TafPostAnalData;
import kama.daemon.postanal.TafPostAnalysis;
import kama.daemon.util.DatabaseUtil;
import kama.daemon.util.EvaluationUtils;

public class PostAnalAmisTaf_Process extends DaemonProcess {
	
	private SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private Configuration config;
	
	private TafParser tafParser = new TafParser();
	private MetarParser metarParser = new MetarParser();
	
	private DataBaseManager aamiDBManager;
	private DataBaseManager amisDBManager;
	
	private TafPostAnalysis tafPostAnalysis = new TafPostAnalysis();
	
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
			
			postAnalAmisTaf();
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		} finally {
			
			this.destroy();
		}		
	}
	
	public void postAnalAmisTaf() {
		
		List<String> stnCdList = EvaluationUtils.getAirportStnCdListForPostAnalTaf();
		
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
			
			List<Map<String, Object>> tafInfoList = this.evaluationDatabaseUtil.getAmisTafInfoListForPostAnal(stnCd, msgType.replaceAll("_", " "), startTmStr, endTmStr);
			
			for(int j=0 ; j<tafInfoList.size() ; j++) {
				
				Map<String, Object> tafInfo = tafInfoList.get(j);
				
				try {
					
					List<Map<String, Object>> tafPostAnalInfoList = getTafPostAnalInfoList(tafInfo, stnCd);
				
					this.insertPostAnalResultList(tafPostAnalInfoList);
					
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("-> Fatal Error On Taf PostAnal Insert");
				}
			}

			System.out.println("==============================================================================");	
		}
	}
	
	@SuppressWarnings("unchecked")
	private void insertPostAnalResultList(List<Map<String, Object>> tafPostAnalInfoList) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		System.out.println("-> Start Insert Taf PostAnal Result");
		
		for(int i=0 ; i<tafPostAnalInfoList.size() ; i++) {
			
			final String postAnalId = EvaluationUtils.createTafPostAnalId(this.evaluationDatabaseUtil.getPostAnalNextSeq("TAF"));
			
			Map<String, Object> tafPostAnalInfo = tafPostAnalInfoList.get(i);
			
			Map<String, Object> tafInfo = (Map<String, Object>)tafPostAnalInfo.get("tafInfo");
			TafData tafData = (TafData)tafPostAnalInfo.get("tafData");

			List<TafPostAnalData> tafPostAnalDataList = (List<TafPostAnalData>)tafPostAnalInfo.get("tafPostAnalDataList");
			String postAnalType = (String)tafPostAnalInfo.get("postAnalType");
			String postAnalResult = (String)tafPostAnalInfo.get("postAnalResult");
			
			String stnCd = (String)tafInfo.get("stnCd");
			String msgSts = (String)tafInfo.get("msgSts");
			String fcstKind = (String)tafInfo.get("msgType");
			String postAnalTmStr = (String)tafInfo.get("tm");
			String stTafTmStr = sdf.format(tafData.getStTafTm());
			String edTafTmStr = sdf.format(tafData.getEdTafTm());
			String inpNm = (String)tafInfo.get("inpNm");
			String msgSrc = (String)tafInfo.get("msgSrc");			
			
			System.out.println("->\t Check Duplicated Row ...");
			
			List<String> dupPostAnalUIDList = this.evaluationDatabaseUtil.getTafPostAnalResultCount(stnCd, fcstKind, msgSts, postAnalTmStr);
			
			if(dupPostAnalUIDList != null && dupPostAnalUIDList.size() >= 0) {
				
				System.out.println("->\t\t Find Duplicated Row (Count: " + dupPostAnalUIDList.size() + ")");
				System.out.println("->\t\t Delete Duplicated Row (Count: " + dupPostAnalUIDList.size() + ")");
				
				this.evaluationDatabaseUtil.removeTafPostAnalResultData(dupPostAnalUIDList);
			}	
			
			if(postAnalType != null && postAnalResult != null && tafPostAnalDataList != null) {
				
				System.out.println("->\t Insert Taf PostAnal Result Detail ...");
				
				for(int j=0 ; j<tafPostAnalDataList.size() ; j++) {
					
					List<Object> paramList = new ArrayList<Object>();
					
					// 분석 고유 번호
					paramList.add(postAnalId);
					
					TafPostAnalData tafPostAnalData = tafPostAnalDataList.get(j);
					
					// 분석 시간
					Date postAnalTmDetail = tafPostAnalData.getPostAnalTm();
					
					String tafElem = null;
					String metarElem = null;
					String postAnalResultDetail = null;
					String tafState = tafPostAnalData.getTafState();
					
					if("1".equals(postAnalType)) {
						
						
					} else if("2".equals(postAnalType)) {
						
						tafElem = tafPostAnalData.getTafSkyCondition();
						metarElem = tafPostAnalData.getMetarSkyCondition();
						postAnalResultDetail = tafPostAnalData.getFgAnalResult();
					}
					
					paramList.add(sdf.format(postAnalTmDetail));
					paramList.add(tafElem);
					paramList.add(metarElem);
					paramList.add(postAnalResultDetail);
					paramList.add(tafState);
					
					if(this.evaluationDatabaseUtil.insertPostAnalTafResultDetail(paramList) < 0) {
						this.aamiDBManager.rollback();
						return;
					}
				}
			}
			
			System.out.println("->\t Insert Taf PostAnal Result  ...");
			
			List<Object> paramList = new ArrayList<Object>();
			
			paramList.add(postAnalId);
			paramList.add(stnCd);
			paramList.add(postAnalTmStr);
			paramList.add(stTafTmStr);
			paramList.add(edTafTmStr);
			paramList.add(msgSts);
			paramList.add(fcstKind);
			paramList.add(inpNm);
			
			if(postAnalType != null && postAnalResult != null) {
				
				paramList.add(postAnalType);
				paramList.add(postAnalResult);
				paramList.add("Y");
				
			} else {
				
				paramList.addAll(DatabaseUtil.getEmptyParamList(2));
				paramList.add("N");
			}
			
			paramList.add(msgSrc);
			
			if(this.evaluationDatabaseUtil.insertPostAnalTafResult(paramList) < 0) {
				this.aamiDBManager.rollback();
				return;
			}
			
			this.aamiDBManager.commit();
		}
	}
	
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getTafPostAnalInfoList(Map<String, Object> tafInfo, String stnCd) {
			
		System.out.println("-> " + tafInfo.get("msgSrc"));
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		List<Map<String, Object>> tafPostAnalInfoList = null;
			
		try {
			
			String tafString = (String)tafInfo.get("msgSrc");
			
			Date tafStdTm = sdf.parse((String)tafInfo.get("tm"));

			System.out.print("-> Start Taf Parse");
			
			TafData tafData = tafParser.parse(stnCd, tafString, tafStdTm);
			
			if(!tafData.isAvailable()) {
				
				tafPostAnalInfoList = new ArrayList<Map<String, Object>>();
				
				Map<String, Object> tafPostAnalInfo = new HashMap<String, Object>();
				
				tafPostAnalInfo.put("tafData", tafData);
				tafPostAnalInfo.put("tafInfo", tafInfo);
				
				System.out.println(" ... Fail");
				
				tafData.printErrorMsgList();				
				return tafPostAnalInfoList;
				
			} else {
				
				Date stTafTm = tafData.getStTafTm();
				Date edTafTm = tafData.getEdTafTm();
				
				System.out.println(" ... Success (Taf Period: " + sdf2.format(stTafTm) +" ~ " + sdf2.format(edTafTm) + ")");
				
				System.out.print("-> Get Metar Obs List For Taf Period From Amis Database");
				
				List<Map<String, Object>> metarObsDataList = this.evaluationDatabaseUtil.getAmisMetarObsDataList(stnCd, sdf.format(stTafTm), sdf.format(edTafTm));
				
				System.out.println(" ... Success");
				
				System.out.println("-> Start Taf PostAnal");
				
				List<TafElementSet> tafElementSetList = tafData.getTafElementSetList();
				
				tafPostAnalInfoList = tafPostAnalysis.analysis(stnCd, stTafTm, edTafTm, tafElementSetList, metarObsDataList);
				
				for(int i=0 ; i<tafPostAnalInfoList.size() ; i++) {
				
					Map<String, Object> tafPostAnalInfo = tafPostAnalInfoList.get(i);
					
					tafPostAnalInfo.put("tafInfo", tafInfo);
					tafPostAnalInfo.put("tafData", tafData);
				}
			}				
			
		} catch (Exception e) {
			
			System.out.println("-> Fatal Error On Taf PostAnal");
			e.printStackTrace();
			return null;
		}
		
		return tafPostAnalInfoList;
	}
}