package com.github.vadimmiheev.vectordocs.searchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestEvent {
    private String query;
    private HashMap<String, String> context;
    private String userId;
}
