package com.discordmini.server.model;

import java.util.HashSet;
import java.util.Set;

public class Group {
    private String groupId;
    private String name;
    private String owner;
    private Set<String> members;

    public Group(String groupId, String name, String owner) {
        this.groupId = groupId;
        this.name = name;
        this.owner = owner;
        this.members = new HashSet<>();
        this.members.add(owner);
    }

    public Group(String groupId, String name, String owner, Set<String> members) {
        this.groupId = groupId;
        this.name = name;
        this.owner = owner;
        this.members = members != null ? members : new HashSet<>();
    }

    public String getGroupId() { return groupId; }
    public String getName() { return name; }
    public String getOwner() { return owner; }
    public Set<String> getMembers() { return members; }

    public void addMember(String username) { this.members.add(username); }
    public void removeMember(String username) { this.members.remove(username); }
}
