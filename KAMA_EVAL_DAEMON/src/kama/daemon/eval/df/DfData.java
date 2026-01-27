package kama.daemon.eval.df;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DfData {
		
	private List<String> errorMsgList = new ArrayList<String>();
	
	private boolean isAvailable = true;
	
	// LF 발표 시각
	private Date anncTm;
	
	// LF 예보 시작 시각
	private Date stDfTm;
	
	// LF 예보 종료 시각
	private Date edDfTm;
	
	private String stnCd;
	
	private List<DfElementSet> dfElementSetList = new ArrayList<DfElementSet>();
			
	public List<String> getErrorMsgList() {
		return errorMsgList;
	}

	public void setErrorMsgList(List<String> errorMsgList) {
		this.errorMsgList = errorMsgList;
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	public Date getAnncTm() {
		return anncTm;
	}

	public void setAnncTm(Date anncTm) {
		this.anncTm = anncTm;
	}

	public Date getStDfTm() {
		return stDfTm;
	}

	public void setStDfTm(Date stDfTm) {
		this.stDfTm = stDfTm;
	}

	public Date getEdDfTm() {
		return edDfTm;
	}

	public void setEdDfTm(Date edDfTm) {
		this.edDfTm = edDfTm;
	}

	public String getStnCd() {
		return stnCd;
	}

	public void setStnCd(String stnCd) {
		this.stnCd = stnCd;
	}

	public List<DfElementSet> getDfElementSetList() {
		return dfElementSetList;
	}

	public void setDfElementSetList(List<DfElementSet> dfElementSetList) {
		this.dfElementSetList = dfElementSetList;
	}
	
	@Override
	public String toString() {
		
		String s = "";
		
		SimpleDateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		s += "====================================================\n";
		s += "\t\tLF HEADER\n";
		s += "====================================================\n";
		s += "\tanncTm: " + datePattern.format(this.anncTm) + "\n";
		s += "\tstLfTm: " + datePattern.format(this.stDfTm) + "\n";
		s += "\tedLfTm: " + datePattern.format(this.edDfTm) + "\n";		
		s += "====================================================\n";
		s += "\n";
		s += "====================================================\n";
		s += "\t\tLF CONTENTS\n";		
		s += "====================================================\n";
			
		for(int i=0 ; i<this.dfElementSetList.size() ; i++) {			
			s += this.dfElementSetList.get(i).toString();			
		}
		
		s += "====================================================\n";
		
		return s;
	}
	
	public void printErrorMsgList() {
		
		for(int i=0 ; i<this.errorMsgList.size() ; i++) {
			System.out.println(this.errorMsgList.get(i));
		}
	}

	public void importDfInfoList(List<Map<String, Object>> dfInfoList) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		for(int i=0 ; i<dfInfoList.size() ; i++) {
			
			Map<String, Object> dfInfo = dfInfoList.get(i);
						
			try {
				
				String stnCd = dfInfo.get("stnCd").toString();
				
				if(this.stnCd == null) {
					this.stnCd = stnCd;
				}
				
				Date anncTm = sdf.parse(dfInfo.get("tm").toString());
				
				if(this.anncTm == null) {
					this.anncTm = anncTm;
				}
				
				Date dfTm = sdf.parse(dfInfo.get("tmFc").toString());
				
				if(i == 0) {
					this.stDfTm = dfTm;
				}
				
				if(i == dfInfoList.size()-1) {
					this.edDfTm = dfTm;
				}
				
				Double wdir = Double.parseDouble(dfInfo.get("wd").toString());
				Double wspd = Double.parseDouble(dfInfo.get("wspd").toString());
				Double temp = Double.parseDouble(dfInfo.get("temp").toString());
				Double qnh = Double.parseDouble(dfInfo.get("qnh").toString());
								
				DfElementSet dfElementSet = new DfElementSet();
				dfElementSet.setDfTm(dfTm);
				
				DfElement dfElement = new DfElement();
				dfElement.setDfTm(dfTm);
				dfElement.setWdir(wdir);
				dfElement.setWspd(wspd);
				dfElement.setTemp(temp);
				dfElement.setQnh(qnh);
				
				dfElementSet.setDfElement(dfElement);
				
				this.dfElementSetList.add(dfElementSet);
				
			} catch (Exception e) {
				e.printStackTrace();
				this.isAvailable = false;
			}
		}
	}
}