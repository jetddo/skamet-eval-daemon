package kama.daemon.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import kama.daemon.eval.lf.LfElement;
import kama.daemon.eval.metar.MetarData;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.metar.MetarParser;
import kama.daemon.eval.taf.TafElement;

public class EvaluationUtils {

	public static void printDate(Date date, String datePattern) {
		
		SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
		
		System.out.println(sdf.format(date));
	}
	
	public static void printDateList(List<Date> dateList, String datePattern) {
		
		SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
		
		for(int i=0 ; i<dateList.size() ; i++) {
			
			System.out.println(sdf.format(dateList.get(i)));
			
		}
	}
	
	public static List<Date> makeMinTmList(Date stdTm, int hours) {
		
		Calendar c = new GregorianCalendar();
		
		List<Date> minTmList = new ArrayList<Date>();
		
		c.setTime(stdTm);
		
		while(true) {
			
			Date tm = c.getTime();
			
			if(Math.abs(stdTm.getTime() - tm.getTime()) > 1000 * 60 * 60 * 2) {
				break;
			}
			
			minTmList.add(new Date(tm.getTime()));
			
			c.add(Calendar.MINUTE, 1);
		}
		
		return minTmList;
	}
	
	public static Float[] maxCloudAmountScores(Float[] cloudAmountScores1, Float[] cloudAmountScores2) {
		
		Float[] cloundAmountScores3 = new Float[3];
		
		cloundAmountScores3[0] = Math.max(cloudAmountScores1[0], cloudAmountScores2[0]);
		cloundAmountScores3[1] = Math.max(cloudAmountScores1[1], cloudAmountScores2[1]);
		cloundAmountScores3[2] = (cloundAmountScores3[0]+cloundAmountScores3[1])/2;
		
		return cloundAmountScores3;
	}
	
	public static Float getCloudAmountScore(Float cloudAmountLayer1Score, Float cloudAmountLayer2Score) {
		
		if(cloudAmountLayer1Score == -1f && cloudAmountLayer2Score >= 0) {
			return cloudAmountLayer2Score * 1;
		} else if(cloudAmountLayer1Score >= 0 && cloudAmountLayer2Score == -1f) {
			return cloudAmountLayer1Score * 1;
		} else if(cloudAmountLayer1Score >= 0 && cloudAmountLayer2Score >= 0) {
			return cloudAmountLayer1Score * 0.5f + cloudAmountLayer2Score * 0.5f;
		} else {
			return 0f;
		}
	}
	
	public static List<String> getAirportStnCdListForEvalTaf() {
		
		String[] stnCds = new String[]{
			"RKSI","RKNY","RKJY","RKSS","RKPC","RKJB","RKPU","RKPK","RKTU","RKTN","RKJJ","RKTH","RKPS","RKNW"			
		};
		
		return Arrays.asList(stnCds);
	}
	
	public static List<String> getAirportStnCdListForPostAnalTaf() {
		
		String[] stnCds = new String[]{
			"RKSI","RKNY","RKJY","RKSS","RKPC","RKJB","RKPU","RKPK","RKTU","RKTN","RKJJ","RKTH","RKPS","RKNW"			
		};
		
		return Arrays.asList(stnCds);
	}
	
	public static List<String> getAirportStnCdListForEvalLf() {
		
		String[] stnCds = new String[]{
			"RKSI","RKSS","RKPC","RKJB","RKPU","RKNY","RKJY"
		};
		
		return Arrays.asList(stnCds);
	}
	
	public static List<String> getAirportStnCdListForEvalDf() {
		
		String[] stnCds = new String[]{
			"RKSI","RKNY","RKJY","RKSS","RKPC","RKJB","RKPU","RKPK","RKTU","RKTN","RKJJ","RKTH","RKPS","RKNW"
		};
		
		return Arrays.asList(stnCds);
	}
	
