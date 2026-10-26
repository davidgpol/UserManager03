import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
public class OperatorHash {
    public static void main(String[] args) throws Exception {
        // Caller must obtain input with getpass; no password in argv or output.
        char[] value = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).toCharArray();
        var encoder = new Argon2PasswordEncoder(16,32,1,19456,2);
        var hash = encoder.encode(new String(value));
        long start = System.nanoTime();
        if (!encoder.matches(new String(value), hash)) throw new IllegalStateException("Verification failed");
        System.err.printf("Argon2id verification: %.1f ms%n", (System.nanoTime()-start)/1e6);
        Arrays.fill(value, '\0');
        System.out.print("{argon2}" + hash);
    }
}
