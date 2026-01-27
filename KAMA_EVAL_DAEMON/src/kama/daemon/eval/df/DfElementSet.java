package kama.daemon.eval.df;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DfElementSet {
	
	// LF 시각
	private Date dfTm;
	
	private DfElement dfElement;
	
	public String toString() {
		
		String s = "";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		if(!sdf.format(this.dfTm).endsWith("00")) {
			return "";
		}
			
		s += this.dfElement.toString();
			
		return s;
	}

	public Date getDfTm() {
		return dfTm;
	}

	public void setDfTm(Date dfTm) {		
		this.dfTm = dfTm;
		this.dfElement = new DfElement();
		this.dfElement.setDfTm(dfTm);
	}

	public DfElement getDfElement() {
		return dfElement;
	}

	public void setDfElement(DfElement dfElement) {
		this.dfElement = dfElement;
	}
}
