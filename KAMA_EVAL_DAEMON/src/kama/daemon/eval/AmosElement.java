package kama.daemon.eval;

import java.util.Date;

public class AmosElement {
	
	private Date tafTm;
	
	// 기온 평가용 AMOS 평균 최저기온
	private Double tn;
	
	// 기온 평가용 AMOS 평균 최대기온
	private Double tx;

	public Date getTafTm() {
		return tafTm;
	}

	public void setTafTm(Date tafTm) {
		this.tafTm = tafTm;
	}

	public Double getTn() {
		return tn;
	}

	public void setTn(Double tn) {
		this.tn = tn;
	}

	public Double getTx() {
		return tx;
	}

	public void setTx(Double tx) {
		this.tx = tx;
	}
}
