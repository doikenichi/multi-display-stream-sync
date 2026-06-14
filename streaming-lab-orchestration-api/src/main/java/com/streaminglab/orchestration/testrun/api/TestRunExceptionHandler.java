package com.streaminglab.orchestration.testrun.api;

import com.streaminglab.orchestration.testrun.application.InvalidTestRunStateTransitionException;
import com.streaminglab.orchestration.testrun.application.TestRunNotFoundException;
import com.streaminglab.orchestration.testrun.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TestRunExceptionHandler {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(TestRunNotFoundException.class)
  public ApiErrorResponse handleTestRunNotFound(TestRunNotFoundException e) {
    return new ApiErrorResponse(e.getMessage());
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler(InvalidTestRunStateTransitionException.class)
  public ApiErrorResponse handleInvalidStateTransition(InvalidTestRunStateTransitionException e) {
    return new ApiErrorResponse(e.getMessage());
  }
}
