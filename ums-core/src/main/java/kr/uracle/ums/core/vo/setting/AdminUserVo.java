package kr.uracle.ums.core.vo.setting;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2019. 2. 11..
 */
public class AdminUserVo implements Serializable {
    private String ADMINID = "";
    private String ADMINPASS="";
    private String ADMINNAME="";
    private String MOBILE="";
    private int AUTHCODE=0;
    private String EMAIL="";
    private String REGDATE="";
    private String EDITDATE ="";
    private String USE_YN = "Y";

    public String getADMINID() {
        return ADMINID;
    }

    public void setADMINID(String ADMINID) {
        this.ADMINID = ADMINID;
    }

    public String getADMINPASS() {
        return ADMINPASS;
    }

    public void setADMINPASS(String ADMINPASS) {
        this.ADMINPASS = ADMINPASS;
    }

    public String getADMINNAME() {
        return ADMINNAME;
    }

    public void setADMINNAME(String ADMINNAME) {
        this.ADMINNAME = ADMINNAME;
    }

    public String getMOBILE() {
        return MOBILE;
    }

    public void setMOBILE(String MOBILE) {
        this.MOBILE = MOBILE;
    }

    public int getAUTHCODE() {
        return AUTHCODE;
    }

    public void setAUTHCODE(int AUTHCODE) {
        this.AUTHCODE = AUTHCODE;
    }

    public String getEMAIL() {
        return EMAIL;
    }

    public void setEMAIL(String EMAIL) {
        this.EMAIL = EMAIL;
    }

    public String getREGDATE() {
        return REGDATE;
    }

    public void setREGDATE(String REGDATE) {
        this.REGDATE = REGDATE;
    }

    public String getEDITDATE() {
        return EDITDATE;
    }

    public void setEDITDATE(String EDITDATE) {
        this.EDITDATE = EDITDATE;
    }

    public String getUSE_YN() {
        return USE_YN;
    }

    public void setUSE_YN(String USE_YN) {
        this.USE_YN = USE_YN;
    }

    @Override
    public String toString() {
        return "AdminUserVo{" +
            "ADMINID='" + ADMINID + '\'' +
            ", ADMINPASS='" + ADMINPASS + '\'' +
            ", ADMINNAME='" + ADMINNAME + '\'' +
            ", MOBILE='" + MOBILE + '\'' +
            ", AUTHCODE=" + AUTHCODE +
            ", EMAIL='" + EMAIL + '\'' +
            ", REGDATE='" + REGDATE + '\'' +
            ", EDITDATE='" + EDITDATE + '\'' +
            ", USE_YN='" + USE_YN + '\'' +
            '}';
    }
}
