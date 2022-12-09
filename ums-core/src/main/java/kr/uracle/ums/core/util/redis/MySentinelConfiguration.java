package kr.uracle.ums.core.util.redis;

import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;

import java.util.Set;

public class MySentinelConfiguration extends RedisSentinelConfiguration {
    MySentinelConfiguration(){
        super();
    }

    public Set<RedisNode> getMySentinels(){
        Set<RedisNode> sets=super.getSentinels();
        return sets;
    }

    public void setMySentinels( Set<RedisNode> sentinels){
        super.setSentinels(sentinels);
    }
}