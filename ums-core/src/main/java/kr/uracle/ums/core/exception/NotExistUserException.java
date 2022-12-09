package kr.uracle.ums.core.exception;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2019. 5. 13..
 */
public class NotExistUserException extends Exception implements Serializable {

    public NotExistUserException(String message) {
        super(message);
    }
}
