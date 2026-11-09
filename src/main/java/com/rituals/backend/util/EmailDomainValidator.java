package com.rituals.backend.util;

import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Pattern;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class EmailDomainValidator {

    private static final Pattern EMAIL_SYNTAX_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    // Common disposable/temporary throwaway email domains
    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            "mailinator.com", "tempmail.com", "10minutemail.com", "guerrillamail.com",
            "throwawaymail.com", "yopmail.com", "trashmail.com", "sharklasers.com",
            "dispostable.com", "fakeinbox.com", "getairmail.com", "maildrop.cc",
            "temp-mail.org", "inboxkitten.com", "burnermail.io", "mytemp.email"
    );

    public static boolean isRealEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        var matcher = EMAIL_SYNTAX_PATTERN.matcher(email.trim());
        if (!matcher.matches()) {
            return false;
        }

        String domain = matcher.group(1).toLowerCase();

        // Check if disposable
        if (DISPOSABLE_DOMAINS.contains(domain)) {
            return false;
        }

        return hasMxOrARecord(domain);
    }

    private static boolean hasMxOrARecord(String domain) {
        if (domain.endsWith(".test") || domain.endsWith(".local") || domain.equals("localhost")) {
            return true;
        }

        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "2500");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            DirContext ctx = new InitialDirContext(env);

            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            Attribute attr = attrs.get("MX");
            if (attr != null && attr.size() > 0) {
                return true;
            }

            attrs = ctx.getAttributes(domain, new String[]{"A"});
            attr = attrs.get("A");
            return attr != null && attr.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}

