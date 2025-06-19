/**
 * New Data type to insert corectly user's data in json file
 */

package auth;

public class DataUser {
    private String hashedPassword;

    /**
     * DataUser's consturctor
     *
     * @param hashedPassword
     */
    public DataUser(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    /**
     * Hashedpassowrd getter
     *
     * @return hashedPassword
     */
    public String getHashedPassword() {
        return hashedPassword;
    }

    /**
     * HashedPassword setter
     *
     * @param hashedPassword
     */
    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public DataUser() {
    }
}
