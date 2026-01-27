package kama.daemon.postanal;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TafPostAnalData {
	
	private Date postAnalTm;
  
	private Date tafTm;
	private Date metarTm;
	
	private String tafState;
	
	private String tafSkyCondition;	
	private String metarSkyCondition;
	
	// 관측표 강수량
	private String metarRa;
	// 관측표 신적설
	private String metarSn;
	
	// 안개 분석결과 (1은 정상, 0은 비정상)
	private String fgAnalResult = "1";
	
	// 강수 분석결과 (1은 정상, 0은 비정상)
	private String raAnalResult = "1";
	
	private boolean isAvailable = true;
		
	@Override
	public String toString() {
		
		String s = "";
		
		try {
		
			s += "--------------------------\n";
			s += "사후분석 시각: " + new SimpleDateFormat("ddHHmm").format(this.postAnalTm);
			s += ", TAF 시각: " + new SimpleDateFormat("ddHHmm").format(this.tafTm);
			s += ", METAR 시각: " + new SimpleDateFormat("ddHHmm").format(this.metarTm) + "\n";
			
			s += "TAF 현천: " + this.tafSkyCondition;
			s += ", METAR 현천: " + this.metarSkyCondition + "\n";
			
			s += "안개 사후분석: " + ("1".equals(this.fgAnalResult) ? "정상" : "비정상") + "\n";
			s += "--------------------------\n";
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return s;
	}

	public Date getPostAnalTm() {
		return postAnalTm;
	}

	public void setPostAnalTm(Date postAnalTm) {
		this.postAnalTm = postAnalTm;
	}

	public Date getTafTm() {
		return tafTm;
	}

	public void setTafTm(Date tafTm) {
		this.tafTm = tafTm;
	}

	public Date getMetarTm() {
		return metarTm;
	}

	public void setMetarTm(Date metarTm) {
		this.metarTm = metarTm;
	}

	public String getTafSkyCondition() {
		return tafSkyCondition;
	}

	public void setTafSkyCondition(String tafSkyCondition) {
		this.tafSkyCondition = tafSkyCondition;
	}

	public String getMetarSkyCondition() {
		return metarSkyCondition;
	}

	public void setMetarSkyCondition(String metarSkyCondition) {
		this.metarSkyCondition = metarSkyCondition;
	}

	public String getMetarRa() {
		return metarRa;
	}

	public void setMetarRa(String metarRa) {
		this.metarRa = metarRa;
	}

	public String getMetarSn() {
		return metarSn;
	}

	public void setMetarSn(String metarSn) {
		this.metarSn = metarSn;
	}

	public String getFgAnalResult() {
		return fgAnalResult;
	}

	public void setFgAnalResult(String fgAnalResult) {
		this.fgAnalResult = fgAnalResult;
	}

	public String getRaAnalResult() {
		return raAnalResult;
	}

	public void setRaAnalResult(String raAnalResult) {
		this.raAnalResult = raAnalResult;
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	public String getTafState() {
		return tafState;
	}

	public void setTafState(String tafState) {
		this.tafState = tafState;
	}

}
