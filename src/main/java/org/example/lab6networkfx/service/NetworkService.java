package org.example.lab6networkfx.service;

import org.example.lab6networkfx.domain.Friendship;
import org.example.lab6networkfx.domain.Tuple;
import org.example.lab6networkfx.domain.User;
import org.example.lab6networkfx.domain.friendships.FriendshipRequest;
import org.example.lab6networkfx.domain.friendships.FriendshipStatus;
import org.example.lab6networkfx.exceptions.ServiceException;
import org.example.lab6networkfx.repository.Repository;
import org.example.lab6networkfx.repository.database.UserRepo;
import org.example.lab6networkfx.utils.events.EventType;
import org.example.lab6networkfx.utils.events.FriendshipStatusType;
import org.example.lab6networkfx.utils.events.NetworkEvent;
import org.example.lab6networkfx.utils.observer.Observable;
import org.example.lab6networkfx.utils.observer.Observer;
import org.example.lab6networkfx.utils.paging.Page;
import org.example.lab6networkfx.utils.paging.Pageable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Service for the network
 */
public class NetworkService implements Service<Integer>, Observable<NetworkEvent> {
    private final UserRepo userRepo;
    private final Repository friendshipRepo;
    private final Repository friendshipRequestRepo;
    private List<Observer<NetworkEvent>> observers=new ArrayList<>();
    public Set<User> set; //for verifying if a user has been traversed
    private final String type;

    /**
     * Constructor
     * @param userRepo - the repository for the users
     * @param friendshipRepo - the repository for the friendships
     */
    public NetworkService(UserRepo userRepo, Repository friendshipRepo, Repository friendshipRequestRepo, String type) {
        this.userRepo = userRepo;
        this.friendshipRepo = friendshipRepo;
        this.friendshipRequestRepo = friendshipRequestRepo;
        this.type = type;
    }

    /**
     * Method for adding a user
     * @param firstName - the first name of the user
     * @param lastName - the last name of the user
     * @param username - the username of the user
     * @return true - if the user was added
     *         false - if the user already exists
     */
    @Override
    public boolean addUser(String firstName, String lastName, String username, String password) {
        User newUser = new User(firstName, lastName, username, password);
        userRepo.save(newUser);
        notifyObservers(new NetworkEvent(EventType.ADD, newUser));
        return true;
    }

    /**
     * Method for removing a user
     * @param username - the username of the user to be removed
     * @return true - if the user was removed
     *         false - if the user does not exist
     */
    @Override
    public User removeUser(String username) {
        User foundUser = findUsername(username);
        if (foundUser == null) {
            throw new ServiceException("User not found!");
        }

        if (Objects.equals(type, "InMemory")) {
            List<User> friends = new ArrayList<>(foundUser.getFriendships());
            List<User> pendingFriends = new ArrayList<>(foundUser.getPendingFriendships());
            try {
                friends.forEach(friend->removeFriendship(foundUser.getUsername(), friend.getUsername()));
                friends.forEach(friend->removeFriendship(friend.getUsername(), foundUser.getUsername()));
                pendingFriends.forEach(pendingFriend->rejectFriendshipRequest(foundUser.getUsername(), pendingFriend.getUsername()));
                pendingFriends.forEach(pendingFriend->rejectFriendshipRequest(pendingFriend.getUsername(), foundUser.getUsername()));
            } catch (ServiceException e) {

            }
            foundUser.getFriendships().clear();
            foundUser.getPendingFriendships().clear();
        }

        Optional<User> deletedUser = userRepo.delete(foundUser.getId());
        notifyObservers(new NetworkEvent(EventType.DELETE, deletedUser.get()));
        return deletedUser.get();
    }

