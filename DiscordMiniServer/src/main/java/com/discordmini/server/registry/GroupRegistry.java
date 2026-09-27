package com.discordmini.server.registry;

import com.discordmini.server.model.Group;
import com.discordmini.server.utils.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class GroupRegistry {
    private static final Map<String, Set<String>> groupMembersMap = new ConcurrentHashMap<>();

    public static void registerGroup(String groupId, Set<String> members) {
        if (groupId != null && members != null) {
            Set<String> memberSet = new CopyOnWriteArraySet<>(members);
            groupMembersMap.put(groupId, memberSet);
            Logger.info("Group '" + groupId + "' registered with " + members.size() + " members.");
        }
    }

    public static void addMemberToGroup(String groupId, String username) {
        groupMembersMap.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(username);
    }

    public static void removeMemberFromGroup(String groupId, String username) {
        Set<String> members = groupMembersMap.get(groupId);
        if (members != null) {
            members.remove(username);
        }
    }

    public static Set<String> getGroupMembers(String groupId) {
        return groupMembersMap.getOrDefault(groupId, Collections.emptySet());
    }
}
