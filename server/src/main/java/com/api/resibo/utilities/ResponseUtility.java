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
        Optional<StackWalker.StackFrame> caller = callerFrame();
        res.setClassName(caller.map(frame -> frame.getDeclaringClass().getSimpleName()).orElse(null));
        res.setMethodName(caller.map(StackWalker.StackFrame::getMethodName).orElse(null));
        res.setResponseTime(new Date());
        return res;
    }

    /**
     * The first frame outside this class — i.e. whoever asked for the response.
     * RETAIN_CLASS_REFERENCE lets us report the simple class name
     * ("HealthController") rather than the fully-qualified one, which is what
     * a caller actually finds useful and leaks less about internal packaging.
     */
    private static Optional<StackWalker.StackFrame> callerFrame() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames
                .filter(frame -> !ResponseUtility.class.equals(frame.getDeclaringClass()))
                .findFirst());
    }
}
