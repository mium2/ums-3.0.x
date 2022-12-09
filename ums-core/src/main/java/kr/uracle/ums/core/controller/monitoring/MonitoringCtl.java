package kr.uracle.ums.core.controller.monitoring;

import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.service.UmsMonitoringService;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.service.queue.FileQueueHelperManager;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

@Controller
@RequestMapping("/api/monit")
public class MonitoringCtl {
	
	class RequestBodyInfo{
		public RequestBodyInfo(boolean required, Class type, String defaultValue) {
			this.required = required;			this.type = type;			this.defaultValue = defaultValue;
		}
		private boolean required;
		private Class<?> type;
		private String defaultValue;
		public boolean isRequired() { return required; }
		public void setRequired(boolean required) { this.required = required; }
		public Class<?> getType() { return type; }
		public String getDefaultValue() { return defaultValue; }
		public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
	}
	
	protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    @Autowired(required=true)
    protected MessageSource messageSource;
    
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    
    @Autowired(required = true)
    private UmsMonitoringService umsMonitoringService;
    
    // 등록 요청 전문 필수 여부
    private Map<String, RequestBodyInfo> requestBodyInfoMap = new HashMap<String, RequestBodyInfo>(15);

    public MonitoringCtl() {
    	requestBodyInfoMap.put("PROGRAM_ID", new RequestBodyInfo(true, String.class, null));
    	requestBodyInfoMap.put("SERVER_ID", new RequestBodyInfo(true, String.class, null));
    	requestBodyInfoMap.put("SERVER_NAME", new RequestBodyInfo(false, String.class, "$SERVER_ID"));
    	requestBodyInfoMap.put("MONITOR_URL", new RequestBodyInfo(false, String.class, null));
    	requestBodyInfoMap.put("REQUESTER_ID", new RequestBodyInfo(false, String.class, "$PROGRAM_ID"));
    	requestBodyInfoMap.put("CHART", new RequestBodyInfo(true, Map.class, null));
    	requestBodyInfoMap.put("SUMMARY", new RequestBodyInfo(true, Map.class, null));
	}
    
    

	//	REQ BODY 구성
	//	{
	//	
	//		"PROGRAM_ID": "STRING:필수",
	//		"SERVER_ID":"STRING:필수",
	//		"SERVER_NAME":"STRING:옵션",
	//		"MONITOR_URL":"STRING:옵션",
	//		"REQUESTER_ID":"STRING:옵션-DEFAULT:PROGRAM_ID",
	//		"CHART" :{
	//			"Map<String,Object>:필수",
	//		},
	//		
	//		"SUMMARY":{
	//			"Map<String,Object>:필수"
	//		}
	//	}
	//	REP BODY 구성
	//	{
	//		"HEADER" : {
	//			"RESULTCODE"	:"",
	//			"RESULTMSG"		:""	
	//		}
	//	}
	@RequestMapping(value = {"/report.ums"}, produces = "application/json; charset=utf8")
	public @ResponseBody String storeDatas(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> requestBodyMap){
		try {
	        //접근이 허용되지 않은 아이피일경우 실패처리함.
	        if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
	            String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
	            logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
	            return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(), requestBodyMap);
	        }
	        
