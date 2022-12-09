import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;

import java.util.Set;

public class JunitTest {

    public static void main(String[] args){
        try {
            new JunitTest().testGetFirstSendChannel();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void testGetFirstSendChannel() throws Exception{
        UmsSendMacroService umsSendMacroService = new UmsSendMacroService();
        UmsSendMsgBean umsSendMsgBean = makeUmsSendMsgBean();
        Set<SendType> sendTypeSet = umsSendMacroService.getFirstSendChannel(umsSendMsgBean);

        for(SendType sendType : sendTypeSet){
            System.out.println("발송채널 : "+sendType.toString());
            Set<SendType> daecheSendTypeSet = umsSendMacroService.getNextSendChannel(umsSendMsgBean,sendType);
            for(SendType daecheSendType : daecheSendTypeSet){
                System.out.println("대체발송채널 : "+daecheSendType.toString());
            }
        }


    }

    private UmsSendMsgBean makeUmsSendMsgBean(){
        UmsSendMsgBean umsSendMsgBean = new UmsSendMsgBean(TransType.REAL.toString());
        umsSendMsgBean.setPUSH_MSG("푸시발송");
        umsSendMsgBean.setALLIMTOLK_TEMPLCODE("1000006");
        umsSendMsgBean.setFRIENDTOLK_MSG("친구톡 메세지 발송");
        umsSendMsgBean.setRCS_MSG("RCS 발송");
        umsSendMsgBean.setSMS_MSG("SMS 발송");
        umsSendMsgBean.setSEND_MACRO_CODE("MACRO_003");
        return umsSendMsgBean;
    }
}