	public static List<String> getAirportStnCdListForEvalWarn() {
		
		String[] stnCds = new String[]{
			"RKSI","RKSS","RKPC","RKJB","RKPU","RKNY","RKJY"
		};
		
		return Arrays.asList(stnCds);
	}
	
	public static String getAirportId(String stnCd) {
		
		String stnId = null;
		
		switch(stnCd) {
			
		case "RKSI": stnId = "113"; break;
        case "RKNY": stnId = "92"; break;
        case "RKJY": stnId = "167"; break;
        case "RKSS": stnId = "110"; break;
        case "RKPC": stnId = "182"; break;
        case "RKJB": stnId = "163"; break;
        case "RKPU": stnId = "151"; break;
        case "RKPK": stnId = "153"; break;
        case "RKTU": stnId = "128"; break;
        case "RKTN": stnId = "142"; break;
        case "RKJJ": stnId = "158"; break;
        case "RKTH": stnId = "139"; break;
        case "RKPS": stnId = "161"; break;
        case "RKNW": stnId = "118"; break;
		default: stnId = null; break;	
		}
		
		return stnId;
	}
	
	public static String getAirportCd(String stnId) {
		
		String stnCd = null;
		
		switch(stnId) {
			
		case "113": stnCd = "RKSI"; break;
        case "92": stnCd = "RKNY"; break;
        case "167": stnCd = "RKJY"; break;
        case "110": stnCd = "RKSS"; break;
        case "182": stnCd = "RKPC"; break;
        case "163": stnCd = "RKJB"; break;
        case "151": stnCd = "RKPU"; break;
        case "153": stnCd = "RKPK"; break;
        case "128": stnCd = "RKTU"; break;
        case "142": stnCd = "RKTN"; break;
        case "158": stnCd = "RKJJ"; break;
        case "139": stnCd = "RKTH"; break;
        case "161": stnCd = "RKPS"; break;
        case "118": stnCd = "RKNW"; break;
		default: stnCd = null; break;	
		}
		
		return stnCd;
	}
		
	public static Double getAirportMsa(String stnCd) {
		
		double msa = 0;
		
		switch(stnCd) {
		
		case "RKSI": msa = 3900.0; break;
        case "RKNY": msa = 5000.0; break;
        case "RKJY": msa = 5000.0; break;
        case "RKSS": msa = 4000.0; break;
        case "RKPC": msa = 5000.0; break;
        case "RKJB": msa = 3800.0; break;
        case "RKPU": msa = 5000.0; break;
        case "RKPK": msa = 5000.0; break;
        case "RKTU": msa = 4600.0; break;
        case "RKTN": msa = 5000.0; break;
        case "RKJJ": msa = 5000.0; break;
        case "RKTH": msa = 4800.0; break;
        case "RKPS": msa = 5000.0; break;
        case "RKNW": msa = 5000.0; break;
		default: msa = 5000.0; break;	
		}
		
		return Math.max(msa,  5000);
	}
	
	public static String getAirportAmosRwyDir(String stnId) {
			
		String rwyDirs = "'NIL'";
		
		switch(stnId) {
		
		case "113": rwyDirs = "'15L'"; break;
        case "92": rwyDirs = "'33'"; break;
        case "167": rwyDirs = "'17'"; break;
        case "110": rwyDirs = "'14R'"; break;
        case "182": rwyDirs = "'07'"; break;
        case "163": rwyDirs = "'01'"; break;
        case "151": rwyDirs = "'18','36'"; break;
        case "153": rwyDirs = "'36'"; break;
        case "128": rwyDirs = "'06','24'"; break;
        case "142": rwyDirs = "'13','31'"; break;
        case "158": rwyDirs = "'04','22'"; break;
        case "139": rwyDirs = "'10','28'"; break;
        case "161": rwyDirs = "'06','24'"; break;
        case "118": rwyDirs = "'03','21'"; break;
		}
		
		return rwyDirs;
	}
	
