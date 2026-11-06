package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.Date;

import kama.daemon.eval.warn.WarnData;

public class WarnEvaluationLeadTimeData {
  
	private WarnData.Element warnType;
	
	// 경보 평가 시각 (경보 발표 시각)
	private Date evaluationTm;
	
	// 경보 유효시작시각
	private Date stEffctTm;
	
	// 경보 유효종료시각 (해제경보의 경우 미리 계산)
	private Date edEffctTm;
	
	// 경보 발표 시점 관측시간
	private Date issueObsTm;
	
	// 경보 발표 시점 관측값
	private String issueObsVal;
	
	// 최초 도달 시각
	private Date firstArrTm;
	
	// 최초 도달 현상
	private String firstArrVal;
	
	// 선행시간
	private Integer firstArrMin;
	
	private boolean isAvailable = true;
		
	@Override
	public String toString() {
		
		String s = "";
		
		try {
		
			s += "--------------------------\n";
			s += "경보종류: " + this.warnType + "\n";
			s += "평가시각: " + new SimpleDateFormat("ddHHmm").format(this.evaluationTm) + "\n";
			s += "유효시작시각: " + new SimpleDateFormat("ddHHmm").format(this.stEffctTm) + "\n";
			s += "유효종료시각: " + new SimpleDateFormat("ddHHmm").format(this.edEffctTm) + "\n";
			
			if(this.issueObsTm != null) {

			    s += "발표시점 관측시각: " + new SimpleDateFormat("ddHHmm").format(this.issueObsTm) + "\n";
			    s += "발표시점 관측값: " + this.issueObsVal + "\n";
			}
			
			if(this.firstArrTm != null) {
				
				s += "최초도달시각: " + new SimpleDateFormat("ddHHmm").format(this.firstArrTm) + "\n";
				s += "최초도달현상: " + this.firstArrVal + "\n";
				s += "선행시간(분): " + this.firstArrMin + "\n";
			}
			
			s += "--------------------------";
			
		} catch (Exception e) {
			
		}
		
		return s;
	}

	public WarnData.Element getWarnType() {
		return warnType;
	}

	public void setWarnType(WarnData.Element warnType) {
		this.warnType = warnType;
	}

	public Date getEvaluationTm() {
		return evaluationTm;
	}

	public void setEvaluationTm(Date evaluationTm) {
		this.evaluationTm = evaluationTm;
	}

	public Date getFirstArrTm() {
		return firstArrTm;
	}

	public void setFirstArrTm(Date firstArrTm) {
		this.firstArrTm = firstArrTm;
	}

	public String getFirstArrVal() {
		return firstArrVal;
	}

	public void setFirstArrVal(String firstArrVal) {
		this.firstArrVal = firstArrVal;
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	public Date getStEffctTm() {
		return stEffctTm;
	}

	public void setStEffctTm(Date stEffctTm) {
		this.stEffctTm = stEffctTm;
	}

	public Date getEdEffctTm() {
		return edEffctTm;
	}

	public void setEdEffctTm(Date edEffctTm) {
		this.edEffctTm = edEffctTm;
	}

	public Date getIssueObsTm() {
		return issueObsTm;
	}

	public void setIssueObsTm(Date issueObsTm) {
		this.issueObsTm = issueObsTm;
	}

	public String getIssueObsVal() {
		return issueObsVal;
	}

	public void setIssueObsVal(String issueObsVal) {
		this.issueObsVal = issueObsVal;
	}

	public Integer getFirstArrMin() {
		return firstArrMin;
	}

	public void setFirstArrMin(Integer firstArrMin) {
		this.firstArrMin = firstArrMin;
	}
}
