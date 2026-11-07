package com.cse3230.university.exception;

import com.cse3230.university.controller.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = {AdminController.class, TeacherController.class, StudentController.class})
public class MvcExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ModelAndView missing(NotFoundException exception) { return page(HttpStatus.NOT_FOUND, exception.getMessage()); }
    @ExceptionHandler(ConflictException.class)
    public ModelAndView conflict(ConflictException exception) { return page(HttpStatus.CONFLICT, exception.getMessage()); }

    private ModelAndView page(HttpStatus status, String message) {
        ModelAndView result = new ModelAndView("error");
        result.setStatus(status);
        result.addObject("status", status.value());
        result.addObject("safeMessage", message);
        return result;
    }
}