	public static List<Integer> filterLandingForcastsForRKJY() {
		
		int sHour = 22;
		int eHour = 8;
		
		List<Integer> applyHourList = new ArrayList<Integer>();
		
		int _hour = sHour;
		
		while(true) {
			
			if(_hour >= sHour || _hour <= eHour) {
				applyHourList.add(_hour);
			}
			
			if(_hour == eHour) {
				break;
			}
			
			_hour++;
			
			if(_hour >= 24) {
				_hour -= 24;
			}
		}
		
		return applyHourList;
	}
	
	// 메타관측정보의 착륙예보를 필터링
	public static void filterLandingForcasts(List<Map<String, Object>> lfInfoList, String stnCd) {
		
		int sHour = 0;
		int eHour = 24;
		
		switch(stnCd) {
		
		case "RKJB":
			sHour = 21;
			eHour = 11;
			break;
		case "RKPU":
			sHour = 21;
			eHour = 11;
			break;
		case "RKNY":
			sHour = 23;
			eHour = 7;
			break;
		case "RKJY":
			sHour = 22;
			eHour = 8;		// 25.03.30 19시 이후로 9 -> 8시로 변경
			break;
		}
		
		List<Integer> applyHourList1 = new ArrayList<Integer>();
		
		int _hour = sHour;
		
		while(true) {
			
			if(_hour >= sHour || _hour <= eHour) {
				applyHourList1.add(_hour);
			}
			
			if(_hour == eHour) {
				break;
			}
			
			_hour++;
			
			if(eHour < 24) {
				
				if(_hour >= 24) {
					_hour -= 24;
				}
			}
		}
		
		List<Integer> applyHourList2 = filterLandingForcastsForRKJY();
		
		List<Integer> applyHourList = applyHourList1;
		
		for(int i=0 ; i<lfInfoList.size() ; i++) {
			
			Map<String, Object> lfInfo = lfInfoList.get(i);
			
			String tmStr = (String)lfInfo.get("tm");
			
			if("RKJY".equals(stnCd)) {
				
				try {
					
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
					
					Date t1 = sdf.parse("202404171500");
					Date t2 = sdf.parse("202410251500");
					Date t = sdf.parse(tmStr);
					
					if(t.getTime() >= t1.getTime() && t.getTime() <= t2.getTime()) {
						applyHourList = applyHourList2;
					} else {
						applyHourList = applyHourList1;
					}
					
				} catch (Exception e) {
					
				}
			}
			
			String hourStr = tmStr.substring(8,12);
			
			int hour1 = Integer.valueOf(hourStr.substring(0,2))*100;
			int hour2 = Integer.valueOf(hourStr);
			
			// 분이 존재한다면
			if(hour2 > hour1) {
				
				int index = applyHourList.indexOf(hour1/100);
				
				// 인덱스 마지막이라면 제거함
				if(index == applyHourList.size()-1 || index < 0) {
					lfInfoList.remove(i--);
				}
				
			} else {
				
				if(applyHourList.indexOf(hour1/100) < 0) {
					lfInfoList.remove(i--);
				}
			}
		}
	}
	
