package com.streaminglab.orchestration.testrun.api;

import com.streaminglab.orchestration.testrun.application.TestRunNotFoundException;
import com.streaminglab.orchestration.testrun.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
    assignableTypes = {TestRunCommandController.class, TestRunQueryController.class})
public class CommonApiExceptionHandler {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(
      value = TestRunNotFoundException.class,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ApiErrorResponse handleTestRunNotFound(TestRunNotFoundException e) {
    return new ApiErrorResponse(e.getMessage());
  }
}
