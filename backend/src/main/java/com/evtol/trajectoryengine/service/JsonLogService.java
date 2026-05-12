package com.evtol.trajectoryengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class JsonLogService {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private static final String LOG_DIR = "logs";

    /*
     * Default save
     */
    public void saveResponse(Object response) {

        saveResponse(response, "trajectory");
    }

    /*
     * Save with custom prefix
     */
    public void saveResponse(
            Object response,
            String prefix
    ) {

        try {

            /*
             * Create logs directory
             */
            Path logPath = Paths.get(LOG_DIR);

            if (!Files.exists(logPath)) {

                Files.createDirectories(logPath);
            }

            /*
             * Timestamp
             */
            String timestamp =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss_SSS"
                    ).format(new Date());

            /*
             * Filename
             */
            String fileName =
                    prefix
                            + "_response_"
                            + timestamp
                            + ".json";

            File outputFile =
                    new File(
                            LOG_DIR + "/" + fileName
                    );

            /*
             * Save JSON
             */
            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(outputFile, response);

            System.out.println(
                    "Saved response log: "
                            + fileName
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}