    @Override
    public boolean pendingFriendshipRequest(String username1, String username2) {
        User user1 = findUsername(username1);
        User user2 = findUsername(username2);

        if (user1.getFriendships().contains(user2) || user2.getFriendships().contains(user1)) {
            throw new ServiceException("They are already friends!");
        }

        try {
            FriendshipRequest friendshipRequest = new FriendshipRequest(user1, user2, FriendshipStatus.PENDING, LocalDateTime.now());
            friendshipRequestRepo.save(friendshipRequest);
            notifyObservers(new NetworkEvent(EventType.PEND, friendshipRequest));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean rejectFriendshipRequest(String username1, String username2) {
        User user1 = findUsername(username1);
        User user2 = findUsername(username2);

        FriendshipRequest friendshipRequest = findFriendshipRequest(user1, user2);

        if (friendshipRequest == null) {
            throw new ServiceException("The friendship request was not found!");
        }

        friendshipRequest.setStatus(FriendshipStatus.REJECTED);
        friendshipRequestRepo.update(friendshipRequest);
        notifyObservers(new NetworkEvent(EventType.REJECT, friendshipRequest));
        return true;
    }

    private FriendshipRequest findFriendshipRequest(User user1, User user2) {
        Tuple<Integer, Integer> id = new Tuple<>(user1.getId(), user2.getId());

        if (type.equals("InMemory")) {
            Optional<FriendshipRequest> friendshipRequest = friendshipRequestRepo.findOne(id);

            if (friendshipRequest == null) {
                Tuple<Integer, Integer> id2 = new Tuple<>(user2.getId(), user1.getId());
                friendshipRequest = friendshipRequestRepo.findOne(id2);
            }

            return friendshipRequest.get();
        } else {
            System.out.println("Finding entered successfully");
            return (FriendshipRequest) friendshipRequestRepo.findOne(id).get();
        }
    }

    @Override
    public boolean acceptFriendshipRequest(String username1, String username2) {
        User user1 = findUsername(username1);
        User user2 = findUsername(username2);

        if (user1.getFriendships().contains(user2) || user2.getFriendships().contains(user1)) {
            throw new ServiceException("They are already friends!");
        }

        FriendshipRequest friendshipRequest = findFriendshipRequest(user1, user2);

        if (friendshipRequest == null) {
            throw new ServiceException("The friendship request was not found!");
        }

        friendshipRequest.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRequestRepo.update(friendshipRequest);
        notifyObservers(new NetworkEvent(EventType.ACCEPT, friendshipRequest));

        Friendship friendship = new Friendship(user1, user2);
        friendshipRepo.save(friendship);
        notifyObservers(new NetworkEvent(EventType.ADD, friendship));

        return true;
    }

    public List<FriendshipRequest> getPendingFriendshipRequests(String username) {
        User user = findUsername(username);
        List<FriendshipRequest> pendingFriendshipRequests = new ArrayList<>();
        Iterable<FriendshipRequest> friendshipRequests = friendshipRequestRepo.findAll();

        StreamSupport.stream(friendshipRequests.spliterator(), false)
                .filter(friendshipRequest -> friendshipRequest.getUser2().equals(user))
                .forEach(pendingFriendshipRequests::add);

        return pendingFriendshipRequests;
    }

    public List<User> getFriendRequests(String username) {
        User user = findUsername(username);
        List<User> friendRequests = new ArrayList<>();
        Iterable<FriendshipRequest> friendshipRequests = friendshipRequestRepo.findAll();

        StreamSupport.stream(friendshipRequests.spliterator(), false)
                .filter(friendshipRequest -> friendshipRequest.getUser2().equals(user))
                .forEach(friendshipRequest -> friendRequests.add(friendshipRequest.getUser1()));

        return friendRequests;
    }

    /**
     * Method for adding a friendship
     * @param username1 - the username of the first user
     * @param username2 - the username of the second user
     * @return true - if the friendship was added
     *         false - if the friendship already exists
     */
    @Override
    public boolean addFriendship(String username1, String username2) {
        User user1 = findUsername(username1);
        User user2 = findUsername(username2);

        if (user1.getFriendships().contains(user2) || user2.getFriendships().contains(user1)) {
            throw new ServiceException("They are already friends!");
        }

        Friendship friendship = new Friendship(user1, user2);
        friendshipRepo.save(friendship); // This should insert the friendship into the database
        observers.forEach(observer -> observer.update(new NetworkEvent(EventType.ADD, friendship)));

        return true;
    }

    /**
     * Method for removing a friendship
     * @param username1 - the username of the first user
     * @param username2 - the username of the second user
     * @return true - if the friendship was removed
     *         false - if the friendship does not exist
     */
    @Override
    public boolean removeFriendship(String username1, String username2) {
        User user1 = findUsername(username1);
        User user2 = findUsername(username2);

        Friendship friendship = findFriendship(user1, user2);

        if (friendship == null) {
            throw new ServiceException("The friendship was not found!");
        }

        if (type.equals("InMemory")) {
            ArrayList<User> friendshipListuser1=user1.getFriendships();
            ArrayList<User> friendshipListUser2=user2.getFriendships();

            friendshipListuser1.remove(user2);
            friendshipListUser2.remove(user1);

            user1.setFriendships(friendshipListuser1);
            user2.setFriendships(friendshipListUser2);

            userRepo.update(user1);
            userRepo.update(user2);
        }

        friendshipRepo.delete(friendship.getId());
        observers.forEach(observer -> observer.update(new NetworkEvent(EventType.DELETE, friendship)));
        return true;
    }

    /**
     * Method for getting all users
     * @return all users
     */
    @Override
    public Iterable getAllUsers() {
        return userRepo.findAll();
    }

    public List<User> getFriends(String username) {
        User user = findUsername(username);
        return user.getFriendships();
    }

    public Page<User> findAllOnPage(Pageable pageable) {
        return userRepo.findAllOnPage(pageable);
    }

    public Page<User> findAllBySearchText(Pageable pageable, String searchText) {
        return userRepo.findAllBySearchText(pageable, searchText);
    }

    /**
     * Method for getting all friendships
     * @return all friendships
     */
    @Override
    public Iterable getAllFriendships() {
        return friendshipRepo.findAll();
    }

    @Override
    public Iterable getAllFriendshipRequests() {
        return friendshipRequestRepo.findAll();
    }

    /**
     * Method for getting the number of communities
     * @return the number of communities
     */
    @Override
    public int numberOfCommunities() {
        Iterable<User> users = userRepo.findAll();
        set = new HashSet<>(); //for verifying if a user has been traversed

        //I traverse all the users that have not been traversed before
        return StreamSupport.stream(users.spliterator(),false)
                .filter(user->!set.contains(user))
                .mapToInt(user->
                {   set.add(user); //I mark the user as traversed
                    DFS(user); //traverse all the friends of the user
                    return 1; })
                .sum();
    }

    /**
     * Method for getting the biggest community
     * @return the biggest community
     */
    @Override
    public List<List<User>> biggestCommunity() {
        // Retrieve all communities
        List<List<User>> allCommunities = getAllCommunities();

        // Determine the maximum route size among all communities
        int maxRouteSize = allCommunities.stream()
                .mapToInt(community -> longestPath((ArrayList<User>) community)) // Find longest path in each community
                .max() // Get the maximum path length
                .orElse(Integer.MIN_VALUE); // Default to MIN_VALUE if no communities exist

        // Filter communities to only include those with the maximum route size
        return allCommunities.stream()
                .filter(community -> longestPath((ArrayList<User>) community) == maxRouteSize)
                .map(ArrayList::new) // Create a new ArrayList for each community
                .collect(Collectors.toList()); // Collect the results into a list
    }

    /**
     * Method for finding a user by username
     * @param username - the username of the user
     * @return the user with the specified username
     */
    public User findUsername(String username) {
        return StreamSupport.stream(userRepo.findAll().spliterator(),false)
                .filter(user->user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Method for finding a friendship between two users
     * @param user1 - the first user
     * @param user2 - the second user
     * @return the friendship between the two users
     */
    private Friendship findFriendship(User user1, User user2) {
        Tuple<Integer, Integer> id = new Tuple<>(user1.getId(), user2.getId());

        if (type.equals("InMemory")) {
            Optional<Friendship> friendship = friendshipRepo.findOne(id);

            if (friendship == null) {
                Tuple<Integer, Integer> id2 = new Tuple<>(user2.getId(), user1.getId());
                friendship = friendshipRepo.findOne(id2);
            }

            return friendship.get();
        } else {
            return (Friendship) friendshipRepo.findOne(id).get();
        }
    }

    /**
     * Method for adding a friendship between two users
     * @param u1 - the first user
     * @param u2 - the second user
     */
//    private void addFriendToUsers(User u1, User u2) {
//        ArrayList<User> newFriends1 = new ArrayList<>(u1.getFriendships());
//        ArrayList<User> newFriends2 = new ArrayList<>(u2.getFriendships());
//        if(newFriends1.contains(u2)) {
//            throw new ServiceException("They are already friends!");
//        }
//
//        newFriends1.add(u2);
//
//        if(u2.getFriendships().contains(u2)) {
//            throw new ServiceException("They are already friends!");
//        }
//        newFriends2.add(u1);
//
//        u1.setFriendships(newFriends1);
//        u2.setFriendships(newFriends2);
//
//        userRepo.update(u1);
//        userRepo.update(u2);
//
//    }

    /**
     * Method for getting all communities
     * @return all communities
     */
    private ArrayList<User> DFS(User user) {
        ArrayList<User> community = new ArrayList<>();
        community.add(user);
        set.add(user);

        //I traverse all of its friends that have not been traversed before
        user.getFriendships().stream().filter(friend->!set.contains(friend)).forEach(friend->{
            ArrayList<User> newL = DFS(friend);
            community.addAll(newL);
        });
        return community;
    }

    /**
     * Method for getting all communities
     * @return all communities
     */
    public List<List<User>> getAllCommunities()
    {
        Iterable<User> users = userRepo.findAll();
        List<List<User>> communities;
        set= new HashSet<>();
        communities=StreamSupport.stream(users.spliterator(),false)
                .filter(user->!set.contains(user)).
                map(this::DFS).
                collect(Collectors.toList());

        return communities;
    }

    /**
     * Method for getting the adjacency list
     * @param pairs - the pairs of users
     * @param V - the number of nodes
     * @return the adjacency list
     */
    private LinkedList<Integer>[] getAdjList(Map<User, Integer> pairs, int V) {
        LinkedList<Integer>[] adj;
        adj = new LinkedList[V];
        IntStream.range(0, V).forEach(i->adj[i] = new LinkedList<>()); // initialize the adjacency list

        if (type.equals("InMemory")) {
            pairs.keySet().forEach(user -> user.getFriendships().forEach(user1 -> adj[pairs.get(user)].add(pairs.get(user1))));
        } else {
            pairs.keySet().forEach(user -> getFriendships(user).forEach(user1 -> adj[pairs.get(user)].add(pairs.get(user1))));

        }

        return adj;
    }

    public List<User> getFriendships(User u)
    {
        List<Optional<User>> friends=new ArrayList<>();
        Iterable <Friendship> friendships=friendshipRepo.findAll();

        return StreamSupport.stream(friendships.spliterator(),false).
                filter(friendship -> Objects.equals(friendship.getId().getSecond(), u.getId()) || Objects.equals(friendship.getId().getFirst(), u.getId()))
                .map(friendship ->
                {
                    if(Objects.equals(friendship.getId().getFirst(), u.getId()))
                        return userRepo.findOne(friendship.getId().getSecond()).orElse(null);
                    else
                        return userRepo.findOne(friendship.getId().getFirst()).orElse(null);
                }).filter(Objects::nonNull).map(element->(User)element).collect(Collectors.toList());
    }

    @Override
    public void addObserver(Observer<NetworkEvent> e) {
        observers.add(e);
    }

    @Override
    public void removeObserver(Observer<NetworkEvent> e) {
        observers.remove(e);
    }

    @Override
    public void notifyObservers(NetworkEvent networkEvent) {
        observers.forEach(observer -> observer.update(networkEvent));
    }

    /**
     * class Pair for the BFS method
     * @param <T> - the first element of the pair
     * @param <V> - the second element of the pair
     */
    static class Pair<T,V> {
        T first; // maximum distance Node
        V second; // distance of maximum distance node

        //Constructor
        Pair(T first, V second) {
            this.first = first;
            this.second = second;
        }
    }

    /**
     * Method for the BFS algorithm
     * @param u - the starting node
     * @param V - the number of nodes
     * @param adj - the adjacency list
     * @return the pair of the maximum distance node and the distance
     */
    private Pair<Integer,Integer> BFS(int u,int V,LinkedList<Integer>[] adj)
    {
        int [] dis = new int[V];
        Arrays.fill(dis,-1); // unvisited nodes
        Queue<Integer> q = new LinkedList<>(); // queue for BFS
        q.add(u);
        dis[u] = 0; // starting node

        while (!q.isEmpty()) {
            int t = q.poll();
            Arrays.stream(adj[t].toArray(new Integer[0]))
                    .filter(v -> dis[v] == -1) // if adjacent node is unvisited
                    .forEach(v -> {
                        q.add(v); // add to queue
                        dis[v] = dis[t] + 1; // update distance of v
                    });
        }

        int[] result = new int[] { 0, 0 }; // result[0] = node, result[1] = distance
        IntStream.range(0, V)
                .filter(i -> dis[i] > result[1]) // find the maximum distance node
                .forEach(i -> {
                    result[0] = i;
                    result[1] = dis[i];
                });

        return new Pair<>(result[0], result[1]);
    }

    /**
     * Method for getting the longest path
     * @param nodes - the nodes
     * @return the longest path
     */
    public int longestPath(ArrayList<User> nodes)
    {
        //working on a graph to find the longest path
        int V = nodes.size();

        Map<User, Integer> pairs = IntStream.range(0, V) // create a stream of integers from 0 to V
                .boxed() // convert the stream of primitives to a Stream<Integer>
                .collect(Collectors.toMap(nodes::get, i -> i)); // create a map of the nodes

        LinkedList<Integer>[] adj = getAdjList(pairs, V); // get the adjacency list
        Pair<Integer,Integer> t1, t2; // pair of the maximum distance node and the distance
        t1 = BFS(0, V, adj); // find the maximum distance node
        t2 = BFS(t1.first, V, adj); // find the maximum distance node from the previous node
        return t2.second; // return the distance
    }


}
