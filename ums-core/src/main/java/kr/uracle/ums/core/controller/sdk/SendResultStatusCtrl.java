package kr.uracle.ums.core.controller.sdk;

import com.google.gson.Gson;

import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.util.StringUtil;
import kr.uracle.ums.codec.redis.config.ErrorManager;

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
@SuppressWarnings("unchecked")
@RequestMapping(value="/api/sdk")
@Controller
public class SendResultStatusCtrl {
    private Logger logger = LoggerFactory.getLogger(SendResultStatusCtrl.class);

    @Autowired
    protected SessionLocaleResolver localeResolver;

    @Autowired(required = true)
    private Gson gson;

    @Autowired(required = true)
    private Properties myProperties = null;

    @Autowired(required=true)
    private MessageSource messageSource;

    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    private SqlSessionTemplate sqlSessionTemplate;

    @RequestMapping(value = { "/sendResultlist.ums" }, produces = "application/json; charset=utf8")
    public @ResponseBody
    String list(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, String> reqParamMap) {
        try {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            Map<String,String> verifyResultMap =  (Map<String,String>)request.getAttribute("verifyResultMap");
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                //접근이 허용되지 않은 아이피일경우 실패처리함.
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}", ERRMSG);
                return responseJsonString(ErrorManager.ERR_9404, ERRMSG, new HashMap<String,Object>(), request, reqParamMap);
            }
            /////////////////////////////////////////////////////////////////////////////////////////
            // 필수 파라미터 검증 로직. startDt, endDt, PUSH_MSG
            if(!reqParamMap.containsKey("startDt") || !reqParamMap.containsKey("endDt") || !reqParamMap.containsKey("tranType") || !reqParamMap.containsKey("processSeqno")){
                return responseJsonString(ErrorManager.ERR_1001, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1001)+", [startDt,endDt,tranType,processSeqno]", new HashMap<String, Object>(), request, reqParamMap);
            }
            List<SendResultVo> list = sqlSessionTemplate.selectList("mybatis.sdk.selSendResultList",reqParamMap);
            Map<String,Object> resultMsg = new HashMap<>();
            resultMsg.put("DATA", list);

            /////////////////////////////////////////////////////////////////////////////////////////
            return responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultMsg, request, reqParamMap);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+","+e.getMessage(), new HashMap<String, Object>(), request, reqParamMap);
        }
    }

    private Map<String, Object> paging(List<SendResultVo> list,int tot_cnt, int page_size, int page_num, int pageBSzie) {
        Map<String, Object> resultDataMap = new HashMap<String, Object>();
        int page_cnt = 1;
        int firstPage = 1;
        if (!list.isEmpty()) {
            page_cnt = tot_cnt / page_size;
            if (tot_cnt > (page_cnt * page_size))
                page_cnt += 1;

            firstPage = (((page_num - 1) / pageBSzie) * pageBSzie) + 1;
        }
        List<String> pageList = new ArrayList<String>();
        if (!list.isEmpty()) {
            for (int i = firstPage; i < firstPage + pageBSzie; i++) {
                pageList.add(""+i);
                if (i == page_cnt)
                    break;
            }
        }
        resultDataMap.put("list", list);
        resultDataMap.put("pageList", pageList);
        resultDataMap.put("totCnt", ""+tot_cnt);
        resultDataMap.put("pageCnt", ""+page_cnt);
        resultDataMap.put("pageNum", ""+page_num);
        return resultDataMap;
    }

    private String responseJsonString(String resultCode, String resultMsg, Map<String,Object> resultBodyMap, HttpServletRequest request, Object reqParamObj){
        Map<String,Object> rootMap = new HashMap<String, Object>();
        Map<String,Object> headMap = new HashMap<String, Object>();
        if(resultCode.equals("200")){
            resultCode = "0000";
        }
        headMap.put(Constants.RESULT_CODE, resultCode);
        headMap.put(Constants.RESULT_MSG, resultMsg);

        rootMap.put("HEADER", headMap);
        rootMap.put("BODY", resultBodyMap);
        // ApplicationContext.xml에 정의 되어 있는 model id jsonReport 호출 하여 json 포멧으로 응답

        String[] tempURI = request.getRequestURI().split("/");

        String responseJson = gson.toJson(rootMap);
        logger.debug("[REQ /{}]: {}", tempURI[tempURI.length - 1], gson.toJson(reqParamObj));
        if ("0000".equals(resultCode)) {
            logger.debug("[RES {}]: {}", responseJson);
        } else {
            logger.info("[RES /{} fail]: ERRCODE:[{}] ERRMSG:{} reqParam:{}", tempURI[tempURI.length - 1], resultCode, resultMsg, gson.toJson(reqParamObj));
        }
        return responseJson;
    }
}
