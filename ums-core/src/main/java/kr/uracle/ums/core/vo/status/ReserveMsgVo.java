package kr.uracle.ums.core.vo.status;

/**
 * Created by Y.B.H(mium2) on 2019. 6. 10..
 */
public class ReserveMsgVo {
    private int TOT_CNT = 0;
    private long RESERVE_SEQNO= 0;
    private String MSG_TYPE= "";
    private String TITLE= "";
    private String RESERVEDATE= "";
    private String APP_ID= "";
    private String PUSH_MSG= "";
    private String ALLIMTOLK_TEMPLCODE= "";
    private String KKOALT_SVCID= "";
    private String FRIENDTOLK_MSG= "";
    private String KKOFRT_SVCID= "";
    private String SMS_MSG= "";
    private String SENDERID= "";
    private String SENDGROUPCODE= "";
    private String PLUS_ID= "";
    private String FRT_TEMPL_ID= "";
    private String SMS_TEMPL_ID= "";
    private String CALLBACK_NUM= "";
    private String START_SEND_KIND= "";
    private String TRANS_TYPE= "";
    private String REG_UMS_HOST= "";
    private String TARGET_TYPE= "";
    private String PROCESS_STATUS= "";
    private String RETRY_CNT= "";
    private String ERRMSG= "";
    private String REGDATE= "";
    private String SEND_RESERVE_DATE= "";

    private String MSG = "";
    private String MSG_TYPE_LABEL = "";
    private String SENDER = "";
    private String PROCESS_STATUS_TEXT = "";
    private String SEND_RESERVE_DATE_YMD = "";
    private String SEND_RESERVE_DATE_HM = "";
    private String PUSH_SEND_YN = "Y";
    private String ALTK_SEND_YN = "Y";
    private String FRID_SEND_YN = "Y";
    private String SMS_SEND_YN = "Y";

    public int getTOT_CNT() {
        return TOT_CNT;
    }

    public void setTOT_CNT(int TOT_CNT) {
        this.TOT_CNT = TOT_CNT;
    }

    public long getRESERVE_SEQNO() {
        return RESERVE_SEQNO;
    }

    public void setRESERVE_SEQNO(long RESERVE_SEQNO) {
        this.RESERVE_SEQNO = RESERVE_SEQNO;
    }

    public String getMSG_TYPE() {
        return MSG_TYPE;
    }

    public void setMSG_TYPE(String MSG_TYPE) {
        this.MSG_TYPE = MSG_TYPE;
    }

    public String getTITLE() {
        return TITLE;
    }

    public void setTITLE(String TITLE) {
        this.TITLE = TITLE;
    }

    public String getRESERVEDATE() {
        return RESERVEDATE;
    }

    public void setRESERVEDATE(String RESERVEDATE) {
        this.RESERVEDATE = RESERVEDATE;
    }

    public String getAPP_ID() {
        return APP_ID;
    }

    public void setAPP_ID(String APP_ID) {
        this.APP_ID = APP_ID;
    }

    public String getPUSH_MSG() {
        return PUSH_MSG;
    }

    public void setPUSH_MSG(String PUSH_MSG) {
        this.PUSH_MSG = PUSH_MSG;
    }

    public String getALLIMTOLK_TEMPLCODE() {
        return ALLIMTOLK_TEMPLCODE;
    }

    public void setALLIMTOLK_TEMPLCODE(String ALLIMTOLK_TEMPLCODE) {
        this.ALLIMTOLK_TEMPLCODE = ALLIMTOLK_TEMPLCODE;
    }

    public String getKKOALT_SVCID() {
        return KKOALT_SVCID;
    }

    public void setKKOALT_SVCID(String KKOALT_SVCID) {
        this.KKOALT_SVCID = KKOALT_SVCID;
    }

    public String getFRIENDTOLK_MSG() {
        return FRIENDTOLK_MSG;
    }

    public void setFRIENDTOLK_MSG(String FRIENDTOLK_MSG) {
        this.FRIENDTOLK_MSG = FRIENDTOLK_MSG;
    }

    public String getKKOFRT_SVCID() {
        return KKOFRT_SVCID;
    }

    public void setKKOFRT_SVCID(String KKOFRT_SVCID) {
        this.KKOFRT_SVCID = KKOFRT_SVCID;
    }

    public String getSMS_MSG() {
        return SMS_MSG;
    }

    public void setSMS_MSG(String SMS_MSG) {
        this.SMS_MSG = SMS_MSG;
    }

    public String getSENDERID() {
        return SENDERID;
    }

    public void setSENDERID(String SENDERID) {
        this.SENDERID = SENDERID;
    }

    public String getSENDGROUPCODE() {
        return SENDGROUPCODE;
    }

    public void setSENDGROUPCODE(String SENDGROUPCODE) {
        this.SENDGROUPCODE = SENDGROUPCODE;
    }

    public String getPLUS_ID() {
        return PLUS_ID;
    }

    public void setPLUS_ID(String PLUS_ID) {
        this.PLUS_ID = PLUS_ID;
    }

    public String getFRT_TEMPL_ID() {
        return FRT_TEMPL_ID;
    }

