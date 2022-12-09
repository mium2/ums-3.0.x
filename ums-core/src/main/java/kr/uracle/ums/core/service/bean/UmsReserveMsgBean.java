package kr.uracle.ums.core.service.bean;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 8..
 */
public class UmsReserveMsgBean extends UmsSendMsgBean {
	
    private String TARGET_USERS_JSON = "";
    private String TARGET_TYPE = "";
    private String CSV_FILE = "";
    private String CSV_ORG_FILENAME = "";
    private String SEND_RESERVE_DATE = "";
    private String REG_UMS_HOST = "";

    public UmsReserveMsgBean(String TRANS_TYPE){
        super(TRANS_TYPE);
    }

    public String getTARGET_USERS_JSON() {
        return TARGET_USERS_JSON;
    }

    public void setTARGET_USERS_JSON(String TARGET_USERS_JSON) {
        this.TARGET_USERS_JSON = TARGET_USERS_JSON;
    }

    public String getTARGET_TYPE() {
        return TARGET_TYPE;
    }

    public void setTARGET_TYPE(String TARGET_TYPE) {
        this.TARGET_TYPE = TARGET_TYPE;
    }

    public String getCSV_FILE() {
        return CSV_FILE;
    }

    public void setCSV_FILE(String CSV_FILE) {
        this.CSV_FILE = CSV_FILE;
    }

    public String getCSV_ORG_FILENAME() {
        return CSV_ORG_FILENAME;
    }

    public void setCSV_ORG_FILENAME(String CSV_ORG_FILENAME) {
        this.CSV_ORG_FILENAME = CSV_ORG_FILENAME;
    }

    public String getSEND_RESERVE_DATE() {
        return SEND_RESERVE_DATE;
    }

    public void setSEND_RESERVE_DATE(String SEND_RESERVE_DATE) {
        this.SEND_RESERVE_DATE = SEND_RESERVE_DATE;
    }

    public String getREG_UMS_HOST() {
        return REG_UMS_HOST;
    }

    public void setREG_UMS_HOST(String REG_UMS_HOST) {
        this.REG_UMS_HOST = REG_UMS_HOST;
    }

}