	        // 리퀘스트 검증 - 필수 필드 값 여부 확인
	        String errorMsg = validateRequestBody(requestBodyMap);
	        if(errorMsg != null) return umsSendCommonService.responseJsonString(ErrorManager.ERR_1001, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1001)+", "+errorMsg, new HashMap<String,Object>(), request.getRequestURI(), requestBodyMap);
	        
	        // 데이터 저장/갱신
	        errorMsg = umsMonitoringService.storeDatas(requestBodyMap);
	        final String rsltCode = (errorMsg == null) ? ErrorManager.SUCCESS: ErrorManager.ERR_500;
	        
	        return umsSendCommonService.responseJsonString(rsltCode, errorMsg == null ?  ErrorManager.getInstance().getMsg(rsltCode):  ErrorManager.getInstance().getMsg(rsltCode)+", "+errorMsg , new HashMap<String,Object>(), request.getRequestURI(), requestBodyMap);
		} catch (Exception e) {
			e.printStackTrace();
			return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(), request.getRequestURI(), requestBodyMap);
		}
	}
    
	
	//	REQ BODY 구성 - 없음
	//	REP BODY 구성
	//	{
	//		"HEADER" : {
	//			"RESULTCODE"	:"",
	//			"RESULTMSG"		:""	
	//		},
	//		
	//		"BODY" :{
	//			"CHART" :[{}]
	//				
	//			
	//		}
	//	}
    @RequestMapping(value = {"/chart.ums"}, method = RequestMethod.GET, produces = "application/json; charset=utf8")
	public @ResponseBody String selectChartDatas(Locale locale, HttpServletRequest request, HttpServletResponse response){
    	try {
    		   //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(), null);
            }
            
            // DB 조회
            final List<Map<String, Object>> chartMapList = umsMonitoringService.getChartDatas();
       
            // DATA 전달    	
            Map<String,Object> resultBodyMap = new HashMap<String,Object>();
            if(chartMapList != null)resultBodyMap.put("CHART", chartMapList);
            
    		return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, request.getRequestURI(), null);
		} catch (Exception e) {
			e.printStackTrace();
			return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(), request.getRequestURI(), null);
		}
    	
	}
    
    
	//	REQ BODY 구성 - 없음
	//	REP BODY 구성
	//	{
	//		"HEADER" : {
	//			"RESULTCODE"	:"",
	//			"RESULTMSG"		:""	
	//		},
	//		
	//		"BODY" :{
	//			"SUMMARY":{
	//				
	//			}
	//		}
	//	}
    @RequestMapping(value = {"/summary.ums"}, method = RequestMethod.GET, produces = "application/json; charset=utf8")
	public @ResponseBody String selectSummaryDatas(Locale locale, HttpServletRequest request, HttpServletResponse response){
    	try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(), null);
            }
        
            // DB 조회 - DATA 변경 일자 확인 - 상태 필드 추가 후 전달
            List<Map<String, Object>> summaryMap = umsMonitoringService.getSummaryDatas();

            Map<String,Object> resultBodyMap = new HashMap<String,Object>();
            if(summaryMap != null)resultBodyMap.put("SUMMARY", summaryMap);
            
    		return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, request.getRequestURI(), null);
		} catch (Exception e) {
			e.printStackTrace();
			return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(), request.getRequestURI(), null);
		}
	}
    
	//	REQ BODY 구성 - 없음
	//	REP BODY 구성
	//	{
	//		"HEADER" : {
	//			"RESULTCODE"	:"",
	//			"RESULTMSG"		:""	
	//		},
	//		
	//		"BODY" :{
	//			"SUMMARY":{
	//				
	//			}
	//		}
	//	}
    @RequestMapping(value = {"/queue/summary.ums"}, method = RequestMethod.GET, produces = "application/json; charset=utf8")
	public @ResponseBody String selectAllFileQueueInfo(Locale locale, HttpServletRequest request, HttpServletResponse response){
    	try {
            //접근이 허용되지 않은 아이피일경우 실패처리함.
            if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
                String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
                logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
                return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, request.getRequestURI(), null);
            }
        
            // DB 조회 - DATA 변경 일자 확인 - 상태 필드 추가 후 전달
            Map<String, Map<String, String>> summaryMap = FileQueueHelperManager.getInstance().getAllDataDetail();
            FileQueueHelperManager.getInstance().putData("D:\\TEST\\QUEUE\\TEST.UQ", "TEST1");

            Map<String,Object> resultBodyMap = new HashMap<String,Object>();
            if(summaryMap != null)resultBodyMap.put("SUMMARY", summaryMap);
            
    		return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), resultBodyMap, request.getRequestURI(), null);
		} catch (Exception e) {
			e.printStackTrace();
			return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>(), request.getRequestURI(), null);
		}
	}
    
    private String validateRequestBody(Map<String, Object> requestBodyMap) {
    	
    	for(Entry<String, RequestBodyInfo> e : requestBodyInfoMap.entrySet()) {
    		String key = e.getKey();
    		RequestBodyInfo info = e.getValue();
    		
    		Object valueObject = requestBodyMap.get(key);
    		boolean isRequired = info.isRequired();
    		if(isRequired) {
    			if(ObjectUtils.isEmpty(valueObject)) return "["+key+"] 필드 누락됨";
    		}else {
    			if(ObjectUtils.isEmpty(valueObject) && info.getDefaultValue() != null) {
    				String defaultValue = info.getDefaultValue();
    				if(defaultValue.startsWith("$")) {
    					defaultValue = (String) requestBodyMap.get(defaultValue.substring(1));
    				}
    				requestBodyMap.put(key, defaultValue);
    			}
    		}

    		Class<?> classtype =info.getType();
    		if(classtype.isInstance(valueObject) == false) return "["+key+"] 필드 잘못된 타입";
    	}

    	return null;
    }
}
