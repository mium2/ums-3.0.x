package kr.uracle.ums.core.vo.setting;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 2. 12..
 */
public class OrganizationVo implements Serializable {
    private String ORGANID = "";
    private String ORGANNAME = "";
    private String P_ORGANID = "";
    private int DEPTH = 0;
    private int SORTNO = 0;
    private String USE_YN = "Y";
    private String REGDATE = "";

    public String getORGANID() {
        return ORGANID;
    }

    public void setORGANID(String ORGANID) {
        this.ORGANID = ORGANID;
    }

    public String getP_ORGANID() {
        return P_ORGANID;
    }

    public void setP_ORGANID(String p_ORGANID) {
        P_ORGANID = p_ORGANID;
    }

    public String getORGANNAME() {
        return ORGANNAME;
    }

    public void setORGANNAME(String ORGANNAME) {
        this.ORGANNAME = ORGANNAME;
    }

    public int getDEPTH() {
        return DEPTH;
    }

    public void setDEPTH(int DEPTH) {
        this.DEPTH = DEPTH;
    }

    public int getSORTNO() {
        return SORTNO;
    }

    public void setSORTNO(int SORTNO) {
        this.SORTNO = SORTNO;
    }

    public String getUSE_YN() {
        return USE_YN;
    }

    public void setUSE_YN(String USE_YN) {
        this.USE_YN = USE_YN;
    }

    public String getREGDATE() {
        return REGDATE;
    }

    public void setREGDATE(String REGDATE) {
        this.REGDATE = REGDATE;
    }

    @Override
    public String toString() {
        return "OrganizationVo{" +
            "ORGANID='" + ORGANID + '\'' +
            ", ORGANNAME='" + ORGANNAME + '\'' +
            ", P_ORGANID='" + P_ORGANID + '\'' +
            ", DEPTH=" + DEPTH +
            ", SORTNO=" + SORTNO +
            ", USE_YN='" + USE_YN + '\'' +
            ", REGDATE='" + REGDATE + '\'' +
            '}';
    }
    public Map<String, Object> toMap() {
    	Map<String, Object> map = new HashMap<String, Object>();
    	map.put("p_organid", P_ORGANID);
    	return map;
    }
}
