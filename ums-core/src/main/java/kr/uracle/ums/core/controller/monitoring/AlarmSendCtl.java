package kr.uracle.ums.core.controller.monitoring;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.uracle.ums.core.controller.send.PotalSendApiCtrl;
import kr.uracle.ums.core.vo.ReqUmsSendVo;

@Controller
@RequestMapping(value = "/api/monit/")
public class AlarmSendCtl {
	
    @Value("${ALRAM.TARGET.INFO:}")
    private String ALRAM_TARGET_INFO;
    @Value("${ALRAM.SENDERID:}")
    private String ALRAM_SENDERID;
    @Value("${ALRAM.SENDERGROUP:}")
    private String ALRAM_SENDERGROUP;
    @Value("${ALRAM.APPID:}")
    private String ALRAM_APPID;
    @Value("${ALRAM.SENDER.NUM:}")
    private String ALRAM_SENDER_NUM;
	
	private final PotalSendApiCtrl potalSendApiCtrl;
	
	@Autowired
	public AlarmSendCtl(PotalSendApiCtrl potalSendApiCtrl) {
		this.potalSendApiCtrl = potalSendApiCtrl;
	}

	
	@RequestMapping(value = { "/alarmSendApi.ums" }, produces = "application/json; charset=utf8")
	public @ResponseBody String umsAlarmSend(Locale locale, HttpServletRequest request, HttpServletResponse response, @ModelAttribute ReqUmsSendVo reqUmsSendVo) {

		// 알람 설정 셋팅

		if("".equals(reqUmsSendVo.getSENDERID())) {
			reqUmsSendVo.setSENDERID(ALRAM_SENDERID);
		}
		if("".equals(reqUmsSendVo.getSENDGROUPCODE())) {
			reqUmsSendVo.setSENDGROUPCODE(ALRAM_SENDERGROUP);
		}
		if("".equals(reqUmsSendVo.getAPP_ID())) {
			reqUmsSendVo.setAPP_ID(ALRAM_APPID);
		}
		if("".equals(reqUmsSendVo.getCALLBACK_NUM())) {
			reqUmsSendVo.setCALLBACK_NUM(ALRAM_SENDER_NUM);
		}
		if("".equals(reqUmsSendVo.getCUIDS())) {
			reqUmsSendVo.setCUIDS(ALRAM_TARGET_INFO);
		}

		return potalSendApiCtrl.umsSend(locale, request, response, reqUmsSendVo);
	}
}
