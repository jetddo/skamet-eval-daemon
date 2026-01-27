package kama.daemon.eval.warn;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WarnData {
	
	// 공항경보 요소
	public enum Element {
		
		TS, //천둥번개
		HVY_RA, //호우
		CIG, // 구름고도
		SFC_WSPD, //강풍
		SFC_VIS, //저시정
		HVY_SN //대설
	}
	
	@Override
	public String toString() {
		
		String s = "====================================================\n";
		
		s += "경보 전문: " + this.warnSource + "\n";
				
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		s += "경보 발표 공항: " + this.stnCd + "\n";
		s += "경보 발표 번호: " + this.warnNum + "\n";
		s += "경보 발표 시각: " + sdf.format(this.anncTm) + "\n";
		s += "경보 발효 시작 시각: " + sdf.format(this.stEffctTm) + "\n";
		s += "경보 발효 종료 시각: " + sdf.format(this.edEffctTm) + "\n";
		
		if(this.stCnlTm != null && this.edCnlTm != null) {
		
			s += "경보 취소 시작 시각: " + sdf.format(this.stCnlTm) + "\n";
			s += "경보 취소 종료 시각: " + sdf.format(this.edCnlTm) + "\n";
		}
		
		if(this.stExtTm != null && this.edExtTm != null) {
			
			s += "경보 연장 시작 시각: " + sdf.format(this.stExtTm) + "\n";
			s += "경보 연장 종료 시각: " + sdf.format(this.edExtTm) + "\n";
		}
		
		s += "해당 취소/연장 전문: "+this.infWarnSources + "\n";

		// 취소전문
		if(this.cnl) {
			s += "[취소전문]\n";
			s += "대상 경보 발표 번호: " + this.targetWarnNum + "\n";
			s += "대상 경보 시작 시각: " + sdf.format(this.targetStEffctTm) + "\n";
			s += "대상 경보 종료 시각: " + sdf.format(this.targetEdEffctTm) + "\n";
			s += "====================================================\n";
			return s;
		}
		
		// 연장전문
		if(this.extended) {
			s += "[연장전문]\n";
			s += "대상 경보 발표 번호: " + this.targetWarnNum + "\n";
			s += "대상 경보 시작 시각: " + sdf.format(this.targetStEffctTm) + "\n";
			s += "대상 경보 종료 시각: " + sdf.format(this.targetEdEffctTm) + "\n";
			s += "====================================================\n";
			return s;
		}
		
		switch(this.warnType) {
		
		case TS:
			
			s += "경보종류: 천둥번개 (TS)" + "\n";
			
			break;
			
		case CIG:
			
			s += "경보종류: 구름고도(CIG)" + "\n";
			s += "경보기준치: " + this.cig + "\n";
			
			break;
			
		case HVY_RA:
			
			s += "경보종류: 호우(HVY_RA)" + "\n";
			s += "경보기준치: " + this.ra + "\n";
			
			break;			
			
		case HVY_SN:
			
			s += "경보종류: 대설(HVY_SN)" + "\n";
			s += "경보기준치: " + this.sn + "\n";
			
			break;
			
		case SFC_VIS:
			
			s += "경보종류: 저시정(SFC_VIS)" + "\n";
			s += "경보기준치: " + this.vis + "\n";
			
			break;
			
		case SFC_WSPD:
			
			s += "경보종류: 강풍(SFC_WSPD)" + "\n";
			s += "경보기준치(평균): " + this.wspd + "\n";
			s += "경보기준치(최대): " + this.maxWspd + "\n";
			
			break;
		default:
			break; 
		}		

		s += "====================================================\n";
		
		return s;
	}
	
	private boolean isAvailable = true;
	
	private boolean isAutoCancel = false;
	
	private boolean isPrevCancel = false;
	
	// 경보 전문
	private String warnSource;
	
	// 경보 발표 시간
	private Date anncTm;
	
	// 경보 번호
	private Integer warnNum;
	
	// 대상 경보 번호 (취소,연장)
	private Integer targetWarnNum;
	
	// 대상 경보 시작 시간
	private Date targetStEffctTm;
	
	// 대상 경보 종료 시간
	private Date targetEdEffctTm;
	
	// 경보 발표 공항
	private String stnCd;
	
	// 경보 입력자
	private String inpNm;
	
	private List<String> errorMsgList = new ArrayList<String>();
	
	// 경보 시작 시간
	private Date stEffctTm;
	
	// 경보 종료 시간
	private Date edEffctTm;
	
	// 경보 타입
	private Element warnType;
	
	// 경보 타입 코드
	private Integer warnTypeCode;
	
	// 경보 타입 한글
	private String warnTypeKor;
	
	// 신적설 경보 수치
	private Double sn;
	
	// 강풍 경보 수치
	private Double wspd;
	
	private Double maxWspd;
	
	// 구름고도 경보 수치
	private Double cig;
	
	// 저시정 경보 수치
	private Double vis;
	
	// 호우 경보 수치
	private Double ra;
	
	// 취소 전문 여부 
	private boolean cnl = false;
	
	// 연장 전문 여부
	private boolean extended = false;
	
	// 경보 취소 시작 시간 (평가시 경보시작~경보종료에서 경보취소시작~경보취소종료 시간을 뺀다)
	// 경보 취소 전문을 읽으면 해당 전문에 업데이트한다
	private Date stCnlTm;
	
	// 경보 취소 종료 시간
	private Date edCnlTm;
	
	private Date stExtTm;
	
	private Date edExtTm;
	
	private String infWarnSources = "";

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
	
	public boolean isPrevCancel() {
		return this.isPrevCancel;
	}
	
	public void setPrevCancel(boolean isPrevCancel) {
		this.isPrevCancel = isPrevCancel;
	}
	
	public boolean isAutoCancel() {
		return this.isAutoCancel;
	}
	
	public void setAutoCancel(boolean isAutoCancel) {
		this.isAutoCancel = isAutoCancel;
	}

	public String getWarnSource() {
		return warnSource;
	}

	public void setWarnSource(String warnSource) {
		this.warnSource = warnSource;
	}

	public Date getAnncTm() {
		return anncTm;
	}

	public void setAnncTm(Date anncTm) {
		this.anncTm = anncTm;
	}

	public String getStnCd() {
		return stnCd;
	}

	public void setStnCd(String stnCd) {
		this.stnCd = stnCd;
	}

	public List<String> getErrorMsgList() {
		return errorMsgList;
	}

	public void setErrorMsgList(List<String> errorMsgList) {
		this.errorMsgList = errorMsgList;
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

	public Element getWarnType() {
		return warnType;
	}

	public void setWarnType(Element warnType) {
		this.warnType = warnType;
	}

	public Double getSn() {
		return sn;
	}

	public void setSn(Double sn) {
		this.sn = sn;
	}

	public Double getWspd() {
		return wspd;
	}

	public void setWspd(Double wspd) {
		this.wspd = wspd;
	}

	public Double getMaxWspd() {
		return maxWspd;
	}

	public void setMaxWspd(Double maxWspd) {
		this.maxWspd = maxWspd;
	}

	public Double getCig() {
		return cig;
	}

	public void setCig(Double cig) {
		this.cig = cig;
	}

	public Double getVis() {
		return vis;
	}

	public void setVis(Double vis) {
		this.vis = vis;
	}

	public Double getRa() {
		return ra;
	}

	public void setRa(Double ra) {
		this.ra = ra;
	}

	public Date getStCnlTm() {
		return stCnlTm;
	}

	public void setStCnlTm(Date stCnlTm) {
		this.stCnlTm = stCnlTm;
	}

	public Date getEdCnlTm() {
		return edCnlTm;
	}

	public void setEdCnlTm(Date edCnlTm) {
		this.edCnlTm = edCnlTm;
	}

	public Integer getWarnNum() {
		return warnNum;
	}

	public void setWarnNum(Integer warnNum) {
		this.warnNum = warnNum;
	}
	
	public boolean isCnl() {
		return cnl;
	}	
	
	public void setCnl(boolean cnl) {
		this.cnl = cnl;
	}

	public String getInpNm() {
		return inpNm;
	}

	public void setInpNm(String inpNm) {
		this.inpNm = inpNm;
	}

	public String getWarnTypeKor() {
		return warnTypeKor;
	}

	public void setWarnTypeKor(String warnTypeKor) {
		this.warnTypeKor = warnTypeKor;
	}

	public Integer getWarnTypeCode() {
		return warnTypeCode;
	}

	public void setWarnTypeCode(Integer warnTypeCode) {
		this.warnTypeCode = warnTypeCode;
	}
	
	public void checkAvaliable() {
		
		if(this.cnl) {
			
			if(this.stEffctTm == null || this.edEffctTm == null || this.stCnlTm == null || this.edCnlTm == null || this.anncTm == null || this.warnNum == null) {
				
				this.errorMsgList.add("this.stEffctTm == null || this.edEffctTm == null || this.stCnlTm == null || this.edCnlTm == null || this.anncTm == null || this.warnNum == null");
				this.isAvailable = false;
			}
			
		} else if(this.extended) {
			
			if(this.stEffctTm == null || this.edEffctTm == null || this.stExtTm == null || this.edExtTm == null || this.anncTm == null || this.warnNum == null) {
				
				this.errorMsgList.add("this.stEffctTm == null || this.edEffctTm == null || this.stCnlTm == null || this.edCnlTm == null || this.anncTm == null || this.warnNum == null");
				this.isAvailable = false;
			}
			
		} else {
			
			if(this.stEffctTm == null || this.edEffctTm == null || this.anncTm == null || this.warnType == null || this.warnNum == null) {
				
				this.errorMsgList.add("this.stEffctTm == null || this.edEffctTm == null || this.anncTm == null || this.warnType == null || this.warnNum == null");
				this.isAvailable = false;
			}
			
			switch(this.warnType) {
			
			case TS:
				
				break;
				
			case CIG:
				
				if(this.cig == null) {
					this.isAvailable = false;
				}
				
				break;
				
			case HVY_RA:
				
				if(this.ra == null) {
					this.isAvailable = false;
				}
				
				break;			
				
			case HVY_SN:
				
				if(this.sn == null) {
					this.isAvailable = false;
				}
				
				break;
				
			case SFC_VIS:
				
				if(this.vis == null) {
					this.isAvailable = false;
				}
				
				break;
				
			case SFC_WSPD:
				
				if(this.wspd == null || this.maxWspd == null) {
					this.isAvailable = false;
				}
				
				break;
			default:
				break; 
			}		
		}
	}

	public Date getStExtTm() {
		return stExtTm;
	}

	public void setStExtTm(Date stExtTm) {
		this.stExtTm = stExtTm;
	}

	public Date getEdExtTm() {
		return edExtTm;
	}

	public void setEdExtTm(Date edExtTm) {
		this.edExtTm = edExtTm;
	}

	public boolean isExtended() {
		return extended;
	}

	public void setExtended(boolean extended) {
		this.extended = extended;
	}

	public Integer getTargetWarnNum() {
		return targetWarnNum;
	}

	public void setTargetWarnNum(Integer targetWarnNum) {
		this.targetWarnNum = targetWarnNum;
	}

	public String getInfWarnSources() {
		return infWarnSources;
	}

	public void setInfWarnSources(String infWarnSources) {
		this.infWarnSources = infWarnSources;
	}
	
	public void addInfWarnSources(String infWarnSource) {
		this.infWarnSources += infWarnSource + "|" ;
	}

	public Date getTargetStEffctTm() {
		return targetStEffctTm;
	}

	public void setTargetStEffctTm(Date targetStEffctTm) {
		this.targetStEffctTm = targetStEffctTm;
	}

	public Date getTargetEdEffctTm() {
		return targetEdEffctTm;
	}

	public void setTargetEdEffctTm(Date targetEdEffctTm) {
		this.targetEdEffctTm = targetEdEffctTm;
	}
	
	
}
