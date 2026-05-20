package com.projectwork.Smart.Parking.System.security;

import com.projectwork.Smart.Parking.System.repository.UserRepository;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailAndDeletedAtIsNull(email.trim().toLowerCase())
                .map(user -> {
                    if (user.isBanned()) {
                        throw new UsernameNotFoundException("User is banned");
                    }

                    if (user.getRole() == UserRole.VENDOR && !user.isApproved()) {
                        throw new UsernameNotFoundException("Vendor is not approved");
                    }

                    return org.springframework.security.core.userdetails.User
                            .withUsername(user.getEmail())
                            .password(user.getPassword())
                            .roles(user.getRole().name())
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
