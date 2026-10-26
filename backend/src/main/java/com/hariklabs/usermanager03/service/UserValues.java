package com.hariklabs.usermanager03.service;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.text.Normalizer;

@Component
public class UserValues {
    public record Input(String firstName,String lastName,String dni,String email) {}
    private record EmailValue(@Email String value) {}
    private final Validator validator;
    public UserValues(Validator validator) { this.validator=validator; }
    public Input validate(Input input) {
        String first=UnicodeCategories.name(input.firstName(),"firstName",100);
        String last=UnicodeCategories.name(input.lastName(),"lastName",150);
        String dni=canonicalDni(input.dni());
        if(dni==null) throw DomainException.invalid("dni",input.dni()==null?"REQUIRED":"INVALID_FORMAT");
        if(input.email()==null) throw DomainException.invalid("email","REQUIRED");
        if(input.email().codePoints().allMatch(UserValues::space)) throw DomainException.invalid("email","BLANK");
        if(!validator.validate(new EmailValue(input.email())).isEmpty()) throw DomainException.invalid("email","INVALID_FORMAT");
        return new Input(first,last,dni,input.email());
    }
    private static boolean space(int cp) { return Character.isWhitespace(cp)||Character.isSpaceChar(cp); }
    public static String trimSpaces(String value) {
        int a=0,b=value.length();
        while(a<b && space(value.codePointAt(a))) a+=Character.charCount(value.codePointAt(a));
        while(b>a && space(value.codePointBefore(b))) b-=Character.charCount(value.codePointBefore(b));
        return value.substring(a,b);
    }
    public static String canonicalDni(String value) {
        if(value==null) return null;
        String s=trimSpaces(value);
        if(!s.matches("[0-9]{8}[A-Za-z]")) return null;
        s=s.toUpperCase(Locale.ROOT);
        return s.charAt(8)=="TRWAGMYFPDXBNJZSQVHLCKE".charAt(Integer.parseInt(s.substring(0,8))%23)?s:null;
    }
    /** Search-only projection; never replaces the stored representation. */
    public static String nameSearch(String value) {
        String n=Normalizer.normalize(value,Normalizer.Form.NFD); var result=new StringBuilder();
        n.codePoints().filter(cp -> !UnicodeCategories.category(cp).startsWith("M")).forEach(result::appendCodePoint);
        return result.toString().toLowerCase(Locale.ROOT);
    }
    public static String emailSearch(String value) { return value.toLowerCase(Locale.ROOT); }
}
