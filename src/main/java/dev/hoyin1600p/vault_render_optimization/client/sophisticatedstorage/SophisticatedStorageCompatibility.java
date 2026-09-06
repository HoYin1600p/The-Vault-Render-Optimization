package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

public final class SophisticatedStorageCompatibility {
    public static final String STORAGE_VERSION = "1.18.2-0.9.8.915";
    public static final String CORE_VERSION = "1.18.2-0.6.4.604";

    private SophisticatedStorageCompatibility() {
    }

    public static boolean supports(String storageVersion, String coreVersion) {
        return STORAGE_VERSION.equals(storageVersion) && CORE_VERSION.equals(coreVersion);
    }
}