    public void setFRT_TEMPL_ID(String FRT_TEMPL_ID) {
        this.FRT_TEMPL_ID = FRT_TEMPL_ID;
    }

    public String getSMS_TEMPL_ID() {
        return SMS_TEMPL_ID;
    }

    public void setSMS_TEMPL_ID(String SMS_TEMPL_ID) {
        this.SMS_TEMPL_ID = SMS_TEMPL_ID;
    }

    public String getCALLBACK_NUM() {
        return CALLBACK_NUM;
    }

    public void setCALLBACK_NUM(String CALLBACK_NUM) {
        this.CALLBACK_NUM = CALLBACK_NUM;
    }

    public String getSTART_SEND_KIND() {
        return START_SEND_KIND;
    }

    public void setSTART_SEND_KIND(String START_SEND_KIND) {
        this.START_SEND_KIND = START_SEND_KIND;
    }

    public String getMASTER_TABLE_KIND() {
        return TRANS_TYPE;
    }

    public void setMASTER_TABLE_KIND(String MASTER_TABLE_KIND) {
        this.TRANS_TYPE = MASTER_TABLE_KIND;
    }

    public String getREG_UMS_HOST() {
        return REG_UMS_HOST;
    }

    public void setREG_UMS_HOST(String REG_UMS_HOST) {
        this.REG_UMS_HOST = REG_UMS_HOST;
    }

    public String getTARGET_TYPE() {
        return TARGET_TYPE;
    }

    public void setTARGET_TYPE(String TARGET_TYPE) {
        this.TARGET_TYPE = TARGET_TYPE;
    }

    public String getPROCESS_STATUS() {
        return PROCESS_STATUS;
    }

    public void setPROCESS_STATUS(String PROCESS_STATUS) {
        this.PROCESS_STATUS = PROCESS_STATUS;
    }

    public String getRETRY_CNT() {
        return RETRY_CNT;
    }

    public void setRETRY_CNT(String RETRY_CNT) {
        this.RETRY_CNT = RETRY_CNT;
    }

    public String getERRMSG() {
        return ERRMSG;
    }

    public void setERRMSG(String ERRMSG) {
        this.ERRMSG = ERRMSG;
    }

    public String getREGDATE() {
        return REGDATE;
    }

    public void setREGDATE(String REGDATE) {
        this.REGDATE = REGDATE;
    }

    public String getSEND_RESERVE_DATE() {
        return SEND_RESERVE_DATE;
    }

    public void setSEND_RESERVE_DATE(String SEND_RESERVE_DATE) {
        this.SEND_RESERVE_DATE = SEND_RESERVE_DATE;
    }

    public String getMSG() {
        return MSG;
    }

    public void setMSG(String MSG) {
        this.MSG = MSG;
    }

    public String getMSG_TYPE_LABEL() {
        return MSG_TYPE_LABEL;
    }

    public void setMSG_TYPE_LABEL(String MSG_TYPE_LABEL) {
        this.MSG_TYPE_LABEL = MSG_TYPE_LABEL;
    }

    public String getSENDER() {
        return SENDER;
    }

    public void setSENDER(String SENDER) {
        this.SENDER = SENDER;
    }

    public String getPROCESS_STATUS_TEXT() {
        return PROCESS_STATUS_TEXT;
    }

    public void setPROCESS_STATUS_TEXT(String PROCESS_STATUS_TEXT) {
        this.PROCESS_STATUS_TEXT = PROCESS_STATUS_TEXT;
    }

    public String getSEND_RESERVE_DATE_YMD() {
        return SEND_RESERVE_DATE_YMD;
    }

    public void setSEND_RESERVE_DATE_YMD(String SEND_RESERVE_DATE_YMD) {
        this.SEND_RESERVE_DATE_YMD = SEND_RESERVE_DATE_YMD;
    }

    public String getSEND_RESERVE_DATE_HM() {
        return SEND_RESERVE_DATE_HM;
    }

    public void setSEND_RESERVE_DATE_HM(String SEND_RESERVE_DATE_HM) {
        this.SEND_RESERVE_DATE_HM = SEND_RESERVE_DATE_HM;
    }

    public String getPUSH_SEND_YN() {
        return PUSH_SEND_YN;
    }

    public void setPUSH_SEND_YN(String PUSH_SEND_YN) {
        this.PUSH_SEND_YN = PUSH_SEND_YN;
    }

    public String getALTK_SEND_YN() {
        return ALTK_SEND_YN;
    }

    public void setALTK_SEND_YN(String ALTK_SEND_YN) {
        this.ALTK_SEND_YN = ALTK_SEND_YN;
    }

    public String getFRID_SEND_YN() {
        return FRID_SEND_YN;
    }

    public void setFRID_SEND_YN(String FRID_SEND_YN) {
        this.FRID_SEND_YN = FRID_SEND_YN;
    }

    public String getSMS_SEND_YN() {
        return SMS_SEND_YN;
    }

    public void setSMS_SEND_YN(String SMS_SEND_YN) {
        this.SMS_SEND_YN = SMS_SEND_YN;
    }
}
