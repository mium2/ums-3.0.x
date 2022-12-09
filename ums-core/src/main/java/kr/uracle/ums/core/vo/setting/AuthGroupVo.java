package kr.uracle.ums.core.vo.setting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Y.B.H(mium2) on 2019. 1. 22..
 */
public class AuthGroupVo implements Serializable {
	private int AUTHCODE;
	private String AUTHNAME;
	private String REGDATE;
	private String EDITDATE;
	private List<String> SELMENUCODE = new ArrayList<String>();
	
	public int getAUTHCODE() {
		return AUTHCODE;
	}

	public void setAUTHCODE(int AUTHCODE) {
		this.AUTHCODE = AUTHCODE;
	}

	public String getAUTHNAME() {
		return AUTHNAME;
	}

	public void setAUTHNAME(String AUTHNAME) {
		this.AUTHNAME = AUTHNAME;
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
	public List<String> getSELMENUCODE() {
		return SELMENUCODE;
	}
	@Override
	public String toString() {
		return "AuthGroupVo{" + "AUTHCODE=" + AUTHCODE + ", AUTHNAME='" + AUTHNAME + '\'' + ", REGDATE='" + REGDATE
				+ '\'' + ", EDITDATE='" + EDITDATE + '\'' + '}';
	}
}
