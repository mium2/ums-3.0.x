package kr.uracle.ums.core.vo.member;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2019. 2. 20..
 */
public class MemberVo implements Serializable {
    private String MEMBERID = "";
    private String MEMBERNAME = "";
    private String MOBILE = "";
    private String ORGANID = "";
    private String VAR1 = "";
    private String VAR2 = "";
    private String VAR3 = "";
    private String VAR4 = "";
    private String VAR5 = "";
    private String REGDATE = "";
    private String EDITDATE = "";

    public String getMEMBERID() {
        return MEMBERID;
    }

    public void setMEMBERID(String MEMBERID) {
        this.MEMBERID = MEMBERID;
    }

    public String getMEMBERNAME() {
        return MEMBERNAME;
    }

    public void setMEMBERNAME(String MEMBERNAME) {
        this.MEMBERNAME = MEMBERNAME;
    }

    public String getMOBILE() {
        return MOBILE;
    }

    public void setMOBILE(String MOBILE) {
        this.MOBILE = MOBILE;
    }

    public String getORGANID() {
        return ORGANID;
    }

    public void setORGANID(String ORGANID) {
        this.ORGANID = ORGANID;
    }

    public String getVAR1() {
        return VAR1;
    }

    public void setVAR1(String VAR1) {
        this.VAR1 = VAR1;
    }

    public String getVAR2() {
        return VAR2;
    }

    public void setVAR2(String VAR2) {
        this.VAR2 = VAR2;
    }

    public String getVAR3() {
        return VAR3;
    }

    public void setVAR3(String VAR3) {
        this.VAR3 = VAR3;
    }

    public String getVAR4() {
        return VAR4;
    }

    public void setVAR4(String VAR4) {
        this.VAR4 = VAR4;
    }

    public String getVAR5() {
        return VAR5;
    }

    public void setVAR5(String VAR5) {
        this.VAR5 = VAR5;
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

    @Override
    public String toString() {
        return "MemberVo{" +
            "MEMBERID='" + MEMBERID + '\'' +
            ", MEMBERNAME='" + MEMBERNAME + '\'' +
            ", MOBILE='" + MOBILE + '\'' +
            ", ORGANID='" + ORGANID + '\'' +
            ", VAR1='" + VAR1 + '\'' +
            ", VAR2='" + VAR2 + '\'' +
            ", VAR3='" + VAR3 + '\'' +
            ", VAR4='" + VAR4 + '\'' +
            ", VAR5='" + VAR5 + '\'' +
            ", REGDATE='" + REGDATE + '\'' +
            ", EDITDATE='" + EDITDATE + '\'' +
            '}';
    }
}
