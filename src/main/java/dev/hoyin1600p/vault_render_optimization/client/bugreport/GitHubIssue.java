package dev.hoyin1600p.vault_render_optimization.client.bugreport;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Builds the pre-filled GitHub new-issue link. The body is trimmed to keep the whole URL under
 * {@link #MAX_URL_LENGTH}: GPU status lines go first, then non-default settings, then a hard cut.
 */
public final class GitHubIssue {
    public static final String NEW_ISSUE_URL =
            "https://github.com/HoYin1600p/The-Vault-Render-Optimization/issues/new";
    public static final int MAX_URL_LENGTH = 7500;
    static final String TRUNCATED = "\n... (truncated to fit the link)";

    /** Everything the issue reports, already scrubbed. */
    public record Report(
            String title,
            List<String> versions,
            List<String> renderingStack,
            List<String> vroState,
            List<String> nonDefaultSettings,
            List<String> gpuStatus
    ) {
    }

    /**
     * The crash section; null when the player opens GitHub without the crash report. The report
     * itself never goes into the link: it is on the clipboard for the player to paste.
     */
    public record Crash(String exceptionLine) {
    }

    private GitHubIssue() {
    }

    public static String url(String title, String body) {
        return NEW_ISSUE_URL + "?title=" + encode(title) + "&body=" + encode(body);
    }

    /** The body as it will be sent, trimmed so {@link #url} stays within the limit. */
    public static String fittedBody(Report report, Crash crash) {
        int gpu = report.gpuStatus().size();
        int settings = report.nonDefaultSettings().size();
        String body = body(report, crash, settings, gpu);
        while (url(report.title(), body).length() > MAX_URL_LENGTH && (gpu > 0 || settings > 0)) {
            if (gpu > 0) {
                gpu--;
            } else {
                settings--;
            }
            body = body(report, crash, settings, gpu);
        }
        if (url(report.title(), body).length() <= MAX_URL_LENGTH) {
            return body;
        }
        int low = 0;
        int high = body.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (url(report.title(), body.substring(0, mid) + TRUNCATED).length() <= MAX_URL_LENGTH) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return body.substring(0, low) + TRUNCATED;
    }

    static String body(Report report, Crash crash, int settingsShown, int gpuShown) {
        StringBuilder body = new StringBuilder();
        body.append("**What happened?**\n")
                .append("<!-- Describe the problem and the steps to make it happen again. -->\n\n");
        if (crash != null) {
            body.append("### Crash report\n")
                    .append("Crash report: paste it below (copied to your clipboard).\n");
            if (!crash.exceptionLine().isEmpty()) {
                body.append("- Exception: `").append(crash.exceptionLine().replace('`', '\'')).append("`\n");
            }
            body.append("\n<!-- Paste the crash report here. -->\n\n");
        }
        section(body, "Versions", report.versions());
        section(body, "Rendering stack", report.renderingStack());
        section(body, "VRO state", report.vroState());
        List<String> settings = report.nonDefaultSettings();
        body.append("Non-default settings:");
        if (settings.isEmpty()) {
            body.append(" none\n");
        } else {
            body.append('\n');
            for (int i = 0; i < settingsShown; i++) {
                body.append("- `").append(settings.get(i)).append("`\n");
            }
            if (settingsShown < settings.size()) {
                body.append("- ... and ").append(settings.size() - settingsShown).append(" more\n");
            }
        }
        List<String> gpu = report.gpuStatus();
        if (!gpu.isEmpty()) {
            body.append("\nGPU path status:\n```text\n");
            for (int i = 0; i < gpuShown; i++) {
                body.append(gpu.get(i)).append('\n');
            }
            if (gpuShown < gpu.size()) {
                body.append("... ").append(gpu.size() - gpuShown).append(" more line(s)\n");
            }
            body.append("```\n");
        }
        return body.toString();
    }

    private static void section(StringBuilder body, String heading, List<String> lines) {
        body.append("### ").append(heading).append('\n');
        for (String line : lines) {
            body.append("- ").append(line).append('\n');
        }
        body.append('\n');
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
