package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import kama.daemon.eval.df.DfElementSet;
import kama.daemon.eval.metar.MetarElement;

public class DfEvaluationData {
  
	private Date evaluationTm;
	private Date dfTm;
	private Date metarTm;
	private DfElementSet dfElementSet;
	private MetarElement metarElement;
	private Map<String, Object> metarOriginInfo;
	
	private boolean isAvailable = true;

	private Score score = new Score();
	
	public class Score {
		
		private Float windDirection = 0f;
		private Float windSpeed = 0f;
		private Float temperature = 0f;
		private Float qnh = 0f;
		
		private Float total = 0f;
		
		@Override
		public String toString() {
			
			String s = "";
			
			s += "풍향: " + this.windDirection;
			s += ", 풍속: " + this.windSpeed;
			s += ", 기온: " + this.temperature;
			s += ", 기압: " + this.qnh;
			
			s += ", 총점: " + this.total + "\n";
			
			return s;
		}

		public Float getWindDirection() {
			return windDirection;
		}

		public void setWindDirection(Float windDirection) {
			this.windDirection = windDirection;
		}

		public Float getWindSpeed() {
			return windSpeed;
		}

		public void setWindSpeed(Float windSpeed) {
			this.windSpeed = windSpeed;
		}

		public Float getTotal() {
			return total;
		}

		public void setTotal(Float total) {
			this.total = total;
		}

		public Float getTemperature() {
			return temperature;
		}

		public void setTemperature(Float temperature) {
			this.temperature = temperature;
		}

		public Float getQnh() {
			return qnh;
		}

		public void setQnh(Float qnh) {
			this.qnh = qnh;
		}
		
		
	}
		
	@Override
	public String toString() {
		
		String s = "";
		
		try {
		
			s += "--------------------------\n";
			s += "평가시각: " + new SimpleDateFormat("ddHHmm").format(this.evaluationTm) + ", " + this.score.toString();
			s += "--------------------------";
			
		} catch (Exception e) {
			
		}
		
		return s;
	}

	public Date getEvaluationTm() {
		return evaluationTm;
	}

	public void setEvaluationTm(Date evaluationTm) {
		this.evaluationTm = evaluationTm;
	}
	
	public Score getScore() {
		return score;
	}

	public void setScore(Score score) {
		this.score = score;
	}

	public Date getDfTm() {
		return dfTm;
	}

	public void setDfTm(Date dfTm) {
		this.dfTm = dfTm;
	}

	public Date getMetarTm() {
		return metarTm;
	}

	public void setMetarTm(Date metarTm) {
		this.metarTm = metarTm;
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	public DfElementSet getDfElementSet() {
		return dfElementSet;
	}

	public void setDfElementSet(DfElementSet dfElementSet) {
		this.dfElementSet = dfElementSet;
	}

	public MetarElement getMetarElement() {
		return metarElement;
	}

	public void setMetarElement(MetarElement metarElement) {
		this.metarElement = metarElement;
	}

	public Map<String, Object> getMetarOriginInfo() {
		return metarOriginInfo;
	}

	public void setMetarOriginInfo(Map<String, Object> metarOriginInfo) {
		this.metarOriginInfo = metarOriginInfo;
	}
}
