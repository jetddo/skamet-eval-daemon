package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.Date;

import kama.daemon.eval.warn.WarnData;

public class WarnEvaluationData {
  
	private WarnData.Element warnType;
	
	// 경보 평가 시각 (경보 발표 시각)
	private Date evaluationTm;
	
	// 경보 유효시작시각
	private Date stEffctTm;
	
	// 경보 유효종료시각 (해제경보의 경우 미리 계산)
	private Date edEffctTm;
	
	// 최초 도달 시각
	private Date firstArrTm;
	
	// 최초 도달 현상
	private String firstArrVal;
	
	// 최종 관측 시각
	private Date lastObsTm;
		
	// 최종 현상
	private String lastObsVal;
	
	// 선행시간
	private Integer prevMin;
	
	// 최초도달-발표
	private Integer firstArrMin;
	
	private boolean isAvailable = true;

	private Score score = new Score();
	
	public class Score {
		
		// 현상 발생 점수
		private Float effct = 0f;
		
		// 선행 점수
		private Float prev = 0f;
		
		private Float total = 0f;
		
		@Override
		public String toString() {
			
			String s = "";
			
			s += "현상점수: " + this.effct;
			s += ", 선행점수: " + this.prev;
			s += ", 총점: " + this.total + "\n";
			
			return s;
		}
		
		public Float getEffct() {
			return effct;
		}

		public void setEffct(Float effct) {
			this.effct = effct;
		}
		
		public Float getPrev() {
			return prev;
		}

		public void setPrev(Float prev) {
			this.prev = prev;
		}

		public Float getTotal() {
			return total;
		}
		
		public void setTotal(Float total) {
			this.total = total;
		}
	}
		
	@Override
	public String toString() {
		
		String s = "";
		
		try {
		
			s += "--------------------------\n";
			s += "경보종류: " + this.warnType + "\n";
			s += "평가시각: " + new SimpleDateFormat("ddHHmm").format(this.evaluationTm) + "\n";
			s += "유효시작시각: " + new SimpleDateFormat("ddHHmm").format(this.stEffctTm) + "\n";
			s += "유효종료시각: " + new SimpleDateFormat("ddHHmm").format(this.edEffctTm) + "\n";
			
			if(this.firstArrTm != null) {
				
				s += "최초도달시각: " + new SimpleDateFormat("ddHHmm").format(this.firstArrTm) + "\n";
				s += "최초도달현상: " + this.firstArrVal + "\n";				
				s += "최종관측시각: " + new SimpleDateFormat("ddHHmm").format(this.lastObsTm) + "\n";
				s += "최종관측현상: " + this.lastObsVal + "\n";
				s += "유효시작-발표: " + this.prevMin + "\n";
				s += "최초도달-발표: " + this.firstArrMin + "\n";
			}
			
			s += this.score.toString();
			
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

	public Date getLastObsTm() {
		return lastObsTm;
	}

	public void setLastObsTm(Date lastObsTm) {
		this.lastObsTm = lastObsTm;
	}

	public String getLastObsVal() {
		return lastObsVal;
	}

	public void setLastObsVal(String lastObsVal) {
		this.lastObsVal = lastObsVal;
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	public Score getScore() {
		return score;
	}

	public void setScore(Score score) {
		this.score = score;
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

	public Integer getPrevMin() {
		return prevMin;
	}

	public void setPrevMin(Integer prevMin) {
		this.prevMin = prevMin;
	}

	public Integer getFirstArrMin() {
		return firstArrMin;
	}

	public void setFirstArrMin(Integer firstArrMin) {
		this.firstArrMin = firstArrMin;
	}
	
}
