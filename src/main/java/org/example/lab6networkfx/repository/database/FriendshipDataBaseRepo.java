package org.example.lab6networkfx.repository.database;

import org.example.lab6networkfx.domain.Friendship;
import org.example.lab6networkfx.domain.Tuple;
import org.example.lab6networkfx.domain.User;
import org.example.lab6networkfx.domain.validators.Validator;
import org.example.lab6networkfx.exceptions.RepoException;
import org.example.lab6networkfx.repository.database.utils.AbstractDataBaseRepo;
import org.example.lab6networkfx.repository.database.utils.DataBaseAcces;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Database repository for friendships
 */
public class FriendshipDataBaseRepo extends AbstractDataBaseRepo<Tuple<Integer, Integer>, Friendship> {
    /**
     * Constructor
     * @param validator - the validator
     * @param data - the database access
     * @param table - the table name
     */
    public FriendshipDataBaseRepo(Validator validator, DataBaseAcces data, String table) {
        super(validator, data, table);
    }

    /**
     * Method to save a friendship
     * @param entity - the friendship to save
     * @return an {@code Optional} - null if the friendship was saved, the friendship otherwise
     */
    @Override
    public Optional<Friendship> save(Friendship entity) {
        String insertSQL = "INSERT INTO \"" + table + "\"" + " (user1_id, user2_id, since) VALUES (?, ?, ?)";

        try (PreparedStatement statement = data.createStatement(insertSQL)) {
            statement.setInt(1, entity.getId().getFirst());
            statement.setInt(2, entity.getId().getSecond());

            // Format the 'since' LocalDateTime
            String formattedDate = formatter(entity.getSince());
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate parsedDate = LocalDate.parse(formattedDate, dateFormatter);

            statement.setDate(3, java.sql.Date.valueOf(parsedDate));

            int response = statement.executeUpdate();
            return response == 0 ? Optional.of(entity) : Optional.empty();
        } catch (SQLException e) {
            throw new RepoException(e);
        }
    }

    /**
     * Method to format a LocalDateTime
     * @param time - the LocalDateTime to format
     * @return a string - the formatted LocalDateTime
     */
    private String formatter(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return time.format(formatter);
    }

    /**
     * Method to delete a friendship
     * @param id - the id of the friendship to delete
     * @return an {@code Optional} - null if the friendship was deleted, the friendship otherwise
     */
    @Override
    public Optional<Friendship> delete(Tuple<Integer, Integer> id) {
        Optional<Friendship> entity = findOne(id);

        if (id != null) {
            String deleteStatement  = "DELETE FROM \"" + table + "\"" + " WHERE (user1_id = ? AND user2_id = ?) OR (user2_id = ? AND user1_id = ?)";
            int response = 0;
            try {
                PreparedStatement statement = data.createStatement(deleteStatement);
                statement.setInt(1, id.getFirst());
                statement.setInt(2, id.getSecond());
                statement.setInt(3, id.getFirst());
                statement.setInt(4, id.getSecond());
                if (entity.isPresent()) {
                    response = statement.executeUpdate();
                }
                return response == 0 ? Optional.empty() : entity;
            } catch (SQLException e) {
                throw new RepoException(e);
            }
        }
        else {
            throw new IllegalArgumentException("ID must not be null");
        }
    }

    /**
     * Method to update a friendship
     * @param entity - the friendship to update
     * @return an {@code Optional} - null if the friendship was updated, the friendship otherwise
     */
    @Override
    public Optional<Friendship> update(Friendship entity) {
        if (entity == null) {
            throw new RepoException("Entity must not be null");
        }

        String updateStatement = "UPDATE \"" + table + "\"" + " SET user1_id = ?, user2_id = ? WHERE (user1_id = ? AND user2_id = ?) OR (user2_id = ? AND user1_id = ?)";

        try {
            PreparedStatement statement = data.createStatement(updateStatement);
            statement.setInt(1, entity.getId().getFirst());
            statement.setInt(2, entity.getId().getSecond());
            statement.setInt(3, entity.getId().getFirst());
            statement.setInt(4, entity.getId().getSecond());
            statement.setInt(5, entity.getId().getSecond());
            statement.setInt(6, entity.getId().getFirst());
            int response = statement.executeUpdate();
            return response == 0 ? Optional.of(entity) : Optional.empty();
        } catch (SQLException e) {
            throw new RepoException(e);
        }
    }

    /**
     * Method to find a friendship
     * @param id - the id of the friendship to find
     * @return an {@code Optional} - null if the friendship was found, the friendship otherwise
     */
    @Override
    public Optional<Friendship> findOne(Tuple<Integer, Integer> id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }

        String sql = """
        SELECT 
            f.user1_id, f.user2_id,
            u1.firstname AS firstName1, u1.lastname AS lastName1, u1.username AS username1, u1.password AS password1,
            u2.firstname AS firstName2, u2.lastname AS lastName2, u2.username AS username2, u2.password AS password2,
            f.since
        FROM 
            "Friendship" f
        JOIN 
            "User" u1 ON f.user1_id = u1.id
        JOIN 
            "User" u2 ON f.user2_id = u2.id
        WHERE 
            (f.user1_id = ? AND f.user2_id = ?) OR (f.user1_id = ? AND f.user2_id = ?);
        """;

        try (PreparedStatement statement = data.createStatement(sql)) {
            statement.setInt(1, id.getFirst());
            statement.setInt(2, id.getSecond());
            statement.setInt(3, id.getSecond());
            statement.setInt(4, id.getFirst());

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Friendship friendship = getFriendship(resultSet);
                return Optional.of(friendship);
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RepoException(e);
        }
    }

    /**
     * Method to get a friendship from a ResultSet
     * @param resultSet - the ResultSet
     * @return a friendship - the friendship from the ResultSet
     * @throws SQLException
     */
    private Friendship getFriendship(ResultSet resultSet) throws SQLException {
        Integer id1 = resultSet.getInt("user1_id");
        Integer id2 = resultSet.getInt("user2_id");
        String firstName1 = resultSet.getString("firstName1");
        String lastName1 = resultSet.getString("lastName1");
        String username1 = resultSet.getString("username1");
        String password1 = resultSet.getString("password1");
        String firstName2 = resultSet.getString("firstName2");
        String lastName2 = resultSet.getString("lastName2");
        String username2 = resultSet.getString("username2");
        String password2 = resultSet.getString("password2");

        User user1 = new User(firstName1, lastName1, username1, password1);
        user1.setId(id1);
        User user2 = new User(firstName2, lastName2, username2, password2);
        user2.setId(id2);

        return new Friendship(user1, user2);
    }

    /**
     * Method to find all friendships
     * @return an {@code Iterable} - the friendships
     */
    @Override
    public Iterable<Friendship> findAll() {
        String findAllStatement = """
        SELECT f.user1_id, f.user2_id,
               u1.firstname AS firstName1, u1.lastname AS lastName1, u1.username AS username1, u1.password AS password1,
               u2.firstname AS firstName2, u2.lastname AS lastName2, u2.username AS username2, u2.password AS password2,
               f.since
        FROM "Friendship" f
        JOIN "User" u1 ON f.user1_id = u1.id
        JOIN "User" u2 ON f.user2_id = u2.id
        """;

        Set<Friendship> friendships = new HashSet<>();

        try (PreparedStatement statement = data.createStatement(findAllStatement);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Friendship friendship = getFriendship(resultSet);
                friendships.add(friendship);
            }
        } catch (SQLException e) {
            throw new RepoException(e);
        }

        return friendships;
    }
}
