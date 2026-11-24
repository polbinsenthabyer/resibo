package com.api.resibo.utilities;

import com.api.resibo.dtos.ResponseDto;
import com.api.resibo.types.ResponseType;

import java.util.Date;
import java.util.Optional;

/**
 * Static factories for {@link ResponseDto}. Build every response through these
 * so the envelope stays uniform — see resibo-plan.md §6.1.2.
 */
public class ResponseUtility {

    public static final String CODE_SUCCESS = "S0001";

    private ResponseUtility() {
        // static factories only
    }

    public static ResponseDto getSuccessResponse(String message) {
        return getSuccessResponse(message, null);
    }

    public static ResponseDto getSuccessResponse(String message, Object data) {
        return build(ResponseType.SUCCESS, CODE_SUCCESS, message, data);
    }

    public static ResponseDto getErrorResponse(String code, String message) {
        return build(ResponseType.ERROR, code, message, null);
    }

    private static ResponseDto build(ResponseType type, String code, String message, Object data) {
        ResponseDto res = new ResponseDto();
        res.setResponseType(type);
        res.setCode(code);
        res.setMessage(message);
        res.setData(data);
        res.setClassName(callerFrame().map(StackWalker.StackFrame::getClassName).orElse(null));
        res.setMethodName(callerFrame().map(StackWalker.StackFrame::getMethodName).orElse(null));
        res.setResponseTime(new Date());
        return res;
    }

    /** The first frame outside this class — i.e. whoever asked for the response. */
    private static Optional<StackWalker.StackFrame> callerFrame() {
        return StackWalker.getInstance().walk(frames -> frames
                .filter(frame -> !ResponseUtility.class.getName().equals(frame.getClassName()))
                .findFirst());
    }
}
