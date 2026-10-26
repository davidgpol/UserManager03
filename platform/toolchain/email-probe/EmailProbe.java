import jakarta.validation.Validation;
import jakarta.validation.constraints.Email;

// Provider probe only: required/null/blank validation belongs to the service.
class EmailProbe {
    record Input(@Email String email) {}
    public static void main(String[] args) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var values = new String[] {"ana.prueba@example.test", "a+b@example.test",
                "not-an-email", "a@", "@example.test", "a b@example.test",
                " a@example.test ", "a@localhost", "", "a..b@example.test"};
            for (var value : values) {
                System.out.println(value + "\t" + validator.validate(new Input(value)).isEmpty());
            }
        }
    }
}
