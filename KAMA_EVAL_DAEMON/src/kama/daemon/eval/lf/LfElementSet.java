package kama.daemon.eval.lf;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LfElementSet {
	
	// LF 시각
	private Date lfTm;
	
	// lfTm 시각의 예보값 (BECMG 이후의 예보값은 변경 된다)
	private LfElement fcstLfElement;

	// lfTm 시각의 BECMG 값 (BECMG 이후의 예보값은 변경 된다)
	private LfElement becmgLfElement;
	
	// lfTm 시각의 TEMP 값 (TEMP 기간내의 예보값은 변경 된다)
	private LfElement tempoLfElement;
	
	// 1: 변화 이전 평가, 2: BECMG ADOPT 후 평가
	private Integer becmgEvaluateMode = 1;
	
	public String toString() {
		
		String s = "";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		if(!sdf.format(this.lfTm).endsWith("00")) {
			return "";
		}
			
		s += this.fcstLfElement.toString();
		
		if(this.becmgLfElement != null) {
			s += this.becmgLfElement.toString();	
		}
		
		if(this.tempoLfElement != null) {
			s += this.tempoLfElement.toString();	
		}
		
		return s;
	}

	public Date getLfTm() {
		return lfTm;
	}

	public void setLfTm(Date lfTm) {		
		this.lfTm = lfTm;
		this.fcstLfElement = new LfElement();
		this.fcstLfElement.setState(LfData.State.FCST);
		this.fcstLfElement.setLfTm(lfTm);
	}
	
	public void activateBecmgLf(Integer stateIdx, Integer stateStatus) {	
		this.becmgLfElement = new LfElement();
		this.becmgLfElement.setState(LfData.State.BECMG);
		this.becmgLfElement.setLfTm(this.lfTm);
		this.becmgLfElement.setStateIdx(stateIdx);
		this.becmgLfElement.setStateStatus(stateStatus);
	}
	
	public void activateTempoLf(Integer stateIdx, Integer stateStatus) {		
		this.tempoLfElement = new LfElement();
		this.tempoLfElement.setState(LfData.State.TEMPO);
		this.tempoLfElement.setLfTm(this.lfTm);
		this.tempoLfElement.setStateIdx(stateIdx);
		this.tempoLfElement.setStateStatus(stateStatus);
	}
	
	public LfElement getStateLfElement(LfData.State state) {
			
		if(LfData.State.FCST.equals(state)) {
			return fcstLfElement;			
		} else if(LfData.State.BECMG.equals(state)) {		
			return becmgLfElement;
		} else if(LfData.State.TEMPO.equals(state)) {
			return tempoLfElement;
		} else {
			return null;
		}
	}

	public LfElement getFcstLfElement() {
		return fcstLfElement;
	}

	public void setFcstLfElement(LfElement fcstLfElement) {
		this.fcstLfElement = fcstLfElement;
	}

	public LfElement getBecmgLfElement() {
		return becmgLfElement;
	}

	public void setBecmgLfElement(LfElement becmgLfElement) {
		this.becmgLfElement = becmgLfElement;
	}

	public LfElement getTempoLfElement() {
		return tempoLfElement;
	}

	public void setTempoLfElement(LfElement tempoLfElement) {
		this.tempoLfElement = tempoLfElement;
	}

	public Integer getBecmgEvaluateMode() {
		return becmgEvaluateMode;
	}

	public void setBecmgEvaluateMode(Integer becmgEvaluateMode) {
		this.becmgEvaluateMode = becmgEvaluateMode;
	}
}
