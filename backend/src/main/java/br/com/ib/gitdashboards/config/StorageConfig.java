package br.com.ib.gitdashboards.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "storage")
public interface StorageConfig {

    @WithDefault("local")
    String type();

    Local local();

    S3 s3();

    interface Local {
        @WithName("base-dir")
        @WithDefault("/tmp/gitlab-dashboard/parquet")
        String baseDir();
    }

    interface S3 {
        @WithDefault("http://localhost:9000")
        String endpoint();

        @WithDefault("gitlab-dashboard")
        String bucket();

        @WithDefault("us-east-1")
        String region();

        @WithName("access-key")
        String accessKey();

        @WithName("secret-key")
        String secretKey();

        @WithName("path-style-access")
        @WithDefault("true")
        boolean pathStyleAccess();
    }
}
