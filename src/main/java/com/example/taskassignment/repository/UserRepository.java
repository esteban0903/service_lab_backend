package com.example.taskassignment.repository;

import com.example.taskassignment.domain.Role;
import com.example.taskassignment.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByWorkerId(Long workerId);

    @Query("SELECT u FROM User u WHERE u.worker.id IN :workerIds")
    java.util.List<User> findByWorkerIdIn(@org.springframework.data.repository.query.Param("workerIds") java.util.Collection<Long> workerIds);

    java.util.List<User> findByRole(Role role);

    @Modifying
    @Query("DELETE FROM User u WHERE u.role = :role")
    void deleteByRole(Role role);
}

