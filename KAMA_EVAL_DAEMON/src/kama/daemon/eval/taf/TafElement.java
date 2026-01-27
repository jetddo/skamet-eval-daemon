 package kama.daemon.eval.taf;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TafElement {
		
	// TAF 시각
	private Date tafTm;
	
	// 변화군 종류
	private TafData.State state;

	// 변화군 상태 (시작:0, 중간:1, 종료:2)
	private Integer stateStatus = null;
	
	// 변화군 번호
	private Integer stateIdx = -1;
	
	// 풍속
	private Double wspd;
	
	// 풍향
	private Double wdir;
	
	// 최대풍속
	private Double maxWspd;
	
	// 시정
	private Double vis;
	
	// 최저기온
	private Double tn;
	
	// 최대기온
	private Double tx;
	
	// VRB 가 적용되었는지
	private boolean vrb = false;
	
	// CAVOK 이 적용되었는지
	private boolean cavok = false;
	
	// SKC 가 적용되었는지
	private boolean skc = false;
	
	// NSW 가 적용되었는지
	private boolean nsw = false;
	
	// NSC 가 적용되었는지
	private boolean nsc = false;
	
	// 현천
	private String skyCondition = "";
	
	// 1층 구름 
	private List<String> cloudAmountLayer1List = new ArrayList<String>();
	private List<Double> cloudHeightLayer1List = new ArrayList<Double>();
	private List<Boolean> cbCloudLayer1List = new ArrayList<Boolean>();
	
	// 2층 구름
	private List<String> cloudAmountLayer2List = new ArrayList<String>();
	private List<Double> cloudHeightLayer2List = new ArrayList<Double>();
	private List<Boolean> cbCloudLayer2List = new ArrayList<Boolean>();
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		SimpleDateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		if(!datePattern.format(this.tafTm).endsWith("00")) {
			return "";
		}
		
		String stateStr = "";
		
		if(TafData.State.FCST.equals(this.state)) {
			stateStr = "FCST";
		} else if(TafData.State.BECMG.equals(this.state)) {
			stateStr = "BECMG";
		} else if(TafData.State.TEMPO.equals(this.state)) {
			stateStr = "TEMPO";
		} else if(TafData.State.FM.equals(this.state)) {
			stateStr = "FM";
		}
		
		sb.append("==============================================================================\n");
		sb.append("TAF 변화군 : " + stateStr + "\n");
		sb.append("TAF 시각: " + datePattern.format(this.tafTm) + "\n");
		
		if(this.isVrb() || this.getWdir() != null) {
			sb.append("풍향: " + (this.isVrb() ? "VRB" : this.getWdir()) + "\n");
		}
		
		if(this.getWspd() != null) {
			sb.append("풍속: " + this.getWspd() + "\n");	
		}
		
		if(this.maxWspd != null) {
			sb.append("최대풍속: " + this.getMaxWspd() + "\n");
		}
		
		if(this.getVis() != null) {
			sb.append("시정: " + this.getVis() + "\n");
		}
		
		if(!"".equals(this.skyCondition)) {	
			sb.append("현천: " + this.skyCondition + "\n");
		}
		
		if(this.getTx() != null) {
			sb.append("최대기온: " + this.getTx() + "\n");
		}
		
		if(this.getTn() != null) {
			sb.append("최저기온: " + this.getTn() + "\n");
		}
		
		if(this.cloudAmountLayer1List.size() > 0 || this.cloudAmountLayer2List.size() > 0) {
				
			if(this.cloudAmountLayer1List.size() > 0) {
				
				for(int i=0 ; i<this.cloudAmountLayer1List.size() ; i++) {
					sb.append("구름: 1층(" + this.cloudAmountLayer1List.get(i) + ", " + this.cloudHeightLayer1List.get(i) + "ft" + (this.cbCloudLayer1List.get(i) ? " CB " : "") + ")\n" );
				}				
			}
			
			if(this.cloudAmountLayer2List.size() > 0) {
				
				for(int i=0 ; i<this.cloudAmountLayer2List.size() ; i++) {
					sb.append("구름: 2층(" + this.cloudAmountLayer2List.get(i) + ", " + this.cloudHeightLayer2List.get(i) + "ft" + (this.cbCloudLayer2List.get(i) ? " CB " : "") + ")\n");
				}				
			}
		}
		
		if(this.isCavok()) {
			sb.append("CAVOK: " + this.isCavok() + "\n");
		}
		
		if(this.isNsw()) {
			sb.append("NSW: " + this.isNsw() + "\n");
		}
		
		if(this.isSkc()) {
			sb.append("SKC: " + this.isSkc() + "\n");
		}
		
		if(this.isNsc()) {
			sb.append("NSC: " + this.isNsc() + "\n");
		}
		
		return sb.toString();
	}
	
	public TafElement makeClone() {
		
		TafElement tafElement = new TafElement();
		
		tafElement.setTafTm(new Date(this.getTafTm().getTime()));
		tafElement.setWspd(this.getWspd());
		tafElement.setWdir(this.getWdir());
		tafElement.setMaxWspd(this.getMaxWspd());
		tafElement.setVis(this.getVis());
		tafElement.setTn(this.getTn());
		tafElement.setTx(this.getTx());
		tafElement.setState(this.getState());
		tafElement.setVrb(this.isVrb());
		tafElement.setCavok(this.isCavok());
		tafElement.setSkc(this.isSkc());
		tafElement.setNsw(this.isNsw());
		tafElement.setNsc(this.isNsc());
		tafElement.setSkyCondition(this.getSkyCondition());
		
		tafElement.setCloudAmountLayer1List(new ArrayList<String>(this.getCloudAmountLayer1List()));
		tafElement.setCloudHeightLayer1List(new ArrayList<Double>(this.getCloudHeightLayer1List()));
		tafElement.setCbCloudLayer1List(new ArrayList<Boolean>(this.getCbCloudLayer1List()));
		
		tafElement.setCloudAmountLayer2List(new ArrayList<String>(this.getCloudAmountLayer2List()));
		tafElement.setCloudHeightLayer2List(new ArrayList<Double>(this.getCloudHeightLayer2List()));
		tafElement.setCbCloudLayer2List(new ArrayList<Boolean>(this.getCbCloudLayer2List()));
		
		return tafElement;
	}

	public Date getTafTm() {
		return tafTm;
	}

	public void setTafTm(Date tafTm) {
		this.tafTm = tafTm;
	}

	public TafData.State getState() {
		return state;
	}

	public void setState(TafData.State state) {
		this.state = state;
	}

	public Integer getStateIdx() {
		return stateIdx;
	}

	public void setStateIdx(Integer stateIdx) {
		this.stateIdx = stateIdx;
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

	public Double getMaxWspd() {
		return maxWspd;
	}

	public void setMaxWspd(Double maxWspd) {
		this.maxWspd = maxWspd;
	}

	public Double getVis() {
		return vis;
	}

	public void setVis(Double vis) {
		this.vis = vis;
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

	public boolean isVrb() {
		return vrb;
	}

	public void setVrb(boolean vrb) {
		this.vrb = vrb;
	}

	public boolean isCavok() {
		return cavok;
	}

	public void setCavok(boolean cavok) {
		this.cavok = cavok;
	}

	public boolean isSkc() {
		return skc;
	}

	public void setSkc(boolean skc) {
		this.skc = skc;
	}

	public boolean isNsw() {
		return nsw;
	}

	public void setNsw(boolean nsw) {
		this.nsw = nsw;
	}

	public boolean isNsc() {
		return nsc;
	}

	public void setNsc(boolean nsc) {
		this.nsc = nsc;
	}

	public String getSkyCondition() {
		return skyCondition;
	}

	public void setSkyCondition(String skyCondition) {
		this.skyCondition = skyCondition;
	}

	public List<String> getCloudAmountLayer1List() {
		return cloudAmountLayer1List;
	}

	public void setCloudAmountLayer1List(List<String> cloudAmountLayer1List) {
		this.cloudAmountLayer1List = cloudAmountLayer1List;
	}

	public List<Double> getCloudHeightLayer1List() {
		return cloudHeightLayer1List;
	}

	public void setCloudHeightLayer1List(List<Double> cloudHeightLayer1List) {
		this.cloudHeightLayer1List = cloudHeightLayer1List;
	}

	public List<Boolean> getCbCloudLayer1List() {
		return cbCloudLayer1List;
	}

	public void setCbCloudLayer1List(List<Boolean> cbCloudLayer1List) {
		this.cbCloudLayer1List = cbCloudLayer1List;
	}

	public List<String> getCloudAmountLayer2List() {
		return cloudAmountLayer2List;
	}

	public void setCloudAmountLayer2List(List<String> cloudAmountLayer2List) {
		this.cloudAmountLayer2List = cloudAmountLayer2List;
	}

	public List<Double> getCloudHeightLayer2List() {
		return cloudHeightLayer2List;
	}

	public void setCloudHeightLayer2List(List<Double> cloudHeightLayer2List) {
		this.cloudHeightLayer2List = cloudHeightLayer2List;
	}

	public List<Boolean> getCbCloudLayer2List() {
		return cbCloudLayer2List;
	}

	public void setCbCloudLayer2List(List<Boolean> cbCloudLayer2List) {
		this.cbCloudLayer2List = cbCloudLayer2List;
	}

	public Integer getStateStatus() {
		return stateStatus;
	}

	public void setStateStatus(Integer stateStatus) {
		this.stateStatus = stateStatus;
	}
}