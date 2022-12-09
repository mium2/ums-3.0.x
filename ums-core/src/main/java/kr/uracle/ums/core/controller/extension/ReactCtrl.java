package kr.uracle.ums.core.controller.extension;

import kr.uracle.ums.core.processor.react.ReactMgr;
import kr.uracle.ums.core.processor.react.ReactProcessBean;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.codec.redis.config.ErrorManager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RequestMapping(value="/api/extension")
@Controller
public class ReactCtrl {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    private ReactMgr reactMgr;

    @RequestMapping(value = {"/react.ums"},produces = "application/json; charset=utf8")
    public @ResponseBody
    String umsSendPushMnApi(HttpServletRequest request, HttpServletResponse response, @ModelAttribute ReactProcessBean reactProcessBean){
        try {
            /////////////////////////////////////////////////////////////////////////////////////////
            // 해당 아래부분의 로직을 구현 하세요.
            /////////////////////////////////////////////////////////////////////////////////////////
            // 필수 파라미터 검증 로직 추가해야함. APPID, SERVICECODE, PUSH_MSG
            try {
                if(StringUtils.isBlank(reactProcessBean.getCUST_TRANSKEY())){
                    return umsSendCommonService.responseJsonString(ErrorManager.ERR_1001, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1001)+", [CUST_TRANSKEY]", request.getRequestURI(), reactProcessBean);
                }
                reactMgr.putWork(reactProcessBean);
                Map<String,Object> resultBodyMap = new HashMap<String,Object>();
                return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, request.getRequestURI(), reactProcessBean);
            } catch (Exception e) {
                logger.info("[REQ cancel_sendingMsg.ctl fail] : SEQNO:{}",request.getParameter("SEQNO"));
                e.printStackTrace();
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", UMS발송 취소에 실패하였습니다.("+e.getMessage()+")", new HashMap<String,Object>(),request.getRequestURI(),reactProcessBean);
            }

            /////////////////////////////////////////////////////////////////////////////////////////

        } catch (Exception e) {
            e.printStackTrace();
            return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(),request.getRequestURI(),reactProcessBean);
        }
    }

}
