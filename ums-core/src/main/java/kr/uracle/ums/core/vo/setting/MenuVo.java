package kr.uracle.ums.core.vo.setting;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 1. 28..
 */
public class MenuVo implements Serializable {
	private int MENU_ID = 0;
	public int getMENU_ID() {
		return MENU_ID;
	}
	public void setMENU_ID(int mENU_ID) {
		MENU_ID = mENU_ID;
	}
	
	private String MENU_NAME = "";
	public String getMENU_NAME() {
		return MENU_NAME;
	}
	public void setMENU_NAME(String mENU_NAME) {
		MENU_NAME = mENU_NAME;
	}
	
	private String MENU_URL = "";
	public String getMENU_URL() {
		return MENU_URL;
	}
	public void setMENU_URL(String mENU_URL) {
		MENU_URL = mENU_URL;
	}
	
	private String ICON = "";
	public String getICON() {
		return ICON;
	}
	public void setICON(String iCON) {
		ICON = iCON;
	}
	
	private String AUTH_GROUP = "";
	public String getAUTH_GROUP() {
		return AUTH_GROUP;
	}
	public void setAUTH_GROUP(String aUTH_GROUP) {
		AUTH_GROUP = aUTH_GROUP;
	}
	
	private int DEPTH = 1;
	public int getDEPTH() {
		return DEPTH;
	}
	public void setDEPTH(int dEPTH) {
		DEPTH = dEPTH;
	}
	
	private String P_MENU_CODE = "0";
	public String getP_MENU_CODE() {
		return P_MENU_CODE;
	}
	public void setP_MENU_CODE(String p_MENU_CODE) {
		P_MENU_CODE = p_MENU_CODE;
	}
	
	private int SORT_NO = 1;
	public int getSORT_NO() {
		return SORT_NO;
	}
	public void setSORT_NO(int sORT_NO) {
		SORT_NO = sORT_NO;
	}
	
	private String USE_YN = "Y";
	public String getUSE_YN() {
		return USE_YN;
	}
	public void setUSE_YN(String uSE_YN) {
		USE_YN = uSE_YN;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MenuVo [");
		builder.append(String.format(" MENU_ID=%d", MENU_ID));
		builder.append(String.format(",MENU_NAME=%s", MENU_NAME));
		builder.append(String.format(",MENU_URL=%s", MENU_URL));
		builder.append(String.format(",ICON=%s", ICON));
		builder.append(String.format(",AUTH_GROUP=%s", AUTH_GROUP));
		builder.append(String.format(",DEPTH=%s", DEPTH));
		builder.append(String.format(",P_MENU_CODE=%s", P_MENU_CODE));
		builder.append(String.format(",SORT_NO=%s", SORT_NO));
		builder.append(String.format(",USE_YN=%s", USE_YN));
		builder.append("]");
		return builder.toString();
	}
	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("MENU_ID", MENU_ID);
		map.put("MENU_NAME", MENU_NAME);
		map.put("MENU_URL", MENU_URL);
		map.put("ICON", ICON);
		map.put("AUTH_GROUP", AUTH_GROUP);
		map.put("DEPTH", DEPTH);
		map.put("P_MENU_CODE", P_MENU_CODE);
		map.put("SORT_NO", SORT_NO);
		map.put("USE_YN", USE_YN);
		return map;
	}
}