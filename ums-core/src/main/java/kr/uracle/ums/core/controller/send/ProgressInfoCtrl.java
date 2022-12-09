package kr.uracle.ums.core.controller.send;

import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.processor.bean.SentInfoBean;
import kr.uracle.ums.core.service.UmsSendCommonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RequestMapping(value="/api/send")
@Controller
public class ProgressInfoCtrl{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Autowired(required = true)
    private SentInfoManager sentInfoManager;

    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;

    @ResponseBody
    @RequestMapping(value = {"/sendProcessInfo.ums"},produces = "application/json; charset=utf8")
    public String sendProcessInfo(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String,String> reqParamMap){
        try {

            if(!reqParamMap.containsKey("processSeqno")){
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_1001, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1001), request.getRequestURI(), reqParamMap);
            }
            Map<String,Object> resultBodyMap = new HashMap<String,Object>();
            Map<String,String> processMap = new HashMap<String, String>();
            // processSeqno 체크
            SentInfoBean sentInfoBean = sentInfoManager.getSentInfoMap(TransType.REAL,reqParamMap.get("processSeqno"));
            if(sentInfoBean==null){
                // 프로세스가 완료 되었다고 전달.
                processMap.put("processPercent","100");
                processMap.put("processTxt","Completed.");
            }else{
                int totalCnt = sentInfoBean.getREQ_SEND_CNT();
                int processCnt = sentInfoBean.getSUCC_CNT()+sentInfoBean.getFAIL_CNT();

                int processPercent = 0;
                processPercent = processCnt*100/totalCnt;
                if(processPercent==0 && processCnt>=1){
                    processPercent = 1;
                }
                processMap.put("processPercent",""+processPercent);
                processMap.put("processTxt",processCnt+"/"+totalCnt);
            }
            resultBodyMap.put("processInfo",processMap);

            return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, request.getRequestURI(), reqParamMap);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+ e.getMessage(), request.getRequestURI(), reqParamMap);
        }
    }

}
