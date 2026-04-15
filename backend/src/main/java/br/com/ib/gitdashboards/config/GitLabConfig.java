package br.com.ib.gitdashboards.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "gitlab")
public interface GitLabConfig {

    String url();

    String token();

    Api api();

    interface Api {
        @WithName("projects-path")
        @WithDefault("/api/v4/projects")
        String projectsPath();

        @WithName("per-page-default")
        @WithDefault("50")
        int perPageDefault();

        Pagination pagination();

        interface Pagination {
            @WithName("max-per-page")
            @WithDefault("100")
            int maxPerPage();
        }
    }
}
