package kr.uracle.ums.core.service;

import kr.uracle.ums.core.ehcache.ChannelAllotRateCache;
import kr.uracle.ums.core.vo.setting.ProviderVo;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChannelAllotter {
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
	private final String channel;
	private final ChannelAllotRateCache channelAllotRateCache;

	private final long MAX_TOTAL = 1000000L;
	// 중계사 분배건수
	private long totalCnt = 0;
	// 중계사 구간 분배건수(캐싱 초기화때 같이 클렌징함)
	private long sectionTotalCnt = 0;

	private final static int HISTORY_MAX = 4000;

	private final LinkedHashMap<String, String> allotNumberHistory = new LinkedHashMap<String, String>(HISTORY_MAX, .75f, true){
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() >= HISTORY_MAX;
		}
	};
    
	public ChannelAllotter(String channel, ChannelAllotRateCache _channelAllotRateCache) {
		this.channel = channel;
		this.channelAllotRateCache = _channelAllotRateCache;
	}
	
    private void refresh() { channelAllotRateCache.refreshProvider(channel); sectionTotalCnt=0;}
    public String getProvider(String identifyKey) {
    	// 갱신필요 여부 확인
		synchronized (channelAllotRateCache) {
			boolean isExpiry = channelAllotRateCache.isExpiry(channel);
			if (isExpiry) refresh();
		}
		// SMS 프로바이더 가져오기
		List<ProviderVo> pVoList = channelAllotRateCache.getProviders(channel);
		
		// 프로바이더 없다면 AS-IS 방식으로 프로바이더 서비스 리턴
		if(ObjectUtils.isEmpty(pVoList))return null;
		
		// 분배 프로바이더 가져오기
		ProviderVo pVo = allot(identifyKey, pVoList, allotNumberHistory);
		
		// 분배할 프로바이더가 없다면 첫번째 프로바이더로 지정
		if(pVo == null) pVo = pVoList.get(0);
		
		// 분배 이력 기록 - 동일번호 동일 공급사 분배 기능
		String providerName = pVo.getPROVIDER();
		recordHistory(identifyKey, providerName);
		
		// 분배 프로바이더에 맞게 프로바이더 서비스 리턴
		return providerName;
    }

    public synchronized ProviderVo allot(String identifyKey, List<ProviderVo> list, Map<String, String> allotHistory) {
		if(totalCnt > MAX_TOTAL) totalCnt = 0;

    	if(allotHistory != null && identifyKey != null) {
    		String sentProviderName = allotHistory.get(identifyKey);
    		if(sentProviderName != null) {
    			for(ProviderVo target :list) {
    				if(target.getPROVIDER().equalsIgnoreCase(sentProviderName)) {
    					// 프로바이더 건수 증가
						target.increaseSentCnt(1);
						// 디버깅 로그
						logger.debug("{} - (동일 번호)분배 대상:{}, 총건수:{}, 구간 총건수:{}, 분배건수:{}, 기대 분배 비율:{}, 실제 분배 비율:{}", channel, target.getPROVIDER(), totalCnt, sectionTotalCnt, target.getSENTCNT(), target.getRATIO(), ((double)target.getSENTCNT()/(double)sectionTotalCnt)*100);
						// 채널 전체건수 증가, 구간 채널 전체 건수 증가
						increaseChannleTotalCnt(channel);
						increaseChannleSectionTotalCnt(channel);
						return target;
					}
    			}
    		}    		
    	}
    	
		// 기존 채널 총 건수 + 현재 건수
		int index = 0;
		double bigDiffCnt = 0;
		ProviderVo target = null;
		for(int i=0 ; i<list.size(); i++) {
			target = list.get(i);
			// 요청 전 발송 건수
			long sentCnt = target.getSENTCNT();
			// 기대 발송 비율
			double rate = target.getRATE();
			// 발송 비율이 0이거나 음수면 제외
			if(rate <= 0)continue;
			// 발송 기대 건수 추출
			double estimateCnt = (double)sectionTotalCnt * rate;
			logger.debug("{} - 분배 이력:{}, 총건수:{}, 구간 총건수:{}, 분배건수:{}, 기대 분배 비율:{}, 실제 분배 비율:{}", channel, target.getPROVIDER(), totalCnt, sectionTotalCnt, target.getSENTCNT(), target.getRATIO(), target.getSENTCNT()>0?((double)target.getSENTCNT()/(double)sectionTotalCnt)*100:0);
			double diffCnt = estimateCnt-sentCnt;
			if(diffCnt>0) {
				if(diffCnt > bigDiffCnt){
					index = i;
					bigDiffCnt = diffCnt;
				}
			}
		}
		target = list.get(index);
		target.increaseSentCnt(1);

		// 채널 전체건수 증가, 구간 채널 전체 건수 증가
		increaseChannleTotalCnt(channel);
		increaseChannleSectionTotalCnt(channel);
		logger.debug("{} - 분배 대상:{}, 총건수:{}, 구간 총건수:{}, 분배건수:{}, 기대 분배 비율:{}, 실제 분배 비율:{}", channel, target.getPROVIDER(), totalCnt, sectionTotalCnt, target.getSENTCNT(), target.getRATIO(), ((double)target.getSENTCNT()/(double)sectionTotalCnt)*100);
		return target;
	}
	
    private synchronized void recordHistory(String phoneNumber, String providerName) {allotNumberHistory.put(phoneNumber, providerName);}
    private long increaseChannleTotalCnt(String channel) { return totalCnt=totalCnt+1; }
    private long increaseChannleSectionTotalCnt(String channel) { return sectionTotalCnt=sectionTotalCnt+1; }
}
