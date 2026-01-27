package kama.daemon.proc;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.junit.Test;

import kama.daemon.db.DataBaseManager;
import kama.daemon.eval.metar.MetarData;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.metar.MetarParser;
import kama.daemon.util.DaemonUtil;

public class DecodeAmisMetar_Process extends DaemonProcess {
	
	private SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private Configuration config;
	
	private MetarParser metarParser = new MetarParser();
	
	private DataBaseManager aamiDBManager;
	private DataBaseManager amisDBManager;
	
	
	@Test
	public void asdf() {
		
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setPassword("pwkey");
		System.out.println(encryptor.decrypt("2CqMBEKV6hNypkYo3UgaIaWn2O3sh1eN"));
		
	}
	
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
			
			System.out.println("Error : DecodeAmisMetar.initialize -> " + e);
			
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
		
		System.out.println("-> Start Initialize");
		
		if(!this.initialize()) {
			
			System.out.println("Error : DecodeAmisMetar.process -> initialize failed");
			return;
		}
		
		try {
			
			decodeAmisMetar();
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		} finally {
			
			this.destroy();
		}		
	}
	
	public void decodeAmisMetar() {
		
		this.aamiDBManager.setAutoCommit(false);
				
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		String defaultLastAmisMetarDateStr = "2021070610";
		
		Map<String, Object> lastMetarDateInfo = this.aamiDBManager.selectOneWithCamelcase(selectLastMetarDate);
		
		String lastAmisMetarDateStr = (String)lastMetarDateInfo.get("lastDt");
		
		if(lastAmisMetarDateStr == null || lastAmisMetarDateStr.compareTo(defaultLastAmisMetarDateStr) <= 0) {
			lastAmisMetarDateStr = defaultLastAmisMetarDateStr;
		}
		
		System.out.println("-> Amis Metar Last Date : " + lastAmisMetarDateStr);
//		
		List<Map<String, Object>> amisMetarInfoList = this.amisDBManager.selectWithCamelcase(MessageFormat.format(this.selectAmisMetarInfoList, new Object[]{
			lastAmisMetarDateStr
		}));
				
//		
		System.out.println("-> Amis Metar Found : " + amisMetarInfoList.size());
//		
		
		int decodedCnt = 0;
		
		for(int i=0 ; i<amisMetarInfoList.size() ; i++) {
			
			try {
				
				Map<String, Object> amisMetarInfo = amisMetarInfoList.get(i);
				
				String stdTmStr = (String)amisMetarInfo.get("tm");
				Date stdTm = sdf.parse(stdTmStr);
				String stnCd = (String)amisMetarInfo.get("stnCd");
				String inpTmStr = (String)amisMetarInfo.get("inpTm");
				String msgSrc = (String)amisMetarInfo.get("msgText");
				
				// 이미처리가 되있는지 확인해본다
				
				String query = MessageFormat.format(this.selectDecodedAmisMetarInfo, new Object[]{
					stdTmStr, stnCd	
				});
				
				Map<String, Object> decodedAmisMetarInfo = this.aamiDBManager.selectOneWithCamelcase(query);
				
				// 디코딩 정보가 있다면 디코딩이 성공했는지 못했는지 판단하여 지우거나 한다				
				if(decodedAmisMetarInfo != null) {
					
					String _amisMetarUID = (String)decodedAmisMetarInfo.get("metarUid");
					String _decodeYn = (String)decodedAmisMetarInfo.get("decodeYn");
					String _inpTmStr = (String)decodedAmisMetarInfo.get("inpTm");
					
					// 입력시각이 다르다면 업데이트가 되었을수 있으므로 삭제하고 재입력한다
					if(!inpTmStr.equals(_inpTmStr)) {
						
						System.out.println("--> inpTm had changed, delete old one");
						System.out.println("--> Old inpTm: "+_inpTmStr +", New inpTm: "+inpTmStr);
						
						query = MessageFormat.format(this.deleteDecodedAmisMetarDecodeInfo, new Object[]{
							_amisMetarUID
						});
						
						this.aamiDBManager.delete(query);	
						
						query = MessageFormat.format(this.deleteDecodedAmisMetarInfo, new Object[]{
							_amisMetarUID
						});
						
						this.aamiDBManager.delete(query);
						
					// 입력시각이 같다면 디코딩 여부를 판단하여 결정한다
					} else {
						
						// 디코딩이 성공한 METAR 면 넘긴다
						if("Y".equals(_decodeYn)) {
//							System.out.println("-> Already Decoded Metar Info continue to next");
							decodedCnt++;
							continue;
						
						// 디코딩이 실패한 METAR 면 삭제한다
						} else {
							
							System.out.println("--> old one had failed decode, delete old one");
													
							query = MessageFormat.format(this.deleteDecodedAmisMetarDecodeInfo, new Object[]{
								_amisMetarUID
							});
							
							this.aamiDBManager.delete(query);	
							
							query = MessageFormat.format(this.deleteDecodedAmisMetarInfo, new Object[]{
								_amisMetarUID
							});
							
							this.aamiDBManager.delete(query);					
						}		
					}		
				}
				
				System.out.println("================================================================================");
				
				System.out.println("-> New Metar : " + stnCd + ", " + sdf.format(stdTm) + ", " + msgSrc);
				
				System.out.println("-> Start Decoding Amis Metar ...");
				
				MetarData metarData = metarParser.parse(stnCd, msgSrc, stdTm);
				
				if(!metarData.isAvailable()) {
					System.out.println("--> [Metar Decode Fail]");
					continue;
				} else {
					System.out.println("--> [Metar Decode Success]");
				}
				
				Map<String, Object> nextAmisMetarSeqInfo = this.aamiDBManager.selectOneWithCamelcase(this.selectNextAmisMetarSeq);
				
				String amisMetarUID = "AMIS_METAR_" + String.format("%09d", Integer.valueOf((String)nextAmisMetarSeqInfo.get("nextSeq")));
					
				query = MessageFormat.format(this.insertAmisMetarInfo, new Object[]{
						
					amisMetarInfo.get("tm"),
					amisMetarInfo.get("stnCd"),
					amisMetarInfo.get("msgType"),
					amisMetarInfo.get("msgSts"),
					amisMetarInfo.get("inpType"),
					amisMetarInfo.get("msgText"),
					amisMetarInfo.get("inpTm"),
					amisMetarUID,
					metarData.isAvailable() ? "Y" : "N"
				});
				
				System.out.println("-> Insert Into AMIS_METAR Table: " + amisMetarUID);
				
				if(this.aamiDBManager.insert(query) < 0) {
					this.aamiDBManager.rollback();
					continue;
				}
				
				System.out.println("-> Insert Into AMIS_METAR_DECODE Table: " + amisMetarUID);
				
				if(!this.insertDecodedMetarData(metarData, amisMetarUID)) {
					this.aamiDBManager.rollback();
					continue;
				}
				
				this.aamiDBManager.commit();				
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}	
		}
		
		System.out.println("================================================================================");
		System.out.println("Already Decoded Metar Count: " + decodedCnt);
		System.out.println("New Decoded Metar Count: " + (amisMetarInfoList.size() - decodedCnt));
	}
	
	private boolean insertDecodedMetarData(MetarData metarData, String metarUID) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		MetarElement metarElement = metarData.getMetarElement();
		
		boolean queryCheck = true;
		
		String query = "";
				
		List<Object> metarDecodeList = this.getDecodedMetarElementList(metarElement, metarUID);
		query = MessageFormat.format(this.insertAmisMetarDecodeInfo, metarDecodeList.toArray()).replaceAll("'null'", "null");
		
		if(this.aamiDBManager.insert(query) < 0) {
			queryCheck = false;
		}
	
		if(!queryCheck) {
			return false;
		}
		
		return true;
	}
	
	private List<Object> getDecodedMetarElementList(MetarElement metarElement, String metarUID) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		List<Object> metarDecodeList = new ArrayList<Object>();
		
		metarDecodeList.add(sdf.format(metarElement.getMetarTm()));
		metarDecodeList.add(metarElement.getWspd() == null ? null : metarElement.getWspd().toString());
		metarDecodeList.add(metarElement.getWdir() == null ? null : metarElement.getWdir().toString());
		metarDecodeList.add(metarElement.getMaxWspd() == null ? null : metarElement.getMaxWspd().toString());
		metarDecodeList.add(metarElement.getVis() == null ? null : metarElement.getVis().toString());
		metarDecodeList.add(metarElement.getTn() == null ? null : metarElement.getTn().toString());
		metarDecodeList.add(metarElement.getTx() == null ? null : metarElement.getTx().toString());			
		metarDecodeList.add(metarElement.isVrb() ? "Y" : "N");
		metarDecodeList.add(metarElement.isCavok() ? "Y" : "N");
		metarDecodeList.add(metarElement.isSkc() ? "Y" : "N");
		metarDecodeList.add(metarElement.isNsw() ? "Y" : "N");
		metarDecodeList.add(metarElement.isNsc() ? "Y" : "N");			
		metarDecodeList.add(metarElement.getSkyCondition());
		metarDecodeList.add(DaemonUtil.join(" ", metarElement.getCloudAmountLayer1List()));
		metarDecodeList.add(DaemonUtil.join(" ", metarElement.getCloudAmountLayer2List()));
		metarDecodeList.add(DaemonUtil.join(" ", metarElement.getCloudHeightLayer1List()));
		metarDecodeList.add(DaemonUtil.join(" ", metarElement.getCloudHeightLayer2List()));
		metarDecodeList.add(metarUID);
		
		return metarDecodeList;
	}
	
				
	private	final String selectAmisMetarInfoList = " SELECT "+ 
					 " 	TO_CHAR(TM, ''YYYYMMDDHH24MI'') AS TM, "+ 
					 " 	STN_CD AS STN_CD, "+
					 "  MSG_STS AS MSG_STS, "+
					 " 	MSG_TYPE AS MSG_TYPE, "+
					 "  TO_CHAR(INP_TM, ''YYYYMMDDHH24MI'') AS INP_TM, " +
					 "  INP_TYPE AS INP_TYPE, "+
					 "  MSG_TEXT AS MSG_TEXT "+
					 " FROM AMISUSER.METAR_MSG "+
					 " WHERE 1=1 "+
					 " AND TM >= TO_DATE(''{0}'', ''YYYYMMDDHH24'') - 3/24 "+ 
					 " AND MSG_TYPE IN (''METARSCIAL'', ''SPECI'', ''METAR'', ''METARSPECI'') "+
					 " AND MSG_TEXT NOT LIKE ''%NIL%''"+
					// " AND MSG_TEXT NOT LIKE ''%AUTO%''"+
					 " AND INP_TYPE NOT IN (3,4) "+				 
					 " ORDER BY TM ASC ";
	
	private final String selectLastMetarDate = "SELECT TO_CHAR(MAX(TM), 'YYYYMMDDHH24') AS LAST_DT FROM AAMI.AMIS_METAR";
	
	private final String selectNextAmisMetarSeq = "SELECT AAMI.AMIS_METAR_SEQ.NEXTVAL AS NEXT_SEQ FROM DUAL "; 
		
	private final String insertAmisMetarInfo = 
			" INSERT INTO AAMI.AMIS_METAR(TM, STN_CD, MSG_TYPE, MSG_STS, INP_TYPE, MSG_TEXT, INP_TM, METAR_UID, DECODE_YN) VALUES "+
			" (TO_DATE(''{0}'', ''YYYYMMDDHH24MI''), ''{1}'', ''{2}'', ''{3}'', ''{4}'', ''{5}'', TO_DATE(''{6}'', ''YYYYMMDDHH24MI''), ''{7}'', ''{8}'') ";
						
	private final String insertAmisMetarDecodeInfo = 
			" INSERT INTO AAMI.AMIS_METAR_DECODE(METAR_TM, WSPD, WDIR, MAXWSPD, VIS, TN, TX, VRB, CAVOK, SKC, NSW, NSC, SKYCONDITION, CLOUD_AMOUNT_LAYER_1, CLOUD_AMOUNT_LAYER_2, CLOUD_HEIGHT_LAYER_1, CLOUD_HEIGHT_LAYER_2, METAR_UID) VALUES "+
			" (TO_DATE(''{0}'', ''YYYYMMDDHH24MI''), ''{1}'', ''{2}'', ''{3}'', ''{4}'', ''{5}'', ''{6}'', ''{7}'', ''{8}'', ''{9}'', ''{10}'', ''{11}'', ''{12}'', ''{13}'', ''{14}'', ''{15}'', ''{16}'', ''{17}'') ";
			
	private final String selectDecodedAmisMetarInfo = 
			" SELECT METAR_UID, TO_CHAR(INP_TM, ''YYYYMMDDHH24MI'') AS INP_TM, DECODE_YN FROM AAMI.AMIS_METAR WHERE 1=1 "+
			" AND TM = TO_DATE(''{0}'', ''YYYYMMDDHH24MI'') "+
			" AND STN_CD = ''{1}'' ";
	
	private final String deleteDecodedAmisMetarInfo = 
			" DELETE AAMI.AMIS_METAR WHERE METAR_UID = ''{0}'' ";
	
	private final String deleteDecodedAmisMetarDecodeInfo = 
			" DELETE AAMI.AMIS_METAR_DECODE WHERE METAR_UID = ''{0}'' ";
}