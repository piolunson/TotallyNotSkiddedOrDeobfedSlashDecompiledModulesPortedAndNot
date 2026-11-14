package net.hachimi.client.api.account.type.impl;

import java.util.Optional;
import java.util.UUID;
import net.hachimi.client.api.account.type.MinecraftAccount;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;

public record CrackedAccount(String username) implements MinecraftAccount {
   public net.minecraft.client.session.Session login() {
      return new net.minecraft.client.session.Session(this.username(), UUID.randomUUID(), "", Optional.empty(), Optional.empty(), net.minecraft.client.session.Session.AccountType.LEGACY);
   }
}
