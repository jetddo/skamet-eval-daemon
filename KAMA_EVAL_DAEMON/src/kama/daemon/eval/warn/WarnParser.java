package kama.daemon.eval.warn;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class WarnParser {
	
	//case '00': return '급변풍';
	//case '1': return '저시정';
	//case '2': return '강풍';
	//case '3': return '호우';
	//case '4': return '운고';
	//case '5': return '천둥번개';
	//case '7': return '태풍';
	//case '8': return '대설';
	//case '13': return '황사';
	//case '99': return '기타';
	
	private int findTokenIndex(String[] tokens, String qt, int count) {
		
		int _count = 0;
		
		for(int i=0 ; i<tokens.length ; i++) {
			
			if(qt.equals(tokens[i])) {
				
				_count++;
				
				if(_count == count) {
					return i;	
				}
			}
		}
		
		return -1;
	}
			
	public WarnData parse(String stnCd, String warnSource, Date stdTm) throws WarnParseException {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHHmm");
		
		WarnData warnData = new WarnData();
						
		try {
			
			warnData.setAnncTm(stdTm);
			warnData.setStnCd(stnCd);
			warnData.setWarnSource(warnSource);
			
			String[] tokens = warnSource.split("\\s+");
			
			int index = this.findTokenIndex(tokens, "WRNG", 1);
			
			if(index < 0) {
				throw new WarnParseException("Cannot find 'WRNG' token");
			}
			
			Integer warnNum = Integer.valueOf(tokens[index+1]);
			
			warnData.setWarnNum(warnNum);
			
			index = this.findTokenIndex(tokens, "VALID", 1);
			
			if(index < 0) {
				throw new WarnParseException("Cannot find 'VALID' token");
			}
			
			Date stEffctTm = sdf2.parse(sdf.format(stdTm) + tokens[index+1].split("\\/")[0]);
			Date edEffctTm = sdf2.parse(sdf.format(stdTm) + tokens[index+1].split("\\/")[1]);
			
			if(stEffctTm.getTime() > edEffctTm.getTime()) {
				
				Calendar cal = new GregorianCalendar();
				cal.setTime(stEffctTm);
				cal.add(Calendar.MONTH, 1);
				
				edEffctTm = sdf2.parse(sdf.format(cal.getTime()) + tokens[index+1].split("\\/")[1]);
			}
			
			// 경보전문 첫번째 유효기간정보 (취소전문의 경우에는 취소유효기간, 연장전문의 경우에는 연장유효기간)
			warnData.setStEffctTm(stEffctTm);
			warnData.setEdEffctTm(edEffctTm);
						
			switch(tokens[index+2]) {
			
			case "CNL": {
				
				index = this.findTokenIndex(tokens, "WRNG", 2);
				
				if(index < 0) {
					throw new WarnParseException("Cannot find second 'WRNG' token");
				}
				
				// 취소전문인 경우에는 취소할 전문번호와 전문유효기간을 셋팅해준다
				warnData.setCnl(true);
				warnData.setTargetWarnNum(Integer.valueOf(tokens[index+1]));
				
				Date targetStEffctTm = sdf2.parse(sdf.format(stdTm) + tokens[index+2].split("\\/")[0]);
				Date targetEdEffctTm = sdf2.parse(sdf.format(stdTm) + tokens[index+2].split("\\/")[1]);
				
				if(targetStEffctTm.getTime() > targetEdEffctTm.getTime()) {
					
					Calendar cal = new GregorianCalendar();
					cal.setTime(targetStEffctTm);
					cal.add(Calendar.MONTH, 1);
					
					targetEdEffctTm = sdf2.parse(sdf.format(cal.getTime()) + tokens[index+2].split("\\/")[1]);
				}
				
				warnData.setTargetStEffctTm(targetStEffctTm);
				warnData.setTargetEdEffctTm(targetEdEffctTm);
				
				break;
			}
			
			case "EXTENDED": {
				
				index = this.findTokenIndex(tokens, "WRNG", 2);
				
				if(index < 0) {
					throw new WarnParseException("Cannot find second 'WRNG' token");
				}
				
				// 연장전문인 경우에는 연장할 전문번호와 전문유효기간을 셋팅해준다
				warnData.setExtended(true);
				warnData.setTargetWarnNum(Integer.valueOf(tokens[index+1]));
				
				Date targetStEffctTm = sdf2.parse(sdf.format(stdTm) + tokens[index+2].split("\\/")[0]);
				Date targetEdEffctTm = sdf2.parse(sdf.format(stdTm) + tokens[index+2].split("\\/")[1]);
				
				if(targetStEffctTm.getTime() > targetEdEffctTm.getTime()) {
					
					Calendar cal = new GregorianCalendar();
					cal.setTime(targetStEffctTm);
					cal.add(Calendar.MONTH, 1);
					
					targetEdEffctTm = sdf2.parse(sdf.format(cal.getTime()) + tokens[index+2].split("\\/")[1]);
				}
				
				warnData.setTargetStEffctTm(targetStEffctTm);
				warnData.setTargetEdEffctTm(targetEdEffctTm);
				
				break;
			}
			
			case "TS":
				
				warnData.setWarnType(WarnData.Element.TS);
				warnData.setWarnTypeKor("천둥번개");
				warnData.setWarnTypeCode(5);
				
				break;
				
			case "HVY":
				
				if("SN".equals(tokens[index+3])) {
					
					warnData.setWarnType(WarnData.Element.HVY_SN);
					warnData.setWarnTypeKor("대설");
					warnData.setWarnTypeCode(8);
					
					// 적설량을 찾는다
					Double sn = null;						
					
					for(String token : tokens) {
						
						if(token.matches("[0-9]{2}CM")) {
							sn = Double.valueOf(token.replaceAll("CM", ""));
						}
					}
					
					warnData.setSn(sn);
					
				} else if("RA".equals(tokens[index+3])) {
					
					warnData.setWarnType(WarnData.Element.HVY_RA);
					warnData.setWarnTypeKor("호우");
					warnData.setWarnTypeCode(3);
					
					// 강수량을 찾는다
					Double ra = null;						
					
					for(String token : tokens) {
						
						if(token.matches("[0-9]{2}MM")) {
							ra = Double.valueOf(token.replaceAll("MM", ""));
						}
					}
					
					warnData.setRa(ra);
				}
				
				break;
				
			case "CIG":
				
				warnData.setWarnType(WarnData.Element.CIG);
				warnData.setWarnTypeKor("구름고도");
				warnData.setWarnTypeCode(4);
				
				// 운량을 찾는다
				Double cig = null;						
				
				for(String token : tokens) {
					
					if(token.matches("[0-9]{3,4}FT")) {
						cig = Double.valueOf(token.replaceAll("FT", ""));
					}
				}
				
				warnData.setCig(cig);
				
				break;
				
			case "SFC":
				
				if("WSPD".equals(tokens[index+3])) {
					
					warnData.setWarnType(WarnData.Element.SFC_WSPD);
					warnData.setWarnTypeKor("강풍");
					warnData.setWarnTypeCode(2);
					
					// 풍속을 찾는다
					Double wspd = null;
					Double maxWspd = null;
					
					for(int i=0 ; i<tokens.length ; i++) {
						
						if(tokens[i].matches("[0-9]{2}KT")) {
							wspd = Double.valueOf(tokens[i].replaceAll("KT", ""));
							maxWspd = Double.valueOf(tokens[i+2]);
						}
					}
					
					warnData.setWspd(wspd);
					warnData.setMaxWspd(maxWspd);
					
				} else if("VIS".equals(tokens[index+3])) {
					
					warnData.setWarnType(WarnData.Element.SFC_VIS);
					warnData.setWarnTypeKor("저시정");
					warnData.setWarnTypeCode(1);
					
					// 시정을 찾는다
					Double vis = null;						
					
					for(String token : tokens) {
						
						if(token.matches("[0-9]{3,4}M")) {
							vis = Double.valueOf(token.replaceAll("M", ""));
						}
					}
					
					warnData.setVis(vis);
				}
				
				break;	
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new WarnParseException("");
		}
		
		warnData.checkAvaliable();
		
		return warnData;
	} 
	
	public void filterWarnDataList(List<WarnData> warnDataList) {
		
		for(int i=0 ; i<warnDataList.size() ; i++) {
		
			WarnData warnData = warnDataList.get(i);
			
			// 취소전문인 경우 대상 전문에 전문취소시간을 업데이트하고 지운다
			if(warnData.isCnl()) {
			
				for(int j=0 ; j<warnDataList.size() ; j++) {
				
					WarnData targetWarnData = warnDataList.get(j);
					
					if(i == j) {
						continue;
					}
					
					if(warnData.getStnCd().equals(targetWarnData.getStnCd()) &&
						warnData.getTargetWarnNum() == targetWarnData.getWarnNum() &&
						warnData.getTargetStEffctTm().getTime() == targetWarnData.getStEffctTm().getTime() &&
						warnData.getTargetEdEffctTm().getTime() == targetWarnData.getEdEffctTm().getTime()) {
							
						targetWarnData.setStCnlTm(warnData.getStEffctTm());
						targetWarnData.setEdCnlTm(warnData.getEdEffctTm());
						targetWarnData.addInfWarnSources(warnData.getWarnSource());
						
						// 해제 시각이 유효시작시간보다 앞서는 경우 비활성화한다
						if(targetWarnData.getStEffctTm().getTime() > targetWarnData.getStCnlTm().getTime()) {
							targetWarnData.setAvailable(false);
							targetWarnData.setPrevCancel(true);
						}
					}	
				}
				
				warnDataList.remove(i--);
			} 
		}
		
		for(int i=0 ; i<warnDataList.size() ; i++) {
			
			WarnData warnData = warnDataList.get(i);
			
			// 연장전문인 경우 대상 전문에 전문연장시간을 업데이트하고 지운다
			if(warnData.isExtended()) {
				
				for(int j=0 ; j<warnDataList.size() ; j++) {
				
					WarnData targetWarnData = warnDataList.get(j);
					
					if(i == j) {
						continue;
					}
					
					if(warnData.getStnCd().equals(targetWarnData.getStnCd()) &&
						warnData.getTargetWarnNum() == targetWarnData.getWarnNum() &&
						warnData.getTargetStEffctTm().getTime() == targetWarnData.getStEffctTm().getTime() &&
						warnData.getTargetEdEffctTm().getTime() == targetWarnData.getEdEffctTm().getTime()) {
						
						// 연장전문애 취소전문 정보가 있다면 대상전문에 취소정보를 업데이트해준다
						if(warnData.getStCnlTm() != null && warnData.getEdCnlTm() != null) {
							targetWarnData.setStCnlTm(warnData.getStCnlTm());
							targetWarnData.setEdCnlTm(warnData.getEdCnlTm());
						}						
						
						// 대상 전문이 연장전문인 경우 경보종료시간을 업데이트해준다
						if(targetWarnData.isExtended()) {
							
							targetWarnData.setEdEffctTm(warnData.getEdEffctTm());
							
							targetWarnData.addInfWarnSources(warnData.getInfWarnSources() + warnData.getWarnSource());
							
						} else {
						// 대상 전문이 연장전문이 아닌 경우 연장 시간을 입력해준다
							targetWarnData.setStExtTm(warnData.getStEffctTm());
							targetWarnData.setEdExtTm(warnData.getEdEffctTm());
							
							if(warnData.getStCnlTm() != null) {
								targetWarnData.setStCnlTm(warnData.getStCnlTm());
								targetWarnData.setEdCnlTm(warnData.getEdCnlTm());
							}
							
							targetWarnData.addInfWarnSources(warnData.getInfWarnSources() + warnData.getWarnSource());
						}
					}	
				}
				
				warnDataList.remove(i--);
			}
		}
		
		// AUTO 시간대를 제외한다
		
		final int MAX_LOOP = 24;
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH");
		
		for(int i=0 ; i<warnDataList.size() ; i++) {
			
			WarnData warnData = warnDataList.get(i);
			
			if(WarnData.Element.SFC_WSPD.equals(warnData.getWarnType()) || WarnData.Element.HVY_RA.equals(warnData.getWarnType()) || WarnData.Element.HVY_SN.equals(warnData.getWarnType())) {
				continue;
			}
			
			if("RKSI".equals(warnData.getStnCd()) || "RKSS".equals(warnData.getStnCd()) || "RKPC".equals(warnData.getStnCd())) {
				continue;
			}
			
			Date stEffctTm = warnData.getStEffctTm();
			Date edEffctTm = warnData.getEdEffctTm();
			
			Calendar cal = new GregorianCalendar();
			
			cal.setTime(stEffctTm);
			
			int loop = 0;
			
			while(loop++ < MAX_LOOP && cal.getTime().getTime() <= edEffctTm.getTime()) {
				
				int hour = Integer.valueOf(sdf.format(cal.getTime()));
				
				switch(warnData.getStnCd()) {
				
				case "RKJB":
					
					if(hour > 13 && hour < 21) {
						warnData.setAvailable(false);
						warnData.setAutoCancel(true);
						continue;
					}
					
					break;
				case "RKPU":
					
					if(hour > 13 && hour < 21) {
						warnData.setAvailable(false);
						warnData.setAutoCancel(true);
						continue;
					}
					
					break;
				case "RKNY":
					
					if(hour > 9 && hour < 23) {
						warnData.setAvailable(false);
						warnData.setAutoCancel(true);
						continue;
					}
					
					break;
				case "RKJY":
					
					if(hour > 11 && hour < 22) {
						warnData.setAvailable(false);
						warnData.setAutoCancel(true);
						continue;
					}
					
					break;
				}
			}
		}
				
	}
	
	public static void main(String[] args) throws Exception {
		
		String s = "	METAR RKSI 150400Z 18008KT 160V230 6000 FEW006 SCT020 BKN070    "+
				"	          13/11 Q1005 NOSIG=                                    "+
				"	METAR RKSS 150400Z 28011KT 9999 FEW010 SCT025 OVC080 13/12      "+
				"	          Q1005 NOSIG=                                          "+
				"	METAR RKPC 150400Z 29006KT 220V340 9999 FEW012 BKN035 OVC080    "+
				"	          16/13 Q1007 NOSIG=                                    "+
				"	METAR RKPK 150400Z 31007KT 9999 SCT015 BKN030 16/13 Q1003=      "+
				"	METAR RKTU 150400Z 31009KT 9999 -RA FEW020 BKN050 OVC070 14/11  "+
				"	          Q1005=                                                "+
				"	METAR RKTN 150400Z 12001KT 9999 SCT040 BKN060 16/11 Q1004=      "+
				"	METAR RKJB NIL=  "+
				"	METAR RKNY 150400Z 36013KT 9000 FEW025 BKN050 OVC100 08/06      "+
				"	          Q1005 NOSIG=                                          "+
				"	                                                                ";
		
		
		String[] metars = s.split("METAR");
		
		for(int i=0 ; i<metars.length ; i++) {
			
			String[] tokens = metars[i].trim().split("\\s+");
			
			for(int j=0 ;j<tokens.length ; j++) {
				System.out.println(tokens[j]);
				
				if("NIL=".equals(tokens[j])) {
					System.out.println("맞음");
				}
			}
		}
		
//		
//		String[] warnSourceList = new String[] {
//				
//			
//				
//				"RKSI AD WRNG 1 VALID 302126/302330 SFC VIS LESS THAN 400M FCST="
//				,"RKSI AD WRNG 1 VALID 280100/280200 CNL AD WRNG 5 271300/280200="
//				,"RKSI AD WRNG 9 VALID 272200/280100 EXTENDED AD WRNG 7 271800/272200="
//				,"RKSI AD WRNG 8 VALID 271940/280300 CNL AD WRNG 6 271600/280300="
//				,"RKSI AD WRNG 7 VALID 271800/272200 TS FCST INTSF="
//				,"RKSI AD WRNG 6 VALID 271600/280300 SFC WSPD 25KT MAX 35 FCST="
//				,"RKSI AD WRNG 5 VALID 271300/280200 EXTENDED AD WRNG 3 270600/271300="
//				,"RKSI AD WRNG 4 VALID 270700/270900 CNL AD WRNG 2 270530/270900="
//				,"RKSI AD WRNG 3 VALID 270600/271300 EXTENDED AD WRNG 7 270000/270600="
//				,"RKSI AD WRNG 2 VALID 270530/270900 SFC WSPD 25KT MAX 35 FCST="
//				,"RKSI AD WRNG 1 VALID 270010/271500 CNL AD WRNG 5 261500/271500="
//				,"RKSI AD WRNG 7 VALID 270000/270600 HVY SN MORE THAN 03CM FCST="
//				,"RKSI AD WRNG 6 VALID 261700/262200 EXTENDED AD WRNG 4 261400/261700="
//				,"RKSI AD WRNG 5 VALID 261500/271500 EXTENDED AD WRNG 2 260900/261500="
//				,"RKSI AD WRNG 4 VALID 261400/261700 TS FCST INTSF="
//				,"RKSI AD WRNG 3 VALID 261050/261300 TS FCST INTSF="
//				,"RKSI AD WRNG 2 VALID 260900/261500 EXTENDED AD WRNG 1 252300/260900="
//				,"RKSI AD WRNG 1 VALID 260429/260700 TS FCST INTSF="
//				,"RKSI AD WRNG 1 VALID 252300/260900 SFC WSPD 25KT MAX 35 FCST="
//				,"RKSI AD WRNG 1 VALID 180340/180500 CNL AD WRNG 1 171300/180500="
//				,"RKSI AD WRNG 1 VALID 171300/180500 EXTENDED AD WRNG 1 162200/171300="
//				,"RKSI AD WRNG 1 VALID 162200/171300 SFC WSPD 25KT MAX 35 FCST="
//				,"RKSI AD WRNG 1 VALID 151508/151700 SFC WSPD 25KT MAX 35 FCST="
//				,"RKSI AD WRNG 2 VALID 272332/272350 CNL AD WRNG 1 272230/272350="
//				,"RKSI AD WRNG 1 VALID 272230/272350 SFC VIS LESS THAN 400M FCST="
//				,"RKSI AD WRNG 1 VALID 230400/230900 CNL AD WRNG 1 221400/230900="
//				,"RKSI AD WRNG 1 VALID 221400/230900 SFC WSPD 25KT MAX 35 FCST="
//				,"RKSI AD WRNG 2 VALID 180710/181600 CNL AD WRNG 1 180412/181600="
//				,"RKSI AD WRNG 1 VALID 180412/181600 SFC WSPD 25KT MAX 35 FCST="
//			
//		};
//		
//		Date d = new SimpleDateFormat("yyyyMMddHHmm").parse("202012312230");
//		
//		List<WarnData> warnDataList = new ArrayList<WarnData>();
//		
//		WarnParser warnParser = new WarnParser();
//		
//		for(int i=0 ; i<warnSourceList.length ; i++) {
//			
//			WarnData warnData = warnParser.parse("RKSI", warnSourceList[i], d);
//			
//			warnDataList.add(warnData);
//		}
//		
//		warnParser.filterWarnDataList(warnDataList);
//		
//		System.out.println(warnDataList);
	}
}
