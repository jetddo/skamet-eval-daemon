package kama.daemon.eval.metar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class MetarParser {
	
	// METAR 발표시각
	private Date anncTm;
			
	public MetarData parse(String stnCd, String metarSource, Date stdTm) throws MetarParseException {
		
		MetarData metarData = new MetarData();
		
		// METAR 발표시각
		this.anncTm = null;
		
		// Token 위치
		int cursor = 0;

		boolean isMetarEnd = false;
						
		metarData.setStnCd(stnCd);
		metarData.setMetarSource(metarSource);
		
		metarSource = metarSource.replaceAll("=", "").trim();
		
		String[] metarTokens = metarSource.split("\\s+");
		
		while(cursor < metarTokens.length) {
			
			if(isMetarEnd) {
				break;
			}
			
			int identifyCode = MetarTokenParser.identifyMetarToken(metarTokens[cursor]);
			
			switch(identifyCode) {
				
			case 0: // 발표시각 패턴
				
				anncTm = MetarTokenParser.parsePartialDate(stdTm, metarTokens[cursor], 6);				
				metarData.setAnncTm(anncTm);
				break;
				
			case 1: // 바람 패턴
				
				Map<String, Object> windMap = MetarTokenParser.parseWind(metarTokens[cursor]);
			
				metarData.updateWind(windMap);
				
				break;
				
			case 2: // 시정 패턴
				
				Double vis = MetarTokenParser.parseVis(metarTokens[cursor]);
				
				metarData.updateVis(vis);
				
				break;
				
			case 3: // 기온 패턴
				
				Map<String, Double> tempMap = MetarTokenParser.parseTemperature(metarTokens[cursor]);
				
				metarData.updateMaxTemp(tempMap.get("tx"));
				metarData.updateMinTemp(tempMap.get("tn"));
				
				break;
				
			case 4: // 현천 패턴	
				
				metarData.updateSkyCondition(metarTokens[cursor]);
				
				break;
				
			case 5: // 구름 패턴
				
				metarData.updateCloudCondition(metarTokens[cursor]);
				
				break;	
				
			case 6: // 기압 패턴
				
				Double qnh = MetarTokenParser.parseQnh(metarTokens[cursor]);
				
				metarData.updateQnh(qnh);
				
				break;
				
			case 7: // CAVOK 패턴
				
				metarData.updateCavok();
				
				break;
				
			case 8: // SKC 패턴
				
				metarData.updateSkc();
				
				break;	
				
			case 9: // NSW 패턴
				
				metarData.updateNsw();
				
				break;	
				
			case 10: // NSC 패턴
				
				metarData.updateNsc();
				
				break;	
				
			case 11: // END  패턴
				
				isMetarEnd = true;
			}
			
			cursor++;
		}
		
		metarData.checkAvaliable();
		
		return metarData;
	}
	
	public static void main(String[] args) throws Exception {
		
		String metarSource = 
				"METAR RKPM 180059Z 15019KT 000 -RA FG OVC020 16/16 A2979 RMK CIG000 CB SW MOV NE SLP075 = ";		
		
		MetarParser metarParser = new MetarParser();
		
		Date d = new SimpleDateFormat("yyyyMMddHHmm").parse("202101312300");
		
		MetarData metarData = metarParser.parse("RKPM", metarSource, d);
		
		System.out.println(metarData);

	}
}
