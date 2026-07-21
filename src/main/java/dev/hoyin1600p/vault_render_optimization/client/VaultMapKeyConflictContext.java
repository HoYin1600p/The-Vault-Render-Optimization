package dev.hoyin1600p.vault_render_optimization.client;

import iskallia.vault.core.vault.ClientVaults;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

public enum VaultMapKeyConflictContext implements IKeyConflictContext {
    INSTANCE;

    @Override
    public boolean isActive() {
        return KeyConflictContext.IN_GAME.isActive() && ClientVaults.getActive().isPresent();
    }

    @Override
    public boolean conflicts(IKeyConflictContext other) {
        return other == this || KeyConflictContext.IN_GAME.conflicts(other);
    }
}
