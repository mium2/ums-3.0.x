package kr.uracle.ums.core.controller.extension.edoc;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.config.ErrorManager;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping(value="/api/extension/edoc")
@Controller
public class NaverEdocCtrl {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    @Autowired(required = true)
    private EdocDbMgr edocDbMgr;
    @Autowired
    private Gson gson;

    @RequestMapping(value = {"/naverEdocSend.ums"}, produces = "application/json; charset=utf8")
    public @ResponseBody String naverEdocSend(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> requestBodyMap) {
        try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                response.setStatus(400);
                return "{\"errorCode\":\"400\", \"errorMsg\":\"invalid_param\"}";
            }

            String naverClientId = request.getHeader("X-Naver-Client-Id");
            if(StringUtils.isBlank(naverClientId)){
                response.setStatus(402);
                return "{\"errorCode\":\"402\", \"errorMsg\":\"invalid_client_info [X-Naver-Client-Id]\"}";
            }
            String naverClientSecret = request.getHeader("X-Naver-Client-Secret");
            if(StringUtils.isBlank(naverClientId)){
                response.setStatus(402);
                return "{\"errorCode\":\"402\", \"errorMsg\":\"invalid_client_info [X-Naver-Client-Secret]\"}";
            }

            if(!requestBodyMap.containsKey("requestTranId")){
                response.setStatus(400);
                return "{\"errorCode\":\"400\", \"errorMsg\":\"invalid_param [requestTranId]\"}";
            }
            String requestTranId = requestBodyMap.get("requestTranId").toString();

            if(!requestBodyMap.containsKey("data")){
                response.setStatus(400);
                return "{\"errorCode\":\"400\", \"errorMsg\":\"invalid_param [data]\"}";
            }

            Object dataObj = requestBodyMap.get("data");

            Map<String, String> httpHeadParam = new HashMap();
            httpHeadParam.put("Content-Type","application/json");
            httpHeadParam.put("X-Naver-Client-Id", naverClientId);
            httpHeadParam.put("X-Naver-Client-Secret", naverClientSecret);

            Map<String, Object> postParam = new HashMap<String,Object>();
//            String jsonData = gson.toJson(dataObj);
//            logger.info("data:"+jsonData);
            postParam.put("data",dataObj);

            int responsTimeout = 30;
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(responsTimeout * 1000).build();

            String apiUrl = "https://nsign-gw.naver.com/invoice/v1/request";
            ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(apiUrl, httpHeadParam, postParam, requestConfig);

            if (responseBean.getStatusCode() == 200 || responseBean.getStatusCode() == 201) {
                response.setStatus(200);
                logger.info("받은정보 :"+responseBean.getBody());
                putEdocDbResult(dataObj, requestTranId, true, responseBean.getBody(),"200");
            } else {
                response.setStatus(responseBean.getStatusCode());
                logger.error("네이버 전자문서 발송요청 http response code: "+responseBean.getStatusCode()+" http body:"+responseBean.getBody());
                putEdocDbResult(dataObj, requestTranId, false, null, ""+responseBean.getStatusCode());
            }

            return responseBean.getBody();

        } catch (Exception e) {
            response.setStatus(500);
            Map<String,String> errMsgMap = new HashMap<>();
            errMsgMap.put("rtnMsg",e.getMessage());
            return gson.toJson(errMsgMap);
        }
    }

    private void putEdocDbResult(Object dataObj, String requestTranId, boolean isSuccess, String respJsonStr, String httpStatusCode){
        List<Object> dataObjList = (List<Object>) dataObj;
        List<Map<String,String>> responseDatas = null;
        if(isSuccess){
            responseDatas = gson.fromJson(respJsonStr,List.class);
        }
        for(int i=0; i<dataObjList.size(); i++){
            try {
                Object workObj = dataObjList.get(i);
                Map<String, Object> workObjMap = (Map<String, Object>) workObj;
                EdocProcessBean edocProcessBean = new EdocProcessBean();
                edocProcessBean.setPROVIDER("NAVER");
                edocProcessBean.setREQUEST_TRANID(requestTranId);
                if (workObjMap.containsKey("clientDocId")) {
                    edocProcessBean.setDOCID(workObjMap.get("clientDocId").toString());
                }
                if (workObjMap.containsKey("ci")) {
                    edocProcessBean.setCI(workObjMap.get("ci").toString());
                }
                if (workObjMap.containsKey("documentHash")) {
                    edocProcessBean.setDOCHASH(workObjMap.get("documentHash").toString());
                }
                edocProcessBean.setSENDMSG(gson.toJson(workObjMap));
                // 결과정보 셋팅
                if(responseDatas!=null && responseDatas.size()>i) {
                    Map<String, String> responseMap = responseDatas.get(i);
                    if(responseMap.containsKey("errorCode")){ // 실패일 경우 해당 값 있음.
                        edocProcessBean.setRESULTCODE(responseMap.get("errorCode"));
                        edocProcessBean.setRESULTMSG(responseMap.get("errorMsg"));
                    }
                }else{
                    // 네이버 HTTP 실패 일 경우
                    edocProcessBean.setRESULTCODE(httpStatusCode);
                    edocProcessBean.setRESULTMSG("네이버 HTTP 응답에러");
                }
                edocDbMgr.putWork(edocProcessBean);
            }catch (Exception e){
                logger.error("전자문서 결과 DB 저장처리 중 에러 : "+ e.getMessage());
            }
        }
    }

}