	// 이륙에보정보 필터링
	public static void filterDepartureForcasts(List<Map<String, Object>> dfInfoList, String stnCd) {
		
		int sHour = 0;
		int eHour = 24;
		
		switch(stnCd) {
		
		case "RKJB":
			sHour = 00;
			eHour = 16;
			break;
		case "RKPU":
			sHour = 00;
			eHour = 16;
			break;
		case "RKNY":
			sHour = 02;
			eHour = 12;
			break;
		case "RKJY":
			sHour = 01;
			eHour = 13;		// 25.03.30 19시 이후로 14 -> 13시로 변경
			break;	
		case "RKPK":
			sHour = 23;
			eHour = 17;
			break;
		case "RKTU":
			sHour = 00;
			eHour = 16;
			break;
		case "RKTN":
			sHour = 00;
			eHour = 16;
			break;
		case "RKJJ":
			sHour = 00;
			eHour = 16;
			break;
		case "RKTH":
			sHour = 01;
			eHour = 13;
			break;
		case "RKPS":
			sHour = 00;
			eHour = 15;	
			break;	
		}
		
		List<Integer> applyHourList = new ArrayList<Integer>();
		
		int _hour = sHour;
		
		while(true) {
			
			if(_hour >= sHour || _hour <= eHour) {
				applyHourList.add(_hour);
			}
			
			if(_hour == eHour) {
				break;
			}
			
			_hour++;
			
			if(eHour < 24) {
				
				if(_hour >= 24) {
					_hour -= 24;
				}
			}
		}
		
		for(int i=0 ; i<dfInfoList.size() ; i++) {
			
			Map<String, Object> dfInfo = dfInfoList.get(i);
			
			String tmStr = (String)dfInfo.get("tmFc");
			
			String hourStr = tmStr.substring(8,12);
			
			int hour1 = Integer.valueOf(hourStr.substring(0,2))*100;
			int hour2 = Integer.valueOf(hourStr);
			
			// 분이 존재한다면
			if(hour2 > hour1) {
				
				int index = applyHourList.indexOf(hour1/100);
				
				// 인덱스 마지막이라면 제거함
				if(index == applyHourList.size()-1 || index < 0) {
					dfInfoList.remove(i--);
				}
				
			} else {
				
				if(applyHourList.indexOf(hour1/100) < 0) {
					dfInfoList.remove(i--);
				}
			}
		}
	}
	
	// 공항예보의 메타관측정보를 필터링
	public static void filterAirportForcasts(List<Map<String, Object>> metarInfoList, String stnCd) {
			
		int sHour = 0;
		int eHour = 24;
		
		switch(stnCd) {
		
		case "RKJB":
			sHour = 21;
			eHour = 13;
			break;
		case "RKPU":
			sHour = 21;
			eHour = 13;
			break;
		case "RKNY":
			sHour = 23;
			eHour = 9;
			break;
		case "RKJY":
			sHour = 22;
			eHour = 11;
			break;
		}
		
		List<Integer> applyHourList = new ArrayList<Integer>();
		
		int _hour = sHour;
		
		while(true) {
			
			if(_hour >= sHour || _hour <= eHour) {
				applyHourList.add(_hour);
			}
			
			if(_hour == eHour) {
				break;
			}
			
			_hour++;
			
			if(eHour < 24) {
				
				if(_hour >= 24) {
					_hour -= 24;
				}
			}
		}
		
		for(int i=0 ; i<metarInfoList.size() ; i++) {
			
			Map<String, Object> metarInfo = metarInfoList.get(i);
			
			String tmStr = (String)metarInfo.get("tm");
			
			String hourStr = tmStr.substring(8,12);
			
			int hour1 = Integer.valueOf(hourStr.substring(0,2))*100;
			int hour2 = Integer.valueOf(hourStr);
			
			// 분이 존재한다면
			if(hour2 > hour1) {
				
				int index = applyHourList.indexOf(hour1/100);
				
				// 인덱스 마지막이라면 제거함
				if(index == applyHourList.size()-1 || index < 0) {
					metarInfoList.remove(i--);
				}
				
			} else {
				
				if(applyHourList.indexOf(hour1/100) < 0) {
					metarInfoList.remove(i--);
				}
			}
		}
	}
	
