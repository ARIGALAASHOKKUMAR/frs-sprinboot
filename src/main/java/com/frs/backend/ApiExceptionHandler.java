package com.frs.backend;
import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestControllerAdvice public class ApiExceptionHandler {@ExceptionHandler(Exception.class) public ResponseEntity<Map<String,Object>> error(Exception e){String msg=e.getMessage()==null?"Internal server error":e.getMessage();System.err.println("Server error: "+msg);return ResponseEntity.status(500).body(Map.of("success",false,"message",msg));}}
