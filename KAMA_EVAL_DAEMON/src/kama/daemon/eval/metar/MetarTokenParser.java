package kama.daemon.eval.metar;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class MetarTokenParser {
	
	private static String[] metarTokenRegexes = new String[]{
		
		"[0-9]{6}Z", // 발표시각 패턴
		"(VRB[0-9]{2}|[0-9]{5})(G[0-9]{2})*(KT|MPS)", // 바람 패턴
		"[0-9]{3,4}|P*[0-9]+SM|[0-9]+\\/[0-9]+SM", // 시정패턴
		"M*[0-9]{2}/M*[0-9]{2}", // 기온 패턴
		"(\\+|\\-|VC)*(MI|BC|PR|DR|BL|SH|TS|FZ)*(DZ|RA|SN|SG|PL|GR|GS|UP|BR|FG|FU|VA|DU|SA|HZ|PO|SQ|FC|SS|DS)*(DZ|RA|SN|SG|PL|GR|GS|UP|BR|FG|FU|VA|DU|SA|HZ|PO|SQ|FC|SS|DS)*", // 현천 패턴
		"(FEW|SCT|BKN|OVC)([0-9]{3})([A-Z]*)|VV([0-9]{3}|\\/\\/\\/)", // 구름 패턴
		"Q[0-9]{4}", // 기압 패턴
		"CAVOK", // CAVOK 패턴
		"SKC", // SKC 패턴
		"NSW", // NSW 패턴
		"NSC", // NSC 패턴
		"(BECMG|TEMPO|RMK)" // END 패턴
	};
	
	public static int identifyMetarToken(String metarToken) {
			
		for(int i=0 ; i<metarTokenRegexes.length ; i++) {
			
			if(metarToken.matches(metarTokenRegexes[i])) {								
				return i;
			}
		}
		
		return -1;
	}

	public static Date parsePartialDate(Date stdTm, String metarToken, int parseLen) throws MetarParseException {
		
		String dateFormatStr = "yyyyMMddHHmm";
		
		Date tm = null;
		
		try {
			
			String tmStr = new SimpleDateFormat("yyyyMM").format(stdTm) + metarToken.replaceAll("(.*)([0-9]{" + parseLen + "})(.*)", "$2"); 
			
			tm = new SimpleDateFormat(dateFormatStr.substring(0, 6 + parseLen)).parse(tmStr);
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(tm);
			
			tm = cal.getTime();
			
		} catch (Exception e) {
			
			throw new MetarParseException();
		}
		
		return tm;		
	}
	
	public static Map<String, Object> parseWind(String metarToken) throws MetarParseException {
		
		Map<String, Object> windMap = new HashMap<String, Object>();
		
		try {
			
			Boolean vrb = false;
			Double wdir = null;
			
			if(metarToken.startsWith("VRB")) {	
				vrb = true;								
			} else {
				wdir = Double.valueOf(metarToken.substring(0, 3));
			}
			
			Double wspd = Double.valueOf(metarToken.substring(3, 5));
			Double maxWspd = null;
			
			if(metarToken.contains("G")) {				
				maxWspd = Double.valueOf(metarToken.substring(6, 8));				
			}
			
			windMap.put("vrb", vrb);
			windMap.put("wdir", wdir);
			
			if(metarToken.contains("MPS")) {
				
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
			throw new MetarParseException();
		}
		
		return windMap;		
	}
	
	public static Double parseVis(String metarToken) throws MetarParseException {
		
		Double vis = null;
		
		try {
			
			if(metarToken.matches("P*[0-9]+SM")) {
				
				if("P6SM".equals(metarToken)) {
					vis = 9999.0;
				} else {
					vis = Double.valueOf(metarToken.replaceAll("P","").replaceAll("SM", "")) * 1609;
				}
				
			} else if(metarToken.matches("[0-9]+\\/[0-9]+SM")) {
				
				String[] args = metarToken.replaceAll("P","").replaceAll("SM", "").split("\\/");
				
				float a = Float.valueOf(args[0]);
				float b = Float.valueOf(args[1]);
				
				vis = (double)Math.round((a/b)*1609);
				
			} else {
				vis = Double.valueOf(metarToken);
			}
			
			if(vis > 9999) {
				vis = 9999.0;
			}
			
		} catch (Exception e) {
			throw new MetarParseException();
		}
		
		return vis;	
	}
	
	public static Double parseQnh(String metarToken) throws MetarParseException {
		
		Double qnh = null;
		
		try {
			
			qnh = Double.parseDouble(metarToken.replaceAll("Q", ""));
			
		} catch (Exception e) {
			throw new MetarParseException();
		}
		
		return qnh;	
	}
	
	public static Map<String, Double> parseTemperature(String metarToken) throws MetarParseException {
		
		Map<String, Double> tempMap = new HashMap<String, Double>();
		
		try {
			
			metarToken = metarToken.replaceAll("M", "-");
			
			Double tx = Double.valueOf(metarToken.split("\\/")[0]);
			Double tn = Double.valueOf(metarToken.split("\\/")[1]);
			
			tempMap.put("tx", tx);
			tempMap.put("tn", tn);
			
		} catch (Exception e) {
			
			throw new MetarParseException();
		}
		
		return tempMap;		
	}
}
