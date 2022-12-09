package kr.uracle.ums.core.service;

import kr.uracle.ums.core.ehcache.ChannelAllotRateCache;
import kr.uracle.ums.core.service.send.kko.BaseKkoAltSendService;
import kr.uracle.ums.core.service.send.kko.BaseKkoFrtSendService;
import kr.uracle.ums.core.service.send.kko.LgcnsKkoAltSendService;
import kr.uracle.ums.core.service.send.kko.LgcnsKkoFrtSendService;
import kr.uracle.ums.core.service.send.mms.BaseMmsSendService;
import kr.uracle.ums.core.service.send.mms.KtMmsSendService;
import kr.uracle.ums.core.service.send.mms.LguMmsSendService;
import kr.uracle.ums.core.service.send.naver.BaseNaverSendService;
import kr.uracle.ums.core.service.send.naver.MtsNaverSendService;
import kr.uracle.ums.core.service.send.rcs.BaseRcsSendService;
import kr.uracle.ums.core.service.send.rcs.LguRcsSendService;
import kr.uracle.ums.core.service.send.sms.BaseSmsSendService;
import kr.uracle.ums.core.service.send.sms.KtSmsSendService;
import kr.uracle.ums.core.service.send.sms.LguSmsSendService;
import kr.uracle.ums.core.vo.setting.ProviderVo;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * 채널별 공급사 발송비율 관리하는 매니저
 * @author golde
 *
 */
@Service
public class UmsChannelAllotFactory {

    @Autowired(required = true)
    private ChannelAllotRateCache channelAllotRateCache;
	
    @Autowired(required = true)
    private UmsChannelProviderFactory umsChannelProvierFactory;
    
    // SMS 공급업체 서비스
    @Autowired(required = true)
    private KtSmsSendService ktSmsSendService;
    @Autowired(required = true)
    private LguSmsSendService lguSmsSendService;
    
    // MMS 공급업체 서비스
    @Autowired(required = true)
    private KtMmsSendService ktMmsSendService;
    @Autowired(required = true)
    private LguMmsSendService lguMmsSendService;
    
    // RCS 공급 업체 서비스
    @Autowired(required = true)
    private LguRcsSendService lguRCSSendService;
    
    //알림톡 공급업체 서비스
    @Autowired(required = true)
    private LgcnsKkoAltSendService lgcnsKkoAltSendService;
    
    //친구톡 공급업체 서비스
    @Autowired(required = true)
    private LgcnsKkoFrtSendService lgcnsKkoFrtSendService;
        
    //네이버톡 공급업체 서비스
    @Autowired(required = true)
    private MtsNaverSendService mtsNaverSendService;
    
    
    final static int HISTORY_MAX = 4000;
    
