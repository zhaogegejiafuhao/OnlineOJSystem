package cn.edu.zjnu.acm.exception;

import cn.edu.zjnu.acm.util.RestfulResult;
import cn.edu.zjnu.acm.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionResolver {
    public static final Result pleaseLoginResult = new Result(403, "请登录 Please Login");

    @ExceptionHandler(NeedLoginException.class)
    @ResponseBody
    public Result exceptionHandle() {
        return pleaseLoginResult;
    }

    @ExceptionHandler(UnavailableException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result unavilableHandle() {
        return new Result(503, "维护中，不可用");
    }

    @ExceptionHandler({BindException.class, ConstraintViolationException.class})
    public Result validatorExceptionHandler(Exception e) {
        String msg = "输入数据验证失败";
        if (e instanceof BindException) {
            BindException bindException = (BindException) e;
            StringBuilder errorMsg = new StringBuilder();
            bindException.getBindingResult().getFieldErrors().forEach((error) -> {
                errorMsg.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
            });
            if (errorMsg.length() > 0) {
                msg = errorMsg.substring(0, errorMsg.length() - 2);
            }
        } else if (e instanceof ConstraintViolationException) {
            ConstraintViolationException cvException = (ConstraintViolationException) e;
            StringBuilder errorMsg = new StringBuilder();
            cvException.getConstraintViolations().forEach((violation) -> {
                String propertyPath = violation.getPropertyPath().toString();
                errorMsg.append(propertyPath).append(": ").append(violation.getMessage()).append("; ");
            });
            if (errorMsg.length() > 0) {
                msg = errorMsg.substring(0, errorMsg.length() - 2);
            }
        }
        return new Result(400, msg);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseBody
    public Result handleBindException(MethodArgumentNotValidException ex) {
        StringBuilder msg = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach((error) -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            msg.append(fieldName).append(": ").append(errorMessage).append("; ");
        });
        String errorMsg = msg.length() > 0 ? msg.substring(0, msg.length() - 2) : "输入数据验证失败";
        return new Result(400, errorMsg);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public Result notFoundExceptionHandle(NotFoundException e) {
        return new Result(404, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public Result forbiddenExceptionHandle(ForbiddenException e) {
        return new Result(403, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public Result serverExceptionHandle(Exception e) {
        e.printStackTrace();
        return new Result(500, "Internal Server Error");
    }
}
