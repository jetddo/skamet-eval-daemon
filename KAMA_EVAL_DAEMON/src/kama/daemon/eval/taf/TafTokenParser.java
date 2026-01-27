package kama.daemon.eval.taf;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TafTokenParser {
	
	private static String[] tafTokenRegexes = new String[]{
		
		"[0-9]{6}Z", // 발표시각 패턴
		"[0-9]{4}/[0-9]{4}", // 유효기간 패턴
		"(VRB[0-9]{2}|[0-9]{5})(G[0-9]{2})*(KT|MPS)", // 바람 패턴
		"[0-9]{3,4}|P*[0-9]+SM|[0-9]+\\/[0-9]+SM", // 시정패턴
		"(TX|TXM)[0-9]{2,3}/[0-9]{4}Z", // 최대기온패턴
		"(TN|TNM)[0-9]{2,3}/[0-9]{4}Z", // 최소기온패턴
		"(\\+|\\-|VC)*(MI|BC|PR|DR|BL|SH|TS|FZ)*(DZ|RA|SN|SG|PL|GR|GS|UP|BR|FG|FU|VA|DU|SA|HZ|PO|SQ|FC|SS|DS)*(DZ|RA|SN|SG|PL|GR|GS|UP|BR|FG|FU|VA|DU|SA|HZ|PO|SQ|FC|SS|DS)*", // 현천 패턴
		"(FEW|SCT|BKN|OVC)([0-9]{3})([A-Z]*)|VV([0-9]{3}|\\/\\/\\/)", // 구름 패턴
		"BECMG", // BECMG 변화군 지시어 패턴
		"TEMPO", // TEMPO 변화군 지시어 패턴
		"FM[0-9]{6}", // FM 변화군 지시어 패턴
		"PROB(30|40)", // 확률지시어 패턴
		"CAVOK", // CAVOK 패턴
		"SKC", // SKC 패턴
		"NSW", // NSW 패턴
		"NSC", // NSC 패턴
		"RMK" // END 패턴
	};
	
	public static int identifyTafToken(String tafToken) {
			
		for(int i=0 ; i<tafTokenRegexes.length ; i++) {
			
			if(tafToken.matches(tafTokenRegexes[i])) {								
				return i;
			}
		}
		
		return -1;
	}

	public static Date parsePartialDate(Date stdTm, String tafToken, int parseLen) throws TafParseException {
		
		String dateFormatStr = "yyyyMMddHHmm";
		
		Date tm = null;
		
		try {
			
			String tmStr = new SimpleDateFormat("yyyyMM").format(stdTm) + tafToken.replaceAll("(.*)([0-9]{" + parseLen + "})(.*)", "$2"); 
			
			tm = new SimpleDateFormat(dateFormatStr.substring(0, 6 + parseLen)).parse(tmStr);
			
			// 기준일시보다 작아진다면 달을 1더해준다
			if(tm.getTime() < stdTm.getTime()) {
				
				Calendar cal = new GregorianCalendar();
				cal.setTime(tm);;
				cal.add(Calendar.MONTH, 1);
				tm = cal.getTime();
			}
			
		} catch (Exception e) {
			
			throw new TafParseException();
		}
		
		return tm;		
	}
	
	public static List<Date> parseTmList(Date stdTm, String tafToken) throws TafParseException {
		
		List<Date> tmList = new ArrayList<Date>();
		
		Calendar cal = new GregorianCalendar();
		
		try {
			
			String ddHH1 = tafToken.split("\\/")[0];
			String ddHH2 = tafToken.split("\\/")[1];
				
			String stTmStr = new SimpleDateFormat("yyyyMM").format(stdTm) + ddHH1;
			String edTmStr = new SimpleDateFormat("yyyyMM").format(stdTm) + ddHH2;
			
			Date stTm = new SimpleDateFormat("yyyyMMddHH").parse(stTmStr);
			Date edTm = new SimpleDateFormat("yyyyMMddHH").parse(edTmStr);
				
			if(stdTm.getTime() - stTm.getTime() > 7 * 24 * 60 * 60 * 1000) {
				cal.setTime(new Date(stTm.getTime()));
				cal.add(Calendar.MONTH, 1);
				stTm = cal.getTime();
				cal.setTime(new Date(edTm.getTime()));
				cal.add(Calendar.MONTH, 1);
				edTm = cal.getTime();
			}
			
			if(Integer.valueOf(ddHH1) > Integer.valueOf(ddHH2)) {
				cal.setTime(new Date(edTm.getTime()));
				cal.add(Calendar.MONTH, 1);
				edTm = cal.getTime();
			}
	
			cal.setTime(stTm);
			
			while(cal.getTime().getTime() <= edTm.getTime()) {
				
				tmList.add(cal.getTime());
				
				cal.add(Calendar.MINUTE, 1);
			}		
			
		} catch (Exception e) {
			
			throw new TafParseException();
		}
		
		return tmList;
	}
	
	public static Map<String, Object> parseWind(String tafToken) throws TafParseException {
		
		Map<String, Object> windMap = new HashMap<String, Object>();
		
		try {
			
			Boolean vrb = false;
			Double wdir = null;
			
			if(tafToken.startsWith("VRB")) {	
				vrb = true;								
			} else {
				wdir = Double.valueOf(tafToken.substring(0, 3));
			}
			
			Double wspd = Double.valueOf(tafToken.substring(3, 5));
			Double maxWspd = null;
			
			if(tafToken.contains("G")) {				
				maxWspd = Double.valueOf(tafToken.substring(6, 8));				
			}
			
			windMap.put("vrb", vrb);
			windMap.put("wdir", wdir);
			
			if(tafToken.contains("MPS")) {
				
				windMap.put("wspd", Math.round((wspd*1.94384f) * 10) / 10.0);
				
				if(maxWspd != null) {
					windMap.put("maxWspd", Math.round((maxWspd*1.94384f) * 10) / 10.0);	
				}
				
			} else {
				
				windMap.put("wspd", wspd);
				
				if(maxWspd != null) {
					windMap.put("maxWspd", maxWspd);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new TafParseException();
		}
		
		return windMap;		
	}
	
	public static Double parseVis(String tafToken) throws TafParseException {
		
		Double vis = null;
		
		try {
			
			if(tafToken.matches("P*[0-9]+SM")) {
				
				if("P6SM".equals(tafToken)) {
					vis = 9999.0;
				} else {
					vis = Double.valueOf(tafToken.replaceAll("P","").replaceAll("SM", "")) * 1609;
				}
				
			} else if(tafToken.matches("[0-9]+\\/[0-9]+SM")) {
				
				String[] args = tafToken.replaceAll("P","").replaceAll("SM", "").split("\\/");
				
				float a = Float.valueOf(args[0]);
				float b = Float.valueOf(args[1]);
				
				vis = (double)Math.round((a/b)*1609);
				
			} else {
				vis = Double.valueOf(tafToken);
			}
			
			if(vis > 9999) {
				vis = 9999.0;
			}
			
		} catch (Exception e) {
			throw new TafParseException();
		}
		
		return vis;		
	}
	
	public static Double parseTemperature(String tafToken) throws TafParseException {
		
		Double temperature = null;
		
		try {
			
			temperature = Double.valueOf(tafToken.split("\\/")[0].replaceAll("([A-Z]+)([0-9]+)", "$2"));
			
			if(tafToken.contains("M")) {
				temperature *= -1;
			}
			
		} catch (Exception e) {
			
			throw new TafParseException();
		}
		
		return temperature;		
	}
}
