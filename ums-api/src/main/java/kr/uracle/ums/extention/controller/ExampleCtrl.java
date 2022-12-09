package kr.uracle.ums.extention.controller;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.core.controller.send.PotalSendApiCtrl;
import kr.uracle.ums.core.service.UmsSendCommonService;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import kr.uracle.ums.extention.vo.ReqExampleVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;

@RequestMapping(value="/api/extention/example")
@Controller
public class ExampleCtrl {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private UmsSendCommonService umsSendCommonService;

    @Autowired(required = true)
    private PotalSendApiCtrl potalSendApiCtrl;

    @RequestMapping(value = {"/potalSendApi.ums"}, produces = "application/json; charset=utf8")
    public @ResponseBody String umsSend(Locale locale, HttpServletRequest request, HttpServletResponse response, @ModelAttribute ReqExampleVo reqExampleVo) {
        String responseBody = "";
        try {
            // TODO : 해당 부분에 reqExampleVo 정보를 이용하여 ReqUmsSendVo에 맵핑시키는 비즈로직 수행

            // STEP 1 : ReqUmsSendVo에 맵핑시켜야 함. HTTP API 문서 참조
            ReqUmsSendVo reqUmsSendVo = new ReqUmsSendVo();

            // STEP 2 : UMS 발송요청 메서드 호출
            responseBody = potalSendApiCtrl.umsSend(locale, request, response, reqUmsSendVo);

        } catch (Exception e) {
            e.printStackTrace();
            return umsSendCommonService.responseJsonString(ErrorManager.ERR_500, ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.getMessage(), request.getRequestURI(), reqExampleVo);
        }
        return responseBody;
    }

    @RequestMapping(value = {"/testApi.ums"}, produces = "application/json; charset=utf8")
    public @ResponseBody String testTemp(Locale locale, HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> params) {
        String responseBody = "";
        Gson gson = new Gson();
        String reqBody = gson.toJson(params);
        logger.info("요청데이터(응답데이터)");
        logger.info(reqBody);
        return reqBody;
    }
}