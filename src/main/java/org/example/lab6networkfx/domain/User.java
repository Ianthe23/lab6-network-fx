package org.example.lab6networkfx.domain;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class for a user
 */
public class User extends Entity<Integer>{
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private ArrayList<User> friendships;
    private ArrayList<User> pendingFriendships;

    /**
     * Constructor for a user
     * @param firstName - the first name
     * @param lastName - the last name
     * @param username - the username
     */
    public User(String firstName, String lastName, String username, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.friendships = new ArrayList<>();
        this.pendingFriendships = new ArrayList<>();
    }

    /**
     * Getter for the first name
     * @return firstName - the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Setter for the first name
     * @param firstName - the new first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Getter for the last name
     * @return lastName - the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Setter for the last name
     * @param lastName - the new last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Getter for the username
     * @return username - the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Setter for the username
     * @param username - the new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Getter for the friendships
     * @return friendships - the friendships
     */
    public ArrayList<User> getFriendships() {
        return friendships;
    }

    /**
     * Setter for the friendships
     * @param friendships - the new friendships
     */
    public void setFriendships(ArrayList<User> friendships) {
        this.friendships = friendships;
    }

    /**
     * Getter for the pending friendships
     * @return pendingFriendships - the pending friendships
     */
    public ArrayList<User> getPendingFriendships() {
        return pendingFriendships;
    }

    /**
     * Setter for the pending friendships
     * @param pendingFriendships - the new pending friendships
     */
    public void setPendingFriendships(ArrayList<User> pendingFriendships) {
        this.pendingFriendships = pendingFriendships;
    }

    /**
     * Getter for the password
     * @return password - the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Setter for the password
     * @param password - the new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * toString method
     * @return toPrint - the string to print
     */
    @Override
    public String toString() {
        String toPrint =  "\u001B[33mName\u001B[0m: " + firstName + " " + lastName + "; \u001B[36musername\u001B[0m: " + username + "; \u001B[34mfriends\u001B[0m: ";
        if(!friendships.isEmpty())
        {
            String friendslist=friendships.stream().map(User::getUsername).collect(Collectors.joining(" "));
            toPrint+=friendslist;
        }
        else
        {
            toPrint+="no friends";
        }

        return toPrint;
    }

    /**
     * equals method
     * @param obj - the object to compare to
     * @return true - if the objects are equal, false - otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User user)) return false;
        return getUsername().equals(user.getUsername());
    }

    /**
     * hashCode method
     * @return hash - the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(getUsername());
    }
}