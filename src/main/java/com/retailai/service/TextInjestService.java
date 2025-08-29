package com.retailai.service;

import com.retailai.model.TextType;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class TextInjestService {
    private final ScanIngestService scanIngestService;

    public TextInjestService(ScanIngestService scanIngestService) {
        this.scanIngestService = scanIngestService;
    }
    public String injestText(String text, TextType textType, String id) throws Exception {
        String result = "";
        Map<String, Object> data  =  scanIngestService.scanAndIndex(String.valueOf(UUID.randomUUID()),textType.name(),id,text);
        return data.toString();
    }

}
