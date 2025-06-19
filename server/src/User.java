import java.io.IOException;
import java.util.UUID;

import auth.BCrypt;

public class User {
    private static String id;
    private String username;
    String hashedPassword;

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        User.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public boolean checkPassword(String password) {
        return BCrypt.checkpw(password, hashedPassword);
    }

}
