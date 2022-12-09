package kr.uracle.ums.core.controller.monitoring;

import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;
import kr.uracle.ums.codec.redis.config.ErrorManager;

import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
public class TpmCtrl {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required=true)
    protected MessageSource messageSource;
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    private RedisTemplate redisTemplate;
    @Value("${UMS.SELF.URL:}")
    private String localUmsUrl;

    @RequestMapping(value = {"/getSystemInfo.ums"},produces = "application/json; charset=utf8")
    public @ResponseBody
    String getSystemInfo(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, Object> reqParamMap){
        try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(), reqParamMap);
            }
            /////////////////////////////////////////////////////////////////////////////////////////
            // 필수 파라미터 검증 로직 추가해야함. APPID, SERVICECODE, PUSH_MSG


            Map<String,Object> resultBodyMap = new HashMap<String,Object>();
            Map<String,Object> summaryMap = TpsManager.getInstance().getSummaryData();
            resultBodyMap.put("data",summaryMap);
            return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, request.getRequestURI(), reqParamMap);
            /////////////////////////////////////////////////////////////////////////////////////////

        } catch (Exception e) {
            e.printStackTrace();
            return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(),request.getRequestURI(),reqParamMap);
        }
    }

    @RequestMapping(value = {"/tpsInitApi.ums"},produces = "application/json; charset=utf8")
    public @ResponseBody
    String tpsInitApi(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, Object> reqParamMap){
        try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(), reqParamMap);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // 필수 파라미터 검증 로직 추가해야함. APPID, SERVICECODE, PUSH_MSG
            Set umsUrlSet = redisTemplate.opsForSet().members("UMS_SERVER_URL");
            umsUrlSet.remove(localUmsUrl);

            List<Object> umsUrls = new ArrayList();
            umsUrls.add(localUmsUrl);
            for(Object usmUrl : umsUrlSet){
                umsUrls.add(usmUrl);
            }
            Map<String,Object> resultBodyMap = new HashMap<String,Object>();
            resultBodyMap.put("umsUrls",umsUrls);
            return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, request.getRequestURI(), reqParamMap);
            /////////////////////////////////////////////////////////////////////////////////////////

        } catch (Exception e) {
            e.printStackTrace();
            return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(),request.getRequestURI(),reqParamMap);
        }
    }

    @RequestMapping(value = {"/monitorApi.ums"},produces = "application/json; charset=utf8")
    public @ResponseBody
    String monitorApi(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, Object> reqParamMap){
        try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(), reqParamMap);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // 필수 파라미터 검증 로직 추가해야함.
            Map<String, Object> resultBodyMap = new HashMap<String,Object>();

            if(!reqParamMap.containsKey("umsUrl")) {
                resultBodyMap = TpsManager.getInstance().getSummaryData();
                return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, request.getRequestURI(), reqParamMap);
            }else{
                String reqUmsUrl = reqParamMap.get("umsUrl").toString();
                String callUrl = "";
                // 다른 UMS서버의 TPM조회 요청
                if(reqUmsUrl.lastIndexOf("/")>-1){
                    callUrl = reqUmsUrl + "monitorApi.ums";
                }else{
                    callUrl = reqUmsUrl + "/monitorApi.ums";
                }
                try {
                    Map<String, String> httpHeadParam = new HashMap<String, String>();
                    httpHeadParam.put("Content-Type", "application/x-www-form-urlencoded");
                    Map<String, Object> postParam = new HashMap<String, Object>();
                    postParam.put("umsUrl", reqUmsUrl);
                    RequestConfig requestConfig = RequestConfig.custom()
                            .setSocketTimeout(3000)
                            .setConnectTimeout(3000)
                            .setConnectionRequestTimeout(3000)
                            .build();

                    ResponseBean responseBean = HttpPoolClient.getInstance().sendPost(callUrl, httpHeadParam, postParam, requestConfig);
                    if (response.getStatus() == 200 || response.getStatus() == 201) {
                        return responseBean.getBody();
                    } else {
                        return umsSendCommonService.responseJsonString(ErrorManager.ERR_5009, ErrorManager.getInstance().getMsg(ErrorManager.ERR_5009)+", "+responseBean.getStatusCode()+":"+responseBean.getBody(), new HashMap<String, Object>(),request.getRequestURI(), reqParamMap);
                    }
                }catch(Exception e){
                    return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(),request.getRequestURI(),reqParamMap);
                }

            }
            /////////////////////////////////////////////////////////////////////////////////////////

        } catch (Exception e) {
            e.printStackTrace();
            return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(),request.getRequestURI(),reqParamMap);
        }
    }

    @RequestMapping(value = {"/tpsChartApi.ums"},produces = "application/json; charset=utf8")
    public @ResponseBody
    String tpsInfoApi(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, Object> reqParamMap){
        try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(), reqParamMap);
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // 필수 파라미터 검증 로직 추가해야함.
            Map<String, Object> resultBodyMap = new HashMap<String,Object>();

            if(!reqParamMap.containsKey("umsUrl")) {
                resultBodyMap = TpsManager.getInstance().getLineChartDatas();
                return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, request.getRequestURI(), reqParamMap);
            }else{
                String reqUmsUrl = reqParamMap.get("umsUrl").toString();
                String callUrl = "";
                // 다른 UMS서버의 TPM조회 요청
                if(reqUmsUrl.lastIndexOf("/")>-1){
                    callUrl = reqUmsUrl + "tpsChartApi.ums";
                }else{
                    callUrl = reqUmsUrl + "/tpsChartApi.ums";
                }
                try {
                    Map<String, String> httpHeadParam = new HashMap<String, String>();
                    httpHeadParam.put("Content-Type", "application/x-www-form-urlencoded");
                    Map<String, Object> postParam = new HashMap<String, Object>();
                    postParam.put("umsUrl", reqUmsUrl);
                    RequestConfig requestConfig = RequestConfig.custom()
                            .setSocketTimeout(3000)
                            .setConnectTimeout(3000)
                            .setConnectionRequestTimeout(3000)
                            .build();

                    ResponseBean responseBean = HttpPoolClient.getInstance().sendPost(callUrl, httpHeadParam, postParam, requestConfig);
                    if (response.getStatus() == 200 || response.getStatus() == 201) {
                        return responseBean.getBody();
                    } else {
                        redisTemplate.opsForSet().remove("UMS_SERVER_URL", reqUmsUrl);
                        return umsSendCommonService.responseJsonString(ErrorManager.ERR_5009+ "", ErrorManager.getInstance().getMsg(ErrorManager.ERR_5009)+", "+responseBean.getStatusCode()+":"+responseBean.getBody(), new HashMap<String, Object>(),request.getRequestURI(), reqParamMap);
                    }
                }catch(Exception e){
                    redisTemplate.opsForSet().remove("UMS_SERVER_URL", reqUmsUrl);
                    return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(), request.getRequestURI(), reqParamMap);
                }

            }
            /////////////////////////////////////////////////////////////////////////////////////////

        } catch (Exception e) {
            e.printStackTrace();
            return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(), request.getRequestURI(), reqParamMap);
        }
    }

}
