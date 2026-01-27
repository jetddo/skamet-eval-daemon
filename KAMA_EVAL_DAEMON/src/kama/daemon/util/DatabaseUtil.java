package kama.daemon.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kama.daemon.eval.AmosElement;
import kama.daemon.eval.DfEvaluationData;
import kama.daemon.eval.LfEvaluationData;
import kama.daemon.eval.TafEvaluationData;
import kama.daemon.eval.df.DfElement;
import kama.daemon.eval.lf.LfData;
import kama.daemon.eval.lf.LfElement;
import kama.daemon.eval.metar.MetarElement;
import kama.daemon.eval.taf.TafData;
import kama.daemon.eval.taf.TafElement;

public class DatabaseUtil {

	/**
	 * ResultSet 으로부터 자동으로 Map 에 매핑
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Object> getResultSetData(ResultSet rs) throws SQLException {
		
		ResultSetMetaData metaData = rs.getMetaData();
		Map<String, Object> map = new HashMap<String, Object>();

	    for(int i = 1; i <= metaData.getColumnCount(); i++){
	    	
	        String columnName = metaData.getColumnLabel(i);	  
	        
	        Object columnValue = rs.getObject(columnName);
	        
	        if(columnValue == null) {
	        	continue;
	        }
	        
	        if(columnValue instanceof java.sql.Timestamp) {
	        	
	        	java.sql.Timestamp timeStamp = (java.sql.Timestamp)columnValue;
	        	
	        	map.put(columnName, timeStamp.getTime() + (timeStamp.getNanos() / 1000000));
	        	
	        } else {
	        	map.put(columnName, columnValue.toString());
	        }
	    }
	    
	    return map;
	}
	
	/**
	 * ResultSet 으로부터 자동으로 Map 에 매핑
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Object> getCamelcaseResultSetData(ResultSet rs) throws SQLException {
		
		ResultSetMetaData metaData = rs.getMetaData();
		Map<String, Object> map = new HashMap<String, Object>();

	    for(int i = 1; i <= metaData.getColumnCount(); i++){
	    	
	        String columnName = metaData.getColumnLabel(i);	  
	        
	        Object columnValue = rs.getObject(columnName);
	        
	        if(columnValue == null) {
	        	continue;
	        }
	        
	        if(columnValue instanceof java.sql.Timestamp) {
	        	
	        	java.sql.Timestamp timeStamp = (java.sql.Timestamp)columnValue;
	        	
	        	map.put(DaemonUtil.toCamelcase(columnName), timeStamp.getTime() + (timeStamp.getNanos() / 1000000));
	        	
	        } else {
	        	map.put(DaemonUtil.toCamelcase(columnName), columnValue.toString());
	        }
	    }
	    
	    return map;
	}
	
	public static List<Object> getEmptyParamList(int count) {
		
		List<Object> paramList = new ArrayList<Object>();
		
		for(int i=0 ; i<count ; i++) {
			paramList.add(null);
		}
		
		return paramList;
	}
	
	public static List<Object> getTafElementParamList(TafElement tafElement) {
		
		List<Object> paramList = new ArrayList<Object>();
			
		paramList.add(tafElement.getWdir() == null ? null : tafElement.getWdir().toString());
		paramList.add(tafElement.getWspd() == null ? null : tafElement.getWspd().toString());
		paramList.add(tafElement.getVis() == null ? null : tafElement.getVis().toString());
		paramList.add(tafElement.getSkyCondition());
		paramList.add(DaemonUtil.join(tafElement.getCbCloudLayer1List(), " "));
		paramList.add(DaemonUtil.join(tafElement.getCbCloudLayer2List(), " "));
		paramList.add(DaemonUtil.join(tafElement.getCloudAmountLayer1List(), " "));
		paramList.add(DaemonUtil.join(tafElement.getCloudAmountLayer2List(), " "));
		paramList.add(DaemonUtil.join(tafElement.getCloudHeightLayer1List(), " "));
		paramList.add(DaemonUtil.join(tafElement.getCloudHeightLayer2List(), " "));
		paramList.add(tafElement.isVrb() ? "Y" : "N");
		paramList.add(tafElement.isCavok() ? "Y" : "N");
		paramList.add(tafElement.isSkc() ? "Y" : "N");
		paramList.add(tafElement.isNsw() ? "Y" : "N");
		paramList.add(tafElement.isNsc() ? "Y" : "N");
		
		if(tafElement.getState().equals(TafData.State.FCST)) {
			
			paramList.add(tafElement.getTn());
			paramList.add(tafElement.getTx());
			
		} else {
			
			paramList.add(tafElement.getStateStatus());
			paramList.add(tafElement.getStateIdx());
		}
		
		return paramList;
	}
	
	public static List<Object> getMetarElementParamList(MetarElement metarElement) {
		
		List<Object> paramList = new ArrayList<Object>();
			
		paramList.add(metarElement.getWdir() == null ? null : metarElement.getWdir().toString());
		paramList.add(metarElement.getWspd() == null ? null : metarElement.getWspd().toString());
		paramList.add(metarElement.getVis() == null ? null : metarElement.getVis().toString());
		paramList.add(metarElement.getSkyCondition());
		paramList.add(DaemonUtil.join(metarElement.getCbCloudLayer1List(), " "));
		paramList.add(DaemonUtil.join(metarElement.getCbCloudLayer2List(), " "));
		paramList.add(DaemonUtil.join(metarElement.getCloudAmountLayer1List(), " "));
		paramList.add(DaemonUtil.join(metarElement.getCloudAmountLayer2List(), " "));
		paramList.add(DaemonUtil.join(metarElement.getCloudHeightLayer1List(), " "));
		paramList.add(DaemonUtil.join(metarElement.getCloudHeightLayer2List(), " "));
		paramList.add(metarElement.isVrb() ? "Y" : "N");
		paramList.add(metarElement.isCavok() ? "Y" : "N");
		paramList.add(metarElement.isSkc() ? "Y" : "N");
		paramList.add(metarElement.isNsw() ? "Y" : "N");
		paramList.add(metarElement.isNsc() ? "Y" : "N");
			
		return paramList;
	}
	
	public static List<Object> getMetarElementParamListForDfEvaluate(MetarElement metarElement, Map<String, Object> metarOriginInfo) {
		
		List<Object> paramList = new ArrayList<Object>();
			
		paramList.add(metarElement.getWdir() == null ? null : metarElement.getWdir().toString());
		paramList.add(metarElement.getWspd() == null ? null : metarElement.getWspd().toString());
		paramList.add(metarElement.getTx() == null ? null : metarElement.getTx().toString());
		paramList.add(metarOriginInfo == null ? null : metarOriginInfo.get("aqnh").toString());
		paramList.add(metarElement.isVrb() ? "Y" : "N");
			
		return paramList;
	}
	
	public static List<Object> getAmosElementParamList(AmosElement amosElement) {
		
		List<Object> paramList = new ArrayList<Object>();
			
		paramList.add(amosElement.getTn());
		paramList.add(amosElement.getTx());
			
		return paramList;
	}
	
	public static List<Object> getScoreParamList(TafEvaluationData.Score score) {
		
		List<Object> paramList = new ArrayList<Object>();
			
		paramList.add(score.getWindDirection());
		paramList.add(score.getWindSpeed());
		paramList.add(score.getRainOrClear());
		paramList.add(score.getVisibility());		
		paramList.add(score.getCloudAmount1());
		paramList.add(score.getCloudAmount2());
		paramList.add(score.getCloudAmount());
		paramList.add(score.getCloudHeight());
		paramList.add(score.getTemperature());
		paramList.add(score.getTemperatureVer2());
		
		if(score.isMaxTemp()) {
			paramList.add("X");	
		} else if(score.isMinTemp()) {
			paramList.add("N");
		} else {
			paramList.add(null);
		}
			
		return paramList;
	}
	
	public static String createParamQuery(int count) {
		
		String query = "(";
		
		for(int i=0 ; i<count ; i++) {
			
			if(i == count - 1) {
				query += "''{" + i + "}''";	
			} else {
				query += "''{" + i + "}'',";
			}
		}
		
		query += ")";
		
		return query;		
	}
	
	/// LF ///
	
	public static List<Object> getLfElementParamList(LfElement lfElement) {
		
		List<Object> paramList = new ArrayList<Object>();
			
		paramList.add(lfElement.getWdir() == null ? null : lfElement.getWdir().toString());
		paramList.add(lfElement.getWspd() == null ? null : lfElement.getWspd().toString());
		paramList.add(lfElement.getVis() == null ? null : lfElement.getVis().toString());
		paramList.add(lfElement.getSkyCondition());
		paramList.add(DaemonUtil.join(lfElement.getCbCloudLayer1List(), " "));
		paramList.add(DaemonUtil.join(lfElement.getCbCloudLayer2List(), " "));
		paramList.add(DaemonUtil.join(lfElement.getCloudAmountLayer1List(), " "));
		paramList.add(DaemonUtil.join(lfElement.getCloudAmountLayer2List(), " "));
		paramList.add(DaemonUtil.join(lfElement.getCloudHeightLayer1List(), " "));
		paramList.add(DaemonUtil.join(lfElement.getCloudHeightLayer2List(), " "));
		paramList.add(lfElement.isVrb() ? "Y" : "N");
		paramList.add(lfElement.isCavok() ? "Y" : "N");
		paramList.add(lfElement.isSkc() ? "Y" : "N");
		paramList.add(lfElement.isNsw() ? "Y" : "N");
		paramList.add(lfElement.isNsc() ? "Y" : "N");
		
		if(lfElement.getState().equals(LfData.State.FCST)) {
				
		} else {
			
			paramList.add(lfElement.getStateStatus());
			paramList.add(lfElement.getStateIdx());
		}
		
		return paramList;
	}
	
	public static List<Object> getDfElementParamList(DfElement dfElement) {
		
		List<Object> paramList = new ArrayList<Object>();
			
		paramList.add(dfElement.getWdir() == null ? null : dfElement.getWdir().toString());
		paramList.add(dfElement.getWspd() == null ? null : dfElement.getWspd().toString());
		paramList.add(dfElement.getTemp() == null ? null : dfElement.getTemp().toString());
		paramList.add(dfElement.getQnh() == null ? null : dfElement.getQnh().toString());
		
		return paramList;
	}
	
	public static List<Object> getScoreParamList(LfEvaluationData.Score score) {
		
		List<Object> paramList = new ArrayList<Object>();
			
		paramList.add(score.getWindDirection());
		paramList.add(score.getWindSpeed());
		paramList.add(score.getRainOrClear());
		paramList.add(score.getVisibility());		
		paramList.add(score.getCloudAmount1());
		paramList.add(score.getCloudAmount2());
		paramList.add(score.getCloudAmount());
		paramList.add(score.getCloudHeight());
			
		return paramList;
	}
	
	public static List<Object> getScoreParamList(DfEvaluationData.Score score) {
		
		List<Object> paramList = new ArrayList<Object>();
			
		paramList.add(score.getWindDirection());
		paramList.add(score.getWindSpeed());
		paramList.add(score.getTemperature());
		paramList.add(score.getQnh());
			
		return paramList;
	}
}
