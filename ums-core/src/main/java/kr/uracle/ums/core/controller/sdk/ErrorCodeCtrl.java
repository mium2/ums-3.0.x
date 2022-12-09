package kr.uracle.ums.core.controller.sdk;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;

import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.codec.redis.config.ErrorManager;

@Controller
@RequestMapping(value = "/api/error")
public class ErrorCodeCtrl {
	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
		
	private final Gson gson = new Gson();
	
	@RequestMapping(value = { "/all.ums" }, method = RequestMethod.POST, produces = "application/json; charset=utf8")
	public @ResponseBody String getAllCode(Locale locale, HttpServletRequest request, HttpServletResponse response){
		try {
	        //접근이 허용되지 않은 아이피일경우 실패처리함.
	        if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
	            String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
	            logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
	            return responseJsonString(ErrorManager.ERR_9404, ERRMSG, new HashMap<String, Object>());
	        }

	        Map<String, String> errCodeMap = ErrorManager.getInstance().getErrorCodeMap();
	        Map<String, Object> errMap = new HashMap<String, Object>();
	        for(Entry<String, String>  e: errCodeMap.entrySet()) {
	        	errMap.put(e.getKey(), (Object)e.getValue());
	        }

	        return responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), errMap);
		} catch (Exception e) {
			e.printStackTrace();
			return responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String, Object>());
		}
	}
	
	@RequestMapping(value = { "/search.ums" }, method = RequestMethod.POST, produces = "application/json; charset=utf8")
	public @ResponseBody String searchMsg(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> requestBodyMap){
		try {
	        //접근이 허용되지 않은 아이피일경우 실패처리함.
	        if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
	            String ERRMSG =  ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
	            logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
	            return responseJsonString(ErrorManager.ERR_9404, ERRMSG, new HashMap<String,Object>());
	        }
	        

	        String errorCode = requestBodyMap.get("ERROR_CODE") !=null ? requestBodyMap.get("ERROR_CODE").toString():"";
	        
	        if(StringUtils.isBlank(errorCode)) {
	        	return responseJsonString(ErrorManager.ERR_1001, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1001)+", [ERROR_CODE]", new HashMap<String,Object>());
	        }
	        
	        String msg =ErrorManager.getInstance().getMsg(errorCode); 
	      
	        Map<String,Object> bodyMap  = new HashMap<String,Object>(10);
	        bodyMap.put("ERROR_MSG", msg);
	        
	        return responseJsonString(ErrorManager.SUCCESS,  ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), bodyMap);
		} catch (Exception e) {
			e.printStackTrace();
			return responseJsonString(ErrorManager.ERR_500,  ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>());
		}
	}
	
    public String responseJsonString(String resultCode, String resultMsg, Map<String, Object> resultBodyMap){
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
        String responseJson = gson.toJson(rootMap);

        return responseJson;
    }
	
	
}
