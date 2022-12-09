package kr.uracle.ums.core.service;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.ehcache.ChannelAllotRateCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AllotterManager {
	
	private final ChannelAllotRateCache channelAllotRateCache;

	private ChannelAllotter smsAllotter;
	private ChannelAllotter lmsAllotter;
	private ChannelAllotter mmsAllotter;
	private ChannelAllotter altAllotter;
	private ChannelAllotter frtAllotter;
	private ChannelAllotter rcsFreeAllotter;
	private ChannelAllotter rcsSmsAllotter;
	private ChannelAllotter rcsLmsAllotter;
	private ChannelAllotter rcsMmsAllotter;
	private ChannelAllotter naverAllotter;

	@Autowired
	public AllotterManager(ChannelAllotRateCache channelAllotRateCache) {
		this.channelAllotRateCache = channelAllotRateCache;
		this.smsAllotter = new ChannelAllotter(SendType.SMS.toString(), channelAllotRateCache);
		this.lmsAllotter = new ChannelAllotter(SendType.LMS.toString(), channelAllotRateCache);
		this.mmsAllotter = new ChannelAllotter(SendType.MMS.toString(), channelAllotRateCache);
		
		this.altAllotter = new ChannelAllotter(SendType.KKOALT.toString(), channelAllotRateCache);
		this.frtAllotter = new ChannelAllotter(SendType.KKOFRT.toString(), channelAllotRateCache);
		
		this.rcsFreeAllotter = new ChannelAllotter(SendType.RCS_FREE.toString(), channelAllotRateCache);
		this.rcsSmsAllotter = new ChannelAllotter(SendType.RCS_SMS.toString(), channelAllotRateCache);
		this.rcsLmsAllotter = new ChannelAllotter(SendType.RCS_LMS.toString(), channelAllotRateCache);
		this.rcsMmsAllotter = new ChannelAllotter(SendType.RCS_MMS.toString(), channelAllotRateCache);
		
		this.naverAllotter = new ChannelAllotter(SendType.NAVERT.toString(), channelAllotRateCache);
	}

	public String getProvider(String channel, String identifyKey) {
		String providerName = null;
		ChannelAllotter alloter = null;
		switch(channel) {
    	case "SMS":
    		alloter = smsAllotter;	
    		break;
    	case "LMS":
    		alloter = lmsAllotter;	
    		break;
    	case "MMS":
    		alloter = mmsAllotter;	
    		break;
    	case "RCS_FREE": case "RCS_CELL": case "RCS_DESC":
    		alloter = rcsFreeAllotter;	
    		break;
    	case "RCS_SMS": 
    		alloter = rcsSmsAllotter;	
    		break;
    	case "RCS_LMS": 
    		alloter = rcsLmsAllotter;	
    		break;    		
    	case "RCS_MMS": 
    		alloter = rcsMmsAllotter;	
    		break;
    	case "KKOALT":
    		alloter = altAllotter;	
    		break;
    	case "KKOFRT":
    		alloter = frtAllotter;	
    		break;    		
    	case "NAVERT":
    		alloter = naverAllotter;
    		break;
    	}
		if(alloter == null) return null;
		providerName = alloter.getProvider(identifyKey);
		return providerName;
	}

	public ChannelAllotRateCache getChannelAllotRateCache() {
		return channelAllotRateCache;
	}

	public ChannelAllotter getSmsAllotter() {
		return smsAllotter;
	}

	public ChannelAllotter getMmsAllotter() {
		return mmsAllotter;
	}

	public ChannelAllotter getKkoAllotter() {
		return altAllotter;
	}

	public ChannelAllotter getRcsAllotter() {
		return rcsFreeAllotter;
	}

	public ChannelAllotter getNaverAllotter() {
		return naverAllotter;
	}
}
