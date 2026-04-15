package br.com.ib.gitdashboards.filter;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Configures SPA fallback routing: any request that is not an API call,
 * a dev-UI path, or a static asset (identified by containing a dot) is
 * rerouted to {@code /index.html} so that react-router can handle it
 * client-side.
 */
@ApplicationScoped
public class SpaFallbackRoute {

    public void init(@Observes Router router) {
        router.route("/*").last().handler(ctx -> {
            String path = ctx.normalizedPath();
            if (!path.startsWith("/api")
                    && !path.startsWith("/q/")
                    && !path.contains(".")) {
                ctx.reroute("/index.html");
            } else {
                ctx.next();
            }
        });
    }
}
