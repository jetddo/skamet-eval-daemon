package kama.daemon.proc;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import kama.daemon.db.DataBaseManager;
import kama.daemon.eval.taf.TafData;
import kama.daemon.eval.taf.TafElement;
import kama.daemon.eval.taf.TafElementSet;
import kama.daemon.eval.taf.TafParser;
import kama.daemon.util.DaemonUtil;

public class DecodeAmisTaf_Process extends DaemonProcess {
	
	private SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private Configuration config;
	
	private TafParser tafParser = new TafParser();
	
	private DataBaseManager aamiDBManager;
	private DataBaseManager amisDBManager;
	
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
				
		} catch (Exception e ) {
			
			System.out.println("Error : DecodeAmisTaf.initialize -> " + e);
			
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
		
		System.out.println("-> Start Initialize");
		
		if(!this.initialize()) {
			
			System.out.println("Error : DecodeAmisTaf.process -> initialize failed");
			return;
		}
		
		try {
			
			decodeAmisTaf();
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		} finally {
			
			this.destroy();
		}		
	}
	
	public void decodeAmisTaf() {
		
		this.aamiDBManager.setAutoCommit(false);
				
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		String defaultLastAmisTafDateStr = "20210602";
		
		Map<String, Object> lastTafDateInfo = this.aamiDBManager.selectOneWithCamelcase(selectLastTafDate);
		
		String lastAmisTafDateStr = (String)lastTafDateInfo.get("lastDt");
		
		if(lastAmisTafDateStr == null || lastAmisTafDateStr.compareTo(defaultLastAmisTafDateStr) <= 0) {
			lastAmisTafDateStr = defaultLastAmisTafDateStr;
		}
		
		System.out.println("-> Amis Taf Last Date : " + lastAmisTafDateStr);
//		
		List<Map<String, Object>> amisTafInfoList = this.amisDBManager.selectWithCamelcase(MessageFormat.format(this.selectAmisTafInfoList, new Object[]{
			lastAmisTafDateStr
		}));
		
//		
		System.out.println("-> Amis Taf Found : " + amisTafInfoList.size());
//		
		
		int decodedCnt = 0;
		
		for(int i=0 ; i<amisTafInfoList.size() ; i++) {
			
			try {
				
				Map<String, Object> amisTafInfo = amisTafInfoList.get(i);
				
				String stdTmStr = (String)amisTafInfo.get("tm");
				Date stdTm = sdf.parse(stdTmStr);
				String stnCd = (String)amisTafInfo.get("stnCd");
				String fcstKnd = (String)amisTafInfo.get("fcstKnd");
				String inpTmStr = (String)amisTafInfo.get("inpTm");
				String msgSrc = (String)amisTafInfo.get("msgText");
				
				// 이미처리가 되있는지 확인해본다
				
				String query = MessageFormat.format(this.selectDecodedAmisTafInfo, new Object[]{
					stdTmStr, stnCd, fcstKnd	
				});
				
				Map<String, Object> decodedAmisTafInfo = this.aamiDBManager.selectOneWithCamelcase(query);
					
				// 디코딩 정보가 있다면 디코딩이 성공했는지 못했는지 판단하여 지우거나 한다				
				if(decodedAmisTafInfo != null) {
					
					String _amisTafUID = (String)decodedAmisTafInfo.get("tafUid");
					String _decodeYn = (String)decodedAmisTafInfo.get("decodeYn");
					String _inpTmStr = (String)decodedAmisTafInfo.get("inpTm");
					
					// 입력시각이 다르다면 업데이트가 되었을수 있으므로 삭제하고 재입력한다
					if(!inpTmStr.equals(_inpTmStr)) {
						
						System.out.println("--> inpTm had changed, delete old one");
						System.out.println("--> Old inpTm: "+_inpTmStr +", New inpTm: "+inpTmStr);
						
						query = MessageFormat.format(this.deleteDecodedAmisTafDecodeInfo, new Object[]{
							_amisTafUID
						});
						
						this.aamiDBManager.delete(query);	
						
						query = MessageFormat.format(this.deleteDecodedAmisTafInfo, new Object[]{
							_amisTafUID
						});
						
						this.aamiDBManager.delete(query);
						
					// 입력시각이 같다면 디코딩 여부를 판단하여 결정한다
					} else {
						
						// 디코딩이 성공한 TAF 면 넘긴다
						if("Y".equals(_decodeYn)) {
//							System.out.println("-> Already Decoded Taf Info continue to next");
							decodedCnt++;
							continue;
						
						// 디코딩이 실패한 TAF 면 삭제한다
						} else {
							
							System.out.println("--> old one had failed decode, delete old one");
													
							query = MessageFormat.format(this.deleteDecodedAmisTafDecodeInfo, new Object[]{
								_amisTafUID
							});
							
							this.aamiDBManager.delete(query);	
							
							query = MessageFormat.format(this.deleteDecodedAmisTafInfo, new Object[]{
								_amisTafUID
							});
							
							this.aamiDBManager.delete(query);					
						}		
					}		
				}
				
				System.out.println("================================================================================");
				
				System.out.println("-> New Taf : " + stnCd + ", " + sdf.format(stdTm) + ", " + msgSrc);
				
				System.out.println("-> Start Decoding Amis Taf ...");
				
				TafData tafData = tafParser.parse(stnCd, msgSrc, stdTm);
				
				if(!tafData.isAvailable()) {
					System.out.println("--> [Taf Decode Fail]");
					continue;
				} else {
					System.out.println("--> [Taf Decode Success]");
				}
				
				Map<String, Object> nextAmisTafSeqInfo = this.aamiDBManager.selectOneWithCamelcase(this.selectNextAmisTafSeq);
				
				String amisTafUID = "AMIS_TAF_" + String.format("%011d", Integer.valueOf((String)nextAmisTafSeqInfo.get("nextSeq")));
						
				query = MessageFormat.format(this.insertAmisTafInfo, new Object[]{
						
					amisTafInfo.get("tm"),
					amisTafInfo.get("stnCd"),
					amisTafInfo.get("msgSts"),
					amisTafInfo.get("annncDvsn"),
					amisTafInfo.get("fcstKnd"),
					amisTafInfo.get("effctTm"),
					amisTafInfo.get("inpTm"),
					amisTafInfo.get("inpNm"),
					amisTafInfo.get("inpIp"),
					amisTafInfo.get("refNr"),
					amisTafInfo.get("msgText"),
					amisTafUID,
					tafData.isAvailable() ? "Y" : "N"
				});
				
				System.out.println("-> Insert Into AMIS_TAF Table: " + amisTafUID);
				
				if(this.aamiDBManager.insert(query) < 0) {
					this.aamiDBManager.rollback();
					continue;
				}
				
				System.out.println("-> Insert Into AMIS_TAF_DECODE Table: " + amisTafUID);
				
				if(!this.insertDecodedTafData(tafData, amisTafUID)) {
					this.aamiDBManager.rollback();
					continue;
				}
				
				this.aamiDBManager.commit();				
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}	
		}
		
		System.out.println("================================================================================");
		System.out.println("Already Decoded Taf Count: " + decodedCnt);
		System.out.println("New Decoded Taf Count: " + (amisTafInfoList.size() - decodedCnt));
	}
	
	private boolean insertDecodedTafData(TafData tafData, String tafUID) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		List<TafElementSet> tafElementSetList = tafData.getTafElementSetList();
		
		boolean queryCheck = true;
		
		String query = "";
		
		for(int i=0 ; i<tafElementSetList.size() ; i++) {
			
			TafElementSet tafElementSet = tafElementSetList.get(i);
			
			// TAF 는 정시자료만 입력
			if(!sdf.format(tafElementSet.getTafTm()).endsWith("00")) {
				continue;
			}
			
			TafElement fcstTafElement = tafElementSet.getFcstTafElement();
			TafElement becmgTafElement = tafElementSet.getBecmgTafElement();
			TafElement tempoTafElement = tafElementSet.getTempoTafElement();
				
			List<Object> fcstTafDecodeList = this.getDecodedTafElementList(fcstTafElement, tafUID);
			query = MessageFormat.format(this.insertAmisTafDecodeInfo, fcstTafDecodeList.toArray()).replaceAll("'null'", "null");
			
			if(this.aamiDBManager.insert(query) < 0) {
				queryCheck = false;
			}
			
			if(becmgTafElement != null) {
				
				List<Object> becmgTafDecodeList = this.getDecodedTafElementList(becmgTafElement, tafUID);
				query = MessageFormat.format(this.insertAmisTafDecodeInfo, becmgTafDecodeList.toArray()).replaceAll("'null'", "null");
				
				if(this.aamiDBManager.insert(query) < 0) {
					queryCheck = false;
				}
			}
			
			if(tempoTafElement != null) {
				
				List<Object> tempoTafDecodeList = this.getDecodedTafElementList(tempoTafElement, tafUID);
				query = MessageFormat.format(this.insertAmisTafDecodeInfo, tempoTafDecodeList.toArray()).replaceAll("'null'", "null");
				
				if(this.aamiDBManager.insert(query) < 0) {
					queryCheck = false;
				}
			}
			
			if(!queryCheck) {
				return false;
			}
		}
		
		return true;
	}
	
	private List<Object> getDecodedTafElementList(TafElement tafElement, String tafUID) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		List<Object> tafDecodeList = new ArrayList<Object>();
		
		tafDecodeList.add(sdf.format(tafElement.getTafTm()));
		tafDecodeList.add(tafElement.getState());
		tafDecodeList.add(tafElement.getStateStatus());
		tafDecodeList.add(tafElement.getStateIdx());			
		tafDecodeList.add(tafElement.getWspd() == null ? null : tafElement.getWspd().toString());
		tafDecodeList.add(tafElement.getWdir() == null ? null : tafElement.getWdir().toString());
		tafDecodeList.add(tafElement.getMaxWspd() == null ? null : tafElement.getMaxWspd().toString());
		tafDecodeList.add(tafElement.getVis() == null ? null : tafElement.getVis().toString());
		tafDecodeList.add(tafElement.getTn() == null ? null : tafElement.getTn().toString());
		tafDecodeList.add(tafElement.getTx() == null ? null : tafElement.getTx().toString());			
		tafDecodeList.add(tafElement.isVrb() ? "Y" : "N");
		tafDecodeList.add(tafElement.isCavok() ? "Y" : "N");
		tafDecodeList.add(tafElement.isSkc() ? "Y" : "N");
		tafDecodeList.add(tafElement.isNsw() ? "Y" : "N");
		tafDecodeList.add(tafElement.isNsc() ? "Y" : "N");			
		tafDecodeList.add(tafElement.getSkyCondition());
		tafDecodeList.add(DaemonUtil.join(" ", tafElement.getCloudAmountLayer1List()));
		tafDecodeList.add(DaemonUtil.join(" ", tafElement.getCloudAmountLayer2List()));
		tafDecodeList.add(DaemonUtil.join(" ", tafElement.getCloudHeightLayer1List()));
		tafDecodeList.add(DaemonUtil.join(" ", tafElement.getCloudHeightLayer2List()));
		tafDecodeList.add(tafUID);
		
		return tafDecodeList;
	}
	
				
	private	final String selectAmisTafInfoList = " SELECT "+ 
					 " 	TO_CHAR(TM, ''YYYYMMDDHH24MI'') AS TM, "+ 
					 " 	STN_CD AS STN_CD, "+
					 "  MSG_STS AS MSG_STS, "+
					 "  ANNNC_DVSN AS ANNNC_DVSN, "+
					 " 	FCST_KND AS FCST_KND, "+
					 " 	EFFCT_TM AS EFFCT_TM, "+
					 "  TO_CHAR(INP_TM, ''YYYYMMDDHH24MI'') AS INP_TM, " +
					 "  INP_NM AS INP_NM, "+
					 "  INP_IP AS INP_IP, "+
					 "  REF_NR AS REF_NR, "+
					 "  MSG_TEXT AS MSG_TEXT "+
					 " FROM AMISUSER.TAF "+
					 " WHERE 1=1 "+
					 " AND FCST_KND = ''TAF''"+
					 " AND TM >= TO_DATE(''{0}'', ''YYYYMMDD'') "+ 
					 " AND MSG_TEXT NOT LIKE ''%DUE%''"+
					 " AND MSG_TEXT NOT LIKE ''%CNL%''"+
					 " AND ANNNC_DVSN NOT IN (3,4) "+
					 " AND (TO_CHAR(TM, ''HH24MI'') LIKE ''%''||''0500'' OR TO_CHAR(TM, ''HH24MI'') LIKE ''%''||''1100'' OR TO_CHAR(TM, ''HH24MI'') LIKE ''%''||''1700'' OR TO_CHAR(TM, ''HH24MI'') LIKE ''%''||''2300'' "+ 
					 " OR TO_CHAR(TM, ''HH24MI'') LIKE ''%''||''0600'' OR TO_CHAR(TM, ''HH24MI'') LIKE ''%''||''1200'' OR TO_CHAR(TM, ''HH24MI'') LIKE ''%''||''1800'' OR TO_CHAR(TM, ''HH24MI'') LIKE ''%''||''0000'') "+
					 " ORDER BY TM ASC ";
	
	private final String selectLastTafDate = "SELECT TO_CHAR(MAX(TM), 'YYYYMMDD') AS LAST_DT FROM AAMI.AMIS_TAF";
	
	private final String selectNextAmisTafSeq = "SELECT AAMI.AMIS_TAF_SEQ.NEXTVAL AS NEXT_SEQ FROM DUAL "; 
	
	private final String insertAmisTafInfo = 
			" INSERT INTO AAMI.AMIS_TAF(TM, STN_CD, MSG_STS, ANNNC_DVSN, FCST_KND, EFFCT_TM, INP_TM, INP_NM, INP_IP, REF_NR, MSG_TEXT, TAF_UID, DECODE_YN) VALUES "+
			" (TO_DATE(''{0}'', ''YYYYMMDDHH24MI''), ''{1}'', ''{2}'', ''{3}'', ''{4}'', ''{5}'', TO_DATE(''{6}'', ''YYYYMMDDHH24MI''), ''{7}'', ''{8}'', ''{9}'', ''{10}'', ''{11}'', ''{12}'') ";
						
	private final String insertAmisTafDecodeInfo = 
			" INSERT INTO AAMI.AMIS_TAF_DECODE(TAF_TM, STATE, STATE_STATUS, STATE_IDX, WSPD, WDIR, MAXWSPD, VIS, TN, TX, VRB, CAVOK, SKC, NSW, NSC, SKYCONDITION, CLOUD_AMOUNT_LAYER_1, CLOUD_AMOUNT_LAYER_2, CLOUD_HEIGHT_LAYER_1, CLOUD_HEIGHT_LAYER_2, TAF_UID) VALUES "+
			" (TO_DATE(''{0}'', ''YYYYMMDDHH24MI''), ''{1}'', ''{2}'', ''{3}'', ''{4}'', ''{5}'', ''{6}'', ''{7}'', ''{8}'', ''{9}'', ''{10}'', ''{11}'', ''{12}'', ''{13}'', ''{14}'', ''{15}'', ''{16}'', ''{17}'', ''{18}'', ''{19}'', ''{20}'') ";
			
	private final String selectDecodedAmisTafInfo = 
			" SELECT TAF_UID, TO_CHAR(INP_TM, ''YYYYMMDDHH24MI'') AS INP_TM, DECODE_YN FROM AAMI.AMIS_TAF WHERE 1=1 "+
			" AND TM = TO_DATE(''{0}'', ''YYYYMMDDHH24MI'') "+
			" AND STN_CD = ''{1}'' "+
			" AND FCST_KND = ''{2}'' ";
	
	private final String deleteDecodedAmisTafInfo = 
			" DELETE AAMI.AMIS_TAF WHERE TAF_UID = ''{0}'' ";
	
	private final String deleteDecodedAmisTafDecodeInfo = 
			" DELETE AAMI.AMIS_TAF_DECODE WHERE TAF_UID = ''{0}'' ";
}