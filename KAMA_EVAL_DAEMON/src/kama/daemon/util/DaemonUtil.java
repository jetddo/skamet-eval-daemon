package kama.daemon.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class DaemonUtil {
	
	public static boolean isWindow() {
		
		String osName = System.getProperty("os.name");

		if(osName.toLowerCase().contains("window")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 현재 시각
	 * @return
	 */
	public static Date currentDate() {
		
		return new Date(System.currentTimeMillis());
	}
	
	/**
	 * 숫자인지 아닌지..
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(Object str) {
		
		if(str == null || String.valueOf(str).length() < 1) {
			return true;
		}
		
		try {
			
			double d = Double.parseDouble(String.valueOf(str));
			
		} catch(NumberFormatException nfe) {  
			return false;  
		}
		
		return true;  
	}


	/**
	 * 날짜(String) -> 날짜(Date)
	 * @param date
	 * @param format
	 * @return
	 */
	public static String dateToStr(Date date, String format) {		
		return (new SimpleDateFormat(format)).format(date);
	}
	
	/**
	 * 날짜(Date) -> 날짜(String)
	 * @param date
	 * @param format
	 * @return
	 */
	public static String dateToStr(Date date) {		
		return (new SimpleDateFormat("yyyyMMddHH")).format(date);
	}
	
	/**
	 * 날짜(Date) -> 날짜(String)
	 * @param date
	 * @param format
	 * @return
	 */
	public static Date strToDate(String dateStr, String format) {
		
		try {
			return (new SimpleDateFormat(format)).parse(dateStr);
		} catch (ParseException e) {
			return null;
		}
	}
	
	/**
	 * 날짜(String) -> 날짜(Date)
	 * @param date
	 * @param format
	 * @return
	 */
	public static Date strToDate(String dateStr) {
		
		try {
			return (new SimpleDateFormat("yyyyMMddHH")).parse(dateStr);
		} catch (ParseException e) {
			return null;
		}
	}
    
	
    /**
     * 시간 더하기
     * @param date
     * @param hours
     * @return
     */
    public static Date addHours(Date date, int hours) {
    	
    	Calendar cal = new GregorianCalendar();
    	cal.setTime(date);
    	cal.add(Calendar.HOUR_OF_DAY, hours);
    
    	return cal.getTime();
    }
    
    public static String leftPaddingZero(int num, int padding){		
		return String.format("%0"+padding+"d", num); 
	}  
    
   
	public static String toCamelcase(String str) {
		
		String camelcase = "";
		String[] tokens = str.toLowerCase().split("_");
		
		for(String token : tokens) {
			
			if(camelcase.length() == 0) {
				
				camelcase += token;
				
			} else {
				
				camelcase += (token.charAt(0)+"").toUpperCase() + token.substring(1);
			}
		}
		
		return camelcase;		
	}

	public static String join(String delim, List<?> list) {
		
		String result = "";
		
		for(int i=0 ; i<list.size() ; i++) {
			
			Object item = list.get(i);
			
			if(i < list.size() - 1) {
				result += item.toString() + delim;
			} else {
				result += item.toString();
			}			
		}
		
		return result;
	}
	
	public static float setNumberFix(float number, int fixCount) {    	
    	return (float)(Math.round(number * Math.pow(10f, fixCount)) / Math.pow(10f, fixCount));
    }
    
    public static double setNumberFix(double number, int fixCount) {    	
    	return (Math.round(number * Math.pow(10f, fixCount)) / Math.pow(10f, fixCount));
    }
    
    public static String join(List<?> list, String delim) {
    	
    	if(list == null || list.size() == 0) {
    		return "";
    	}
    	
    	String result = "";
    	
    	for(int i=0 ; i<list.size() ; i++) {
    		
    		if(i == list.size()-1) {
    			result += list.get(i).toString();
    		} else {
    			result += list.get(i).toString() + delim;
    		}
    	}
    	
    	return result;
    }
}
