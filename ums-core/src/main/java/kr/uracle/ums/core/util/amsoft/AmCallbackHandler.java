package kr.uracle.ums.core.util.amsoft;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import kr.uracle.ums.core.util.amsoft.result.ResultSqlMgr;
import kr.uracle.ums.core.util.amsoft.result.ResultUmsLogBean;
import kr.uracle.ums.tcppitcher.codec.messages.AckMessage;
import kr.uracle.ums.tcppitcher.codec.messages.BaseBodyMessage;
import kr.uracle.ums.tcppitcher.codec.messages.BaseHeaderMessage;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmCallbackHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Gson gson = new Gson();
    private String AM_SERVER_GROUP = "REAL";
    /**
     * Creates a client-side handler.
     */
    public AmCallbackHandler(String amServerGroup) {
        this.AM_SERVER_GROUP = amServerGroup;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("연결성공");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            BaseHeaderMessage baseHeaderMessage = (BaseHeaderMessage)msg;
            if(baseHeaderMessage.getMsgType().equals(BaseHeaderMessage.ACKMSG)){
                // ACK 받았을 경우 ACK관리매니저에서 삭제처리.
                AckMessage ackMessage = (AckMessage)msg;
                Element element = AmSendMsgECache.getInstance().getCacheElement(ackMessage.getMessageId());

                AmSendMsgECache.getInstance().remove(ackMessage.getMessageId());

                logger.info(gson.toJson(ackMessage));
                if(!"0000".equals(ackMessage.getReturnCode())){
                    // 실패일 경우 T_UMS_LOG 테이블에 담는다.
                    Object obj = element.getObjectValue();
                    BaseBodyMessage baseBodyMessage = (BaseBodyMessage)obj;
                    // 실패일 경우 대체발송을 위한 처리.. T_UMS_LOG에 인설트 하는 큐만들어 담아야 함.
                    ResultUmsLogBean resultUmsLogBean = new ResultUmsLogBean();
                    resultUmsLogBean.setTRANS_TYPE(baseBodyMessage.getTranType());
                    resultUmsLogBean.setSEND_TYPE(baseBodyMessage.getSendChannel());
                    resultUmsLogBean.setPROVIDER("AM");
                    resultUmsLogBean.setMOBILE_NUM(baseBodyMessage.getPhoneNum());
                    resultUmsLogBean.setSEND_TYPE_SEQCODE(ackMessage.getMessageId());
                    resultUmsLogBean.setERRCODE(ackMessage.getReturnCode());
                    resultUmsLogBean.setRESULTMSG(ackMessage.getReturnMsg());
                    ResultSqlMgr.getInstance().putWork(resultUmsLogBean);
                }
            }
        } catch (Throwable ex) {
            logger.error("["+AM_SERVER_GROUP +"] 올바른 프로토콜의 정의된 메세지가 아닙니다. " + ex.getCause());
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        AmClientPoolMgr.getInstance().removeConnectedTcpClient(ctx,AM_SERVER_GROUP);
        logger.warn("!!! ["+AM_SERVER_GROUP +"][TcpHandler] {} : channelInactive!!",ctx.channel().remoteAddress().toString());
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        logger.error("!!! ["+AM_SERVER_GROUP +"] [TcpHandler] {} : exceptionCaught!! cause:{}",ctx.channel().remoteAddress().toString(),cause.toString());
        cause.printStackTrace();
        ctx.close();
    }
}
