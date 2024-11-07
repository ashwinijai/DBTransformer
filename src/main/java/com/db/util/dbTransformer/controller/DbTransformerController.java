package com.db.util.dbTransformer.controller;

import com.db.util.dbTransformer.model.TransformRequest;
import com.db.util.dbTransformer.service.DBTransformerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DbTransformerController {
    @Autowired
    DBTransformerService dbTransformerService;

    @PostMapping("/transform")
    public ResponseEntity<String> transform(@RequestBody TransformRequest transformJson){
        String message = dbTransformerService.transform(transformJson);
        System.out.println(message);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}