	public static String getLowestBknOvcCloudHeight(TafElement element) {
		
		boolean cavok = element.isCavok();
		boolean skc = element.isSkc();
		boolean nsc = element.isNsc();
		
		List<String> cloudAmountLayer1List = element.getCloudAmountLayer1List();
		List<String> cloudAmountLayer2List = element.getCloudAmountLayer2List();		
		List<Double> cloudHeightLayer1List = element.getCloudHeightLayer1List();
		List<Double> cloudHeightLayer2List = element.getCloudHeightLayer2List();
		
		double lowestCloudHeight = 99999;
		String lowestCloudAmount = "";
		
		for(int i=0 ; i<cloudAmountLayer1List.size() ; i++) {
			
			String cloudAmountLayer1 = cloudAmountLayer1List.get(i);
			double cloudHeightLayer1 = cloudHeightLayer1List.get(i);
			
			if("BKN".equals(cloudAmountLayer1) || "OVC".equals(cloudAmountLayer1)) {
				
				if(lowestCloudHeight > cloudHeightLayer1) {
					lowestCloudHeight = cloudHeightLayer1;
					lowestCloudAmount = cloudAmountLayer1;
				}
			}			
		}
		
		for(int i=0 ; i<cloudAmountLayer2List.size() ; i++) {
			
			String cloudAmountLayer2 = cloudAmountLayer2List.get(i);
			double cloudHeightLayer2 = cloudHeightLayer2List.get(i);
			
			if("BKN".equals(cloudAmountLayer2) || "OVC".equals(cloudAmountLayer2)) {
				
				if(lowestCloudHeight > cloudHeightLayer2) {
					lowestCloudHeight = cloudHeightLayer2;
					lowestCloudAmount = cloudAmountLayer2;
				}
			}			
		}
		
		if(cavok) {
			return "CAVOK";
		} else if(skc || nsc) {
			return "NSC";
		} else {
			return "".equals(lowestCloudAmount) ? "" : (int)lowestCloudHeight + "";
		}
	}
	
	public static String getLowestBknOvcCloudHeight(MetarElement element) {
		
		boolean cavok = element.isCavok();
		boolean skc = element.isSkc();
		boolean nsc = element.isNsc();
		
		List<String> cloudAmountLayer1List = element.getCloudAmountLayer1List();
		List<String> cloudAmountLayer2List = element.getCloudAmountLayer2List();		
		List<Double> cloudHeightLayer1List = element.getCloudHeightLayer1List();
		List<Double> cloudHeightLayer2List = element.getCloudHeightLayer2List();
		
		double lowestCloudHeight = 99999;
		String lowestCloudAmount = "";
		
		for(int i=0 ; i<cloudAmountLayer1List.size() ; i++) {
			
			String cloudAmountLayer1 = cloudAmountLayer1List.get(i);
			double cloudHeightLayer1 = cloudHeightLayer1List.get(i);
			
			if("BKN".equals(cloudAmountLayer1) || "OVC".equals(cloudAmountLayer1)) {
				
				if(lowestCloudHeight > cloudHeightLayer1) {
					lowestCloudHeight = cloudHeightLayer1;
					lowestCloudAmount = cloudAmountLayer1;
				}
			}			
		}
		
		for(int i=0 ; i<cloudAmountLayer2List.size() ; i++) {
			
			String cloudAmountLayer2 = cloudAmountLayer2List.get(i);
			double cloudHeightLayer2 = cloudHeightLayer2List.get(i);
			
			if("BKN".equals(cloudAmountLayer2) || "OVC".equals(cloudAmountLayer2)) {
				
				if(lowestCloudHeight > cloudHeightLayer2) {
					lowestCloudHeight = cloudHeightLayer2;
					lowestCloudAmount = cloudAmountLayer2;
				}
			}			
		}
		
		if(cavok) {
			return "CAVOK";
		} else if(skc || nsc) {
			return "NSC";
		} else {
			return "".equals(lowestCloudAmount) ? "" : (int)lowestCloudHeight + "";
		}
	}
	
