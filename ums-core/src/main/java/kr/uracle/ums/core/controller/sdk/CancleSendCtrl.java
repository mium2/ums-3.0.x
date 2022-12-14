package kr.uracle.ums.core.controller.sdk;

import com.google.gson.Gson;

import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.dao.status.StatusReserveDao;
import kr.uracle.ums.core.processor.CancleManager;
import kr.uracle.ums.codec.redis.config.ErrorManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
@SuppressWarnings("unchecked")
@RequestMapping(value="/api/sdk")
@Controller
public class CancleSendCtrl {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required=true)
    private MessageSource messageSource;
    @Autowired(required = true)
    private Gson gson;
    @Autowired(required = true)
    private StatusReserveDao statusReserveDao;
    @Autowired (required = true)
    private CancleManager cancleManager;

    @RequestMapping(value = {"/umsCancleSendApi.ums"},produces = "application/json; charset=utf8")
    public @ResponseBody
    String umsSendPushMnApi(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String> reqMap){
        try {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            Map<String,String> verifyResultMap =  (Map<String,String>)request.getAttribute("verifyResultMap");
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                //????????? ???????????? ?????? ?????????????????? ???????????????.
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}", ERRMSG);
                return responseJsonString(ErrorManager.ERR_9404, ERRMSG, new HashMap<String,Object>(), "/umsCancleSendApi.ums",reqMap);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // ?????? ??????????????? ????????? ?????? ?????????.
            /////////////////////////////////////////////////////////////////////////////////////////
            // ?????? ???????????? ?????? ?????? ???????????????. APPID, SERVICECODE, PUSH_MSG
            try {
                String reqSEQNO = request.getParameter("PROCESS_SEQNO").trim();
                String reqSENDERID = request.getParameter("SENDERID").trim();
                String reqSENDGROUPCODE = request.getParameter("SENDGROUPCODE").trim();
//                String reqDELIVERY_TYPE = request.getParameter("DELIVERY_TYPE").trim();
                // ?????????????????? ???????????? ???????????? ??????
                cancleManager.putCancleSend(reqSEQNO);
                // ???????????? ????????????
                Map<String,Object> dbParamMap = new HashMap<>();
                dbParamMap.put("RESERVE_SEQNO",reqSEQNO); // ??????????????? ????????? reqSEQNO ????????????????????? reserveSeqno??? ?????????.
                statusReserveDao.delete(dbParamMap);

                Map<String,Object> resultBodyMap = new HashMap<String,Object>();
                resultBodyMap.put("PROCESS_SEQNO",reqSEQNO);
                resultBodyMap.put("SENDERID",reqSENDERID);
                resultBodyMap.put("SENDGROUPCODE",reqSENDGROUPCODE);
                return responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, "umsCancleSendApi.ums", reqMap);
            } catch (Exception e) {
                logger.info("[REQ cancel_sendingMsg.ctl fail] : SEQNO:{}",request.getParameter("SEQNO"));
                e.printStackTrace();
                return responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", UMS?????? ????????? ?????????????????????.("+e.getMessage()+")", new HashMap<String,Object>(),"/umsCancleSendApi.ums",reqMap);
            }

            /////////////////////////////////////////////////////////////////////////////////////////

        } catch (Exception e) {
            e.printStackTrace();
            return responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(),"/umsCancleSendApi.ums",reqMap);
        }
    }

    public String responseJsonString(String resultCode, String resultMsg, Map<String,Object> resultBodyMap, String url, Object reqParamObj){
        Map<String,Object> rootMap = new HashMap<String, Object>();
        Map<String,Object> headMap = new HashMap<String, Object>();
        if(resultCode.equals("200")){
            resultCode = "0000";
        }
        headMap.put(Constants.RESULT_CODE, resultCode);
        headMap.put(Constants.RESULT_MSG, resultMsg);

        rootMap.put("HEADER", headMap);
        rootMap.put("BODY", resultBodyMap);
        // ApplicationContext.xml??? ?????? ?????? ?????? model id jsonReport ?????? ?????? json ???????????? ??????
        String responseJson = gson.toJson(rootMap);
        if(logger.isDebugEnabled()){
            logger.debug("[REQ {}]: {}", url, reqParamObj.toString());
            logger.debug("[RES {}]: {}", responseJson);
        } else {
            if(!"0000".equals(resultCode)){
                logger.info("[RES {} fail]: ERRCODE:[{}] ERRMSG:{} reqParam:{}", url, resultCode, resultMsg, reqParamObj.toString());
            }
        }
        return responseJson;
    }
}
