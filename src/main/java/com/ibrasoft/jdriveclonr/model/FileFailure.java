package com.ibrasoft.jdriveclonr.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileFailure {
    private String fileName;
    private String errorMessage;
}