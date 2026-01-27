package kama.daemon.eval.taf;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TafElementSet {
	
	// TAF 시각
	private Date tafTm;
	
	// tafTm 시각의 예보값 (BECMG 이후의 예보값은 변경 된다)
	private TafElement fcstTafElement;

	// tafTm 시각의 BECMG 값 (BECMG 이후의 예보값은 변경 된다)
	private TafElement becmgTafElement;
	
	// tafTm 시각의 TEMP 값 (TEMP 기간내의 예보값은 변경 된다)
	private TafElement tempoTafElement;
	
	// tafTm 시각의 FM 값 (FM 시각 이후의 예보값은 변경 된다)
	private TafElement fmTafElement;
	
	public String toString() {
		
		String s = "";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			
		s += this.fcstTafElement.toString();
		
		if(this.becmgTafElement != null) {
			s += this.becmgTafElement.toString();	
		}
		
		if(this.tempoTafElement != null) {
			s += this.tempoTafElement.toString();	
		}
		
		return s;
	}

	public Date getTafTm() {
		return tafTm;
	}

	public void setTafTm(Date tafTm) {		
		this.tafTm = tafTm;
		this.fcstTafElement = new TafElement();
		this.fcstTafElement.setState(TafData.State.FCST);
		this.fcstTafElement.setTafTm(tafTm);
	}
	
	public void activateBecmgTaf(Integer stateIdx, Integer stateStatus) {	
		this.becmgTafElement = new TafElement();
		this.becmgTafElement.setState(TafData.State.BECMG);
		this.becmgTafElement.setTafTm(this.tafTm);
		this.becmgTafElement.setStateIdx(stateIdx);
		this.becmgTafElement.setStateStatus(stateStatus);
	}
	
	public void activateTempoTaf(Integer stateIdx, Integer stateStatus) {		
		this.tempoTafElement = new TafElement();
		this.tempoTafElement.setState(TafData.State.TEMPO);
		this.tempoTafElement.setTafTm(this.tafTm);
		this.tempoTafElement.setStateIdx(stateIdx);
		this.tempoTafElement.setStateStatus(stateStatus);
	}
	
	public void activateFmTaf(Integer stateIdx, Integer stateStatus) {	
		this.fmTafElement = new TafElement();
		this.fmTafElement.setState(TafData.State.FM);
		this.fmTafElement.setTafTm(this.tafTm);
		this.fmTafElement.setStateIdx(stateIdx);
		this.fmTafElement.setStateStatus(stateStatus);
	}
	
	public TafElement getStateTafElement(TafData.State state) {
			
		if(TafData.State.FCST.equals(state)) {
			return fcstTafElement;			
		} else if(TafData.State.BECMG.equals(state)) {		
			return becmgTafElement;
		} else if(TafData.State.TEMPO.equals(state)) {
			return tempoTafElement;
		} else if(TafData.State.FM.equals(state)) {
			return fmTafElement;
		} else {
			return null;
		}
	}

	public TafElement getFcstTafElement() {
		return fcstTafElement;
	}

	public void setFcstTafElement(TafElement fcstTafElement) {
		this.fcstTafElement = fcstTafElement;
	}

	public TafElement getBecmgTafElement() {
		return becmgTafElement;
	}

	public void setBecmgTafElement(TafElement becmgTafElement) {
		this.becmgTafElement = becmgTafElement;
	}

	public TafElement getTempoTafElement() {
		return tempoTafElement;
	}

	public void setTempoTafElement(TafElement tempoTafElement) {
		this.tempoTafElement = tempoTafElement;
	}

	public TafElement getFmTafElement() {
		return fmTafElement;
	}

	public void setFmTafElement(TafElement fmTafElement) {
		this.fmTafElement = fmTafElement;
	}
}