	public static String getLowestBknOvcCloudHeight(LfElement element) {
		
		boolean cavok = element.isCavok();
		boolean skc = element.isSkc();
		boolean nsc = element.isNsc();
		
		List<String> cloudAmountLayer1List = element.getCloudAmountLayer1List();
		List<String> cloudAmountLayer2List = element.getCloudAmountLayer2List();		
		List<Double> cloudHeightLayer1List = element.getCloudHeightLayer1List();
		List<Double> cloudHeightLayer2List = element.getCloudHeightLayer2List();
		
		double lowestCloudHeight = 99999;
		String lowestCloudAmount = "";
		
		for(int i=0 ; i<cloudAmountLayer1List.size() ; i++) {
			
			String cloudAmountLayer1 = cloudAmountLayer1List.get(i);
			double cloudHeightLayer1 = cloudHeightLayer1List.get(i);
			
			if("BKN".equals(cloudAmountLayer1) || "OVC".equals(cloudAmountLayer1)) {
				
				if(lowestCloudHeight > cloudHeightLayer1) {
					lowestCloudHeight = cloudHeightLayer1;
					lowestCloudAmount = cloudAmountLayer1;
				}
			}			
		}
		
		for(int i=0 ; i<cloudAmountLayer2List.size() ; i++) {
			
			String cloudAmountLayer2 = cloudAmountLayer2List.get(i);
			double cloudHeightLayer2 = cloudHeightLayer2List.get(i);
			
			if("BKN".equals(cloudAmountLayer2) || "OVC".equals(cloudAmountLayer2)) {
				
				if(lowestCloudHeight > cloudHeightLayer2) {
					lowestCloudHeight = cloudHeightLayer2;
					lowestCloudAmount = cloudAmountLayer2;
				}
			}			
		}
		
		if(cavok) {
			return "CAVOK";
		} else if(skc || nsc) {
			return "NSC";
		} else {
			return "".equals(lowestCloudAmount) ? "" : (int)lowestCloudHeight + "";
		}
	}
	
	public static String getCloudInfoJoinedString(List<String> cloudAmountList, List<Double> cloudHeightList) {
		
		if(cloudAmountList.size() != cloudHeightList.size()) {
			return "";
		}
		
		String joinString = "";
		
		for(int i=0 ; i<cloudAmountList.size() ; i++) {
			
			String cloudAmount = cloudAmountList.get(i);
			Double cloudHeight = cloudHeightList.get(i);
			
			
			if(i < cloudAmountList.size()-1) {		
				
				joinString += cloudAmount + String.format("%03d", (int)(cloudHeight/100))  + "\n";
				
			} else {				
				joinString += cloudAmount + String.format("%03d", (int)(cloudHeight/100));
			}			
		}
		
		return joinString;
	}
	
	public static String createTafEvaluationId(String seq) {
		
		return String.format("EVAL_%015d", Integer.valueOf(seq));
	}
	
	public static String createTafPostAnalId(String seq) {
		
		return String.format("POST_ANAL_%010d", Integer.valueOf(seq));
	}
	
	public static String createLfEvaluationId(String seq) {
		
		return String.format("EVAL_%015d", Integer.valueOf(seq));
	}
	
	public static String createDfEvaluationId(String seq) {
		
		return String.format("EVAL_%015d", Integer.valueOf(seq));
	}
	
	public static String createWarnEvaluationId(String seq) {
		
		return String.format("EVAL_%015d", Integer.valueOf(seq));
	}
	
	public static int compareSfcWspdRatio(String val1, String val2, Double wspd, Double maxWspd) {
		
		String[] vals = new String[] {
			val1, val2	
		};
		
		double[] ratios = new double[2];
		
		for(int i=0 ; i<vals.length ; i++) {
			
			String val = vals[i];
			
			double ratio = 0d;
			
			if(val.startsWith("G")) {				
				ratio = Double.valueOf(val.substring(1)) / maxWspd * 100;
			} else {
				ratio = Double.valueOf(val) / maxWspd * 100;
			}			
			
			ratios[i] = ratio;
		}
		
		if(ratios[0] > ratios[1]) {
			return 1;
		} else {
			return 2;
		}
	}
	
