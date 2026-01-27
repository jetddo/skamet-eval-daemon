package kama.daemon.eval;

import java.text.SimpleDateFormat;
import java.util.Date;

import kama.daemon.eval.lf.LfElementSet;
import kama.daemon.eval.metar.MetarElement;

public class LfEvaluationData {
  
	private Date evaluationTm;
	private Date lfTm;
	private Date metarTm;
	private LfElementSet lfElementSet;
	private MetarElement metarElement;
	
	// 평가에 쓰인 AMOS 데이터 셋
	private AmosElement amosElement;
	private boolean isAvailable = true;

	private Score score = new Score();
	
	public class Score {
		
		private Float windDirection = 0f;
		private Float windSpeed = 0f;
		private Float visibility = 0f;
		private Float rainOrClear = 0f;
		private Float cloudAmount = 0f;
		private Float cloudAmount1 = 0f;
		private Float cloudAmount2 = 0f;
		private Float cloudHeight = 0f;
		private Float temperature = -1f;
		
		// 기온 점수가 최저기온인지 최대기온인지에 대한 구분
		private boolean maxTemp = false;
		private boolean minTemp = false;
		private Float total = 0f;
		
		@Override
		public String toString() {
			
			String s = "";
			
			s += "풍향: " + this.windDirection;
			s += ", 풍속: " + this.windSpeed;
			s += ", 시정: " + this.visibility;
			s += ", 강수 유무: " + this.rainOrClear;
			s += ", 운고: " + this.cloudHeight;
			s += ", 운량 1층: " + this.cloudAmount1;
			s += ", 운량 2층: " + this.cloudAmount2;
			
			if(this.temperature >= 0) {
				s += ", 기온: " + this.temperature;	
			}
			
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

		public Float getVisibility() {
			return visibility;
		}

		public void setVisibility(Float visibility) {
			this.visibility = visibility;
		}

		public Float getRainOrClear() {
			return rainOrClear;
		}

		public void setRainOrClear(Float rainOrClear) {
			this.rainOrClear = rainOrClear;
		}

		public Float getTotal() {
			return total;
		}

		public void setTotal(Float total) {
			this.total = total;
		}

		public Float getCloudAmount1() {
			return cloudAmount1;
		}

		public void setCloudAmount1(Float cloudAmount1) {
			this.cloudAmount1 = cloudAmount1;
		}

		public Float getCloudAmount2() {
			return cloudAmount2;
		}

		public void setCloudAmount2(Float cloudAmount2) {
			this.cloudAmount2 = cloudAmount2;
		}

		public Float getCloudHeight() {
			return cloudHeight;
		}

		public void setCloudHeight(Float cloudHeight) {
			this.cloudHeight = cloudHeight;
		}

		public Float getTemperature() {
			return temperature;
		}

		public void setTemperature(Float temperature) {
			this.temperature = temperature;
		}

		public Float getCloudAmount() {
			return cloudAmount;
		}

		public void setCloudAmount(Float cloudAmount) {
			this.cloudAmount = cloudAmount;
		}
		
		public void setCloudAmounts(Float[] cloudAmounts) {
			this.cloudAmount1 = cloudAmounts[0];
			this.cloudAmount2 = cloudAmounts[1];
			this.cloudAmount = cloudAmounts[2];
		}

		public boolean isMaxTemp() {
			return maxTemp;
		}

		public void setMaxTemp(boolean maxTemp) {
			this.maxTemp = maxTemp;
		}

		public boolean isMinTemp() {
			return minTemp;
		}

		public void setMinTemp(boolean minTemp) {
			this.minTemp = minTemp;
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

	public Date getLfTm() {
		return lfTm;
	}

	public void setLfTm(Date lfTm) {
		this.lfTm = lfTm;
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

	public LfElementSet getLfElementSet() {
		return lfElementSet;
	}

	public void setLfElementSet(LfElementSet lfElementSet) {
		this.lfElementSet = lfElementSet;
	}

	public MetarElement getMetarElement() {
		return metarElement;
	}

	public void setMetarElement(MetarElement metarElement) {
		this.metarElement = metarElement;
	}

	public AmosElement getAmosElement() {
		return amosElement;
	}

	public void setAmosElement(AmosElement amosElement) {
		this.amosElement = amosElement;
	}
}
