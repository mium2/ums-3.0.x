package kr.uracle.ums.extention.controller;

import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.core.controller.send.PotalSendApiCtrl;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import kr.uracle.ums.extention.ehcache.SampleCacheMgr;
import kr.uracle.ums.extention.vo.ReqUmsSendExVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

@Controller
@RequestMapping(value = "/api/sample/")
public class SampleController {
    private final PotalSendApiCtrl potalSendApiCtrl;

    @Autowired(required = true)
    private SampleCacheMgr sampleCacheMgr;

    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;

    @Autowired
    public SampleController(PotalSendApiCtrl potalSendApiCtrl) {
        this.potalSendApiCtrl = potalSendApiCtrl;
    }

    @RequestMapping(value = { "/sample.ums" }, produces = "application/json; charset=utf8")
    public @ResponseBody String umsAlarmSend(Locale locale, HttpServletRequest request, HttpServletResponse response, @ModelAttribute ReqUmsSendExVo reqUmsSendExVo) {

        //전처리 부
        // EXvo 받은것을 다시 corevo로 조ㄹ
        ReqUmsSendVo reqUmsSendVo = new ReqUmsSendVo();
        return potalSendApiCtrl.umsSend(locale, request, response, reqUmsSendVo);
    }

    @RequestMapping(value = { "/check/prevent.ums" }, produces = "application/json; charset=utf8")
    public @ResponseBody String umsCheckPrevent(Locale locale, HttpServletRequest request, HttpServletResponse response, @ModelAttribute ReqUmsSendExVo reqUmsSendExVo) {

        try {
            boolean isPrevent = sampleCacheMgr.isPreventUserFromID(reqUmsSendExVo.getAPP_ID(), "TEST_ID", "SMS");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return umsSendCommonService.responseJsonString(ErrorManager.ERR_9404, "에러내용", request.getRequestURI(), reqUmsSendExVo);
    }
}
