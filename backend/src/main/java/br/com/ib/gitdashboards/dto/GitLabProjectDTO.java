package br.com.ib.gitdashboards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record GitLabProjectDTO(
    Long id,
    String name,
    @JsonProperty("name_with_namespace") String nameWithNamespace,
    String path,
    @JsonProperty("path_with_namespace") String pathWithNamespace,
    String description,
    @JsonProperty("web_url") String webUrl,
    String visibility,
    @JsonProperty("default_branch") String defaultBranch,
    @JsonProperty("star_count") Integer starCount,
    @JsonProperty("forks_count") Integer forksCount,
    @JsonProperty("open_issues_count") Integer openIssuesCount,
    @JsonProperty("created_at") OffsetDateTime createdAt,
    @JsonProperty("last_activity_at") OffsetDateTime lastActivityAt
) {}
