 package kama.daemon.eval.df;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DfElement {
		
	// LF 시각
	private Date dfTm;
	
	// 풍속
	private Double wspd;
	
	// 풍향
	private Double wdir;
	
	// 기온
	private Double temp;
	
	// QNH
	private Double qnh;
	
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		SimpleDateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			
		sb.append("==============================================================================\n");	
		sb.append("LF 시각: " + datePattern.format(this.dfTm) + "\n");
		
		if(this.getWdir() != null) {
			sb.append("풍향: " +  "\n");
		}
		
		if(this.getWspd() != null) {
			sb.append("풍속: " + this.getWspd() + "\n");	
		}
		
		if(this.getTemp() != null) {
			sb.append("기온: " + this.getTemp() + "\n");
		}
		
		if(this.getQnh() != null) {
			sb.append("기압: " + this.getQnh() + "\n");
		}		
		
		return sb.toString();
	}


	public Date getDfTm() {
		return dfTm;
	}


	public void setDfTm(Date dfTm) {
		this.dfTm = dfTm;
	}


	public Double getWspd() {
		return wspd;
	}


	public void setWspd(Double wspd) {
		this.wspd = wspd;
	}


	public Double getWdir() {
		return wdir;
	}


	public void setWdir(Double wdir) {
		this.wdir = wdir;
	}


	public Double getTemp() {
		return temp;
	}


	public void setTemp(Double temp) {
		this.temp = temp;
	}


	public Double getQnh() {
		return qnh;
	}


	public void setQnh(Double qnh) {
		this.qnh = qnh;
	}
}