package kama.daemon.eval.lf;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LfTokenParser {
	
	private static String[] lfTokenRegexes = new String[]{
		
		"[0-9]{6}Z", // 발표시각 패턴
		"(VRB[0-9]{2}|[0-9]{5})(G[0-9]{2})*(KT|MPS)", // 바람 패턴
		"[0-9]{3,4}|P*[0-9]+SM|[0-9]+\\/[0-9]+SM", // 시정패턴
		"M*[0-9]{2}/M*[0-9]{2}", // 기온 패턴
		"(\\+|\\-|VC)*(MI|BC|PR|DR|BL|SH|TS|FZ)*(DZ|RA|SN|SG|PL|GR|GS|UP|BR|FG|FU|VA|DU|SA|HZ|PO|SQ|FC|SS|DS)*", // 현천 패턴
		"(FEW|SCT|BKN|OVC)([0-9]{3})([A-Z]*)|VV([0-9]{3}|\\/\\/\\/)", // 구름 패턴
		"BECMG", // BECMG 변화군 지시어 패턴
		"TEMPO", // TEMPO 변화군 지시어 패턴
		"AT[0-9]{4}", // AT 패턴
		"FM[0-9]{4}", // FM 패턴
		"TL[0-9]{4}", // TL 패턴
		"CAVOK", // CAVOK 패턴
		"SKC", // SKC 패턴
		"NSW", // NSW 패턴
		"NSC", // NSC 패턴
		"RMK" // END 패턴
	};
	
	public static int identifyLfToken(String lfToken) {
			
		for(int i=0 ; i<lfTokenRegexes.length ; i++) {
			
			if(lfToken.matches(lfTokenRegexes[i])) {								
				return i;
			}
		}
		
		return -1;
	}

	public static Date parsePartialDate(Date stdTm, String lfToken, int parseLen) throws LfParseException {
		
		String dateFormatStr = "yyyyMMddHHmm";
		
		Date tm = null;
		
		try {
			
			String tmStr = new SimpleDateFormat("yyyyMM").format(stdTm) + lfToken.replaceAll("(.*)([0-9]{" + parseLen + "})(.*)", "$2"); 
			
			tm = new SimpleDateFormat(dateFormatStr.substring(0, 6 + parseLen)).parse(tmStr);
			
		} catch (Exception e) {
			
			throw new LfParseException();
		}
		
		return tm;		
	}
	
	public static Date parsePartialDate(Date stdTm, String format, String lfToken, int parseLen) throws LfParseException {
		
		String dateFormatStr = "yyyyMMddHHmm";
		
		Date tm = null;
		
		try {
			
			String tmStr = new SimpleDateFormat(format).format(stdTm) + lfToken.replaceAll("(.*)([0-9]{" + parseLen + "})(.*)", "$2"); 
			
			tm = new SimpleDateFormat(dateFormatStr.substring(0, format.length() + parseLen)).parse(tmStr);
			
		} catch (Exception e) {
			
			throw new LfParseException();
		}
		
		return tm;		
	}
	
	public static Map<String, Object> parseWind(String lfToken) throws LfParseException {
		
		Map<String, Object> windMap = new HashMap<String, Object>();
		
		try {
			
			Boolean vrb = false;
			Double wdir = null;
			
			if(lfToken.startsWith("VRB")) {	
				vrb = true;								
			} else {
				wdir = Double.valueOf(lfToken.substring(0, 3));
			}
			
			Double wspd = Double.valueOf(lfToken.substring(3, 5));
			Double maxWspd = null;
			
			if(lfToken.contains("G")) {				
				maxWspd = Double.valueOf(lfToken.substring(6, 8));				
			}
			
			windMap.put("vrb", vrb);
			windMap.put("wdir", wdir);
			
			if(lfToken.contains("MPS")) {
				
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
			throw new LfParseException();
		}
		
		return windMap;		
	}
	
	public static Double parseVis(String lfToken) throws LfParseException {
		
		Double vis = null;
		
		try {
			
			if(lfToken.matches("P*[0-9]+SM")) {
				
				if("P6SM".equals(lfToken)) {
					vis = 9999.0;
				} else {
					vis = Double.valueOf(lfToken.replaceAll("P","").replaceAll("SM", "")) * 1609;
				}
				
			} else if(lfToken.matches("[0-9]+\\/[0-9]+SM")) {
				
				String[] args = lfToken.replaceAll("P","").replaceAll("SM", "").split("\\/");
				
				float a = Float.valueOf(args[0]);
				float b = Float.valueOf(args[1]);
				
				vis = (double)Math.round((a/b)*1609);
				
			} else {
				vis = Double.valueOf(lfToken);
			}
			
			if(vis > 9999) {
				vis = 9999.0;
			}
			
		} catch (Exception e) {
			throw new LfParseException();
		}
		
		return vis;		
	}
	
	public static Map<String, Double> parseTemperature(String lfToken) throws LfParseException {
		
		Map<String, Double> tempMap = new HashMap<String, Double>();
		
		try {
			
			lfToken = lfToken.replaceAll("M", "-");
			
			Double tx = Double.valueOf(lfToken.split("\\/")[0]);
			Double tn = Double.valueOf(lfToken.split("\\/")[1]);
			
			tempMap.put("tx", tx);
			tempMap.put("tn", tn);
			
		} catch (Exception e) {
			
			throw new LfParseException();
		}
		
		return tempMap;		
	}
}
