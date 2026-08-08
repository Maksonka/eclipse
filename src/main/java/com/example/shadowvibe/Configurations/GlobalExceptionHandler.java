package com.example.shadowvibe.Configurations;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String AJAX_HEADER = "X-Requested-With";
    private static final String UPLOAD_ERROR = "Файл слишком большой. Максимальный размер — 50 МБ";

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request) {
        if (isAjax(request)) {
            return ResponseEntity.status(413).body(Map.of("error", UPLOAD_ERROR));
        }
        ModelAndView mav = new ModelAndView("error/upload-too-large");
        mav.addObject("uploadError", UPLOAD_ERROR);
        return mav;
    }

    private boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader(AJAX_HEADER));
    }
}
