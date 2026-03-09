package golf.security;

import golf.user.GolfUser;
import golf.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GolfUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.toLowerCase().trim();

        if (loginAttemptService.isLocked(normalizedEmail)) {
            throw new LockedException("Too many failed login attempts. Please try again later.");
        }

        GolfUser user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        return User.builder()
                .username(user.email())
                .password(user.password())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.role())))
                .accountLocked(user.accountLocked())
                .build();
    }

}
