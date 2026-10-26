import com.hariklabs.usermanager03.rest.generated.model.User;
import com.hariklabs.usermanager03.rest.generated.model.UserCreateRequest;
import jakarta.validation.Validation;
import java.time.OffsetDateTime;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

class SerializationProbe {
    public static void main(String[] args) {
        var mapper = JsonMapper.builder().build();
        var time = OffsetDateTime.parse("2026-10-26T00:00:00.123456Z");
        var user = new User(UUID.fromString("00000000-0000-4000-8000-000000000001"),
                "👩".repeat(100), "👩".repeat(150), "00000000T", "test@example.test",
                true, time, time);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            if (!factory.getValidator().validate(user).isEmpty())
                throw new AssertionError("Valid supplementary output rejected by generated validation");
            var input = new UserCreateRequest(" " + "👩".repeat(100) + " ",
                    " " + "👩".repeat(150) + " ", " 00000000t ", "test@example.test");
            if (!factory.getValidator().validate(input).isEmpty())
                throw new AssertionError("Valid raw input rejected before domain transformation");
        }
        String json = mapper.writeValueAsString(user);
        if (!json.contains("2026-10-26T00:00:00.123456Z"))
            throw new AssertionError("UTC timestamp not serialized exactly");
        User copy = mapper.readValue(json, User.class);
        if (!user.equals(copy)) throw new AssertionError("Generated DTO round-trip changed values");
        System.out.println("PASS generated DTO: supplementary boundary input/output and Jackson 3 UTC round-trip");
    }
}
