package kr.uracle.ums.core.exception;

import java.io.Serializable;

import kr.uracle.ums.codec.redis.config.ErrorManager;

public class MacroChannelException extends Exception implements Serializable {

    public MacroChannelException(String message) {
        super(message);
    }
    public String getErrorCode() {
        return ErrorManager.ERR_1200;
    }
}
