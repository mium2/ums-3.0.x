package kr.uracle.ums.core.processor.push;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 5..
 */
public class PushNotSendFailProcessBean extends PushBasicProcessBean implements Serializable{
	
	private static final long serialVersionUID = 1L;

    public PushNotSendFailProcessBean() {
        super.setPROVIDER(this.getClass().getSimpleName());
    }

    private String ERRCODE = "0000";
    private String RESULTMSG = "SUCCESS";

    public String getERRCODE() { return ERRCODE; }
    public void setERRCODE(String ERRCODE) { this.ERRCODE = ERRCODE; }

    public String getRESULTMSG() { return RESULTMSG; }
    public void setRESULTMSG(String RESULTMSG) { this.RESULTMSG = RESULTMSG; }
}