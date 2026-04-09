package com.udtracker.api.repository;

import com.udtracker.api.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);
    // Використовуємо JOIN FETCH, щоб завантажити gameAccounts одним запитом і уникнути LazyInitException
    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.gameAccounts")
    List<AppUser> findAllWithGameAccounts();
    boolean existsByEmail(String email);
}