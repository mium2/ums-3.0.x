package kr.uracle.ums.core.controller.extension.edoc;

import com.google.gson.Gson;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.processor.edoc.EdocDbMgr;
import kr.uracle.ums.core.processor.edoc.EdocProcessBean;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.httppoolclient.ResponseBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RequestMapping(value="/api/extension/edoc")
@Controller
public class KkoEdocS510Ctrl {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    private EdocDbMgr edocDbMgr;
    @Autowired
    private Gson gson;
    @RequestMapping(value = {"/kkoEdocS510Send.ums"}, produces = "application/json; charset=utf8")
    public @ResponseBody String kkoEdocSend(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, Object> requestBodyMap) {
        try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                response.setStatus(401);
                return "{\"errcode\":\"401\", \"errmsg\":\"not_allowed_ip\"}";
            }

            String accessToken = request.getHeader("Authorization");
            if(StringUtils.isBlank(accessToken)){
                response.setStatus(401);
                return "{\"errcode\":\"401\", \"errmsg\":\"Authorization\"}";
            }

            if(!requestBodyMap.containsKey("requestTranId")){
                response.setStatus(400);
                return "{\"errcode\":\"400\", \"errmsg\":\"invalid_param [requestTranId]\"}";
            }
            String requestTranId = requestBodyMap.get("requestTranId").toString();

            Map<String, String> httpHeadParam = new HashMap();
            httpHeadParam.put("Content-Type","application/json");
            httpHeadParam.put("Authorization", accessToken);

            requestBodyMap.remove("requestTranId"); // API 정의서의 포함되지 않은 값을 넘길경우 에러처리가 날수 있어 해당 정보는 삭제 후 넘김.

            int responsTimeout = 20;
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(responsTimeout * 1000).build();
            try {
                String apiUrl = "https://gw-cert.kakao.com/api/sign/request/S510";
                ResponseBean responseBean = HttpPoolClient.getInstance().sendPost(apiUrl, httpHeadParam, requestBodyMap, requestConfig);

                if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                    response.setStatus(200);
                    logger.info("받은정보 :" + responseBean.getBody());
                    putEdocDbResult(requestBodyMap, requestTranId, true, responseBean.getBody(), "200");
                } else {
                    response.setStatus(responseBean.getStatusCode());
                    logger.error("카카오 전자문서발송요청 http response code: " + responseBean.getStatusCode() + " http body:" + responseBean.getBody());
                    putEdocDbResult(requestBodyMap, requestTranId, false, null, "" + responseBean.getStatusCode());
                }
                return responseBean.getBody();
            }catch (Exception e){
                logger.error(e.getMessage());
                putEdocDbResult(requestBodyMap, requestTranId, false, null, "401");
                response.setStatus(401);
                Map<String,String> errMsgMap = new HashMap<>();
                errMsgMap.put("rtnMsg",e.getMessage());
                return gson.toJson(errMsgMap);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            response.setStatus(500);
            Map<String,String> errMsgMap = new HashMap<>();
            errMsgMap.put("rtnMsg",e.getMessage());
            return gson.toJson(errMsgMap);
        }
    }

    // 카카오 발송요청 응답 데이타 파싱 DB저장처리 큐에 등록
    private void putEdocDbResult(Map<String,Object> requestBodyMap, String requestTranId, boolean isSuccess, String respJsonStr, String httpStatusCode){
        EdocProcessBean edocProcessBean = new EdocProcessBean();
        edocProcessBean.setPROVIDER("KAKAO");
        edocProcessBean.setREQUEST_TRANID(requestTranId);
        if(requestBodyMap.containsKey("ci")) {
            edocProcessBean.setCI(requestBodyMap.get("ci").toString());
        }

        if(requestBodyMap.containsKey("data_hash")) {
            edocProcessBean.setCI(requestBodyMap.get("data_hash").toString());
        }
        edocProcessBean.setSENDMSG(gson.toJson(requestBodyMap));
        Map<String,Object> respMap;
        try {
            respMap = gson.fromJson(respJsonStr,Map.class);
        }catch (Exception e){
            logger.error("카카오 전자문서 성공응답 Json 파싱중 에러 발생 :"+e.getMessage());
            return;
        }

        if(isSuccess){
            // 성공
            if(respMap.containsKey("data")) {
                Map<String,String> succMap = (Map<String,String>)respMap.get("data");
                edocProcessBean.setDOCID(succMap.get("tx_id"));
                if(succMap.containsKey("result")){
                    if("N".equals(succMap.get("result"))){
                        edocProcessBean.setRESULTMSG("접수 실패");
                    }
                }
            }
        }else{
            // 실패
            edocProcessBean.setDOCID(requestTranId);
            edocProcessBean.setRESULTCODE(httpStatusCode);
            if(respMap==null){
                edocProcessBean.setRESULTMSG("카카오 전자문서 HTTT 에러(connect timed out)");
            }else{
                if(respMap.containsKey("errcode")){
                    edocProcessBean.setRESULTCODE(respMap.get("errcode").toString());

                }
                if(respMap.containsKey("errmsg")){
                    edocProcessBean.setRESULTMSG(respMap.get("errmsg").toString());
                }
            }
        }
        edocDbMgr.putWork(edocProcessBean);
    }
}
