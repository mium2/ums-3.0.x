package kr.uracle.ums.core.dao.sms;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.sms.LotteSmsProcessBean;
import kr.uracle.ums.core.extention.service.division.DivisionRollManager;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class LotteSmsDao implements ISmsDao{

    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;

    @Autowired(required = true)
    private DivisionRollManager divisionRollManager;

    @Override
    public int inSmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception {
        LotteSmsProcessBean prcsBean = (LotteSmsProcessBean)baseSmsProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseSmsProcessBean, SendType.SMS, prcsBean.getPROVIDER());
        if(tableName!=null){
            prcsBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.sms.lotte.inSmsSendDynamic", prcsBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.sms.lotte.inSmsSend", prcsBean);
        }
    }
    @Override
    public int inFromSmsToMmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception {
        LotteSmsProcessBean prcsBean = (LotteSmsProcessBean)baseSmsProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseSmsProcessBean, SendType.MMS, prcsBean.getPROVIDER());
        if(tableName!=null){
            prcsBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.sms.lotte.inMmsSendDynamic", prcsBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.sms.lotte.inMmsSend", prcsBean);
        }
    }
}
