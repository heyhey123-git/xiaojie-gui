package io.github.heyhey123.xiaojiegui.test;

import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.test.runner.TestTracker;
import org.bukkit.plugin.java.JavaPlugin;

/** Test-only observer of Skript's own assertions; never included in the addon jar. */
public final class AssertionBridge extends JavaPlugin {
    private boolean ready;

    @Override
    public void onEnable() {
        if (!TestMode.ENABLED || !TestMode.DEV_MODE || TestMode.JUNIT || TestMode.GEN_DOCS) {
            throw new IllegalStateException("Assertion bridge requires Skript development test mode only");
        }
        TestTracker.testStarted("xiaojiegui");
        ready = true;
        getLogger().info("XIAOJIE_ASSERT=READY");
    }

    @Override
    public void onDisable() {
        if (!ready) return;
        // collectResults() clears global state; observing a copy preserves Skript's failure evidence.
        // This is the number of failed test keys, not the number of individual assert executions.
        var failures = TestTracker.getFailedTests();
        failures.forEach((test, message) -> getLogger().severe(
                "XIAOJIE_ASSERT=FAILED " + test + ": " + message.replace('\n', ' ').replace('\r', ' ')));
        getLogger().info("XIAOJIE_ASSERT=FINISHED failures=" + failures.size());
    }
}
