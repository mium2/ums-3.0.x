package kr.uracle.ums.core.controller.sdk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.ehcache.ValidityCacheMgr;
import kr.uracle.ums.core.ehcache.ValidityCacheMgr.ValidityType;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.codec.redis.config.ErrorManager;

@Controller
@RequestMapping(value = "/api/valid")
public class ValidationCtrl {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	    
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;
    
    @Autowired(required = true)
    private ValidityCacheMgr validityCacheMgr;
    

//	public ValidationCtrl(ValidityCacheMgr validityCacheMgr) {
//		this.validityCacheMgr = validityCacheMgr;
//	}

	@RequestMapping(value = { "/ban.ums" }, produces = "application/json; charset=utf8")
	public @ResponseBody String checkBanned(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> requestBodyMap){
		try {
	        //접근이 허용되지 않은 아이피일경우 실패처리함.
	        if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
	            String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
	            logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
	            return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, new HashMap<String,Object>());
	        }
	        
	        String rsltMsg = "";
	        //체크 Trie 획득
	        Trie  trie = validityCacheMgr.getTrie(ValidityType.BANNED);
	        if(trie == null) return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS)+", 금지어 설정 정보 없음으로 검증 불가", new HashMap<String,Object>());
	        String title = requestBodyMap.get("TITLE") != null ? requestBodyMap.get("TITLE").toString() :  null;
	        String msg = requestBodyMap.get("MSG") != null ? requestBodyMap.get("MSG").toString() :  null;
	        Collection<Emit> emits = null;
	        if(StringUtils.isNotBlank(title)) {
	        	emits = trie.parseText(title);
	        	if(emits.size() > 0) {
	        		rsltMsg = "제목 금지어 - ";
	        		for(Emit e: emits) {
	        			rsltMsg+=e.getKeyword()+", ";
	        		}
	        	}
	        }
	        
	        if(StringUtils.isNotBlank(msg)) {
	        	emits = trie.parseText(msg);
	        	if(emits.size() > 0) {
	        		rsltMsg += "메시지 금지어 - ";
	        		for(Emit e: emits) {
	        			rsltMsg+=e.getKeyword()+", ";
	        		}
	        	}
	        }
	        
	        if(StringUtils.isNotBlank(rsltMsg)) {
	        	rsltMsg +="발견";
	        	return umsSendCommonService.responseJsonString(ErrorManager.ERR_1002, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1002)+", "+rsltMsg, new HashMap<String,Object>());
	        }
	        
	        return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), new HashMap<String,Object>());
		} catch (Exception e) {
			e.printStackTrace();
			return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>());
		}
	}
	
	@RequestMapping(value = { "/required.ums" }, produces = "application/json; charset=utf8")
	public @ResponseBody String checkRequired(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> requestBodyMap){
		try {
	        //접근이 허용되지 않은 아이피일경우 실패처리함.
	        if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
	            String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
	            logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
	            return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, new HashMap<String,Object>());
	        }
	        String rsltMsg = "";
	        
	        //체크 Trie 획득
	        Trie  trie = validityCacheMgr.getTrie(ValidityType.REQUIRED);
	        List<String> list = new ArrayList<String>(); 
	        List<String> checkList = validityCacheMgr.getCheckList(ValidityType.REQUIRED);
	        if(ObjectUtils.isNotEmpty(checkList))list.addAll(checkList);
	        if(trie == null || ObjectUtils.isEmpty(list)) return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS)+", 필수어 설정 정보 없음으로 검증 불가", new HashMap<String,Object>());
	        
	        String title = requestBodyMap.get("TITLE") != null ? requestBodyMap.get("TITLE").toString() :  null;
	        String msg = requestBodyMap.get("MSG") != null ? requestBodyMap.get("MSG").toString() :  null;
	        
	        Collection<Emit> emits = null;
	        
	        if(StringUtils.isNotBlank(title)) {
	        	emits = trie.parseText(title);
	        	if(emits.size() != list.size()) {
	        		rsltMsg = "제목 필수어 누락 - ";
	        		for(Emit e: emits) {
	        			list.remove(e.getKeyword());
	        		}
	        		for(String s : list) {
	        			rsltMsg+=s+", ";
	        		}
	        	}
	        }
	        
	        list.clear();
	        list.addAll(checkList);
	        if(StringUtils.isNotBlank(msg)) {
	        	emits = trie.parseText(msg);
	        	if(emits.size() != list.size()) {
	        		rsltMsg += "내용 필수어 누락 - ";
	        		for(Emit e: emits) {
	        			list.remove(e.getKeyword());
	        		}
	        		for(String s : list) {
	        			rsltMsg+=s+", ";
	        		}
	        	}
	        }
	        
	        if(StringUtils.isNotBlank(rsltMsg)) {
	        	rsltMsg +="누락으로 실패";
	        	return umsSendCommonService.responseJsonString(ErrorManager.ERR_1002, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1002)+", "+rsltMsg, new HashMap<String,Object>());
	        }
	        
	        return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS), new HashMap<String,Object>());
		} catch (Exception e) {
			e.printStackTrace();
			return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500), new HashMap<String,Object>());
		}

	}
	
	@RequestMapping(value = { "/sendnum.ums" }, produces = "application/json; charset=utf8")
	public @ResponseBody String checkSendNum(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> requestBodyMap){
		try {
	        //접근이 허용되지 않은 아이피일경우 실패처리함.
	        if(!AuthIPCheckManager.getInstance().isAllowIp(request.getRemoteAddr())){
	            String ERRMSG = ErrorManager.getInstance().getMsg(ErrorManager.ERR_9404);
	            logger.error("!! [UMS AUTH CHECK] Unauthorized error]: {}",ERRMSG);
	            return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, ERRMSG, new HashMap<String,Object>());
	        }
	        
	        String rsltMsg = "";
	        String sendNum = requestBodyMap.get("SEND_NUM") != null ? requestBodyMap.get("SEND_NUM").toString() :  "";
	        String channel = requestBodyMap.get("CHANNEL") != null ? requestBodyMap.get("CHANNEL").toString() :  "";
	        if(StringUtils.isAnyBlank(sendNum, channel)) {
	        	rsltMsg = "필수 파라미터 누락 - "+ (StringUtils.isBlank(sendNum)?"[SEND_NUM]":"")+(StringUtils.isBlank(channel)?"[CHANNEL]":"");
	        	return umsSendCommonService.responseJsonString(ErrorManager.ERR_1001, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1001)+", "+rsltMsg, new HashMap<String,Object>()); 
	        }
	        
	        List<String> channelList = validityCacheMgr.getChannelType();
	        if(ObjectUtils.isEmpty(channelList)) return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS)+", 채널 설정 정보 없음으로 검증 불가", new HashMap<String,Object>());
	        
	        if(channelList.contains(channel) == false) {
	        	rsltMsg = channel+" 지원하지 않는 채널, 지원 채널 - ";
	        	for(String c : channelList) {
	        		rsltMsg+="["+c+"] ";
	        	}
	        	return umsSendCommonService.responseJsonString(ErrorManager.ERR_1001, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1001)+", "+rsltMsg, new HashMap<String,Object>());
	        }
	        
	        List<String> sendNumList = validityCacheMgr.getSendNum(channel);
	        
	        if(ObjectUtils.isEmpty(sendNumList)) return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS)+", ["+channel+"]채널 발신번호 설정 정보 없음으로 검증 불가", new HashMap<String,Object>());
	        
	        if(sendNumList.contains(sendNum.replaceAll("\\D", ""))) return umsSendCommonService.responseJsonString(ErrorManager.SUCCESS, ErrorManager.getInstance().getMsg(ErrorManager.SUCCESS)+", 사용 가능한 발신 번호 - "+sendNum, new HashMap<String,Object>());
	      
	        	        
	        return umsSendCommonService.responseJsonString(ErrorManager.ERR_1002, ErrorManager.getInstance().getMsg(ErrorManager.ERR_1002)+", ["+channel+"]채널에 등록된 발신 번호 아님", new HashMap<String,Object>());
		} catch (Exception e) {
			e.printStackTrace();
			return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.toString(), new HashMap<String,Object>());
		}

	}
}