	public static Double findAmosMaxValue(List<Map<String, Object>> subAmosDataList, String valueKey) {
		
		Double maxValue = Double.MIN_VALUE;
		
		for(int i=0 ; i<subAmosDataList.size() ; i++) {
			
			Map<String, Object> amosData = subAmosDataList.get(i);
			
			if(amosData.get(valueKey) == null) {
				continue;
			}
			
			Double value = Double.valueOf(amosData.get(valueKey).toString());		
			
			maxValue = Math.max(maxValue, value);
		}
		
		return maxValue;
	}
	
	public static Double findAmosMinValue(List<Map<String, Object>> subAmosDataList, String valueKey) {
		
		Double minValue = Double.MAX_VALUE;
		
		for(int i=0 ; i<subAmosDataList.size() ; i++) {
			
			Map<String, Object> amosData = subAmosDataList.get(i);
			
			if(amosData.get(valueKey) == null) {
				continue;
			}
			
			Double value = Double.valueOf(amosData.get(valueKey).toString());		
			
			minValue = Math.min(minValue, value);
		}
		
		return minValue;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> splitAmosDatabyTm(List<Map<String, Object>> amosDataList) {
		
		List<Map<String, Object>> splitAmosDataList = new ArrayList<Map<String, Object>>();
		
		Map<String, Object> amosDataMap = new HashMap<String, Object>();
		
		for(int i=0 ; i<amosDataList.size() ; i++) {
			
			Map<String, Object> amosData = amosDataList.get(i);
			
			String tm = (String)amosData.get("tm");
			
			List<Map<String, Object>> subAmosDataList = null;
			
			if(amosDataMap.get(tm) == null) {
				
				subAmosDataList = new ArrayList<Map<String, Object>>();
				amosDataMap.put(tm, subAmosDataList);				
				
			} else {
				subAmosDataList = (List<Map<String, Object>>)amosDataMap.get(tm);
			}
			
			subAmosDataList.add(amosData);
		}
		
		Iterator<String> iter = amosDataMap.keySet().iterator();
		
		while(iter.hasNext()) {
			
			List<Map<String, Object>> subAmosDataList = (List<Map<String, Object>>)amosDataMap.get(iter.next());
			
			String tm = null;
			
			if(subAmosDataList.size() > 0) {
				
				tm = subAmosDataList.get(0).get("tm").toString();
				
				Map<String, Object> map = new HashMap<String, Object>();
				
				map.put("tm", tm);
				map.put("list", subAmosDataList);
				
				splitAmosDataList.add(map);
			}
		}
		
		Collections.sort(splitAmosDataList, new Comparator() {

			@Override
			public int compare(Object arg0, Object arg1) {
				
				String tm0 = ((Map<String, Object>)arg0).get("tm").toString();
				String tm1 = ((Map<String, Object>)arg1).get("tm").toString();
				
				return tm0.compareTo(tm1);
			}
		});
		
		return splitAmosDataList;
	}
	
	public static List<Map<String, Object>> combineMetarLocalList(MetarParser metarParser, String stnCd, List<Map<String, Object>> metarInfoList, List<Map<String, Object>> localInfoList) throws Exception {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		List<Map<String, Object>> obsInfoList = new ArrayList<Map<String, Object>>();
		
		for(int i=0 ; i<metarInfoList.size() ; i++) {
			
			Map<String, Object> obsInfo = new HashMap<String, Object>();
			
			Map<String, Object> metarInfo = metarInfoList.get(i);
			
			String metarString = (String)metarInfo.get("msgSrc");
			
			Date metarStdTm = sdf.parse((String)metarInfo.get("tm"));
			
			MetarData metarData = metarParser.parse(stnCd, metarString, metarStdTm);
			
			if(!metarData.isAvailable()) {
				continue;
			}
			
			MetarElement metarElement = metarData.getMetarElement();
			
			obsInfo.put("type", "METAR");
			
			obsInfo.put("tm", sdf.format(metarElement.getMetarTm()));
			
			List<String> skyConditionList = Arrays.asList(metarElement.getSkyCondition().split("\\s+"));
			
			obsInfo.put("skyCondition", metarElement.getSkyCondition());
			obsInfo.put("skyConditionList", skyConditionList);
			
			Double metarVis = metarElement.isCavok() ? 9999d : metarElement.getVis();
			
			obsInfo.put("vis", metarVis);
			
			String cbString = "";
			
			for(String token : metarString.split("\\s+")) {
				
				if(token.matches("(SCT|FEW|BKN|OVC)([0-9]{3})(CB)")) {
					cbString += token + " ";
				}
			}
			
			obsInfo.put("cbString", cbString.trim());
			
			Double lowestBknOvcHeight = null; 
			
			try {
				
				lowestBknOvcHeight = Double.valueOf(EvaluationUtils.getLowestBknOvcCloudHeight(metarElement));
				
			} catch (Exception e) {}
			
			obsInfoList.add(obsInfo);
			
			if(lowestBknOvcHeight == null) {
				continue;
			}
			
			if(lowestBknOvcHeight != null) {
				obsInfo.put("lowestBknOvcHeight", lowestBknOvcHeight);	
			}
		}
		
		for(int i=0 ; i<localInfoList.size() ; i++) {
			
			Map<String, Object> obsInfo = new HashMap<String, Object>();
			
			Map<String, Object> localInfo = localInfoList.get(i);
			
			String localString = (String)localInfo.get("msgText");
			
			Date localStdTm = sdf.parse((String)localInfo.get("tm"));
			
			obsInfo.put("type", "LOCAL");
			
			obsInfo.put("tm", sdf.format(localStdTm));
			
			String mtph = (String)localInfo.get("mtph");
			
			obsInfo.put("skyCondition", mtph == null ? "" : mtph);
			
			if(mtph == null) {
				obsInfo.put("skyConditionList", new ArrayList<String>());
			} else {
				obsInfo.put("skyConditionList", Arrays.asList((mtph).split("\\s+")));
			}
			
			if(localInfo.get("vis") != null) {
				
				Double localVis = Double.valueOf((String)localInfo.get("vis"));
				
				obsInfo.put("vis", localVis);
			}
			
			String cbString = "";
			
			localString = localString.replaceAll("RMK(.+)", "");
			
			for(String token : localString.split("\\s+")) {
				
				if(token.matches("(CB)|(SCT|FEW|BKN|OVC)([0-9]{3})(CB)")) {
					cbString += token + " ";
				}
			}
			
			obsInfo.put("cbString", cbString.trim());
			
			Double lowestBknOvcHeight = getLocalLowestBknOvcCloudHeight(localString);
			
			if(lowestBknOvcHeight != null) {
				obsInfo.put("lowestBknOvcHeight", lowestBknOvcHeight);	
			}
			
			obsInfoList.add(obsInfo);
		}
		
		Collections.sort(obsInfoList, new Comparator<Map<String, Object>>(){

			@Override
			public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
				
				return ((String)arg0.get("tm")).compareTo((String)arg1.get("tm"));
			}
		});
		
		return obsInfoList;
	}
	
	public static Double getLocalLowestBknOvcCloudHeight(String msgText) {
		
		List<String> tokenList = Arrays.asList(msgText.split("\\s+"));
		
		int cldIndex = tokenList.indexOf("CLD");
		
		List<String> bknOvcList = new ArrayList<String>();
		List<Double> heightList = new ArrayList<Double>();
		
		if(cldIndex >= 0) {

			for(int i=cldIndex ; i<tokenList.size() ; i++) {
			
				try {
					
					String cloudKind = tokenList.get(i);
					
					if("BKN".equals(cloudKind) || "OVC".equals(cloudKind)) {
					
						bknOvcList.add(cloudKind);
						heightList.add(Double.valueOf(tokenList.get(i+1).replaceAll("FT", "")));
					}
					
				} catch (Exception e) {
					
				}
			}
		}
		
		if(heightList.size() > 0) {
			return heightList.get(0);
		} else {
			return null;
		}
	}
}
