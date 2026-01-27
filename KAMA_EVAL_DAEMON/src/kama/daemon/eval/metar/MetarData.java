package kama.daemon.eval.metar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MetarData {
	
	private boolean isAvailable = true;
	
	private String metarSource;
	
	private Date anncTm;
	
	private String stnCd;
	
	private List<String> errorMsgList = new ArrayList<String>();
	
	private MetarElement metarElement = new MetarElement();
	
	public MetarData() {
		
	}

	public Date getAnncTm() {
		return anncTm;
	}

	public void setAnncTm(Date anncTm) {
		this.anncTm = anncTm;
		this.metarElement.setMetarTm(anncTm);
	}

	public MetarElement getMetarElement() {
		return metarElement;
	}

	public void setMetarElement(MetarElement metarElement) {
		this.metarElement = metarElement;
	}
	
	public String getMetarSource() {
		return metarSource;
	}

	public void setMetarSource(String metarSource) {
		this.metarSource = metarSource;
	}
	
	public void updateWind(Map<String, Object> windMap) {
		
		Boolean vrb = (boolean)windMap.get("vrb");
		Object wdir = windMap.get("wdir");
		Double wspd = (double)windMap.get("wspd");
		Object maxWspd = windMap.get("maxWspd");
		
		this.metarElement.setVrb(vrb);
		
		if(wdir != null) {
			this.metarElement.setWdir((double)wdir);	
		}
		
		if(wspd != null) {
			this.metarElement.setWspd(wspd);
		}
		
		if(maxWspd != null) {
			this.metarElement.setMaxWspd((double)maxWspd);
		}
	}
	
	public void updateVis(Double vis) {
		
		if(vis != null) {
			this.metarElement.setVis(vis);	
		}
	}
	
	public void updateQnh(Double qnh) {
		
		if(qnh != null) {
			this.metarElement.setQnh(qnh);	
		}
	}
	
	public void updateMaxTemp(Double temperature) {		
		metarElement.setTx(temperature);
	}
	
	public void updateMinTemp(Double temperature) {
		metarElement.setTn(temperature);
	}
	
	public void updateSkyCondition(String skyCondition) {
		
		this.metarElement.setSkyCondition((this.metarElement.getSkyCondition() + " " + skyCondition).trim());
		this.metarElement.setNsw(false);
	}
	
	public void updateCloudCondition(String cloudCondition) {
		
		String cloudAmount = null;
		Double cloudHeight = null;
		Boolean isCbCloud = false; 
		
		if(cloudCondition.startsWith("VV")) {
			
			cloudAmount = "OVC";
			
			if(cloudCondition.substring(2, 5).equals("///")) {
				cloudHeight = 0.0;
			} else {
				cloudHeight = Double.valueOf(cloudCondition.substring(2, 5)) * 100;
			}
			
		} else {
			
			cloudAmount = cloudCondition.substring(0, 3);
			cloudHeight = Double.valueOf(cloudCondition.substring(3, 6)) * 100;
			isCbCloud = cloudCondition.contains("CB") ? true : false;
		}
		
		if(cloudHeight <= 1500) {
			this.metarElement.getCloudAmountLayer1List().add(cloudAmount);
			this.metarElement.getCloudHeightLayer1List().add(cloudHeight);
			this.metarElement.getCbCloudLayer1List().add(isCbCloud);	
			
			// 1층과 2층 고도에 구름이 있을 경우에는  NSC 및 SKC 를 해제한다
			this.metarElement.setNsc(false);
			this.metarElement.setSkc(false);
			
		} else if(cloudHeight > 1500 && cloudHeight <= 10000) {
			this.metarElement.getCloudAmountLayer2List().add(cloudAmount);
			this.metarElement.getCloudHeightLayer2List().add(cloudHeight);
			this.metarElement.getCbCloudLayer2List().add(isCbCloud);
			
			// 1층과 2층 고도에 구름이 있을 경우에는  NSC 및 SKC 를 해제한다
			this.metarElement.setNsc(false);
			this.metarElement.setSkc(false);
		}
		
		this.metarElement.setCavok(false);
	}
	
	public void updateCavok() {
		
		this.metarElement.setCavok(true);
		this.metarElement.getCloudAmountLayer1List().clear();
		this.metarElement.getCloudAmountLayer2List().clear();
		this.metarElement.getCloudHeightLayer1List().clear();
		this.metarElement.getCloudHeightLayer2List().clear();
		this.metarElement.getCbCloudLayer1List().clear();
		this.metarElement.getCbCloudLayer2List().clear();
		this.metarElement.setVis(9999.0);
		this.metarElement.setSkyCondition("");
	}
	
	public void updateSkc() {
		
		this.metarElement.setSkc(true);
		this.metarElement.setNsc(true);	
		this.metarElement.getCloudAmountLayer1List().clear();
		this.metarElement.getCloudAmountLayer2List().clear();
		this.metarElement.getCloudHeightLayer1List().clear();
		this.metarElement.getCloudHeightLayer2List().clear();
		this.metarElement.getCbCloudLayer1List().clear();
		this.metarElement.getCbCloudLayer2List().clear();
	}
	
	public void updateNsc() {
		
		this.metarElement.setNsc(true);	
		this.metarElement.setSkc(true);
		this.metarElement.getCloudAmountLayer1List().clear();
		this.metarElement.getCloudAmountLayer2List().clear();
		this.metarElement.getCloudHeightLayer1List().clear();
		this.metarElement.getCloudHeightLayer2List().clear();
		this.metarElement.getCbCloudLayer1List().clear();
		this.metarElement.getCbCloudLayer2List().clear();
	}
	
	public void updateNsw() {
		
		this.metarElement.setNsw(true);			
		this.metarElement.setSkyCondition("");
	}
	
	@Override
	public String toString() {
		
		String s = "";
		
		SimpleDateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		s += "====================================================\n";
		s += "\t\tMETAR HEADER\n";
		s += "====================================================\n";
		s += "\tanncTm: " + datePattern.format(this.anncTm) + "\n";		
		s += "====================================================\n";
		s += "\n";
		s += "====================================================\n";
		s += "\t\tMETAR CONTENTS\n";		
		s += "====================================================\n";
		
		s += this.metarElement.toString();	
		
		s += "====================================================\n";
		
		return s;
	}
	
	public void printErrorMsgList() {
		
		for(int i=0 ; i<this.errorMsgList.size() ; i++) {
			System.out.println(this.errorMsgList.get(i));
		}
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
	
	public void checkAvaliable() {
		
		if(this.metarElement == null || this.anncTm == null) {
			this.errorMsgList.add("this.metarElement == null || this.anncTm == null");
			this.isAvailable = false;
		}
		
		if(this.metarElement.getMetarTm() == null) {
			this.errorMsgList.add("this.metarElement.getMetarTm() == null");
			this.isAvailable = false;
		}
		
		if((this.metarElement.getWdir() == null && !this.metarElement.isVrb()) || this.metarElement.getWspd() == null) {
			this.errorMsgList.add("(this.metarElement.getWdir() == null && !this.metarElement.isVrb()) || this.metarElement.getWspd() == null");
			this.isAvailable = false;
		}
		
		if(this.metarElement.getVis() == null && !this.metarElement.isCavok()) {
			this.errorMsgList.add("this.metarElement.getVis() == null && !this.metarElement.isCavok()");
			this.isAvailable = false;
		}
	}

	public String getStnCd() {
		return stnCd;
	}

	public void setStnCd(String stnCd) {
		this.stnCd = stnCd;
	}
}