package com.hariklabs.usermanager03.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Fixed UCD 16.0.0 table; never uses the runtime's implicit categories. */
public final class UnicodeCategories {
    private record Range(int start, int end, String category) {}
    private static final List<Range> RANGES;
    static {
        try (var in = UnicodeCategories.class.getResourceAsStream("/unicode-16-categories.tsv")) {
            if (in == null) throw new IOException("Missing Unicode table");
            var ranges = new ArrayList<Range>(); int cursor=0;
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String[] f=line.split("\t");
                var r=new Range(Integer.parseInt(f[0],16),Integer.parseInt(f[1],16),f[2]);
                if(r.start()!=cursor || r.end()<r.start()) throw new IOException("Invalid Unicode coverage");
                cursor=r.end()+1; ranges.add(r);
            }
            if(cursor!=0x110000) throw new IOException("Incomplete Unicode coverage");
            RANGES=List.copyOf(ranges);
        } catch(IOException e) { throw new ExceptionInInitializerError(e); }
    }
    private UnicodeCategories() {}
    public static String category(int cp) {
        if(cp<0 || cp>0x10ffff) throw new IllegalArgumentException("Invalid code point");
        int lo=0,hi=RANGES.size()-1;
        while(lo<=hi) {
            int mid=(lo+hi)>>>1; var r=RANGES.get(mid);
            if(cp<r.start()) hi=mid-1;
            else if(cp>r.end()) lo=mid+1;
            else return r.category();
        }
        throw new IllegalStateException("Unicode table gap");
    }
    public static String trimZs(String value) {
        int start=0,end=value.length();
        while(start<end && category(value.codePointAt(start)).equals("Zs")) start+=Character.charCount(value.codePointAt(start));
        while(end>start && category(value.codePointBefore(end)).equals("Zs")) end-=Character.charCount(value.codePointBefore(end));
        return value.substring(start,end);
    }
    public static String name(String value,String field,int maximum) {
        if(value==null) throw DomainException.invalid(field,"REQUIRED");
        String result=trimZs(value); boolean content=false; int count=0;
        for(int i=0;i<result.length();) {
            int cp=result.codePointAt(i); String category=category(cp);
            boolean printable="LMNPS".indexOf(category.charAt(0))>=0;
            if(!printable && !category.equals("Zs") && !category.equals("Cf")) throw DomainException.invalid(field,"INVALID_FORMAT");
            content|=printable; count++; i+=Character.charCount(cp);
        }
        if(!content) throw DomainException.invalid(field,"BLANK");
        if(count>maximum) throw DomainException.invalid(field,"OUT_OF_RANGE");
        return result;
    }
}
