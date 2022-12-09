package kr.uracle.ums.core.dao.sms;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.sms.LguSmsProcessBean;
import kr.uracle.ums.core.extention.service.division.DivisionRollManager;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class LguSmsDao implements ISmsDao{
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;
    @Autowired(required = true)
    private DivisionRollManager divisionRollManager;

    @Override
    public int inSmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception {
        LguSmsProcessBean lguSmsProcessBean = (LguSmsProcessBean)baseSmsProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseSmsProcessBean, SendType.SMS, lguSmsProcessBean.getPROVIDER());
        if(tableName!=null){
            lguSmsProcessBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.sms.lgu.inSmsSendDynamic", lguSmsProcessBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.sms.lgu.inSmsSend", lguSmsProcessBean);
        }
    }
    @Override
    public int inFromSmsToMmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception {
        LguSmsProcessBean lguSmsProcessBean = (LguSmsProcessBean)baseSmsProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseSmsProcessBean, SendType.MMS, lguSmsProcessBean.getPROVIDER());
        if(tableName!=null){
            lguSmsProcessBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.sms.lgu.inMmsSendDynamic", lguSmsProcessBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.sms.lgu.inMmsSend", lguSmsProcessBean);
        }
    }
}
