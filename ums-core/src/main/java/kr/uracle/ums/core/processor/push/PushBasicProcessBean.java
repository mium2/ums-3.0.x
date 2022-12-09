package kr.uracle.ums.core.processor.push;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;

import java.util.Set;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 21..
 */
public class PushBasicProcessBean extends BaseProcessBean {
	
    private String APP_ID = "";
    private String PUSH_TYPE = "";
    private String MESSAGE = "";
    private String EXT = "";
    private String BADGENO = "0";
    private String WPUSH_DOMAIN		= "";
    private String WPUSH_TITLE		= "";
    // 필수 옵션
    private String WPUSH_MSG		= "";
    private String WPUSH_ICON		= "";
    private String WPUSH_LINK		= "";
    private String WPUSH_EXT   		= "";
    private String WPUSH_BADGENO 	= "0";

    private String PRIORITY = "3";
    private String RESERVEDATE = "";
    private String SERVICECODE = "";
    private String SENDERCODE = "";
    private String DB_IN = "";
    private String PUSH_FAIL_SMS_SEND = "";
    private String PUSH_FAIL_WAIT_MIN = "0";
    
    // UMS 내에서 실패 로직처리 위한 정보 셋팅.
    private Set<SendType> FAIL_RETRY_SENDTYPE;
    private Set<SendType> FAIL_RETRY_SENDTYPE2; // 푸시발송실패 > 웹푸시발송실패 > 다음 발송채널 or 웹푸시발송실패 > 푸시발송실패 > 다음 발송채널
    private boolean isFailSmsSend = false;

    //푸시5.1에 추가된 파라미터
    private String CUST_KEY = ""; // 원장테이블정보 + _ + 고유시퀀스번호. 예) UMSUI_2342
    private String CUST_VAR1 = "";
    private String CUST_VAR2 = "";
    private String CUST_VAR3 = "";

    public String getAPP_ID() { return APP_ID; }
    public void setAPP_ID(String APP_ID) { this.APP_ID = APP_ID; }

    public String getPUSH_TYPE() { return PUSH_TYPE; }
    public void setPUSH_TYPE(String PUSH_TYPE) { this.PUSH_TYPE = PUSH_TYPE; }
    public String getMESSAGE() { return MESSAGE; }
    public void setMESSAGE(String MESSAGE) { this.MESSAGE = MESSAGE; }

    public String getEXT() { return EXT; }
    public void setEXT(String EXT) { this.EXT = EXT; }

    public String getWPUSH_DOMAIN() {
        return WPUSH_DOMAIN;
    }

    public void setWPUSH_DOMAIN(String WPUSH_DOMAIN) {
        this.WPUSH_DOMAIN = WPUSH_DOMAIN;
    }

    public String getWPUSH_TITLE() {
        return WPUSH_TITLE;
    }

    public void setWPUSH_TITLE(String WPUSH_TITLE) {
        this.WPUSH_TITLE = WPUSH_TITLE;
    }

    public String getWPUSH_MSG() {
        return WPUSH_MSG;
    }

    public void setWPUSH_MSG(String WPUSH_MSG) {
        this.WPUSH_MSG = WPUSH_MSG;
    }

    public String getWPUSH_ICON() {
        return WPUSH_ICON;
    }

    public void setWPUSH_ICON(String WPUSH_ICON) {
        this.WPUSH_ICON = WPUSH_ICON;
    }

    public String getWPUSH_LINK() {
        return WPUSH_LINK;
    }

    public void setWPUSH_LINK(String WPUSH_LINK) {
        this.WPUSH_LINK = WPUSH_LINK;
    }

    public String getWPUSH_EXT() {
        return WPUSH_EXT;
    }

    public void setWPUSH_EXT(String WPUSH_EXT) {
        this.WPUSH_EXT = WPUSH_EXT;
    }

    public String getWPUSH_BADGENO() {
        return WPUSH_BADGENO;
    }

    public void setWPUSH_BADGENO(String WPUSH_BADGENO) {
        this.WPUSH_BADGENO = WPUSH_BADGENO;
    }
    
    public String getPRIORITY() { return PRIORITY; }
    public void setPRIORITY(String PRIORITY) { this.PRIORITY = PRIORITY; }

    public String getRESERVEDATE() { return RESERVEDATE; }
    public void setRESERVEDATE(String RESERVEDATE) { this.RESERVEDATE = RESERVEDATE; }

    public String getSERVICECODE() { return SERVICECODE; }
    public void setSERVICECODE(String SERVICECODE) { this.SERVICECODE = SERVICECODE; }

    public String getSENDERCODE() { return SENDERCODE; }
    public void setSENDERCODE(String SENDERCODE) { this.SENDERCODE = SENDERCODE; }

    public String getDB_IN() { return DB_IN; }
    public void setDB_IN(String DB_IN) { this.DB_IN = DB_IN; }

    public String getBADGENO() { return BADGENO; }
    public void setBADGENO(String BADGENO) { this.BADGENO = BADGENO; }
    
    public String getPUSH_FAIL_SMS_SEND() { return PUSH_FAIL_SMS_SEND; }
    public void setPUSH_FAIL_SMS_SEND(String PUSH_FAIL_SMS_SEND) { this.PUSH_FAIL_SMS_SEND = PUSH_FAIL_SMS_SEND; }

    public String getPUSH_FAIL_WAIT_MIN() { return PUSH_FAIL_WAIT_MIN; }
    public void setPUSH_FAIL_WAIT_MIN(String PUSH_FAIL_WAIT_MIN) { this.PUSH_FAIL_WAIT_MIN = PUSH_FAIL_WAIT_MIN; }

    public Set<SendType> getFAIL_RETRY_SENDTYPE() { return FAIL_RETRY_SENDTYPE; }
    public void setFAIL_RETRY_SENDTYPE(Set<SendType> FAIL_RETRY_SENDTYPE) { this.FAIL_RETRY_SENDTYPE = FAIL_RETRY_SENDTYPE; }

    public Set<SendType> getFAIL_RETRY_SENDTYPE2() {
        return FAIL_RETRY_SENDTYPE2;
    }

    public void setFAIL_RETRY_SENDTYPE2(Set<SendType> FAIL_RETRY_SENDTYPE2) {
        this.FAIL_RETRY_SENDTYPE2 = FAIL_RETRY_SENDTYPE2;
    }

    public boolean isFailSmsSend() { return isFailSmsSend; }
    public void setFailSmsSend(boolean failSmsSend) { isFailSmsSend = failSmsSend; }

    public String getCUST_KEY() { return CUST_KEY; }
    public void setCUST_KEY(String CUST_KEY) { this.CUST_KEY = CUST_KEY; }

    public String getCUST_VAR1() { return CUST_VAR1; }
    public void setCUST_VAR1(String CUST_VAR1) { this.CUST_VAR1 = CUST_VAR1; }

    public String getCUST_VAR2() { return CUST_VAR2; }
    public void setCUST_VAR2(String CUST_VAR2) { this.CUST_VAR2 = CUST_VAR2; }

    public String getCUST_VAR3() { return CUST_VAR3; }
    public void setCUST_VAR3(String CUST_VAR3) { this.CUST_VAR3 = CUST_VAR3; }
}
