package kr.uracle.ums.core.vo.member;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 8..
 */
public class CacheMemberVo implements Serializable{
    private String MEMBERID = "";
    private String MEMBERNAME = "";
    private String MOBILE = "";

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

    @Override
    public String toString() {
        return "CacheMemberVo{" +
            "MEMBERID='" + MEMBERID + '\'' +
            ", MEMBERNAME='" + MEMBERNAME + '\'' +
            ", MOBILE='" + MOBILE + '\'' +
            '}';
    }
}
