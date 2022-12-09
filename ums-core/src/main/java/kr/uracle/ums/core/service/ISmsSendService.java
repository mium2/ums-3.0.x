package kr.uracle.ums.core.service;

import kr.uracle.ums.core.processor.push.PushBasicProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;

import java.util.List;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2020. 12. 8..
 */
public interface ISmsSendService {
    public void umsSmsSend(final Map<String,List<String>> users, final UmsSendMsgBean umsSendMsgBean);
    public void umsSmsSend(final Map<String,List<String>> users, final UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> cuidVarMap);
    public void umsSmsSend(final PushEachProcessBean pushEachProcessBean, final UmsSendMsgBean umsSendMsgBean) throws Exception;
    public void umsSmsSend(final PushBasicProcessBean pushBasicProcessBean) throws Exception;
}
