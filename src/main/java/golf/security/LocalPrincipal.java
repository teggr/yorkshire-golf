package golf.security;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record LocalPrincipal(Long userId) implements Principal, UserDetails, Serializable {
    @Override
    public String getName() {
        return Long.toString(userId);
    }
    @Override public String getUsername() { return getName(); }
    @Override public String getPassword() { return null; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
}
