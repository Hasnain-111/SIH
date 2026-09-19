package com.mplads.service;

import com.mplads.model.Project;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MLService {

    private static final String ML_URL =
            "http://127.0.0.1:8000/analyze";

    public String analyzeProject(Project p) throws Exception {

        String json = "{"
                + "\"project_id\":\"" + p.getProjectId() + "\","
                + "\"allocation_amount\":" + p.getAllocationAmount() + ","
                + "\"state\":\"" + p.getState() + "\","
                + "\"district\":\"" + p.getConstituency() + "\","
                + "\"work_description\":\"" + (p.getWork() != null ? p.getWork().replace("\"", "\\\"").replace("\n", " ") : "") + "\","
                + "\"status\":\"" + p.getProjectStatus() + "\""
                + "}";

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ML_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request,
                        HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
}