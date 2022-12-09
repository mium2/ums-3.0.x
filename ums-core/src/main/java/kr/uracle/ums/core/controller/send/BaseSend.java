package kr.uracle.ums.core.controller.send;

import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;

import com.google.gson.Gson;

import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.service.UmsCsvMemberSendService;
import kr.uracle.ums.core.service.UmsCsvSendService;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.service.UmsSendMemberService;
import kr.uracle.ums.core.service.UmsSendService;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.vo.ReqUmsSendVo;

public abstract class BaseSend {
    protected Logger logger = LoggerFactory.getLogger(BaseSend.class);
    
    @Autowired(required = true)
    protected UmsSendCommonService umsSendCommonService;
    
    @Autowired(required = true)
    protected SentInfoManager sentInfoManager;
    
    @Value("${UMS.NAS.YN:N}")
    protected String NAS_USE_YN;
    
    @Autowired(required = true)
    protected UmsCsvSendService umsCsvSendService;
    
    @Autowired(required = true)
    protected UmsCsvMemberSendService umsCsvMemberSendService;
    
    @Autowired(required = true)
    protected UmsSendService umsSendService;
    
    @Autowired(required = true)
    protected UmsSendMemberService umsSendMemberService;
    
    @Autowired(required=true)
    protected MessageSource messageSource;

    @Autowired(required = true)
    protected Gson gson;

    //TODO 예약발송 테스트시 관리페이지에서 예약발송 케이스에서 마스터테이블 정상 분기 체크
    protected UmsSendMsgBean getUmsSendMsgBean(ReqUmsSendVo reqUmsSendVo) throws Exception{        
        return umsSendCommonService.makeUmsSendMsgBean(reqUmsSendVo);
    }

    protected abstract Map<String,Object> send(ReqUmsSendVo reqUmsSendVo, String requestUri, Locale locale)throws Exception;

}
