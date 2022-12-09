package kr.uracle.ums.core.service;

import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.service.send.kko.*;
import kr.uracle.ums.core.service.send.mms.*;
import kr.uracle.ums.core.service.send.naver.BaseNaverSendService;
import kr.uracle.ums.core.service.send.naver.MtsNaverSendService;

import kr.uracle.ums.core.service.send.rcs.AmRcsSendService;
import kr.uracle.ums.core.service.send.rcs.LotteRcsSendService;
import kr.uracle.ums.core.service.send.sms.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import kr.uracle.ums.core.service.send.rcs.BaseRcsSendService;
import kr.uracle.ums.core.service.send.rcs.LguRcsSendService;

import java.util.List;

@Service
public class UmsChannelProviderFactory {
	
    // SMS 공급업체 서비스
    @Autowired(required = true)
    private KtSmsSendService ktSmsSendService;
	@Autowired(required = true)
	private LotteSmsSendService lotteSmsSendService;
    @Autowired(required = true)
    private LguSmsSendService lguSmsSendService;
	@Autowired(required = true)
	private AmSmsSendService amSmsSendService;
	@Autowired(required = true)
	private ImoSmsSendService imoSmsSendService;

    // MMS 공급업체 서비스
    @Autowired(required = true)
    private KtMmsSendService ktMmsSendService;
	@Autowired(required = true)
	private LotteMmsSendService lotteMmsSendService;
    @Autowired(required = true)
    private LguMmsSendService lguMmsSendService;
	@Autowired(required = true)
	private AmMmsSendService amMmsSendService;
	@Autowired(required = true)
	private ImoMmsSendService imoMmsSendService;

    //알림톡 공급업체 서비스
    @Autowired(required = true)
    private LgcnsKkoAltSendService lgcnsKkoAltSendService;
	@Autowired(required = true)
	private LotteKkoAltSendService lotteKkoAltSendService;
	@Autowired(required = true)
	private AmKkoAltSendService amKkoAltSendService;
    
    //친구톡 공급업체 서비스
    @Autowired(required = true)
    private LgcnsKkoFrtSendService lgcnsKkoFrtSendService;
	@Autowired(required = true)
	private LotteKkoFrtSendService lotteKkoFrtSendService;
	@Autowired(required = true)
	private AmKkoFrtSendService amKkoFrtSendService;

    // RCS 공급업체 서비스
	@Autowired(required = true)
	private LotteRcsSendService lottRCSSendService;
    @Autowired(required = true)
    private LguRcsSendService lguRCSSendService;
	@Autowired(required = true)
	private AmRcsSendService amRCSSendService;

    
    //네이버톡 공급업체 서비스
    @Autowired(required = true)
    private MtsNaverSendService mtsNaverSendService;

    @Value("${SMS.PROVIDER:KT}")
    private String SMS_PROVIDER;

    private String KKO_PROVIDER=null;
    
    @Value("${RCS.PROVIDER:LGU}")
    private String RCS_PROVIDER;

    @Value("${NAVER.PROVIDER:MTS}")
    private String NAVER_PROVIDER;

    public BaseSmsSendService getSmsProviderService(){return getSmsProviderService(null);}
    public BaseSmsSendService getSmsProviderService(String providerName){
    	BaseSmsSendService service = null;
    	if(StringUtils.isBlank(providerName)) providerName = SMS_PROVIDER;
    	
    	switch(providerName.toUpperCase()) {
			case "AM":
				service = amSmsSendService;
				break;
			case "IMO":
				service = imoSmsSendService;
				break;
    		case "KT":
	    		service = ktSmsSendService;
				break;
			case "LOTTE":
				service = lotteSmsSendService;
				break;
    		case "LGU":
				service = lguSmsSendService;
				break;
    	}        
        return service;
    }

    public BaseMmsSendService getMmsProviderService(){return getMmsProviderService(null);}
    public BaseMmsSendService getMmsProviderService(String providerName){
    	BaseMmsSendService service = null;
    	if(StringUtils.isBlank(providerName)) providerName = SMS_PROVIDER;
    	
    	switch(providerName.toUpperCase()) {
			case "AM":
				service = amMmsSendService;
				break;
			case "IMO":
				service = imoMmsSendService;
				break;
			case "KT":
	    		service = ktMmsSendService;
				break;
			case "LOTTE":
				service = lotteMmsSendService;
				break;
			case "LGU":
				service = lguMmsSendService;
				break;
    	} 
        return service;
    }

    public BaseKkoAltSendService getKkoAltProviderService(){return getKkoAltProviderService(null);}
    public BaseKkoAltSendService getKkoAltProviderService(String providerName){
    	BaseKkoAltSendService service = null;
		if(KKO_PROVIDER == null){
			if(UmsInitListener.getUseKkoProviders().size()>0){
				this.KKO_PROVIDER = UmsInitListener.getUseKkoProviders().get(0);
			}else {
				this.KKO_PROVIDER = "LGCNS";
			}
		}
    	if(StringUtils.isBlank(providerName)) providerName = KKO_PROVIDER;
    	
    	switch(providerName.toUpperCase()) {
			case "LOTTE":
				service = lotteKkoAltSendService;
				break;
			case "AM":
				service = amKkoAltSendService;
				break;
			default:
	    		service = lgcnsKkoAltSendService;
				break;
    	}
    	
    	return service;
    }

    public BaseKkoFrtSendService getKkoFrtProviderService(){return getKkoFrtProviderService(null);}
    public BaseKkoFrtSendService getKkoFrtProviderService(String providerName){
    	BaseKkoFrtSendService service = null;

		if(StringUtils.isBlank(providerName)) {
			List<String> confKkoProviders = UmsInitListener.getUseKkoProviders();
			if(confKkoProviders.size()>0){
				providerName = UmsInitListener.getUseKkoProviders().get(0);
			}else {
				providerName = "LGCNS";
			}
		}
    	
    	switch(providerName.toUpperCase()) {
			case "LOTTE":
				service = lotteKkoFrtSendService;
				break;
			case "AM":
				service = amKkoFrtSendService;
				break;
			default:
	    		service = lgcnsKkoFrtSendService;
				break;
    	}
    	
    	return service;
    }
    
    public BaseRcsSendService getRcsProviderService(){return getRcsProviderService(null);}
    public BaseRcsSendService getRcsProviderService(String providerName){
    	BaseRcsSendService service = null;
    	if(StringUtils.isBlank(providerName)) providerName = RCS_PROVIDER;
    	
    	switch(providerName.toUpperCase()) {
			case "LOTTE":
				service = lottRCSSendService;
				break;
			case "AM":
				service = amRCSSendService;
				break;
			default:
	    		service = lguRCSSendService;
				break;
    	}
    	
    	return service;

    }

    public BaseNaverSendService getNaverProviderService(){return getNaverProviderService(null);}
    public BaseNaverSendService getNaverProviderService(String providerName){
    	BaseNaverSendService service = null;
    	if(StringUtils.isBlank(providerName)) providerName = NAVER_PROVIDER;
    	
    	switch(providerName) {
			default:
	    		service = mtsNaverSendService;
				break;
    	}
    	
    	return service;
    }

	public String getSMS_PROVIDER() { return SMS_PROVIDER; }
	
	public String getKKO_PROVIDER() { return KKO_PROVIDER; }
	
	public String getRCS_PROVIDER() { return RCS_PROVIDER; }
	
	public String getNAVER_PROVIDER() { return NAVER_PROVIDER; }
    
}
