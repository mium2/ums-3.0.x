package kr.uracle.ums.core.dao.sms;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.sms.KtSmsProcessBean;
import kr.uracle.ums.core.extention.service.division.DivisionRollManager;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class KtSmsDao implements ISmsDao{
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;
    @Autowired(required = true)
    private DivisionRollManager divisionRollManager;
    @Override
    public int inSmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception {
        KtSmsProcessBean ktSmsProcessBean = (KtSmsProcessBean)baseSmsProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseSmsProcessBean, SendType.SMS, ktSmsProcessBean.getPROVIDER());
        if(tableName!=null){
            ktSmsProcessBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.sms.kt.inSmsSendDynamic", ktSmsProcessBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.sms.kt.inSmsSend", ktSmsProcessBean);
        }
    }
    @Override
    public int inFromSmsToMmsSendMsg(BaseProcessBean baseSmsProcessBean) throws Exception {
        KtSmsProcessBean ktSmsProcessBean = (KtSmsProcessBean)baseSmsProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseSmsProcessBean, SendType.MMS, ktSmsProcessBean.getPROVIDER());
        if(tableName!=null){
            ktSmsProcessBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.sms.kt.inMmsSendDynamic", ktSmsProcessBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.sms.kt.inMmsSend", ktSmsProcessBean);
        }
    }
}
