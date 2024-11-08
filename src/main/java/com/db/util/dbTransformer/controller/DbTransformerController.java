package com.db.util.dbTransformer.controller;

import com.db.util.dbTransformer.model.TransformRequest;
import com.db.util.dbTransformer.service.DBTransformerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
public class DbTransformerController {
    @Autowired
    DBTransformerService dbTransformerService;

    @PostMapping("/transform")
    public ResponseEntity<List<String>> transform(@RequestBody JsonNode transformJson) throws IOException {
        Set<ValidationMessage> validationErrors = dbTransformerService.validateJson(transformJson);
        List<String> output = new ArrayList<>();
        if (validationErrors.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            TransformRequest transformRequest = mapper.treeToValue(transformJson, TransformRequest.class);
            output = dbTransformerService.transform(transformRequest);
        } else {
            output = validationErrors.stream().map(ValidationMessage::toString).toList();
        }
        return new ResponseEntity<>(output, HttpStatus.OK);
    }

    @GetMapping("/getJsonSchema")
    public ResponseEntity<String> getJsonSchema() throws JsonProcessingException {
        return new ResponseEntity<>(dbTransformerService.getJsonSchema(), HttpStatus.OK);
    }
}
