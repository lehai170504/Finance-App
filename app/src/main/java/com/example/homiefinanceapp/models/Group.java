package com.example.homiefinanceapp.models;
import java.util.List;

public class Group {
    private String id;
    private String name;
    private String inviteCode;
    private GroupUser owner;
    private List<GroupUser> members;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getInviteCode() { return inviteCode; }
    public GroupUser getOwner() { return owner; }
    public List<GroupUser> getMembers() { return members; }
}