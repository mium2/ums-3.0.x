package kr.uracle.ums.core.exception;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 28..
 */
public class RequestErrException extends Exception implements Serializable {

    public RequestErrException(String message) {
        super(message);
    }
}
