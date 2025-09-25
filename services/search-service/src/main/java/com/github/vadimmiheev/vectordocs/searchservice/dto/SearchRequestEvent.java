package com.github.vadimmiheev.vectordocs.searchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestEvent {
    private String query;
    private List<String> context;
    private String userId;
}
