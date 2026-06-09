package com.streaminglab.orchestration.testrun.api;

import com.streaminglab.orchestration.testrun.application.TestRunNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TestRunExceptionHandler {

  @ExceptionHandler(TestRunNotFoundException.class)
  ResponseEntity<Void> handleTestRunNotFound() {
    return ResponseEntity.notFound().build();
  }
}
