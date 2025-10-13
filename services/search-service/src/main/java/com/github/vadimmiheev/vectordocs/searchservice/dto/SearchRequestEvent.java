package com.github.vadimmiheev.vectordocs.searchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestEvent {
    private String requestId;
    private String query;
    private ArrayList<SearchContextItem> context;
    private String userId;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class SearchContextItem {
        private String role;
        private String  message;
    }
}