	// SMS 전화번호 중계사 분배 현황 - LRU 지정(가장 오랫동안 참조되지 않은 번호 삭제)
	final static LinkedHashMap<String, String> smsAllotNumberHistory = new LinkedHashMap<String, String>(HISTORY_MAX, .75f, true){
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() >= HISTORY_MAX;
		}
	};
	
	// MMS 전화번호 중계사 분배 현황 - LRU 지정(가장 오랫동안 참조되지 않은 번호 삭제)
	final static LinkedHashMap<String, String> mmsAllotNumberHistory = new LinkedHashMap<String, String>(HISTORY_MAX, .75f, true){
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() >= HISTORY_MAX;
		}
	};
	
	// RCS 전화번호 중계사 분배 현황 - LRU 지정(가장 오랫동안 참조되지 않은 번호 삭제)
	final static LinkedHashMap<String, String> rcsAllotNumberHistory = new LinkedHashMap<String, String>(HISTORY_MAX, .75f, true){
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() >= HISTORY_MAX;
		}
	};
	
	// 알림톡 전화번호 중계사 분배 현황 - LRU 지정(가장 오랫동안 참조되지 않은 번호 삭제)
	final static LinkedHashMap<String, String> altAllotNumberHistory = new LinkedHashMap<String, String>(HISTORY_MAX, .75f, true){
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() >= HISTORY_MAX;
		}
	};
	
	// 친구톡 전화번호 중계사 분배 현황 - LRU 지정(가장 오랫동안 참조되지 않은 번호 삭제)
	final static LinkedHashMap<String, String> frtAllotNumberHistory = new LinkedHashMap<String, String>(HISTORY_MAX, .75f, true){
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() >= HISTORY_MAX;
		}
	};
	
	// 네이버 스마트알림 전화번호 중계사 분배 현황 - LRU 지정(가장 오랫동안 참조되지 않은 번호 삭제)
	final static LinkedHashMap<String, String> navertAllotNumberHistory = new LinkedHashMap<String, String>(HISTORY_MAX, .75f, true){
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() >= HISTORY_MAX;
		}
	};
	
	
	// 중계사 분배건수
	final static Map<String, Integer> channelTotalMap = new HashMap<String, Integer>(30);
	// 중계사 구간 분배건수(캐싱 초기화때 같이 클렌징함)
	final static Map<String, Integer> channelSectionTotalMap = new HashMap<String, Integer>(30);
	
    public UmsChannelAllotFactory() { refresh();}
        
    //발송 비율 정보  1. e캐시 공급사별 발송비율 획득 및 갱신
    //채널별 발송량 Map - 키:중계사 값: 발송량- 추가적으로 마지막 갱신 시간(ms) 키:cleartime value:마지막 클리어 시간
    
    public Object getProviderService(String channel, String identifyKey) {
    	Object providerService =  null;
    	switch(channel) {
    	case "SMS":
    		providerService = getSmsProviderService(identifyKey);	
    		break;
    	case "LMS": case "MMS":
    		providerService = getMmsProviderService(identifyKey);
    		break;
    	case "RCS_SMS": case "RCS_LMS": case "RCS_MMS": case "RCS_FREE": case "RCS_CELL": case "RCS_DESC":
    		providerService = getRcsProviderService(identifyKey);
    		break;
    	case "KKOALT":
    		providerService = getKkoAltProviderService(identifyKey);
    		break;
    	case "KKOFRT":
    		providerService = getKkoFrtProviderService(identifyKey);
    		break;
    	case "NAVERT":
    		providerService = getNaverProviderService(identifyKey);
    		break;
    	}
    	return providerService;
    }
    
	public BaseSmsSendService getSmsProviderService(String phoneNumber){
		String channel = "SMS";
		// 갱신필요 여부 확인
		boolean isExpiry = channelAllotRateCache.isExpiry(channel);
		if(isExpiry) refresh();
		
		// SMS 프로바이더 가져오기
		List<ProviderVo> pVoList = channelAllotRateCache.getProviders(channel);
		
		// 프로바이더 없다면 AS-IS 방식으로 프로바이더 서비스 리턴
		if(ObjectUtils.isEmpty(pVoList))return umsChannelProvierFactory.getSmsProviderService();
		
		// 분배 프로바이더 가져오기
		ProviderVo pVo = allot(phoneNumber, pVoList, smsAllotNumberHistory);
		
		// 분배할 프로바이더가 없다면 첫번째 프로바이더로 지정
		if(pVo == null) pVo = pVoList.get(0);
		
		// 분배 이력 기록 - 동일번호 동일 공급사 분배 기능
		String providerName = pVo.getPROVIDER();
		recordSmsHistory(phoneNumber, providerName);
		
		// 분배 프로바이더에 맞게 프로바이더 서비스 리턴
		if(providerName.equalsIgnoreCase("KT")) return ktSmsSendService;
		if(providerName.equalsIgnoreCase("LGU")) return lguSmsSendService;
		
		return umsChannelProvierFactory.getSmsProviderService();
    }
	
    public BaseMmsSendService getMmsProviderService(String phoneNumber){
		String channel = "MMS";
		// 갱신필요 여부 확인
		boolean isExpiry = channelAllotRateCache.isExpiry(channel);
		if(isExpiry) refresh();
		
		// MMS 프로바이더 가져오기
		List<ProviderVo> pVoList = channelAllotRateCache.getProviders(channel);
		// 프로바이더 없다면 AS-IS 방식으로 프로바이더 서비스 리턴
		if(ObjectUtils.isEmpty(pVoList) )return umsChannelProvierFactory.getMmsProviderService();
		
		// 분배 프로바이더 가져오기
		ProviderVo pVo = allot(phoneNumber, pVoList, mmsAllotNumberHistory);
		
		// 분배할 프로바이더가 없다면 첫번째 프로바이더로 지정
		if(pVo == null) pVo = pVoList.get(0);
				
		// 분배 이력 기록 - 동일번호 동일 공급사 분배 기능
		String providerName = pVo.getPROVIDER();
		recordMmsHistory(phoneNumber, providerName);
		
		// 분배 프로바이더에 맞게 프로바이더 서비스 리턴
		if(providerName.equalsIgnoreCase("KT")) return ktMmsSendService;
		if(providerName.equalsIgnoreCase("LGU")) return lguMmsSendService;
    			
        return umsChannelProvierFactory.getMmsProviderService();  
    }

    public BaseKkoAltSendService getKkoAltProviderService(String phoneNumber){
		String channel = "KKOALT";
		// 갱신필요 여부 확인
		boolean isExpiry = channelAllotRateCache.isExpiry(channel);
		if(isExpiry) refresh();
		
		// 알림톡 프로바이더 가져오기
		List<ProviderVo> pVoList = channelAllotRateCache.getProviders(channel);
		// 프로바이더 없다면 AS-IS 방식으로 프로바이더 서비스 리턴
		if(ObjectUtils.isEmpty(pVoList) )return umsChannelProvierFactory.getKkoAltProviderService();
		
		// 분배 프로바이더 가져오기
		ProviderVo pVo = allot(phoneNumber, pVoList, altAllotNumberHistory);
		
		// 분배할 프로바이더가 없다면 첫번째 프로바이더로 지정
		if(pVo == null) pVo = pVoList.get(0);
				
		// 분배 이력 기록 - 동일번호 동일 공급사 분배 기능
		String providerName = pVo.getPROVIDER();
		recordAltHistory(phoneNumber, providerName);
		
		// 분배 프로바이더에 맞게 프로바이더 서비스 리턴
		if(providerName.equalsIgnoreCase("LGCNS")) return lgcnsKkoAltSendService;

        return umsChannelProvierFactory.getKkoAltProviderService();  
    }

    public BaseKkoFrtSendService getKkoFrtProviderService(String phoneNumber){
		String channel = "KKOFRT";
		// 갱신필요 여부 확인
		boolean isExpiry = channelAllotRateCache.isExpiry(channel);
		if(isExpiry) refresh();
		
		// SMS 프로바이더 가져오기
		List<ProviderVo> pVoList = channelAllotRateCache.getProviders(channel);
		// 프로바이더 없다면 AS-IS 방식으로 프로바이더 서비스 리턴
		if(ObjectUtils.isEmpty(pVoList))return umsChannelProvierFactory.getKkoFrtProviderService();
		
		// 분배 프로바이더 가져오기
		ProviderVo pVo = allot(phoneNumber, pVoList, frtAllotNumberHistory);
		
		// 분배할 프로바이더가 없다면 첫번째 프로바이더로 지정
		if(pVo == null) pVo = pVoList.get(0);
		
		// 분배 이력 기록 - 동일번호 동일 공급사 분배 기능
		String providerName = pVo.getPROVIDER();
		recordFrtHistory(phoneNumber, providerName);
		
		// 분배 프로바이더에 맞게 프로바이더 서비스 리턴
		if(providerName.equalsIgnoreCase("LGCNS")) return lgcnsKkoFrtSendService;
    	
        return umsChannelProvierFactory.getKkoFrtProviderService(); 
    }
    
    public BaseRcsSendService getRcsProviderService(String phoneNumber){
		String channel = "RCS";
		// 갱신필요 여부 확인
		boolean isExpiry = channelAllotRateCache.isExpiry(channel);
		if(isExpiry) refresh();
		
		// SMS 프로바이더 가져오기
		List<ProviderVo> pVoList = channelAllotRateCache.getProviders(channel);
		// 프로바이더 없다면 AS-IS 방식으로 프로바이더 서비스 리턴
		if(ObjectUtils.isEmpty(pVoList))return umsChannelProvierFactory.getRcsProviderService();
		
		
		// 분배 프로바이더 가져오기
		ProviderVo pVo = allot(phoneNumber, pVoList, rcsAllotNumberHistory);
		
		// 분배할 프로바이더가 없다면 첫번째 프로바이더로 지정
		if(pVo == null) pVo = pVoList.get(0);
		
		// 분배 이력 기록 - 동일번호 동일 공급사 분배 기능
		String providerName = pVo.getPROVIDER();
		recordRcsHistory(phoneNumber, providerName);
		
		// 분배 프로바이더에 맞게 프로바이더 서비스 리턴
		if(providerName.equalsIgnoreCase("LGU")) return lguRCSSendService;
    	
        return umsChannelProvierFactory.getRcsProviderService(); 
    }

    public BaseNaverSendService getNaverProviderService(String phoneNumber){
		String channel = "NAVERT";
		// 갱신필요 여부 확인
		boolean isExpiry = channelAllotRateCache.isExpiry(channel);
		if(isExpiry) refresh();
		
		// SMS 프로바이더 가져오기
		List<ProviderVo> pVoList = channelAllotRateCache.getProviders(channel);
		// 프로바이더 없다면 AS-IS 방식으로 프로바이더 서비스 리턴
		if(ObjectUtils.isEmpty(pVoList))return umsChannelProvierFactory.getNaverProviderService();
		
		// 분배 프로바이더 가져오기
		ProviderVo pVo = allot(phoneNumber, pVoList, navertAllotNumberHistory);
		
		// 분배할 프로바이더가 없다면 첫번째 프로바이더로 지정
		if(pVo == null) pVo = pVoList.get(0);

		// 분배 이력 기록 - 동일번호 동일 공급사 분배 기능
		String providerName = pVo.getPROVIDER();
		recordNavertHistory(phoneNumber, providerName);
		
		// 분배 프로바이더에 맞게 프로바이더 서비스 리턴
		if(providerName.equalsIgnoreCase("MTS")) return mtsNaverSendService;
    	
        return umsChannelProvierFactory.getNaverProviderService();
    }
    
    private void refresh() {
    	//channelAllotRateCache.refreshAllProvider(); clearChannleSectionTotalCnt();
    }
    
    private synchronized ProviderVo allot(String number, List<ProviderVo> list, Map<String, String> allotHistory) {
    	
    	String channel = list.get(0).getCHANNEL();
    	if(allotHistory != null && number != null) {
    		String sentProviderName = allotHistory.get(number);
    		if(sentProviderName != null) {
    			for(ProviderVo p :list) {
    				if(p.getPROVIDER().equalsIgnoreCase(sentProviderName)) {
    					// 프로바이더 건수 증가, 채널 전체건수 증가, 구간 채널 전체 건수 증가
    					p.increaseSentCnt(1); increaseChannleTotalCnt(channel); increaseChannleSectionTotalCnt(channel);
    					return p;
    				}
    			}
    		}    		
    	}
		
		// 기존 채널 총 건수 + 현재 건수
		int totalCnt = channelSectionTotalMap.get(channel)+1;
		for(ProviderVo target :list) {
			// 공급사 발송 건수
			long sentCnt = target.getSENTCNT();
			// 공급사 발송 비율
			double rate = target.getRATE();
			// 음수일 경우 제외
			if(rate < 0)continue;
			// 발송 기대 건수 추출
			double estimateCnt = (double)totalCnt * rate;
			// 현재건수가 기대 건수 보다 작을 시 대상
			if(sentCnt < estimateCnt) {
				// 프로바이더 건수 증가, 채널 전체건수 증가, 구간 채널 전체 건수 증가
				target.increaseSentCnt(1); increaseChannleTotalCnt(channel); increaseChannleSectionTotalCnt(channel);
				return target; 
			}
		}
		return null;
	}
    
    private synchronized void recordSmsHistory(String phoneNumber, String providerName) {smsAllotNumberHistory.put(phoneNumber, providerName);}
    private synchronized void recordMmsHistory(String phoneNumber, String providerName) {mmsAllotNumberHistory.put(phoneNumber, providerName);}
    private synchronized void recordAltHistory(String phoneNumber, String providerName) {altAllotNumberHistory.put(phoneNumber, providerName);}
    private synchronized void recordFrtHistory(String phoneNumber, String providerName) {frtAllotNumberHistory.put(phoneNumber, providerName);}
    private synchronized void recordRcsHistory(String phoneNumber, String providerName) {rcsAllotNumberHistory.put(phoneNumber, providerName);}
    private synchronized void recordNavertHistory(String phoneNumber, String providerName) {navertAllotNumberHistory.put(phoneNumber, providerName);}
    
    private synchronized void clearChannleSectionTotalCnt() {channelSectionTotalMap.clear();}
    private synchronized int increaseChannleTotalCnt(String channel) { return channelTotalMap.merge(channel, 1, Integer::sum); }
    private synchronized int increaseChannleSectionTotalCnt(String channel) { return channelSectionTotalMap.merge(channel, 1, Integer::sum); }

}
